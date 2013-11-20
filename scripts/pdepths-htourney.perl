#!/usr/bin/perl
use Carp;
use strict;
use Time::HiRes qw/ time sleep /;
use IPC::Open3;
use vars qw( $debug );
$debug = 1;

if (not defined($ARGV[3])) { 
  print "Usage: parallel.perl <scratch dir> <node limit per move> <game> <CPUs> [gamespermatch]\n";
  exit;
}

my $scratchdir = $ARGV[0];
my $tl = $ARGV[1];
my $game = $ARGV[2];
my $CPUS = $ARGV[3];

my $gamespermatch = 100;
if (defined ($ARGV[4])) { 
  $gamespermatch = $ARGV[4];  
}

my $algprefix = "mcts_h";
if (defined ($ARGV[5])) { 
  $algprefix = $ARGV[5];  
}


open (RESFILE, '>', "$scratchdir/results.txt") or die "cannot open results file.\n";

my @jobs = ();
my @players = (); 

my @parms1 = (); 
my @parms2 = ();


# for estimating time remaining
my $ttljobs = 0;
my $curjob = 0;
my $starttime = 0;

if (not -d "$scratchdir") { 
  print "Need a subdirectory called scratch. Running from base directory recommended.\n";
  exit;
}

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
      my $ptm = prettytime($esttimeremaining); 
      print "Launching '$job->[0]'\n" if $debug;
      print "  job: #$curjob / $ttljobs (~$ptm remaining)\n" if $debug;
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

  my $cmd = "scripts/run.sh experiments.SimGame --game $game --p1 $alg1 --p2 $alg2 --seed $seed --timelimit $tl";
  
  return $cmd;
}

sub queuejobs 
{
  for (my $i = 0; $i < scalar(@players); $i++) { 

    my $j = scalar(@players)-$i-1;
    if ($j <= $i) { last; }

    for (my $run = 1; $run <= $gamespermatch; $run++)
    {
      # a1 as player1 a2 as player 2
      my $seed = int(rand(100000000)) + 1;
      chomp($seed);

      my $pi = $players[$i];
      my $pj = $players[scalar(@players) - $i - 1];

      my $runname = "$pi-$pj-$run";

      my $cmd = get_cmd($pi, $pj, $seed); 
      my $fullcmd = "$cmd >$scratchdir/$runname.log 2>&1";
      
      #print "queueing $fullcmd\n";
      push(@jobs, $fullcmd); 

      # now swap seats
      my $tmp = $pi;
      $pi = $pj;
      $pj = $tmp; 
      $runname = "$pi-$pj-$run";
      $cmd = get_cmd($pi, $pj, $seed); 
      $fullcmd = "$cmd >$scratchdir/$runname.log 2>&1";
      
      #print "queueing $fullcmd\n";
      push(@jobs, $fullcmd); 
    }
  }

}

sub determine_winners {

  my @winners = ();

  for (my $i = 0; $i < scalar(@players); $i++) { 
    
    my $j = scalar(@players)-$i-1;

    if ($j < $i) { last; }

    if ($j <= $i) { 
        print "$players[$j] gets a by\n";
        print RESFILE "$players[$j] gets a by\n";
        push(@winners, $players[$j]);
        last;
    }

    my @counts = (0, 0); 

    for (my $run = 1; $run <= $gamespermatch; $run++) { 

      my $pi = $players[$i];
      my $pj = $players[$j];
      my $runname = "$pi-$pj-$run";

      my $res = `cat $scratchdir/$pi-$pj-$run.log | grep "Game over" | awk '{print \$5}'`;
      chomp($res); 

      #print "##$res##\n";

      if ($res < 3) { $counts[$res-1]++; }

      # now swap seats
      my $tmp = $pi;
      $pi = $pj;
      $pj = $tmp; 
      $runname = "$pi-$pj-$run";
      
      my $res = `cat $scratchdir/$pi-$pj-$run.log | grep "Game over" | awk '{print \$5}'`;
      chomp($res); 

      if ($res < 3) { $counts[2-$res]++; }
    }

    my $winner = "";
    my $loser = "";
    my $wcnt = 0;
    my $lcnt = 0;

    if ($counts[0] > $counts[1] || $counts[0] == $counts[1]) { 
      $winner = $players[$i];
      $loser = $players[$j];
      $wcnt = $counts[0];
      $lcnt = $counts[1];
    }
    elsif ($counts[1] > $counts[0]) { 
      $winner = $players[$j];
      $loser = $players[$i];
      $wcnt = $counts[1];
      $lcnt = $counts[0];
    }
    
    print "winner $winner ($wcnt) vs. loser $loser ($lcnt)\n";
    print RESFILE "winner $winner ($wcnt) vs. loser $loser ($lcnt)\n";
    push(@winners, $winner); 
  }

  #sleep 5;
  print RESFILE "\n";

  @players = @winners; 
}

if ("$algprefix" eq "mcts" or "$algprefix" eq "mcts_h") { 
  @parms1 = ( "0", "1", "2", "3", "4", "5", "8", "10", "20", "30", "50", "100", "1000" ); 
}
else { 
  print "Problem. game not eq2.. \n";
  exit;
}


if (scalar(@parms1) == 0) { 
  print "no parms!\n";
  exit;
}

for (my $i = 0; $i < scalar(@parms1); $i++) {
  my $p1 = $parms1[$i];
  push(@players, "$algprefix" . "_pd$p1"); 
}

my $round = 0;

while (scalar(@players) > 1) 
{
  $round++; 
  $curjob = 0;

  print "Starting round $round\n";
  print RESFILE "round $round\n\n";

  @jobs = ();

  queuejobs(); 

  $ttljobs = scalar(@jobs);
  $starttime = time; 
  print "\n";

  &run_parallel($CPUS, @jobs);
  
  print "\n\nROUND $round done. Determining winners...\n";
  determine_winners();
}

print "Winner: " . $players[0] . "\n";
print RESFILE ("Winner: " . $players[0] . "\n"); 

close(RESFILE);





