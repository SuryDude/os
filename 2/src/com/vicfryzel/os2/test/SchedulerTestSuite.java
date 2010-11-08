// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os2.test;

import junit.framework.Test;
import junit.framework.TestSuite;


public class SchedulerTestSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new FCFSSchedulerTest("testScheduler"));
    suite.addTest(new HPRNSchedulerTest("testScheduler"));
    suite.addTest(new RRSchedulerTest("testScheduler"));
    suite.addTest(new UniSchedulerTest("testScheduler"));
    return suite;
  }
}
