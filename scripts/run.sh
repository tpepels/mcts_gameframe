#!/bin/sh
dirs="alphabeta cannon checkers chinesecheckers kalah lostcities mcts pentalath amazons experiments breakthrough"
class=`shift`

CP="."
for d in $dirs
do
  CP="$CP:build/$d"
done

java -XX:+UseSerialGC -cp $CP $class $@

# Classes with main 
#
#   amazons.gui.Amazons
#   cannon.gui.CannonGui
#   chinesecheckers.gui.CCGui
#   pentalath.gui.PentalathGui
#   lostcities.Game
#   experiments.AITests
# 


