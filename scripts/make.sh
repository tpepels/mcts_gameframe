#!/bin/sh
dirs="alphabeta BRUE domineering cannon checkers chinesecheckers kalah lostcities mcts mcts_tt nogo pentalath amazons experiments breakthrough penguin"

for d in $dirs
do
  mkdir -p build/$d
  cp -r $d/src/* build/$d
done

files=`find build -name *.java`
javac -cp build $files

