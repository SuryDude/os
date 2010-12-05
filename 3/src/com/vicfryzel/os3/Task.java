// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single Task (a.k.a. process,) that needs Resources allocated to
 * it.
 */
public class Task {
  protected int id;
  protected int index;
  protected List<Activity> activities;
  protected boolean aborted;
  protected boolean terminated;
  protected boolean blocked;
  protected int waitTime;
  protected int totalTime;
  protected int computeTime;
  protected Map<Integer, Integer> claims;
  protected Map<Integer, Integer> maximums;

  /**
   * Create a new Task with the given ID.
   * 
   * @param id
   *          Identifier of this Task.
   */
  public Task(int id) {
    this.id = id;
    this.index = id - 1;
    this.activities = new ArrayList<Activity>();
    this.aborted = false;
    this.terminated = false;
    this.waitTime = 0;
    this.totalTime = 0;
    this.computeTime = -1;
    this.claims = new HashMap<Integer, Integer>();
    this.maximums = new HashMap<Integer, Integer>();
  }

  /**
   * Clone the given Task.
   * 
   * @param other
   *          Task to clone.
   */
  public Task(Task other) {
    this.id = other.id;
    this.index = other.index;
    this.activities = new ArrayList<Activity>();
    for (Activity a : other.activities) {
      addActivity(new Activity(a));
    }
    this.aborted = other.aborted;
    this.terminated = other.terminated;
    this.waitTime = other.waitTime;
    this.totalTime = other.totalTime;
    this.computeTime = other.computeTime;
    this.claims = new HashMap<Integer, Integer>();
    for (int resourceType : other.claims.keySet()) {
      this.claims.put(resourceType, other.claims.get(resourceType));
    }
    this.maximums = new HashMap<Integer, Integer>();
    for (int resourceType : other.maximums.keySet()) {
      this.maximums.put(resourceType, other.maximums.get(resourceType));
    }
  }

  /**
   * @return ID of this Task.
   */
  public int getId() {
    return id;
  }

  /**
   * @return Identifying index of this task in various lists. A convenience.
   */
  public int getIndex() {
    return index;
  }

  /**
   * Add the given Activity to this task.
   * 
   * @param a
   *          Activity to add.
   */
  public void addActivity(Activity a) {
    activities.add(a);
  }

  /**
   * @return All Activities of this Task.
   */
  public List<Activity> getActivities() {
    return activities;
  }

  /**
   * @return Next Activity to process in this Task.
   */
  public Activity getNextActivity() {
    return activities.get(0);
  }

  /**
   * Remove the next Activity, exposing the Activity after that.
   */
  public void removeNextActivity() {
    activities.remove(0);
  }

  /**
   * Terminate and abort this Task.
   */
  public void abort() {
    terminate();
    this.aborted = true;
  }

  /**
   * @return True if this Task is aborted, false if otherwise.
   */
  public boolean isAborted() {
    return this.aborted;
  }

  /**
   * Terminate this Task.
   */
  public void terminate() {
    this.terminated = true;
  }

  /**
   * @return True if this Task is terminated, false if otherwise.
   */
  public boolean isTerminated() {
    return terminated;
  }

  /**
   * Block this Task. Blocked Tasks will need to be processed first in some
   * cases.
   */
  public void block() {
    this.blocked = true;
  }

  /**
   * Unblock this Task.
   */
  public void unblock() {
    this.blocked = false;
  }

  /**
   * @return True if this Task is blocked, false if otherwise.
   */
  public boolean isBlocked() {
    return this.blocked;
  }

  /**
   * @return Amount of time (in cycles) this Task has been waiting.
   */
  public int getWaitTime() {
    return waitTime;
  }

  /**
   * Mark this Task as waiting an additional cycle.
   */
  public void incrementWaitTime() {
    waitTime += 1;
  }

  /**
   * @return Total time this Task has been processing.
   */
  public int getTotalTime() {
    return totalTime;
  }

  /**
   * Increment time this Task has been processing by one cycle.
   */
  public void incrementTotalTime() {
    totalTime += 1;
  }

  /**
   * @return Cycles this Task has left to compute.
   */
  public int getComputeTime() {
    return computeTime;
  }

  /**
   * Decrement number of cycles left for this Task to compute.
   */
  public void decrementComputeTime() {
    computeTime -= 1;
  }

  /**
   * @param time
   *          Number of cycles this Task must compute.
   */
  public void setComputeTime(int time) {
    computeTime = time;
  }

  /**
   * @return Percentage of time this Task has waited.
   */
  public double getWaitPercentage() {
    return ((double) getWaitTime() / (double) getTotalTime()) * 100.0;
  }

  /**
   * @param r
   *          Resource to get current claim for.
   * @return Current claim from this Task of the given Resource.
   */
  public int getCurrentClaim(Resource r) {
    int retval = 0;
    if (claims.get(r.getType()) != null) {
      retval = claims.get(r.getType());
    }
    return retval;
  }

  /**
   * Claim given units of given Resource.
   * 
   * @param r
   *          Resource to claim.
   * @param units
   *          Units of Resource to claim.
   */
  public void claim(Resource r, int units) {
    int total = getCurrentClaim(r) + units;
    claims.put(r.getType(), total);
  }

  /**
   * Release given units of given Resource.
   * 
   * @param r
   *          Resource to release.
   * @param units
   *          Units of Resource to release.
   */
  public void release(Resource r, int units) {
    int total = getCurrentClaim(r) - units;
    claims.put(r.getType(), total);
  }

  /**
   * @return All current claims, keyed on Resource type, valued on claim amount.
   */
  public Map<Integer, Integer> getClaims() {
    return claims;
  }

  /**
   * @param r
   *          Resource to get maximum for.
   * @return Maximum number of units this Task can claim of given Resource.
   */
  public int getMaximum(Resource r) {
    return maximums.get(r.getType());
  }

  /**
   * Only allow this Task to claim given max of given Resource.
   * 
   * @param r
   *          Resource to set maximum for.
   * @param max
   *          Maximum units this Task can claim of r.
   */
  public void setMaximum(Resource r, int max) {
    maximums.put(r.getType(), max);
  }

  /**
   * @return String representation of this Task and its Activities.
   */
  public String toString() {
    return "Task " + id + ": " + activities.toString();
  }

  /**
   * @param b
   *          Object to compare for equality.
   * @return True if this Task is equivalent to the given Object.
   */
  public boolean equals(Object b) {
    return getId() == ((Task) b).getId();
  }
}
