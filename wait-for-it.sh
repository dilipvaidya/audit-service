#!/usr/bin/env bash
# usage: wait-for-it.sh host:port -- command args

hostport="$1"
shift

host=$(echo $hostport | cut -d: -f1)
port=$(echo $hostport | cut -d: -f2)

until nc -z $host $port; do
  echo "Waiting for $host:$port..."
  sleep 2
done

exec "$@"
