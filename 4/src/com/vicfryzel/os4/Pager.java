// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os4;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage Pages into Frames as Processes make references.
 */
public abstract class Pager {
  protected List<Frame> frames;
  protected int machineSize;
  protected int pageSize;
  protected Frame lastUsed;

  public Pager(int machineSize, int pageSize) {
    this.machineSize = machineSize;
    this.pageSize = pageSize;
    this.frames = new ArrayList<Frame>();
    for (int i = 0; i < getNumRequiredFrames(); i++) {
      this.frames.add(new Frame(i));
    }
    lastUsed = null;
  }
  
  public int getNumRequiredFrames() {
    return (int) ((double) machineSize / (double) pageSize);
  }
  
  public List<Frame> getFrames() {
    return frames;
  }
  
  public Frame getLastUsedFrame() {
    return lastUsed;
  }

  protected Page getCurrentPageForProcess(Process process) {
    Page retval = null;
    for (Page page : process.getPages()) {
      // Comparing process IDs is the same thing as doing a little bit of math
      // to convert relative addresses to physical addresses.
      // Also would be the same as storing an offset in each process.
      if (page.getProcess().equals(process)
          && page.getStart() <= process.getCurrentWord()
          && process.getCurrentWord() <= page.getEnd()) {
        retval = page;
        break;
      }
    }
    return retval;
  }

  protected Frame getFrameForProcess(Process process) {
    Frame frameForProcess = null;
    for (Frame frame : frames) {
      Page page = frame.getPage();
      if (process.getPages().contains(page)
          && page.getStart() <= process.getCurrentWord()
          && process.getCurrentWord() <= page.getEnd()) {
        frameForProcess = frame;
        break;
      }
    }
    return frameForProcess;
  }
  
  protected Frame getNextFreeFrame() {
    Frame freeFrame = null;
    for (int i = frames.size() - 1; i >= 0; i--) {
      Frame f = frames.get(i);
      if (f.getPage() == null) {
        freeFrame = f;
        break;
      }
    }
    return freeFrame;
  }

  /**
   * 
   * @param address
   *          Address to reference.
   * @return True if this reference caused a fault, false if otherwise.
   */
  public boolean handleReference(Process process, int time) {
    boolean faultOccurred = false;
    Frame frame = getFrameForProcess(process);
    if (frame == null) {
      faultOccurred = true;
      handlePageFault(process, time);
    } else {
      lastUsed = frame;
      frame.use();
    }
    return faultOccurred;
  }

  public void handlePageFault(Process process, int time) {
    Frame frame = getFrameToReplace();
    lastUsed = frame;
    Page currentPage = frame.getPage();
    if (currentPage != null) {
      currentPage.addResidencyTime(time - currentPage.getLastLoadTime());
      currentPage.incrementNumEvictions();
    }
    Page p = getCurrentPageForProcess(process);
    p.setLastLoadTime(time);
    frame.setPage(p);
    frame.use();
  }
  
  public abstract Frame getFrameToReplace();
}
