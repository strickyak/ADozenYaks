case $# in
  1) : ok ;;
  *) echo "Needs 1 arg for the MagicWord" >&2; exit 13;;
esac

cd /home/strick/ADozenYaks

exec java -ea -classpath $PWD/bin/classes  yak.server.AppServer "$1"
# exec java -ea -classpath $PWD/bin/classes  yak.server.StoreServer "$1"
