// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.math.RoundingMode;
import java.text.DecimalFormat;
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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;


public abstract class Scheduler {
  public static class SchedulerParameters {
    @Parameter(description = "Path to input file of processes to schedule.",
               required = true)
    public List<String> inputFile;

    @Parameter(names = { "-t", "--type" },
               description = "Type of scheduler to use. {fcfs,hprn,rr2,uni}. Default: fcfs.")
    public String type = "fcfs";

    @Parameter(names = { "-v", "--verbose" },
               description = "Show verbose output of each cycle.")
    public boolean verbose = false;

    @Parameter(names = { "-r", "--show-random" },
               description = "Show random numbers as they're read.  Only works with --verbose.")
    public boolean showRandom = false;

    @Parameter(names = { "--random-file" },
               description = "Path to input file of random numbers (UDRIs).")
    public String randomFile = "data/random-numbers";
  }

  protected Reader inputReader;
  protected Reader randomNumberReader;
  protected StreamTokenizer randomNumberTokenizer;
  protected List<Process> processes;
  protected Logger logger;
  protected boolean verbose;
  protected boolean showRandom;
  protected String verboseOutput;
  protected int cycle;
  protected int latestUDRI;

  /**
   * Create a new Scheduler based on the given Reader for random input.
   *
   * @param inputReader Reader wrapping input stream.
   * @param randomNumberReader Reader wrapping random int stream.
   * @param verbose True if output should be verbose.
   * @param showRandom True if verbose output should show random numbers used.
   */
  public Scheduler(Reader inputReader, Reader randomNumberReader,
                   boolean verbose, boolean showRandom) {
    this.inputReader = inputReader;
    this.randomNumberReader = randomNumberReader;
    this.verbose = verbose;
    this.showRandom = showRandom;

    reset();

    logger = Logger.getLogger("Scheduler");
    if (verbose) {
      logger.setLevel(Level.INFO);
    } else {
      logger.setLevel(Level.WARNING);
    }
  }

  protected void reset() {
    try {
      inputReader.reset();
    } catch (IOException e) {}

    processes = Process.read(inputReader);

    try {
      randomNumberReader.reset();
    } catch (IOException e) {}
    randomNumberTokenizer = new StreamTokenizer(randomNumberReader);
    randomNumberTokenizer.eolIsSignificant(false);
    randomNumberTokenizer.parseNumbers();

    cycle = 0;
    verboseOutput = "";
    latestUDRI = 0;
  }

  public void scheduleUntilComplete() {
    printUnsortedProcessList(processes);

    Collections.sort(processes, new ProcessArrivalComparator());

    printSortedProcessList(processes);
    if (!verbose) {
      System.out.print("\n\n");
    } else {
      System.out.println();
    }

    // Schedule until all processes are terminated
    while (step()) {}

    printVerboseOutput();
    printProcessSummary();
    printSummary();
  }
 
  public boolean step() {
    boolean retval = true;

    if (verbose) {
      appendVerboseCycleString();
    }

    handleBlocked();
    handleRunning();
    handleArrivals();
    handleReady();
    if (getTerminated().size() < processes.size()) {
      cycle++;
    } else {
      retval = false;
    }
    return retval;
  }

  public abstract void handleBlocked();
  public abstract void handleRunning();
  public abstract void handleArrivals();
  public abstract void handleReady();

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
    for (Process p : getProcesses()) {
      retval += (float) p.getTotalCpuTime();
    }
    retval = retval / (float) cycle;
    return retval;
  }

  protected float getIoUtilization() {
    float retval = 0;
    for (Process p : getProcesses()) {
      retval += (float) p.getTotalIoTime();
    }
    retval = retval / (float) cycle;
    return retval;
  }

  protected float getThroughput() {
    return getTerminated().size() / ((float) cycle / 100);
  }

  protected double getAverageTurnaroundTime() {
    double retval = 0;
    List<Process> processes = getProcesses();
    for (Process p : processes) {
      retval += p.getTurnaroundTime();
    }
    retval = retval / (double) processes.size();
    return retval;
  }

  protected double getAverageWaitTime() {
    double retval = 0;
    List<Process> processes = getProcesses();
    for (Process p : processes) {
      retval += p.getTotalWaitTime();
    }
    retval = retval / (double) processes.size();
    return retval;
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
    DecimalFormat df = new DecimalFormat("#.######");
    df.setRoundingMode(RoundingMode.HALF_UP);
    df.setMinimumFractionDigits(6);
    System.out.println("Summary Data:");
    System.out.println("\tFinishing time: " + cycle);
    System.out.println("\tCPU Utilization: " + df.format(getCpuUtilization()));
    System.out.println("\tI/O Utilization: " + df.format(getIoUtilization()));
    System.out.println("\tThroughput: " + df.format(getThroughput())
        + " processes per hundred cycles");
    System.out.println("\tAverage turnaround time: "
        + df.format(getAverageTurnaroundTime()));
    System.out.println("\tAverage waiting time: "
        + df.format(getAverageWaitTime()));
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
    System.out.println("The (sorted) input is:  " + output);
  }

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
        latestUDRI = value;
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

  protected void printVerboseOutput() {
    if (verbose) {
      System.out.println(
          "This detailed printout gives the state and remaining burst "
          + "for each process\n");
      System.out.println(verboseOutput);
    }
  }

  protected static String padLeft(String s, int n) {
    return String.format("%1$#" + n + "s", s);  
  }

  protected void appendVerboseCycleString() {
    verboseOutput += "Before cycle " + padLeft(cycle + ": ", 6);
    int columnWidth = 14;
    for (Process p : processes) {
      if (!p.isStarted()) {
        verboseOutput += padLeft("unstarted  0", columnWidth);
      } else if (p.isReady()) {
        verboseOutput += padLeft("ready  0", columnWidth);
      } else if (p.isBlocked()) {
        int remaining = p.getIoBurstRemaining();
        verboseOutput += padLeft("blocked  " + remaining, columnWidth);
      } else if (p.isRunning()) {
        int remaining = p.getCpuBurstRemaining();
        verboseOutput += padLeft("running  " + remaining, columnWidth);
      } else if (p.isTerminated()) {
        verboseOutput += padLeft("terminated  0", columnWidth);
      }
    }
    verboseOutput += ".\n";
  }

  protected void appendShowRandomCpuString() {
    if (verbose && showRandom) {
      verboseOutput += "Find burst when choosing ready process to run " + latestUDRI + "\n";
    }
  }

  protected void appendShowRandomIoString() {
    if (verbose && showRandom) {
      verboseOutput += "Find I/O burst when blocking a process " + latestUDRI + "\n";
    }
  }


  /**
   * Given an input file, schedule its processes and output the result.
   *
   * @param args Command-line arguments.
   */
  public static void main(String[] args) {
    SchedulerParameters s = new SchedulerParameters();
    JCommander commander = new JCommander(s, args);

    Reader inputReader = null, randomNumberReader = null;
    // JCommander requires main param to be a List, we only want 1st element
    String inputFile = s.inputFile.get(0);
    try {
      inputReader = new BufferedReader(new InputStreamReader(
          new FileInputStream(inputFile)));
    } catch (FileNotFoundException e) {
      System.err.println("Could not find file: " + inputFile);
      System.exit(1);
    }
    try {
      randomNumberReader = new BufferedReader(new InputStreamReader(
          new FileInputStream(s.randomFile)));
    } catch (FileNotFoundException e) {
      System.err.println("Could not find file: " + s.randomFile);
      System.exit(1);
    }

    Scheduler scheduler = null;
    if (s.type.equals("fcfs")) {
      scheduler = new FCFSScheduler(inputReader, randomNumberReader,
                                    s.verbose, s.showRandom);
    } else if (s.type.equals("uni")) {
      scheduler = new UniScheduler(inputReader, randomNumberReader,
                                   s.verbose, s.showRandom);
    } else if (s.type.equals("rr2")) {
      scheduler = new RRScheduler(inputReader, randomNumberReader,
                                  s.verbose, s.showRandom);
    } else if (s.type.equals("hprn")) {
      scheduler = new HPRNScheduler(inputReader, randomNumberReader,
                                  s.verbose, s.showRandom);
    } else {
      commander.usage();
    }

    scheduler.scheduleUntilComplete();
  }
}
