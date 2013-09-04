PORT=${1:-30331}
MAGIC=${2:-magic}

set -x
exec java -ea -classpath $PWD/bin/classes  yak.server.AppServer -p "$PORT" -m "$MAGIC"
