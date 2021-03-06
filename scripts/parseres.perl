#!/usr/bin/perl

use strict;
use warnings;

if (not defined($ARGV[0])) { 
  print "Usage: parseres.perl <scratch dir>\n";
  exit;
}

my $scratchdir = $ARGV[0];

sub winstats
{
  # compute win rates and confidence intervals. correctly account for draws
  my $lwins = shift;
  my $rwins = shift;
  my $total = shift;

  my $statslistref = shift; # must be a reference to a list

  my $ties = ($total - $lwins - $rwins);

  # count wins as 1, draws as 0.5, and losses as 0
  my $mean = 0;
  if ($total > 0) {
    $mean = ($lwins + 0.5*$ties) / $total;
  }
  my $var = 0;

  # wins
  for (my $i = 0; $i < $lwins; $i++)
  { $var += (1.0 - $mean)*(1.0 - $mean); }

  # draws
  for (my $i = 0; $i < $ties; $i++)
  { $var += (0.5 - $mean)*(0.5 - $mean); }

  # losses
  for (my $i = 0; $i < $rwins; $i++)
  { $var += (0.0 - $mean)*(0.0 - $mean); }

  my $stddev = 0; 
  my $ci95 = 0; 
  my $lrate = 0;
  my $rrate = 0;

  if ($total > 0) { 
    $var = $var / $total;
    $stddev = sqrt($var);
    $ci95 = 1.96*$stddev / sqrt($total);

    # does this make sense..? when there are a lot of ties, not really 
    $lrate = ($lwins + 0.5*$ties) / $total;
    $rrate = ($rwins + 0.5*$ties) / $total;
  }

  my $lperc = $lrate*100.0;
  my $rperc = $rrate*100.0;
  my $ci95perc = $ci95*100.0;

  my $line = sprintf("%.2f %.2f +/- %.2f", $lperc, $rperc, $ci95perc);

  push(@$statslistref, $lrate);
  push(@$statslistref, $rrate);
  push(@$statslistref, $ci95);
  push(@$statslistref, $line);
}


# matchup -> number of games
my %matchmap = (); 

opendir(DIR, "$scratchdir");
my @FILES= readdir(DIR);
foreach my $file (@FILES) 
{
  if ($file =~ /\.log$/) { 
    #print "$file\n"; 
    $file =~ s/\.log$//;
    my @parts = split('-', $file); 
    if ($parts[0] gt $parts[1]) { 
      my $tmp = $parts[1];
      $parts[1] = $parts[0];
      $parts[0] = $tmp;
    }
    my $match = $parts[0] . "," . $parts[1];
    if (not defined $matchmap{$match}) { $matchmap{$match} = 0; }
    if ($matchmap{$match} < $parts[2]) { 
      $matchmap{$match} = $parts[2]; 
    }
    #print "$match\n";
  }
}
closedir(DIR);

my %totalpoints = ();
my %totalgames = ();
my $discards = 0;

foreach my $match (keys %matchmap) { 

  my $gamespermatch = $matchmap{$match};
  my @players = split(",", $match); 

  my $p1 = $players[0]; 
  my $p2 = $players[1];

  my %wins = (); 
  $wins{$p1} = 0;
  $wins{$p2} = 0;
  my $ties = 0;

  #p1 as first player, p2 as second player
  for (my $m = 1; $m <= $gamespermatch; $m++) { 
  
    my $runname = "$p1-$p2-$m";
    #print "opening $scratchdir/$runname.log\n";

    my $doesnotexist = 0; 
    open(FILE, '<', "$scratchdir/$runname.log") or $doesnotexist = 1;
    if ($doesnotexist == 0) {
      #print "reading lines...\n";
      while (my $line = <FILE>) {
        chomp($line);
        # example line is "Game over. Winner is 1"
        if ($line =~ /^Game over/) { 
          #print "$runname $line\n"; 
          my @parts = split(' ', $line); 
          my $winner = $parts[4];
          if ($winner == 1) { 
            $wins{$p1} += 1; 
            $totalpoints{$p1} += 2;
          }
          elsif ($winner == 2) { 
            $wins{$p2} += 1; 
            $totalpoints{$p2} += 2;
          }
          #elsif ($winner eq "DISCARDED") { 
          #  $discards++;
          #}
          else {
            $totalpoints{$p1} += 1;
            $totalpoints{$p2} += 1;
            $ties += 1; 
          }

          $totalgames{$p1} += 1;
          $totalgames{$p2} += 1;

          last;
        }
      }
      close(FILE); 
    }
  }

  #p2 as first player, p1 as second player
  for (my $m = 1; $m <= $gamespermatch; $m++) { 
  
    my $runname = "$p2-$p1-$m";

    my $doesnotexist = 0; 
    open(FILE, '<', "$scratchdir/$runname.log") or $doesnotexist = 1;
    if ($doesnotexist == 0) {
      while (my $line = <FILE>) {
        chomp($line);
        if ($line =~ /^Game over/) { 
          # example line is "Game over. Winner is 1"
          my @parts = split(' ', $line); 
          my $winner = $parts[4];
          if ($winner == 1) { 
            $wins{$p2} += 1; 
            $totalpoints{$p2} += 2;
          }
          elsif ($winner == 2) { 
            $wins{$p1} += 1; 
            $totalpoints{$p1} += 2;
          }
          #elsif ($winner eq "DISCARDED") { 
          #  $discards++;
          #}
          else {
            $totalpoints{$p1} += 1;
            $totalpoints{$p2} += 1;
            $ties += 1;
          }


          last;
        }
      }
      close(FILE); 
    }
  }

  #print "matchup summary: $p1-$p2 " . $wins{$p1} . " " . $wins{$p2} . " " . $ties;
  my $diff = ($wins{$p1} - $wins{$p2});
  my $games = ($wins{$p1} + $wins{$p2} + $ties);
  #print "  (diff $diff, games $games)  ";

  # statsline

  my @statslist = ();
  my $left = $p1;
  my $right = $p2;

  my $total = $wins{$left} + $wins{$right} + $ties;
  #my $total = $wins{$left} + $wins{$right};
  
  winstats($wins{$left}, $wins{$right}, $total, \@statslist);
  
  # just for the kalah exps
  #winstats($wins{$left}, $wins{$right}, $wins{$left}+$wins{$right}, \@statslist);

  my $statsline = $statslist[3];

  my $lperc = $statslist[0]*100.0;
  my $rperc = $statslist[1]*100.0;
  my $ci95perc = $statslist[2]*100.0;

  #print "$left " . $wins{$left} . ", $right " . $wins{$right} . ", ties = $ties, total = $total. $statsline\n"; 
  my $lwinscount = sprintf("(%d)", $wins{$left});
  my $rwinscount = sprintf("(%d)", $wins{$right});
  printf("%40s vs. %40s: %5d %5d %5d (diff %5d, games %5d) %3.2lf %3.2lf +/- %3.2lf\n", 
    $p1, $p2, $wins{$p1}, $wins{$p2}, $ties, $diff, $games, $lperc, $rperc, $ci95perc);
}

#print "discards = $discards\n";

# enable this if we want later
#foreach my $key (sort {$totalpoints{$b} <=> $totalpoints{$a}} keys %totalpoints) {
#  print "total points for $key = " . $totalpoints{$key} . "\n";
#}

