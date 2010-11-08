// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os2.test;

import com.vicfryzel.os2.Scheduler;
import com.vicfryzel.os2.UniScheduler;

import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import junit.framework.TestCase;


public class UniSchedulerTest extends BaseSchedulerTest {
  public UniSchedulerTest(String name) {
    super(name);
  }

  protected Scheduler getSchedulerForFile(
      String inputFile, String randomFile, boolean verbose,
      boolean showRandom) throws FileNotFoundException {
    Reader inputReader = new BufferedReader(new InputStreamReader(
        new FileInputStream(inputFile)));
    Reader randomNumberReader = new BufferedReader(new InputStreamReader(
        new FileInputStream(randomFile)));

    return new UniScheduler(inputReader, randomNumberReader, verbose,
                            showRandom);
  }

  protected String getOutputFilePrefix() {
    return "uni";
  }
}
