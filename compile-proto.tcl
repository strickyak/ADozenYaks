set package [lindex $argv 0]

proc k {a args} { return $a }

proc shift varName {
  upvar 1 $varName var
  k [lindex $var 0] [set var [lrange $var 1 end]]  
}

# Parse input and record info.
while {[gets stdin buf] >= 0} {
  set line [string trim $buf]    ;# Trim.
  regsub {[#].*$} $line "" line  ;# Delete comments
  if { $line eq "" } continue

  set cmd [shift line]
  if { $cmd eq "define" } {
    set cls [shift line]
	set Classes($cls) 1
    set cmd2 [shift line]
    if { $cmd2 eq "extends" } {
      set cls2 [shift line]
	  set Extends($cls) $cls2
    }
  } else {
	set repeat 0
    set type $cmd
	if { $type eq "repeated" } {
	  set repeat 1
      set type [shift line]
	}
	set field [shift line]
	set assign [shift line]
	set number [shift line]
	if { $assign ne "=" } {
	  error "Expected '=' in line: $buf  :: c $cmd r $repeat f $field a $assign n $number "
	}
	if ![string is integer $number] {
	  error "Expected integer in line: $buf"
	}
	lappend Fields($cls) $field
	set Type($cls,$field) $type
	set Repeat($cls,$field) $repeat
	set Number($cls,$field) $number
  }
}

# Extend inherited classes.
foreach cls [array names Classes] {
  set c $cls
  while {[info exists Extends($c)]} {
    set e $Extends($c)
	foreach f $Fields($e) {
	  lappend Fields($c) $f
	  set Type($c,$f) $Type($e,$f)
	  set Repeat($c,$f) $Repeat($e,$f)
	  set Number($c,$f) $Number($e,$f)
	}
	set c $e
  }
}

puts "package $package;

import java.util.ArrayList;
import yak.etc.Bytes;

public class Proto {
"

# Define the classes.
foreach cls [lsort [array names Classes]] {
  if [info exists Extends($cls)] {
    puts "  public static class $cls extends $Extends($cls) {" ;#"}"
  } else {
    puts "  public static class $cls {" ;#"}"
  }
  foreach f [lsort $Fields($cls)] {
    set t $Type($cls,$f)
    if $Repeat($cls,$f) {
	  puts "    ArrayList<$t> $f = new ArrayList<$t>();"
    } else {
	  puts "    $t $f;"
	}
  }
  # "{"
  puts "  }"
}

proc CmpFieldNumbers {cls f1 f2} {
  expr { $::Number($cls,$f1) > $::Number($cls,$f2) }
 }

# Define the Pickle functions.
foreach cls [lsort [array names Classes]] {
  puts "  public static void Pickle$cls ($cls p, Bytes b) {"
  foreach f [lsort -command "CmpFieldNumbers $cls" $Fields($cls)] {
    set t $Type($cls,$f)
    if $Repeat($cls,$f) {
	  # Repeated
	  puts "      for (int i = 0; i < p.$f.size(); i++) {"
      switch $t {
	    int { 
		  puts "    b.appendProtoInt ($Number($cls,$f), p.$f.get(i));"
	    }
	    String { 
		  puts "    b.appendProtoString ($Number($cls,$f), p.$f.get(i));"
	    }
	    default { 
		  puts "    if (p.$f.get(i) != null) {
      Bytes b2 = new Bytes();
      Pickle$t (p.$f.get(i), b2);
	  b.appendProtoBytes ($Number($cls,$f), b2);
    }"
	    }
	  }
	  puts "      } // next i"
	} else {
	  # Not repeated
      switch $t {
	    int { 
		  puts "    b.appendProtoInt ($Number($cls,$f), p.$f);"
	    }
	    String { 
		  puts "    if (p.$f != null) b.appendProtoString ($Number($cls,$f), p.$f);"
	    }
	    default { 
		  puts "    if (p.$f != null) {
      Bytes b2 = new Bytes();
      Pickle$t (p.$f, b2);
	  b.appendProtoBytes ($Number($cls,$f), b2);
}"
	    }
	  }
	}
  }
  puts "  }"
}


# Define the Unpickle functions.
foreach cls [lsort [array names Classes]] {
  puts "public static $cls Unpickle$cls (Bytes b) {"
  puts "  $cls z = new $cls ();"
  puts "  while (b.len > 0) {"
  puts "    int code = b.popVarInt();"
  puts "    switch (code) {"
  foreach f [lsort $Fields($cls)] {
    set t $Type($cls,$f)
    if $Repeat($cls,$f) {
	  # Repeated
      switch $t {
	    int { 
		  puts "      case ($Number($cls,$f) << 3) | 0: { z.$f.add(b.popVarInt()); }"
	    }
	    String { 
		  puts "      case ($Number($cls,$f) << 3) | 2: { z.$f.add(b.popVarString()); }"
	    }
	    default { 
		  puts "      case ($Number($cls,$f) << 3) | 2: {"
          puts "         Bytes b2 = b.popVarBytes();"
          puts "         $t p2 = Unpickle$t (b2);"
	      puts "         z.$f.add(p2);"
		  puts "      } // end case"
	    }
	  }
	} else {
	  # Not repeated
      switch $t {
	    int { 
		  puts "      case ($Number($cls,$f) << 3) | 0: { z.$f = b.popVarInt(); }"
	    }
	    String { 
		  puts "      case ($Number($cls,$f) << 3) | 2: { z.$f = b.popVarString(); }"
	    }
	    default { 
		  puts "      case ($Number($cls,$f) << 3) | 2: {"
          puts "         Bytes b2 = b.popVarBytes();"
          puts "         $t p2 = Unpickle$t (b2);"
	      puts "         z.$f = p2;"
		  puts "      } // end case"
	    }
	  } ;# end switch
	} ;# end if
  } ;# end foreach f
  puts "    }  // end switch"
  puts "  }  // end while"
  puts "  return z;"
  puts "}  // end Unpickle $cls"
}



puts "}"
