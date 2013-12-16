#!/bin/sh
dirs="cannon checkers chinesecheckers lostcities mcts pentalath amazons experiments breakthrough"

for d in $dirs
do
  mkdir -p build/$d
  cp -r $d/src/* build/$d
done

files=`find build -name *.java`
javac -cp build $files

