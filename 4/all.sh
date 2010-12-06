#!/bin/bash

ant jar

function run {
  echo "===== data/run-$1 ====="
  echo
  java -jar build/jar/os4.jar -s $2 -p $3 -o $4 -j $5 -n $6 -a $7 \
      --random-file data/random-numbers
  echo
}

run 01 10 10 20 1 10 lru 0 
run 02 10 10 10 1 100 lru 0
run 03 10 10 10 2 10 lru 0
run 04 20 10 10 2 10 lru 0
run 05 20 10 10 2 10 random 0
run 06 20 10 10 2 10 lifo 0
run 07 20 10 10 3 10 lru 0
run 08 20 10 10 3 10 lifo 0
run 09 20 10 10 4 10 lru 0
run 10 20 10 10 4 10 random 0
run 11 90 10 40 4 100 lru 0
run 12 40 10 90 1 100 lru 0
run 13 40 10 90 1 100 lifo 0
run 14 800 40 400 4 5000 lru 0
run 15 10 5 30 4 3 random 0
