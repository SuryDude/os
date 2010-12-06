// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os4;

/**
 *
 */
public class Page {
  protected int id;
  protected Process process;
  protected int start;
  protected int end;
  protected int residencyTime;
  protected int evictedTime;
  protected int numEvictions;
  protected int lastLoadTime;

  public Page(int id, Process p, int start, int end) {
    this.id = id;
    this.process = p;
    this.start = start;
    this.end = end;
    this.residencyTime = 0;
    this.numEvictions = 0;
    this.lastLoadTime = 0;
  }
  
  public int getId() {
    return id;
  }

  public Process getProcess() {
    return process;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }
  
  public int getLastLoadTime() {
    return lastLoadTime;
  }
  
  public void setLastLoadTime(int time) {
    lastLoadTime = time;
  }
  
  public int getResidencyTime() {
    return residencyTime;
  }
  
  public void addResidencyTime(int time) {
    residencyTime += time;
  }
  
  public int getNumEvictions() {
    return numEvictions;
  }
  
  public void incrementNumEvictions() {
    numEvictions++;
  }

  public boolean equals(Object other) {
    Page p = (Page) other;
    return process.equals(p.getProcess()) && getStart() == p.getStart()
        && getEnd() == p.getEnd();
  }
  
  public String toString() {
    return "Page (" + process.getId() + "," + start + "," + end + ")";
  }
}
