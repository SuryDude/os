// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os4;

/**
 * Represents a single frame in this machine.
 */
public class Frame {
  protected Page currentPage;
  protected int id;
  protected static int latestOverallUse;
  protected int lastUsed;
  
  public Frame(int id) {
    this.id = id;
    latestOverallUse = 0;
    this.lastUsed = 0;
  }
  
  public int getId() {
    return id;
  }
  
  public Page getPage() {
    return currentPage;
  }
  
  public void setPage(Page p) {
    this.currentPage = p;
  }
  
  public void use() {
    lastUsed = latestOverallUse;
    latestOverallUse++;
  }
  
  public int getLastUsed() {
    return lastUsed;
  }
}
