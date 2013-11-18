#!/bin/sh
dirs="cannon chinesecheckers lostcities mcts pentalath amazons Experiments breakthrough"
class=`shift`

CP="."
for d in $dirs
do
  CP="$CP:build/$d"
done

java -cp $CP $class $@

# Classes with main 
#
#   amazons.gui.Amazons
#   cannon.gui.CannonGui
#   chinesecheckers.gui.CCGui
#   pentalath.gui.PentalathGui
#   lostcities.Game
#   experiments.AITests
# 


