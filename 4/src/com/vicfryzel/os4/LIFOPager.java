// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os4;

/**
 * Pager that replaces pages in LIFO manner when choosing frame to replace.
 */
public class LIFOPager extends Pager {
  public LIFOPager(int machineSize, int pageSize)  {
    super(machineSize, pageSize);
  }
  
  public Frame getFrameToReplace() {
    Frame lastFrame = getNextFreeFrame();
    if (lastFrame == null) {
      for (Frame f : frames) {
        if (lastFrame == null) {
          lastFrame = f;
        }
        if (f.getId() < lastFrame.getId()) {
          lastFrame = f;
        }
      }
    }
    return lastFrame;
  }
}
