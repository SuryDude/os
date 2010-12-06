// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os4;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single Process in this system making references.
 */
public class Process {
  protected int id;
  protected int currentWord;
  protected double a;
  protected double b;
  protected double c;
  protected int numFaults;
  protected int numReferences;
  protected List<Page> pages;
  
  public Process(int id, int currentWord, double a, double b, double c) {
    this.id = id;
    this.currentWord = currentWord;
    this.a = a;
    this.b = b;
    this.c = c;
    this.numFaults = 0;
    this.numReferences = 0;
    this.pages = new ArrayList<Page>();
  }
  
  public int getId() {
    return id;
  }
  
  public int getCurrentWord() {
    return currentWord;
  }
  
  public void setCurrentWord(int word) {
    this.currentWord = word;
  }
  
  public double getA() {
    return a;
  }
  
  public double getB() {
    return b;
  }
  
  public double getC() {
    return c;
  }
  
  public void incrementNumFaults() {
    numFaults += 1;
  }
  
  public int getNumFaults() {
    return numFaults;
  }
  
  public void incrementNumReferences() {
    numReferences += 1;
  }
  
  public int getNumReferences() {
    return numReferences;
  }
  
  public List<Page> getPages() {
    return pages;
  }
  
  public void addPage(Page p) {
    pages.add(p);
  }
  
  public boolean equals(Object other) {
    return ((Process) other).getId() == getId();
  }
}
