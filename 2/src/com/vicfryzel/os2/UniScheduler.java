// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os2;

import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class UniScheduler extends FCFSScheduler {
  public UniScheduler(Reader inputReader, Reader randomNumberReader,
      boolean verbose) {
    super(inputReader, randomNumberReader, verbose);
  }

  public void handleBlocked() {
    for (Process p : getBlocked()) {
      if (p.getIoBurstRemaining() > 0) {
        p.addTotalIoTime(1);
        p.setIoBurstRemaining(p.getIoBurstRemaining() - 1);
      }
      if (p.getIoBurstRemaining() == 0) {
        // Make this process ready before all other processes
        // because we're uni-programmed.
        // -1 is NOT a valid cycle
        p.ready(-1);
      }
      processes.set(p.getId(), p);
    }
  }

  public void handleReady() {
    for (Process p : getReady()) {
      if (getRunning().size() == 0 && getBlocked().size() == 0) {
        p.run();
        p.setCpuBurstRemaining(getBurst(p.getBurst(), p.getCpu()));
      } else {
        p.processWait();
      }
      processes.set(p.getId(), p);
    }
  }
}
