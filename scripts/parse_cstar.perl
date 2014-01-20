#!/usr/bin/perl

use strict; 
use warnings; 


if (not defined($ARGV[0])) { 
  print "Usage: parse_cstar.perl <scratch dir>\n";
  exit;
}

my $scratchdir = $ARGV[0];

opendir(DIR, "$scratchdir");
my @FILES= readdir(DIR);

foreach my $file (@FILES) 
{
  if ($file =~ /\.log$/) { 
    print "$scratchdir/$file\n"; 
    #$file =~ s/\.log$//;
    
    open(FILE, "| tail -n 5 $scratchdir/$file") or die "Cannot open file $scratchdir/$file\n";
    
    my $line = ""; 
    foreach my $line (<FILE>) { 
      chomp($line);
      print "line: " . $line . "\n";
    }

    # ABORT!
  }
  
}


