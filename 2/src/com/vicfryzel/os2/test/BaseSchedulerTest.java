// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os2.test;

import com.vicfryzel.os2.FCFSScheduler;
import com.vicfryzel.os2.HPRNScheduler;
import com.vicfryzel.os2.RRScheduler;
import com.vicfryzel.os2.Scheduler;
import com.vicfryzel.os2.UniScheduler;

import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import junit.framework.TestCase;


public abstract class BaseSchedulerTest extends TestCase {
  public static final int NUM_TEST_INPUT_FILES = 7;

  public BaseSchedulerTest(String name) {
    super(name);
  }

  protected abstract Scheduler getSchedulerForFile(
      String inputPath, String randomPath, boolean verbose,
      boolean showRandom) throws FileNotFoundException;

  protected abstract String getOutputFilePrefix();

  protected String readOutputFile(String path) throws Exception {
    String data = "";
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(path)));
    String line;
    while ((line = reader.readLine()) != null) {
      data += line + "\n";
    }
    return data;
  }

  protected String getDataPath() {
    String dataPath = System.getProperty("data.dir");
    if (dataPath == null) {
      dataPath = "";
    } else {
      dataPath += "/";
    }
    return dataPath;
  }

  public void testScheduler() throws Exception {
    for (int i = 1; i <= NUM_TEST_INPUT_FILES; i++) {
      Scheduler s = getSchedulerForFile(
          getDataPath() + "input-" + i,
          getDataPath() + "random-numbers",
          false,
          false);
      assertEquals(
          readOutputFile(
              getDataPath() + getOutputFilePrefix() + "-output-" + i),
          s.scheduleUntilComplete());
    }              
  }

  public void testSchedulerVerbose() throws Exception {
    for (int i = 1; i <= NUM_TEST_INPUT_FILES; i++) {
      Scheduler s = getSchedulerForFile(
          getDataPath() + "input-" + i,
          getDataPath() + "random-numbers",
          true,
          false);
      assertEquals(
          readOutputFile(getDataPath() + "fcfs-output-" + i + "-detailed"),
          s.scheduleUntilComplete());
    }              
  }

  public void testSchedulerVerboseRandom() throws Exception {
    for (int i = 1; i <= NUM_TEST_INPUT_FILES; i++) {
      Scheduler s = getSchedulerForFile(
          getDataPath() + "input-" + i,
          getDataPath() + "random-numbers",
          true,
          true);
      assertEquals(
          readOutputFile(getDataPath() + "fcfs-output-" + i + "-show-random"),
          s.scheduleUntilComplete());
    }              
  }
}
