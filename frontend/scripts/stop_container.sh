#!/bin/bash

if [ -z "$1" ]; then
  echo "usage: $0 <container_id>"
  exit
fi

for container_id in $(docker ps -a --filter="name=$1" -q);do
  docker rm -f $container_id
done
