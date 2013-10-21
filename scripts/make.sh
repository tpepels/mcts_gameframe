#!/bin/sh
dirs="cannon chinesecheckers lostcities mcts pentalath"

for d in $dirs
do
  cp -r $d/src build/$d
done

files=`find build -name *.java`
javac -cp build $files

