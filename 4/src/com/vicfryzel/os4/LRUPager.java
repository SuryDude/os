// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os4;

/**
 * Replace pages in a Least Recently Used manner when choosing frame to replace.
 */
public class LRUPager extends Pager {
  public LRUPager(int machineSize, int pageSize) {
    super(machineSize, pageSize);
  }

  public Frame getFrameToReplace() {
    Frame lruFrame = getNextFreeFrame();
    if (lruFrame == null) {
      for (Frame f : frames) {
        if (lruFrame == null) {
          lruFrame = f;
        }
        if (f.getLastUsed() < lruFrame.getLastUsed()) {
          lruFrame = f;
        }
      }
    }
    return lruFrame;
  }
}
