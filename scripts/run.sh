#!/bin/sh
dirs="cannon chinesecheckers lostcities mcts pentalath"

CP="."
for d in $dirs
do
  CP="$CP:build/$d"
done

java -cp $CP $1



