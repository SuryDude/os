// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class Scheduler {
  protected Reader inputReader;
  protected Reader randomNumberReader;
  protected StreamTokenizer randomNumberTokenizer;
  protected List<Process> processes;
  protected Logger logger;
  protected boolean verbose;
  protected String verboseOutput;
  protected int cycle;

  /**
   * Create a new Scheduler based on the given Reader for random input.
   *
   * @param inputReader Reader wrapping input stream.
   * @param randomNumberReader Reader wrapping random int stream.
   * @param verbose True if output should be verbose.
   */
  public Scheduler(Reader inputReader, Reader randomNumberReader, boolean verbose) {
    this.inputReader = inputReader;
    this.randomNumberReader = randomNumberReader;
    this.verbose = verbose;
    cycle = 0;

    reset();

    logger = Logger.getLogger("Scheduler");
    if (verbose) {
      logger.setLevel(Level.INFO);
    } else {
      logger.setLevel(Level.WARNING);
    }
    verboseOutput = "";
  }

  protected void reset() {
    try {
      inputReader.reset();
    } catch (IOException e) {}

    processes = Process.read(inputReader);
    Collections.sort(processes, new ProcessArrivalComparator());

    try {
      randomNumberReader.reset();
    } catch (IOException e) {}
    randomNumberTokenizer = new StreamTokenizer(randomNumberReader);
    randomNumberTokenizer.eolIsSignificant(false);
    randomNumberTokenizer.parseNumbers();
  }

  public List<Process> getProcesses() {
    return processes;
  }

  public List<Process> getUnstarted() {
    List<Process> retval = new ArrayList<Process>();
    for (Process p : getProcesses()) {
      if (!p.isStarted()) {
        retval.add(p);
      }
    }
    return retval;
  }

  protected class ProcessArrivalComparator implements Comparator<Process> {
    public int compare(Process p1, Process p2) {
      if (p1.getArrival() > p2.getArrival()) {
        return 1;
      } else if (p1.getArrival() == p2.getArrival()) {
        return 0;
      } else {
        return -1;
      }
    }
  }

  public List<Process> getArrivals() {
    List<Process> retval = new ArrayList<Process>();
    for (Process p : getProcesses()) {
      if (p.getArrival() == cycle) {
        retval.add(p);
      }
    }
    Collections.sort(retval, new ProcessArrivalComparator());
    return retval;
  }

  private class ProcessReadyComparator implements Comparator<Process> {
    public int compare(Process p1, Process p2) {
      if (p1.getReadyTime() > p2.getReadyTime()) {
        return 1;
      } else if (p1.getReadyTime() == p2.getReadyTime()) {
        return (new ProcessArrivalComparator()).compare(p1, p2);
      } else {
        return -1;
      }
    }
  }

  protected Comparator<Process> getProcessReadyComparator() {
    return new ProcessReadyComparator();
  }

  public List<Process> getReady() {
    List<Process> retval = new ArrayList<Process>();
    for (Process p : getProcesses()) {
      if (p.isReady()) {
        retval.add(p);
      }
    }
    Collections.sort(retval, getProcessReadyComparator());
    return retval;
  }

  public List<Process> getBlocked() {
    List<Process> retval = new ArrayList<Process>();
    for (Process p : getProcesses()) {
      if (p.isBlocked()) {
        retval.add(p);
      }
    }
    return retval;
  }

  public List<Process> getRunning() {
    List<Process> retval = new ArrayList<Process>();
    for (Process p : getProcesses()) {
      if (p.isRunning()) {
        retval.add(p);
      }
    }
    return retval;
  }

  public List<Process> getTerminated() {
    List<Process> retval = new ArrayList<Process>();
    for (Process p : getProcesses()) {
      if (p.isTerminated()) {
        retval.add(p);
      }
    }
    return retval;
  }

  protected float getCpuUtilization() {
    float retval = 0;
    for (Process p : getTerminated()) {
      retval += (float) p.getTotalCpuTime();
    }
    retval = retval / (float) cycle;
    return retval;
  }

  protected float getIoUtilization() {
    float retval = 0;
    for (Process p : getTerminated()) {
      retval += (float) p.getTotalIoTime();
    }
    retval = retval / (float) cycle;
    return retval;
  }

  protected float getThroughput() {
    return getTerminated().size() / ((float) cycle / 100);
  }

  protected float getAverageTurnaroundTime() {
    float retval = 0;
    List<Process> terminated = getTerminated();
    for (Process p : terminated) {
      retval += p.getTurnaroundTime();
    }
    retval = retval / (float) terminated.size();
    return retval;
  }

  protected float getAverageWaitTime() {
    float retval = 0;
    List<Process> terminated = getTerminated();
    for (Process p : terminated) {
      retval += p.getTotalWaitTime();
    }
    retval = retval / (float) terminated.size();
    return retval;
  }

  protected void printVerboseOutput() {
    if (verbose) {
      System.out.println(verboseOutput);
    }
  }

  protected void printProcessSummary() {
    for (Process p : getTerminated()) {
      System.out.println("Process " + p.getId() + ":");
      System.out.println("\t(A,B,C,IO) = " + p.toAltString());
      System.out.println("\tFinishing time: " + p.getTerminatedTime());
      System.out.println("\tTurnaround time: " + p.getTurnaroundTime());
      System.out.println("\tI/O time: " + p.getTotalIoTime());
      System.out.println("\tWaiting time: " + p.getTotalWaitTime() + "\n");
    }
  }

  protected void printSummary() {
    System.out.println("Summary Data:");
    System.out.println("\tFinishing time: " + cycle);
    System.out.println("\tCPU Utilization: " + getCpuUtilization());
    System.out.println("\tI/O Utilization: " + getIoUtilization());
    System.out.println("\tThroughput: " + getThroughput()
        + " processes per hundred cycles");
    System.out.println("\tAverage turnaround time: " + getAverageTurnaroundTime());
    System.out.println("\tAverage waiting time: " + getAverageWaitTime());
  }

  protected void printUnsortedProcessList(List<Process> processes) {
    String output = processes.size() + " ";
    for (Process p : processes) {
      output += p.toString() + " ";
    }
    System.out.println("The original input was: " + output);
  }

  protected void printSortedProcessList(List<Process> processes) {
    String output = processes.size() + " ";
    for (Process p : processes) {
      output += p.toString() + " ";
    }
    System.out.println("The (sorted) input is: " + output);
  }

  public void scheduleUntilComplete() {
    printUnsortedProcessList(processes);

    // This line is very important, as we're resetting the process list
    // in order to sort it
    processes = getProcesses();

    printSortedProcessList(processes);
    System.out.print("\n\n");

    // Schedule until all processes are terminated
    while (getTerminated().size() < processes.size()) {
      step();
    }

    printVerboseOutput();
    printProcessSummary();
    printSummary();
  }
 
  public void step() {
    if (verbose) {
      appendVerboseCycleString();
    }

    handleBlocked();
    handleRunning();
    handleArrivals();
    handleReady();
    cycle++;
  }

  public abstract void handleBlocked();
  public abstract void handleRunning();
  public abstract void handleArrivals();
  public abstract void handleReady();

  /**
   * @param u mod param for next random number
   * @return 1 + next random integer of input file % u.
   */
  protected int randomOS(int u) {
    int random = 1;
    try {
      int next = randomNumberTokenizer.nextToken();
      if (next == StreamTokenizer.TT_EOF) {
        logger.warning("Ran out of random integers, resetting to start of file.");
        reset();
        next = randomNumberTokenizer.nextToken();
      }
      if (next == randomNumberTokenizer.TT_NUMBER) {
        int value = (int) randomNumberTokenizer.nval;
        random = 1 + (value % u);
      } else {
        logger.severe("Non-numeric token found in random integer file.");
      }
    } catch (IOException e) {
      logger.severe("Problem fetching next random number: " + e);
    }
    return random;
  }

  protected int getBurst(int u, int max) {
    int retval = randomOS(u);
    if (max < retval) {
      retval = max;
    }
    return retval;
  }

  public void appendVerboseCycleString() {
    verboseOutput += "Before cycle  " + cycle + ":";
    for (Process p : processes) {
      if (!p.isStarted()) {
        verboseOutput += "   unstarted  0";
      } else if (p.isReady()) {
        verboseOutput += "       ready  " + p.getReadyTime();
      } else if (p.isBlocked()) {
        int remaining = p.getIoBurstRemaining();
        verboseOutput += "     blocked  " + remaining;
      } else if (p.isRunning()) {
        int remaining = p.getCpuBurstRemaining();
        verboseOutput += "     running  " + remaining;
      } else if (p.isTerminated()) {
        verboseOutput += "  terminated  0";
      }
    }
    verboseOutput += ".\n";
  }

  public static void usageExit() {
    System.out.println("Usage: java {fcfs,hprn,rr2,uni} [--verbose] path-to-input-file");
    System.exit(1);
  }

  /**
   * Given an input file, schedule its processes and output the result.
   *
   * @param args Command-line arguments.
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      usageExit();
    }
    String type = args[0];
    String path = args[1];
    boolean verbose = false;
    if (args.length == 3 && args[1].equals("--verbose")) {
      path = args[2];
      verbose = true;
    } else if (args.length > 2) {
      usageExit();
    }
    Reader inputReader = null, randomNumberReader = null;
    try {
      inputReader = new BufferedReader(new InputStreamReader(
          new FileInputStream(path)));
    } catch (FileNotFoundException e) {
      System.err.println("Could not find file: " + path);
      System.exit(1);
    }
    try {
      randomNumberReader = new BufferedReader(new InputStreamReader(
          new FileInputStream("data/random-numbers")));
    } catch (FileNotFoundException e) {
      System.err.println("Could not find file: data/random-numbers");
      System.exit(1);
    }
    Scheduler scheduler = null;
    if (type.equals("fcfs")) {
      scheduler = new FCFSScheduler(inputReader, randomNumberReader, verbose);
    } else if (type.equals("uni")) {
      scheduler = new UniScheduler(inputReader, randomNumberReader, verbose);
    } else if (type.equals("rr2")) {
      scheduler = new RRScheduler(inputReader, randomNumberReader, verbose);
    } else if (type.equals("hprn")) {
      scheduler = new HPRNScheduler(inputReader, randomNumberReader, verbose);
    } else {
      usageExit();
    }
    scheduler.scheduleUntilComplete();
  }
}
