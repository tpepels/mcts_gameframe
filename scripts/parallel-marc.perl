#!/usr/bin/perl
use Carp;
use strict;
use IPC::Open3;
use Time::HiRes qw/ time sleep /;
use vars qw( $debug );
$debug = 1;

if (not defined($ARGV[4])) { 
  print "Usage: parallel.perl <scratch dir> <timelimit (ms)> <game> <CPUs> <games per match>\n";
  exit;
}

my $scratchdir = $ARGV[0];
my $tl = $ARGV[1];
my $game = $ARGV[2];
my $CPUS = $ARGV[3];
my $gamespermatch = $ARGV[4];

if (not -d "$scratchdir") { 
  print "Need a subdirectory called scratch. Running from base directory recommended.\n";
  exit;
}

# for estimating time remaining
my $ttljobs = 0;
my $curjob = 0;
my $starttime = 0;

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
      my $nowtime = time;
      my $jobspersec = $curjob / ($nowtime - $starttime);
      my $esttimeremaining = ($ttljobs - $curjob) / $jobspersec;
      print "Launching '$job->[0]'\n" if $debug;
      my $ptr = prettytime($esttimeremaining);
      print "  job: #$curjob / $ttljobs (~$ptr seconds remaining)\n" if $debug;
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
      print "Reaped '$running{$proc_id}->[0]'\n" if $debug;
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

  my $cmd = "scripts/run.sh experiments.SimGame --game $game --p1 $alg1 --p2 $alg2 --seed $seed --timelimit $tl --printboard";
  
  return $cmd;
}

my @jobs = (); 

my @matchups = (); 


#push(@matchups, "mcts_h_ege0.1_efv0,mcts_h_pd20_efv0");
#push(@matchups, "mcts_h_ege0.1_efv0,mcts_h_pd4_efv0");
#push(@matchups, "mcts_h_ege0.1_det0.5_efv0,mcts_h_ege0.1_efv0");
#push(@matchups, "mcts_h_ege0.1_det0.5_efv0,mcts_h_pd20_det0.5_efv0");

#push(@matchups, "mcts_h,mcts");
#push(@matchups, "mcts_h_im0.4,mcts_h");
#push(@matchups, "mcts_h_pd20_efv0_im0.4,mcts_h_pd20_efv0");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_im0.4,mcts_h_ege0.1_efv0_det0.5_pb0.4");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_im0.4,mcts_h_ege0.1_efv0_det0.5_pbd");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5,mcts_h_ege0.1_efv0_det0.5_np10_im0.6");

#push(@matchups, "mcts_h_ege0.1_det0.5_efv0,mcts_h_pd20_efv0");

# here!! (will take 22 hours :()
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_im0.4,mcts_h_pd20_np10_im0.4");

#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_mbp10000,mcts_h_ege0.1_efv0_det0.5_im0.4");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_mbp50000,mcts_h_ege0.1_efv0_det0.5_im0.4");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_mbp20000,mcts_h_ege0.1_efv0_det0.5_im0.4");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_mbp2000,mcts_h_ege0.1_efv0_det0.5_im0.4");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_mbp1000,mcts_h_ege0.1_efv0_det0.5_im0.4");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_mbp500,mcts_h_ege0.1_efv0_det0.5_im0.4");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_mbp100,mcts_h_ege0.1_efv0_det0.5_im0.4");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_mbp50,mcts_h_ege0.1_efv0_det0.5_im0.4");
#push(@matchups, "mcts_h_ege0.1_efv0_det0.5_mbp10,mcts_h_ege0.1_efv0_det0.5_im0.4");

push(@matchups, "mcts_h_s_ege0.1_det0.5,mcts_h_s");


print "queuing jobs... \n";

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
    my $runname = "$alg1-$alg2-$run";

    my $cmd = get_cmd($alg1, $alg2, $seed); 
    my $fullcmd = "$cmd >$scratchdir/$runname.log 2>&1";
    
    push(@jobs, $fullcmd); 

    # now swap seats
    $alg1 = $algorithms[1];
    $alg2 = $algorithms[0];
    $runname = "$alg1-$alg2-$run";
    $cmd = get_cmd($alg1, $alg2, $seed); 
    $fullcmd = "$cmd >$scratchdir/$runname.log 2>&1";
    
    push(@jobs, $fullcmd); 
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




