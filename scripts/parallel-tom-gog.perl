#!/usr/bin/perl
use Carp;
use strict;
use IPC::Open3;

# change this as needed
use lib '/home/mlanctot/lib64/perl5/';

use Time::HiRes qw/ time sleep /;
use vars qw( $debug );
$debug = 1;

if (not defined($ARGV[3])) { 
  print "Usage: parallel.perl <scratch dir> <timelimit (ms)> <CPUs> <games per match>\n";
  exit;
}

my $scratchdir = $ARGV[0];
my $tl = $ARGV[1];
my $game = "";
my $CPUS = $ARGV[2];
my $gamespermatch = $ARGV[3];

if (not -d "$scratchdir") { 
  print "Need a subdirectory called scratch. Running from base directory recommended.\n";
  exit;
}

# for estimating time remaining
my $ttljobs = 0;
my $curjob = 0;
my $starttime = 0;
my $donejobs = 0;
my $ptr = "?"; # pretty time remaining

# returns a nicely displayed time
sub prettytime
{
  my $seconds = shift;

  my $hours = int($seconds / 3600.0);
  my $remainder = ($seconds - $hours*3600);

  my $minutes = int($remainder / 60.0);
  $remainder = ($remainder - $minutes*60);

  if ($hours == 0 and $minutes == 0) {
    return sprintf("00:%02d", int($seconds));
  }
  elsif ($hours == 0) {
    return sprintf("%02d:%02d", $minutes, int($remainder));
  }
  else {
    return sprintf("%02d:%02d:%02d", $hours, $minutes, int($remainder));
  }
}

# The first parameter is how many jobs to run at once, the remaining are
# the jobs.  Jobs may be a string, or an anonymous array of the cmd and
# args.
#
# All output from children go to your STDERR and STDOUT.  They get no
# input.  It prints fairly uninformative errors for commands with
# problems, and returns a hash of problems.
#
# The jobs SHOULD NOT depend on each other!
sub run_parallel {
  my $job_count = shift;
  unless (0 < $job_count) {
    confess("run_parallel called without a positive parallel job count!");
  }
  my @to_start = @_;
  my %running;
  my %errors;
  my $is_running = 0;
  while (@to_start or %running) {
    if (@to_start and ($is_running < $job_count)) {
      # Launch a job
      my $job = shift @to_start;
      unless (ref($job)) {
        $job = [$job];
      }
      $curjob++;
      print "Launching '$job->[0]'\n" if $debug;
      print "  job: #$curjob / $ttljobs (~$ptr remaining)\n" if $debug;
      local *NULL;
      my $null_file = ($^O =~ /Win/) ? 'nul': '/dev/null';   
      open (NULL, $null_file) or confess("Cannot read from $null_file:$!");
      my $proc_id = open3("<&NULL", ">&STDOUT", ">&STDERR", @$job);
      $running{$proc_id} = $job;
      ++$is_running;
    }
    else {
      # collect a job
      my $proc_id = wait();
      if (! exists $running{$proc_id}) {
        confess("Reaped unknown process $proc_id!");
      }
      elsif ($?) {
        # Oops
        my $job = $running{$proc_id};
        my ($cmd, @args) = @$job;
        my $err = "Running '$cmd' gave return code '$?'";
        if (@args) {
          $err .= join "\n\t", "\nAdditional args:", @args;
        }
        print STDERR $err, "\n";
        $errors{$proc_id} = $err;
      }

      #reaped!
      $donejobs++; 
      my $nowtime = time;
      my $jobspersec = $donejobs / ($nowtime - $starttime);
      my $esttimeremaining = ($ttljobs - $donejobs) / $jobspersec;
      $ptr = prettytime($esttimeremaining);
      
      print "Reaped '$running{$proc_id}->[0] (~$ptr remaining)'\n" if $debug;

      #Jobs submitted: 1234
      #Jobs done: 1175
      #Time remaining: 32:55:06
      #Last updated: Fri Jun  6 17:58:38 CEST 2014
      #Last updated: Fri Jun  6 18:01:29 CEST 2014

      open (INFOFILE, '>', "infofile.txt");
      print INFOFILE "Jobs submitted $curjob\n";
      print INFOFILE "Jobs done: $donejobs\n";
      print INFOFILE "Time remaining: $ptr\n";
      close(INFOFILE);


      delete $running{$proc_id};
      --$is_running;
    }
  }
  return %errors;
}

sub get_cmd
{
  my $alg1 = shift;
  my $alg2 = shift;
  my $seed = shift;

  my $cmd = "scripts/run.sh experiments.SimGame --game $game --p1 $alg1 --p2 $alg2 --seed $seed --timelimit $tl";
  
  return $cmd;
}

my @jobs = (); 

# here's an example of a loop to initialize matchups instead of a static list
#my @matchups = (); 
#my @parms = ( 0.1, 0.05, 0.2, 0.3, 0.4, 0.5, 0.6, 0.25, 0.7, 0.8 ); 
#for (my $i = 0; $i < scalar(@parms); $i++) { 
#  my $parm = $parms[$i];
#  #my $tag = "mcts_h_pd$pd,mcts_h_pd${pd}_im"; 
#  my $tag = "mcts_h_ege$parm,mcts_h_ege${parm}_im"; 
#  push(@matchups, $tag); 
#}

# this is a list of matchups 
# a matchup is a string of "playertype1,playertype2"
# you can also use a loop to fill this with different player types
my @matchups = (
#	"mcts_sl,mctstt_sl_uct0.2",
#	"mcts_sl,mctstt_sl_uct0.4",
#	"mcts_sl,mctstt_sl_uct0.6",
#	"mcts_sl,mctstt_sl_uct0.8",
#	"mcts_sl,mctstt_sl_uct1.0",
#	"mcts_sl,mctstt_sl_uct1.2",
#	"mctstt_sl_h,srmctstt_shot_sl_h",
#	"mctstt_sl_s_h,srmctstt_shot_sl_s_h"
#	"mctstt_h_s_sl,mctstt_sl_h_s_ph1",
#	"mctstt_h_s_sl,mctstt_sl_h_s_ph5",
#	"mctstt_h_s_sl,mctstt_sl_h_s_ph10",
#	"mctstt_h_s_sl,mctstt_sl_h_s_ph15",
#	"mctstt_h_s_sl,mctstt_sl_h_s_ph20",
#	"mctstt_h_s_sl,mctstt_sl_h_s_ph30",
#	"mctstt_h_s_sl,mctstt_sl_h_s_ph50"
#	"mcts_sl,srmcts_shot_sl",
#	"mcts_sl_s_h,srmcts_shot_sl_s_h",
#	"mcts_sl_h,srmcts_shot_sl_h",
#	"mcts_sl_s,srmcts_shot_sl_s"
#
#   "mctstt_sl,srmctstt_sl_shot"

# "srmctstt_sl_s_shot,srmctstt_sl_shot",
# "mctstt_sl_s,mctstt_sl",	
# "srmctstt_sl_s,srmctstt_sl"
	"mctstt_sl,srmctstt_sl_dcn0.05",
 	"mctstt_sl,srmctstt_sl_dc0.05",
 	"mctstt_sl,srmctstt_sl_dcn0.1",
 	"mctstt_sl,srmctstt_sl_dc0.1",
 	"mctstt_sl,srmctstt_sl_dcn0.15",
 	"mctstt_sl,srmctstt_sl_dc0.15",
 	"mctstt_sl,srmctstt_sl_dcn0.2",
 	"mctstt_sl,srmctstt_sl_dc0.2"
#  "mctstt_sl,srmctstt_sl_bl20",
#  "mctstt_sl,srmctstt_sl_bl10",
#  "mctstt_sl,srmctstt_sl_bl40",
#  "mctstt_sl,srmctstt_sl_bl80",
#  "mctstt_sl,srmctstt_sl_bl60",
# 	"mctstt_sl,srmctstt_sl_bl30_uct0.2",
# 	"mctstt_sl,srmctstt_sl_bl30_uct0.4",
# 	"mctstt_sl,srmctstt_sl_bl30_uct0.6",
#	"mctstt_sl,srmctstt_sl_bl30_uct0.8",
# 	"mctstt_sl,srmctstt_sl_bl30_uct1.0",
#	"mctstt_sl,srmctstt_sl_bl40_uct0.2",
# 	"mctstt_sl,srmctstt_sl_bl40_uct0.4",
# 	"mctstt_sl,srmctstt_sl_bl40_uct0.6",
# 	"mctstt_sl,srmctstt_sl_bl40_uct0.8",
# 	"mctstt_sl,srmctstt_sl_bl40_uct1.0",
# 	"mctstt_sl,srmctstt_sl_bl50_uct0.2",
# 	"mctstt_sl,srmctstt_sl_bl50_uct0.4",
# 	"mctstt_sl,srmctstt_sl_bl50_uct0.6",
# 	"mctstt_sl,srmctstt_sl_bl50_uct0.8",
# 	"mctstt_sl,srmctstt_sl_bl50_uct1.0"
);

my @games = (
#	"lostcities",
#	"cannon",
#	"checkers",	
	"breakthrough",
	"pentalath",	
#	"chinesecheckers",
 	"amazons",
	"nogo9"
#	"nogo19"
);

print "queuing jobs... \n";


for(my $j = 0; $j < scalar(@games); $j++) {
  $game = $games[$j];
  for (my $i = 0; $i < scalar(@matchups); $i++)
  {
    my @algorithms = split(',', $matchups[$i]);

    for (my $run = 1; $run <= $gamespermatch; $run++)
    {
      # a1 as player1 a2 as player 2
      my $seed = int(rand(100000000)) + 1;
      chomp($seed);

      my $alg1 = $algorithms[0];
      my $alg2 = $algorithms[1];
      my $runname = "$game-$alg1-$alg2-$run";

      my $cmd = get_cmd($alg1, $alg2, $seed); 
      my $fullcmd = "$cmd >$scratchdir/$runname.log 2>&1";
      
      #print "queueing $fullcmd\n";
      push(@jobs, $fullcmd); 

      # now swap seats
      $alg1 = $algorithms[1];
      $alg2 = $algorithms[0];
      $runname = "$game-$alg1-$alg2-$run";
      $cmd = get_cmd($alg1, $alg2, $seed); 
      $fullcmd = "$cmd >$scratchdir/$runname.log 2>&1";
      
      #print "queueing $fullcmd\n";
      push(@jobs, $fullcmd); 
    }
  }
}

sub fisher_yates_shuffle
{
    my $array = shift;
    my $i = scalar(@$array);
    while ( --$i )
    {
        my $j = int rand( $i+1 );
        @$array[$i,$j] = @$array[$j,$i];
    }
}

fisher_yates_shuffle( \@jobs );

print "queued " . scalar(@jobs) . " jobs\n";
sleep 1;

$ttljobs = scalar(@jobs);
$starttime = time;

&run_parallel($CPUS, @jobs);
