#!/bin/sh
dirs="cannon chinesecheckers lostcities mcts pentalath amazons Experiments breakthrough"

CP="."
for d in $dirs
do
  CP="$CP:build/$d"
done

java -cp $CP $1

# Classes with main 
#
#   amazons.gui.Amazons
#   cannon.gui.CannonGui
#   chinesecheckers.gui.CCGui
#   pentalath.gui.PentalathGui
#   lostcities.Game
#   experiments.AITests
# 


