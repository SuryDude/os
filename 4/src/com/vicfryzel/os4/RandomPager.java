// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os4;

/**
 * Replace pages into frames by choosing a random frame.
 */
public class RandomPager extends Pager {
  protected Driver driver;

  public RandomPager(int machineSize, int pageSize, Driver driver) {
    super(machineSize, pageSize);

    this.driver = driver;
  }

  public Frame getFrameToReplace() {
    Frame randomFrame = getNextFreeFrame();
    if (randomFrame == null) {
      int randomFrameNumber = driver.getNextRandomNumber() % frames.size();
      for (Frame f : frames) {
        if (f.getId() == randomFrameNumber) {
          randomFrame = f;
        }
      }
    }
    return randomFrame;
  }
}
