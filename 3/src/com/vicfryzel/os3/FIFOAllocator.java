// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allocates Tasks in a FIFO manner. Efficient, but can have deadlocks.
 */
public class FIFOAllocator extends Allocator {
  /**
   * A list of task IDs that are currently blocked.
   */
  protected List<Integer> blocked;

  /**
   * (non-Javadoc)
   * 
   * @see com.vicfryzel.os3.Allocator#Allocator()
   */
  public FIFOAllocator(String input) {
    super(input);

    blocked = new ArrayList<Integer>();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.vicfryzel.os3.Allocator#handleInitiates()
   */
  @Override
  public void handleInitiates() {
    for (Task t : getTasksByCurrentType(Activity.Type.INITIATE)) {
      logger.info("Initiating " + t.getId());
      Activity current = t.getNextActivity();
      t.setMaximum(manager.getResourceByType(current.getResourceType()),
          current.getResources());
      tasksToAdvance.add(t);
      t.incrementTotalTime();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.vicfryzel.os3.Allocator#handleRequests()
   */
  @Override
  public void handleRequests() {
    List<Integer> visited = new ArrayList<Integer>();
    // Avoid concurrent modification
    List<Integer> currentBlocked = new ArrayList<Integer>(blocked);
    Collections.copy(currentBlocked, blocked);
    for (int index : currentBlocked) {
      Task t = getTasks().get(index);
      if (!t.isTerminated()) {
        handleRequest(t);
        visited.add(t.getIndex());
      }
    }
    for (Task t : getTasksByCurrentType(Activity.Type.REQUEST)) {
      if (!visited.contains(t.getIndex())) {
        handleRequest(t);
      }
    }
  }

  /**
   * Handles a single request from the given Task.
   * 
   * @param t
   *          Task from which to handle request.
   */
  protected void handleRequest(Task t) {
    Activity current = t.getNextActivity();
    logger.info("Request from task " + t.getId() + " for "
        + current.getResources() + " units of resource "
        + current.getResourceType());
    if (manager.claim(t, current.getResourceType(), current.getResources())) {
      blocked.remove((Integer) t.getIndex());
      tasksToAdvance.add(t);
    } else {
      t.incrementWaitTime();
      if (!blocked.contains(t.getIndex())) {
        blocked.add(t.getIndex());
      }
    }
    t.incrementTotalTime();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.vicfryzel.os3.Allocator#handleReleases()
   */
  @Override
  public void handleReleases() {
    Activity current = null;
    for (Task t : getTasksByCurrentType(Activity.Type.RELEASE)) {
      current = t.getNextActivity();
      logger.info("Release from task " + t.getId() + " for "
          + current.getResources() + " units of resource "
          + current.getResourceType());
      if (manager.release(t, current.getResourceType(), current.getResources())) {
        // t.release(manager.getResourceByType(current.getResourceType()),
        // current.getResources());
        tasksToAdvance.add(t);
      }
      t.incrementTotalTime();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.vicfryzel.os3.Allocator#handleComputes()
   */
  @Override
  public void handleComputes() {
    Activity current = null;
    for (Task t : getTasksByCurrentType(Activity.Type.COMPUTE)) {
      current = t.getNextActivity();
      if (t.getComputeTime() == -1) {
        t.setComputeTime(current.getNumCycles());
      }
      logger.info("Computing " + t.getId() + " " + t.getComputeTime());
      t.decrementComputeTime();
      if (t.getComputeTime() == 0) {
        t.decrementComputeTime();
        tasksToAdvance.add(t);
      }
      t.incrementTotalTime();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.vicfryzel.os3.Allocator#handleTerminates()
   */
  @Override
  public void handleTerminates() {
    for (Task t : getTasksByCurrentType(Activity.Type.TERMINATE)) {
      logger.info("Terminating " + t.getId());
      t.terminate();
      // Do not increment total time for terminate activity
    }
  }

  public void handleDeadlocks() {
    List<Task> requests = getTasksByCurrentType(Activity.Type.REQUEST);
    List<Integer> releases = new ArrayList<Integer>();
    for (Task t : getTasksByCurrentType(Activity.Type.RELEASE)) {
      releases.add(t.getNextActivity().getResourceType());
    }
    // If there is any unblocked request, we're not deadlocked yet.
    if (blocked.size() > 1 && blocked.size() >= requests.size()) {
      Task aborted = null;

      // Are two or more waiting on the same resource?
      Map<Integer, Integer> required = new HashMap<Integer, Integer>();

      for (Task t : requests) {
        int type = t.getNextActivity().getResourceType();
        if (!tasksToAdvance.contains(t) && !releases.contains(type)) {
          if (required.containsKey(type)) {
            aborted = tasks.get(required.get(type));
            abortTask(aborted);
            blocked.remove((Integer) t.getIndex());
          }
          required.put(type, t.getIndex());
        }
      }
    }
  }

  /**
   * Abort the given Task and release all of its resources.
   * 
   * @param t
   *          Task to abort.
   */
  protected void abortTask(Task t) {
    logger.info("Aborting " + t.getId());
    manager.releaseAll(t);
    t.abort();
  }
}
