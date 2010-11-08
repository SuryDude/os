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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Process {
  protected int arrival;
  protected int burst;
  protected int cpu;
  protected int io;

  protected int id;
  protected int readyTime;

  protected int terminatedTime;
  protected int turnaroundTime;
  protected int totalCpuTime;
  protected int totalIoTime;
  protected int totalWaitTime;

  protected int cpuBurstRemaining;
  protected int ioBurstRemaining;
  protected int timer;

  public enum State {
    UNSTARTED,
    READY,
    RUNNING,
    BLOCKED,
    TERMINATED
  }
  protected State state;

  protected static Logger logger;
  static {
    logger = Logger.getLogger("Process");
    logger.setLevel(Level.WARNING);
  }

  /**
   * Create a new Process with the given parameters.
   *
   * @param id ID of this process to permanently identify it.
   * @param arrival Arrival time.
   * @param burst CPU bursts.
   * @param cpu Required CPU time.
   * @param io IO bursts.
   */
  public Process(int id, int arrival, int burst, int cpu, int io) {
    setState(State.UNSTARTED);
    this.id = id;
    this.arrival = arrival;
    this.burst = burst;
    this.cpu = cpu;
    this.io = io;
    readyTime = terminatedTime = turnaroundTime = totalCpuTime =
        totalIoTime = totalWaitTime = cpuBurstRemaining =
        ioBurstRemaining = timer = 0;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  protected State getState() {
    return state;
  }

  protected void setState(State state) {
    this.state = state;
  }

  public boolean isStarted() {
    return getState() != State.UNSTARTED;
  }

  public void ready(int cycle) {
    this.readyTime = cycle;
    setState(State.READY);
  }

  public int getReadyTime() {
    return readyTime;
  }

  public boolean isReady() {
    return getState() == State.READY;
  }

  public void run() {
    setState(State.RUNNING);
  }

  public boolean isRunning() {
    return getState() == State.RUNNING;
  }

  public void block() {
    setState(State.BLOCKED);
  }

  public boolean isBlocked() {
    return getState() == State.BLOCKED;
  }

  public void processWait() {
    totalWaitTime += 1;
  }

  public void terminate(int cycle) {
    this.terminatedTime = cycle;
    setState(State.TERMINATED);
  }

  public boolean isTerminated() {
    return getState() == State.TERMINATED;
  }

  public String toString() {
    return "( " +
        arrival + " " +
        burst + " " +
        cpu + " " +
        io + " )";
  }

  public String toAltString() {
    return "(" +
        arrival + "," +
        burst + "," +
        cpu + "," +
        io + ")";
  }

  public int getArrival() {
    return arrival;
  }

  public int getBurst() {
    return burst;
  }

  public int getCpu() {
    return cpu;
  }

  public int getIo() {
    return io;
  }

  public int getTerminatedTime() {
    return terminatedTime;
  }

  public int getTurnaroundTime() {
    return getTotalCpuTime() + getTotalIoTime() + getTotalWaitTime();
  }
  
  public int getTotalCpuTime() {
    return totalCpuTime;
  }
 
  public void addTotalCpuTime(int time) {
    totalCpuTime += time;
  }

  public int getTotalIoTime() {
    return totalIoTime;
  }
 
  public void addTotalIoTime(int time) {
    totalIoTime += time;
  }

  public int getTotalWaitTime() {
    return totalWaitTime;
  }

  public void addTotalWaitTime(int time) {
    totalWaitTime += time;
  }

  public int getIoBurstRemaining() {
    return ioBurstRemaining;
  }

  public void setIoBurstRemaining(int time) {
    ioBurstRemaining = time;
  }

  public int getCpuBurstRemaining() {
    return cpuBurstRemaining; 
  }
  
  public void setCpuBurstRemaining(int time) {
    cpuBurstRemaining = time;
  }

  public int getRemainingCpuTime() {
    int retval = getCpu() - getTurnaroundTime();
    if (retval < 0) {
      retval = 0;
    }
    return retval;
  }

  public int getTimer() {
    return timer;
  }

  public void setTimer(int time) {
    timer = time;
  }

  /**
   * Read the given file into a list of processes.
   *
   * @param path Path to file containing a list of processes.
   * @return List of processes.
   */
  public static List<Process> read(String path) throws FileNotFoundException {
    Reader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(path)));
    return read(reader);
  }

  /**
   * Read the given Reader into a list of processes.
   *
   * @param reader Reader to read from.
   * @return List of processes.
   */
   public static List<Process> read(Reader reader) {
    StreamTokenizer tokenizer = new StreamTokenizer(reader);
    tokenizer.eolIsSignificant(false);
    tokenizer.parseNumbers();
    List<Process> processes = new ArrayList<Process>();
    try {
      int next;
      int i = -1;
      int id = 0;
      int arrival = 0, burst = 0, cpu = 0, io = 0;
      int n;
      while ((next = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
        if (next == tokenizer.TT_NUMBER) {
          n = (int) tokenizer.nval;
          // Skip the first one, as it's just the number of processes
          if (i == -1) {
            i = 0;
            continue;
          } else if (i == 3) {
            io = n;
            processes.add(new Process(id, arrival, burst, cpu, io));
            id++;
            i = 0;
            continue;
          } else if (i == 2) {
            cpu = n;
          } else if (i == 1) {
            burst = n;
          } else {
            arrival = n;
          }
          i++;
        }
      }
    } catch (IOException e) {
      logger.severe("There was an error reading the input: " + e);
    }
    return processes;
  }

  /**
   * Given an input file, generate a list of processes and print to stdout.
   *
   * @param args Command-line arguments.
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: java Process path-to-input-file");
      System.exit(1);
    }
    try {
      List<Process> processes = Process.read(args[0]);
      System.out.println(processes);
    } catch (FileNotFoundException e) {
      logger.severe("Could not find file: " + args[0]);
      System.exit(1);
    }
  }
}
