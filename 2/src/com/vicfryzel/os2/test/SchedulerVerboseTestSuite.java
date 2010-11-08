// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os2.test;

import junit.framework.Test;
import junit.framework.TestSuite;


public class SchedulerVerboseTestSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(FCFSSchedulerTest.class);
    suite.addTestSuite(HPRNSchedulerTest.class);
    suite.addTestSuite(RRSchedulerTest.class);
    suite.addTestSuite(UniSchedulerTest.class);
    return suite;
  }
}
