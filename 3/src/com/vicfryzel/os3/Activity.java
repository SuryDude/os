// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os3;

/**
 * Represents an Activity that a Task can take.
 */
public class Activity {
  /**
   * Types of Activities that a Task can take.
   */
  public enum Type {
    INITIATE, REQUEST, RELEASE, COMPUTE, TERMINATE
  }

  protected Type type;
  protected int task;
  protected int resourceType;
  protected int numCycles;
  protected int resources;

  /**
   * Create a new Activity with the given parameters.
   * 
   * @param type
   *          Type of this activity.
   * @param task
   *          Task number of this activity.
   * @param resourceType
   *          Type of resource this activity acts on.
   * @param numCycles
   *          Number of cycles this activity needs to compute.
   * @param resources
   *          Number of resources this activity requests or releases.
   */
  public Activity(Type type, int task, int resourceType, int numCycles,
      int resources) {
    this.type = type;
    this.task = task;
    this.resourceType = resourceType;
    this.numCycles = numCycles;
    this.resources = resources;
  }

  /**
   * Clone the given Activity.
   * 
   * @param other
   *          Activity to clone.
   */
  public Activity(Activity other) {
    this.type = other.type;
    this.task = other.task;
    this.resourceType = other.resourceType;
    this.numCycles = other.numCycles;
    this.resources = other.resources;
  }

  /**
   * @return Type of this Activity.
   */
  public Type getType() {
    return type;
  }

  /**
   * @return Task owning this Activity.
   */
  public int getTask() {
    return task;
  }

  /**
   * @return Resource type this Activity acts on (if applicable.)
   */
  public int getResourceType() {
    return resourceType;
  }

  /**
   * @return Number of cycles of this Activity (if applicable.)
   */
  public int getNumCycles() {
    return numCycles;
  }

  /**
   * @return Number of resources used by this Activity (if applicable.)
   */
  public int getResources() {
    return resources;
  }

  /**
   * @return String representation of this Activity.
   */
  public String toString() {
    return "( " + type + " " + task + " " + resourceType + " " + numCycles
        + " " + resources + " )";
  }

  /**
   * @param other
   *          Object to compare for equality.
   * @return True if this Activity is equivalent to the given Object.
   */
  public boolean equals(Object other) {
    Activity a = (Activity) other;
    boolean retval = getType() == a.getType();
    retval = retval && getTask() == a.getTask();
    retval = retval && getResourceType() == a.getResourceType();
    retval = retval && getNumCycles() == a.getNumCycles();
    retval = retval && getResources() == a.getResources();
    return retval;
  }
}
