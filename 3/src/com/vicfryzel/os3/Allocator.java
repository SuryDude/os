// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os3;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.io.StringBufferInputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract representation of an Allocator that can be used to allocate a list
 * of Tasks to completion.
 */
@SuppressWarnings("deprecation")
public abstract class Allocator {
  public class AllocatorException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AllocatorException(String m) {
      super(m);
    }
  }

  protected String input;

  protected List<Task> tasks;
  protected List<Task> tasksToAdvance;
  protected ResourceManager manager;

  protected Logger logger;
  protected int cycle;

  protected ByteArrayOutputStream data;
  protected PrintStream out;

  /**
   * Create a new Allocator based on the given Reader for input.
   * 
   * @param inputReader
   *          Reader wrapping input stream.
   */
  public Allocator(String input) {
    this.input = input;

    reset();

    logger = Logger.getLogger("Allocator");
    logger.setLevel(Level.WARNING);
  }

  /**
   * Reset this Allocator to an "initial" state.
   */
  protected void reset() {
    read(input);

    tasksToAdvance = new ArrayList<Task>();
    data = new ByteArrayOutputStream();
    out = new PrintStream(data);

    cycle = 0;
  }

  /**
   * Read the given input data, and populate this Allocator with data.
   * 
   * @param input
   *          Data to parse as Allocator input.
   */
  protected void read(String input) {
    StreamTokenizer tokenizer = new StreamTokenizer(
        new StringBufferInputStream(input));
    tokenizer.eolIsSignificant(false);
    tokenizer.parseNumbers();

    tasks = new ArrayList<Task>();
    manager = new ResourceManager();

    try {
      int next;
      int numResources = -1;
      boolean readingResources = false;
      int numResourcesRead = 0;

      int numTasks = -1;

      int n;
      String word;
      while ((next = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
        if (next == StreamTokenizer.TT_NUMBER) {
          n = (int) tokenizer.nval;
          if (numTasks == -1) {
            numTasks = n;
            for (int taskNum = 0; taskNum < numTasks; taskNum++) {
              tasks.add(new Task(taskNum + 1));
            }
          } else if (numResources == -1) {
            numResources = n;
            readingResources = true;
          } else if (readingResources) {
            manager.add(new Resource(numResourcesRead + 1, n));
            numResourcesRead++;
            if (numResourcesRead == numResources) {
              readingResources = false;
            }
          }
        } else if (next == StreamTokenizer.TT_WORD) {
          word = tokenizer.sval;
          next = tokenizer.nextToken();
          int taskNum = (int) tokenizer.nval;
          Task currentTask = tasks.get(taskNum - 1);
          if (word.equals("initiate")) {
            next = tokenizer.nextToken();
            int resourceType = (int) tokenizer.nval;
            next = tokenizer.nextToken();
            int initialClaim = (int) tokenizer.nval;
            currentTask.addActivity(new Activity(Activity.Type.INITIATE,
                taskNum, resourceType, 0, initialClaim));
          } else if (word.equals("request")) {
            next = tokenizer.nextToken();
            int resourceType = (int) tokenizer.nval;
            next = tokenizer.nextToken();
            int numRequested = (int) tokenizer.nval;
            currentTask.addActivity(new Activity(Activity.Type.REQUEST,
                taskNum, resourceType, 0, numRequested));
          } else if (word.equals("release")) {
            next = tokenizer.nextToken();
            int resourceType = (int) tokenizer.nval;
            next = tokenizer.nextToken();
            int numReleased = (int) tokenizer.nval;
            currentTask.addActivity(new Activity(Activity.Type.RELEASE,
                taskNum, resourceType, 0, numReleased));
          } else if (word.equals("compute")) {
            next = tokenizer.nextToken();
            int numCycles = (int) tokenizer.nval;
            currentTask.addActivity(new Activity(Activity.Type.COMPUTE,
                taskNum, 0, numCycles, 0));
          } else if (word.equals("terminate")) {
            currentTask.addActivity(new Activity(Activity.Type.TERMINATE,
                taskNum, 0, 0, 0));
          } else {
            throw new AllocatorException(word
                + " is not a valid activity type.");
          }
          tasks.set(taskNum - 1, currentTask);
        }
      }
    } catch (IOException e) {
      logger.severe("There was an error reading the input: " + e);
    } catch (AllocatorException e) {
      logger.severe("There was an error parsing the input: " + e);
    }
  }

  /**
   * Allocates all Tasks until all tasks are terminated.
   * 
   * @return Information about allocations.
   */
  public String allocateUntilComplete() {
    while (step()) {
    }

    printTaskSummary();
    return data.toString();
  }

  /**
   * Take a single step forward during allocation. This attempts to process the
   * next Activity of each Task.
   * 
   * @return False if all Tasks are terminated, true if otherwise.
   */
  public boolean step() {
    boolean retval = true;

    if (getTerminated().size() < tasks.size()) {
      logger.info("During " + cycle + "-" + (cycle + 1));
      handleInitiates();
      handleRequests();
      handleReleases();
      handleComputes();
      handleDeadlocks();
      cycle++;
      for (Task t : tasksToAdvance) {
        t.removeNextActivity();
      }
      tasksToAdvance.clear();
      handleTerminates();
    } else {
      retval = false;
    }
    return retval;
  }

  /**
   * Handle all Tasks whose next Activity is Activity.Type.INITIATE.
   */
  public abstract void handleInitiates();

  /**
   * Handle all Tasks whose next Activity is Activity.Type.REQUEST.
   */
  public abstract void handleRequests();

  /**
   * Handle all Tasks whose next Activity is Activity.Type.RELEASE.
   */
  public abstract void handleReleases();

  /**
   * Handle all Tasks whose next Activity is Activity.Type.COMPUTE.
   */
  public abstract void handleComputes();

  /**
   * Handle all Tasks whose next Activity is Activity.Type.TERMINATE.
   */
  public abstract void handleTerminates();

  /**
   * Detect and handle any deadlocks between Tasks.
   */
  public abstract void handleDeadlocks();

  /**
   * @return List of all Tasks in their current state.
   */
  public List<Task> getTasks() {
    return tasks;
  }

  /**
   * @param type
   *          Type to search for.
   * @return All Tasks with a next Activity of the given Type.
   */
  public List<Task> getTasksByCurrentType(Activity.Type type) {
    List<Task> tasks = new ArrayList<Task>();
    for (Task t : getTasks()) {
      if (!t.isTerminated() && t.getNextActivity().getType() == type) {
        tasks.add(t);
      }
    }
    return tasks;
  }

  /**
   * @return All non-terminated Tasks.
   */
  public List<Task> getActive() {
    List<Task> active = new ArrayList<Task>();
    for (Task t : tasks) {
      if (!t.isTerminated()) {
        active.add(t);
      }
    }
    return active;
  }

  /**
   * @return All terminated tasks.
   */
  public List<Task> getTerminated() {
    List<Task> terminated = new ArrayList<Task>();
    for (Task t : tasks) {
      if (t.isTerminated()) {
        terminated.add(t);
      }
    }
    return terminated;
  }

  /**
   * Sends a String task summary to out.
   */
  protected void printTaskSummary() {
    DecimalFormat df = new DecimalFormat("#");
    df.setRoundingMode(RoundingMode.HALF_UP);
    df.setMinimumFractionDigits(0);

    StringBuilder summary = new StringBuilder();
    int totalTime = 0;
    int totalWaitTime = 0;
    for (Task t : getTasks()) {
      summary.append("Task ").append(t.getId()).append("\t\t");
      if (!t.isAborted()) {
        totalTime += t.getTotalTime();
        totalWaitTime += t.getWaitTime();
        summary.append(t.getTotalTime()).append("\t").append(t.getWaitTime())
            .append("\t").append(df.format(t.getWaitPercentage())).append("%");
      } else {
        summary.append("aborted");
      }
      summary.append("\n");
    }
    double overallPercentage = ((double) totalWaitTime / (double) totalTime) * 100.0;
    summary.append("total\t\t").append(totalTime).append("\t")
        .append(totalWaitTime).append("\t")
        .append(df.format(overallPercentage)).append("%");
    out.println(summary.toString());
  }

  /**
   * Given an input file, allocate its tasks and output the result to STDOUT.
   * 
   * @param args
   *          Command-line arguments.
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("Usage: java Allocator input-file");
      System.exit(1);
    }

    String input = "";
    byte[] buffer = new byte[(int) new File(args[0]).length()];
    BufferedInputStream f = null;
    try {
      f = new BufferedInputStream(new FileInputStream(args[0]));
      f.read(buffer);
    } catch (FileNotFoundException e) {
      System.err.println("Could not find file: " + args[0]);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (f != null) {
        try {
          f.close();
        } catch (Exception ignored) {
        }
      } else {
        System.exit(1);
      }
    }
    input = new String(buffer);

    Allocator fifo = new FIFOAllocator(input);
    Allocator banker = new BankerAllocator(input);

    System.out.print("FIFO\n" + fifo.allocateUntilComplete());
    System.out.println();
    System.out.print("BANKER'S\n" + banker.allocateUntilComplete());
  }
}
