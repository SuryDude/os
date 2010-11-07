// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os2;

import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class HPRNScheduler extends FCFSScheduler {
  public HPRNScheduler(Reader inputReader, Reader randomNumberReader,
                       boolean verbose, boolean showRandom) {
    super(inputReader, randomNumberReader, verbose, showRandom);
  }

  public double getPenalty(Process p) {
    double penalty = 1.0;
    double T = (double) p.getTotalCpuTime();
    double t = (double) p.getTurnaroundTime();
    if (t > 0.0) {
      // We flip this because our sort is ascending, not descending
      penalty = T / t;
    }
    return penalty;
  }

  private class ProcessHPRNReadyComparator implements Comparator<Process> {
    public int compare(Process p1, Process p2) {
      if (getPenalty(p1) > getPenalty(p2)) {
        return 1;
      } else if (getPenalty(p1) == getPenalty(p2)) {
        return (new ProcessArrivalComparator()).compare(p1, p2);
      } else {
        return -1;
      }
    }
  }

  protected Comparator<Process> getProcessReadyComparator() {
    return new ProcessHPRNReadyComparator();
  }

}
