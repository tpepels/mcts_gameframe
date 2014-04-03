#!/bin/sh
dirs="alphabeta BRUE domineering cannon checkers chinesecheckers kalah lostcities mcts pentalath amazons experiments breakthrough"

for d in $dirs
do
  mkdir -p build/$d
  cp -r $d/src/* build/$d
done

files=`find build -name *.java`
javac -cp build $files

