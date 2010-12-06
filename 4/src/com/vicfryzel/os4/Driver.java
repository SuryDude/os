// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os4;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Drives the Pager to page through references as needed and handle faults.
 */
public class Driver {
  public static class DriverParameters {
    @Parameter(names = { "-s", "--machine-size" },
        description = "Machine size in words.", required = true)
    public int machineSize;
    @Parameter(names = { "-p", "--page-size" },
        description = "Page size in words.", required = true)
    public int pageSize;
    @Parameter(names = { "-o", "--process-size" },
        description = "Size of a process.", required = true)
    public int processSize;
    @Parameter(names = { "-j", "--job-mix" }, description = "Job mix.",
        required = true)
    public int jobMix;
    @Parameter(names = { "-n", "--num-references" },
        description = "Number of references for each process.", required = true)
    public int numReferences;
    @Parameter(
        names = { "-a", "--algorithm" },
        description = "Replacement algorithm. {lifo,random,lru}. Default: lifo.",
        required = true)
    public String algorithm = "lifo";
    @Parameter(names = { "-d", "--debug" },
        description = "Specify this flag to enter debug mode.")
    public boolean debug = false;
    @Parameter(names = { "--random-file" },
        description = "Path to input file of random numbers.")
    public String randomFile = "data/random-numbers";
  }

  protected Pager pager;
  List<Process> processes;
  protected int machineSize;
  protected int pageSize;
  protected int processSize;
  protected int jobMix;
  protected int numReferences;
  protected String algorithm;
  protected boolean debug;

  public static final int QUANTUM = 3;

  protected Reader randomNumberReader;
  protected StreamTokenizer randomNumberTokenizer;
  protected Logger logger;

  protected ByteArrayOutputStream data;
  protected PrintStream out;

  /**
   * Create a new Scheduler based on the given Reader for random input.
   * 
   * @param randomNumberReader
   *          Reader wrapping random int stream.
   */
  public Driver(DriverParameters s, Reader randomNumberReader) {
    machineSize = s.machineSize;
    pageSize = s.pageSize;
    processSize = s.processSize;
    jobMix = s.jobMix;
    numReferences = s.numReferences;
    algorithm = s.algorithm;
    debug = s.debug;

    processes = new ArrayList<Process>();
    int word1 = (111 * 1 + processSize) % processSize;
    int word2 = (111 * 2 + processSize) % processSize;
    int word3 = (111 * 3 + processSize) % processSize;
    int word4 = (111 * 4 + processSize) % processSize;
    if (jobMix == 1) {
      processes.add(new Process(1, word1, 1, 0, 0));
    } else if (jobMix == 2) {
      processes.add(new Process(1, word1, 1, 0, 0));
      processes.add(new Process(2, word2, 1, 0, 0));
      processes.add(new Process(3, word3, 1, 0, 0));
      processes.add(new Process(4, word4, 1, 0, 0));
    } else if (jobMix == 3) {
      processes.add(new Process(1, word1, 0, 0, 0));
      processes.add(new Process(2, word2, 0, 0, 0));
      processes.add(new Process(3, word3, 0, 0, 0));
      processes.add(new Process(4, word4, 0, 0, 0));
    } else if (jobMix == 4) {
      processes.add(new Process(1, word1, 0.75, 0.25, 0));
      processes.add(new Process(2, word2, 0.75, 0, 0.25));
      processes.add(new Process(3, word3, 0.75, 0.125, 0.125));
      processes.add(new Process(4, word4, 0.5, 0.125, 0.125));
    }

    for (Process p : processes) {
      double numPages = (double) processSize / (double) pageSize;
      int currentWord = 0;
      for (int i = 0; i < numPages; i++) {
        p.addPage(new Page(i, p, currentWord, (currentWord += pageSize) - 1));
      }
    }

    if (algorithm.equals("lifo")) {
      pager = new LIFOPager(machineSize, pageSize);
    } else if (algorithm.equals("random")) {
      pager = new RandomPager(machineSize, pageSize, this);
    } else if (algorithm.equals("lru")) {
      pager = new LRUPager(machineSize, pageSize);
    }

    this.randomNumberReader = randomNumberReader;

    reset();

    logger = Logger.getLogger("Pager");
    if (debug) {
      logger.setLevel(Level.INFO);
    } else {
      logger.setLevel(Level.WARNING);
    }
  }

  protected void reset() {
    try {
      randomNumberReader.reset();
    } catch (IOException e) {
    }
    randomNumberTokenizer = new StreamTokenizer(randomNumberReader);
    randomNumberTokenizer.eolIsSignificant(false);
    randomNumberTokenizer.parseNumbers();

    data = new ByteArrayOutputStream();
    out = new PrintStream(data);
  }

  public String referenceUntilComplete() {
    for (int referenceNum = 0; referenceNum < processes.size() * numReferences;) {
      for (Process p : processes) {
        for (int quantumCounter = 0; quantumCounter < QUANTUM; quantumCounter++) {
          // Sometimes we need to stop processing mid-quantum (towards the end)
          if (p.getNumReferences() >= numReferences) {
            break;
          }
          p.incrementNumReferences();

          StringBuilder b = new StringBuilder();
          b.append(p.getId()).append(" references word ")
              .append(p.getCurrentWord()).append(" (page ")
              .append(pager.getCurrentPageForProcess(p).getId())
              .append(") at time ").append(referenceNum).append(": ");

          if (pager.handleReference(p, referenceNum)) {
            // Fault occurred
            p.incrementNumFaults();
            b.append("Fault, using frame ").append(
                pager.getLastUsedFrame().getId());
          } else {
            // No fault occurred
            b.append("Hit in frame ").append(pager.getLastUsedFrame().getId());
          }
          if (debug) {
            out.println(b.toString());
          }

          double y = getNextRandomNumber() / (Integer.MAX_VALUE + 1d);
          int word = p.getCurrentWord();
          if (y < p.getA()) {
            word = (word + 1 + processSize) % processSize;
          } else if (y < p.getA() + p.getB()) {
            word = (word - 5 + processSize) % processSize;
          } else if (y < p.getA() + p.getB() + p.getC()) {
            word = (word + 4 + processSize) % processSize;
          } else {
            word = (getNextRandomNumber()) % processSize;
          }
          p.setCurrentWord(word);

          referenceNum++;
        }
      }
    }

    printSummary();
    return data.toString();
  }

  protected void printSummary() {
    out.println("The machine size is " + machineSize + ".");
    out.println("The page size is " + pageSize + ".");
    out.println("The process size is " + processSize + ".");
    out.println("The job mix number is " + jobMix + ".");
    out.println("The number of references per process is " + numReferences
        + ".");
    out.println("The replacement algorithm is " + algorithm + ".");
    if (debug) {
      out.println("The level of debugging output is 1.");
    } else {
      out.println("The level of debugging output is 0.");
    }
    out.println();

    int totalFaults = 0;
    double totalResidency = 0;
    int totalEvictions = 0;
    for (Process p : processes) {
      out.print("Process " + p.getId() + " had " + p.getNumFaults() + " faults");
      int pageResidency = 0;
      int numEvictions = 0;
      for (Page page : p.getPages()) {
        pageResidency += page.getResidencyTime();
        numEvictions += page.getNumEvictions();
      }
      totalFaults += p.getNumFaults();
      totalResidency += pageResidency;
      totalEvictions += numEvictions;

      if (numEvictions != 0) {
        out.print(" and ");
        out.print((double) pageResidency / (double) numEvictions);
        out.println(" average residency.");
      } else {
        out.println(".");
        out.println("\tWith no evictions, the average residence is undefined.");
      }
    }

    out.println();
    out.print("The total number of faults is " + totalFaults);
    if (totalEvictions == 0) {
      out.println(".");
      out.println("\tWith no evictions, the overall average residence is "
          + "undefined.");
    } else {
      double averageResidency = totalResidency / (double) totalEvictions;
      out.println(" and the overall average residency is " + averageResidency
          + ".");
    }

  }

  /**
   * @param u
   *          mod param for next random number
   * @return 1 + next random integer of input file % u.
   */
  protected int getNextRandomNumber() {
    int random = 1;
    try {
      int next = randomNumberTokenizer.nextToken();
      if (next == StreamTokenizer.TT_EOF) {
        logger
            .warning("Ran out of random integers, resetting to start of file.");
        reset();
        next = randomNumberTokenizer.nextToken();
      }
      if (next == StreamTokenizer.TT_NUMBER) {
        int value = (int) randomNumberTokenizer.nval;
        random = value;
      } else {
        logger.severe("Non-numeric token found in random integer file.");
      }
    } catch (IOException e) {
      logger.severe("Problem fetching next random number: " + e);
    }
    return random;
  }

  /**
   * Create a Driver and Pager of the given type, and reference until complete.
   * 
   * @param args
   *          Command-line arguments.
   */
  public static void main(String[] args) {
    DriverParameters s = new DriverParameters();
    JCommander commander = new JCommander(s, args);

    Reader randomNumberReader = null;
    try {
      randomNumberReader = new BufferedReader(new InputStreamReader(
          new FileInputStream(s.randomFile)));
    } catch (FileNotFoundException e) {
      System.err.println("Could not find file: " + s.randomFile);
      System.exit(1);
    }

    Driver driver = new Driver(s, randomNumberReader);
    System.out.print(driver.referenceUntilComplete());
  }
}
