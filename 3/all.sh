#!/bin/sh

ant jar
for i in 01 02 03 04 05 06 07 08 09 10 11 12 13; do
  echo "===== data/input-$i ====="
  echo
  java -jar build/jar/os3.jar data/input-$i
  echo
done
