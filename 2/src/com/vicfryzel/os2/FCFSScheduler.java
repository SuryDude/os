// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os2;

import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class FCFSScheduler extends Scheduler {
  public FCFSScheduler(Reader inputReader, Reader randomNumberReader,
                       boolean verbose, boolean showRandom) {
    super(inputReader, randomNumberReader, verbose, showRandom);
  }

  public void handleBlocked() {
    for (Process p : getBlocked()) {
      if (p.getIoBurstRemaining() > 0) {
        p.addTotalIoTime(1);
        p.setIoBurstRemaining(p.getIoBurstRemaining() - 1);
      }
      if (p.getIoBurstRemaining() == 0) {
        p.ready(cycle);
      }
      processes.set(p.getId(), p);
    }
  }

  public void handleReady() {
    for (Process p : getReady()) {
      if (getRunning().size() == 0) {
        p.run();
        p.setCpuBurstRemaining(getBurst(p.getBurst(), p.getCpu()));
        appendShowRandomCpuString();
      } else {
        p.processWait();
      }
      processes.set(p.getId(), p);
    }
  }

  public void handleArrivals() {
    for (Process p : getArrivals()) {
      if (p.getArrival() == cycle) {
        p.ready(cycle);
        processes.set(p.getId(), p);
      }
    }
  }

  public void handleRunning() {
    for (Process p : getRunning()) {
      if (p.getCpuBurstRemaining() > 0) {
        p.addTotalCpuTime(1);
        p.setCpuBurstRemaining(p.getCpuBurstRemaining() - 1);
      }
      if (p.getCpuBurstRemaining() == 0 && p.getCpu() != p.getTotalCpuTime()) {
        p.block();
        p.setIoBurstRemaining(randomOS(p.getIo()));
        appendShowRandomIoString();
      }
      if (p.getCpu() == p.getTotalCpuTime()) {
        p.terminate(cycle);
      }
      processes.set(p.getId(), p);
    }
  }
}
