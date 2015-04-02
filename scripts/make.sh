#!/bin/sh
dirs="players domineering cannon checkers chinesecheckers kalah lostcities nogo 
pentalath amazons experiments breakthrough penguin gofish"

for d in $dirs
do
  mkdir -p build/$d
  cp -r $d/src/* build/$d
done

files=`find build -name *.java`
javac -cp build $files

