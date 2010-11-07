// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os2;

import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class RRScheduler extends FCFSScheduler {
  public static final int QUANTUM = 2;

  public RRScheduler(Reader inputReader, Reader randomNumberReader,
                     boolean verbose, boolean showRandom) {
    super(inputReader, randomNumberReader, verbose, showRandom);
  }

  public void handleReady() {
    for (Process p : getReady()) {
      if (getRunning().size() == 0) {
        p.setTimer(QUANTUM);
        if (p.getCpuBurstRemaining() == 0) {
          p.setCpuBurstRemaining(getBurst(p.getBurst(), p.getCpu()));
          appendShowRandomCpuString();
        }
        p.run();
      } else {
        p.processWait();
      }
      processes.set(p.getId(), p);
    }
  }

  public void handleRunning() {
    super.handleRunning();
    for (Process p : getRunning()) {
      if (p.getTimer() > 0) {
        p.setTimer(p.getTimer() - 1);
      }
      if (p.getTimer() == 0) {
        p.ready(cycle);
      }

      processes.set(p.getId(), p);
    }
  }
}
