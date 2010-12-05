// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vicfryzel.os3.Resource.ResourceException;

/**
 * Manages Resources during allocation.
 */
public class ResourceManager {
  protected Map<Integer, Resource> resources;

  /**
   * Create a new ResourceManager.
   */
  public ResourceManager() {
    resources = new HashMap<Integer, Resource>();
  }

  /**
   * Clone the given ResourceManager.
   * 
   * @param other
   *          ResourceManager to clone.
   */
  public ResourceManager(ResourceManager other) {
    resources = new HashMap<Integer, Resource>();
    for (Resource r : other.resources.values()) {
      add(new Resource(r));
    }
  }

  /**
   * Add the given Resource to this manager.
   * 
   * @param r
   *          Resource to add.
   */
  public void add(Resource r) {
    resources.put(r.getType(), r);
  }

  /**
   * @return Resources managed by this ResourceManager.
   */
  public Collection<Resource> getResources() {
    return resources.values();
  }

  /**
   * @param type
   *          Type of Resource to find.
   * @return Resource of given type.
   */
  public Resource getResourceByType(int type) {
    return resources.get(type);
  }

  /**
   * Claim the given units of the given resource type to the given Task.
   * 
   * @param t
   *          Task claiming resources.
   * @param type
   *          Type of Resource to claim.
   * @param units
   *          Number of units to claim.
   * @return True if the claim succeeded, false if otherwise.
   */
  public boolean claim(Task t, int type, int units) {
    return claim(t, getResourceByType(type), units);
  }

  /**
   * Claim the given units of the given Resource to the given Task.
   * 
   * @param t
   *          Task claiming resources.
   * @param r
   *          Resource to claim.
   * @param units
   *          Number of units to claim.
   * @return True if the claim succeeded, false if otherwise.
   */
  public boolean claim(Task t, Resource r, int units) {
    boolean success = false;
    try {
      r.claim(units);
      t.claim(r, units);
      success = true;
    } catch (ResourceException e) {
    }
    return success;
  }

  /**
   * Release the given units of the given resource type from the given Task.
   * 
   * @param t
   *          Task claiming resources.
   * @param type
   *          Type of Resource to release.
   * @param units
   *          Number of units to release.
   * @return True if the release succeeded, false if otherwise.
   */
  public boolean release(Task t, int type, int units) {
    return release(t, getResourceByType(type), units);
  }

  /**
   * Release the given units of the given Resource from the given Task.
   * 
   * @param t
   *          Task claiming resources.
   * @param r
   *          Resource to release.
   * @param units
   *          Number of units to release.
   * @return True if the release succeeded, false if otherwise.
   */
  public boolean release(Task t, Resource r, int units) {
    boolean success = false;
    try {
      r.release(units);
      t.release(r, units);
      success = true;
    } catch (ResourceException e) {
    }
    return success;
  }

  /**
   * Release all Resources claimed by the given Task.
   * 
   * @param t
   *          Task containing Resources to release.
   * @return True if the releases succeeded, false if otherwise.
   */
  public boolean releaseAll(Task t) {
    boolean success = true;
    Map<Integer, Integer> claims = t.getClaims();
    for (int resourceType : claims.keySet()) {
      success = release(t, resourceType, claims.get(resourceType));
      if (!success) {
        break;
      }
    }
    return success;
  }

  /**
   * Determine if it is safe (from Banker's algorithm) to claim the given number
   * of units of the given Resource type.
   * 
   * @param tasks
   *          Current list of Tasks.
   * @param t
   *          Task attempting to make claim.
   * @param type
   *          Type of Resource to claim.
   * @param units
   *          Number of units to claim.
   * @return True if it is safe to claim the Resource, false if otherwise.
   */
  public boolean claimingResourceIsSafe(List<Task> tasks, Task t, int type,
      int units) {
    // Copy everything so that this is a fake run
    ResourceManager manager = new ResourceManager(this);
    List<Task> taskListCopy = new ArrayList<Task>();
    for (Task original : tasks) {
      taskListCopy.add(original.getIndex(), new Task(original));
    }
    Task taskCopy = taskListCopy.get(t.getIndex());
    Resource r = manager.getResourceByType(type);

    if (!manager.claim(taskCopy, type, units)) {
      return false;
    } else if (taskCopy.getMaximum(r) - taskCopy.getCurrentClaim(r) > r
        .getAvailableUnits()) {
      return false;
    }
    return claimingResourceIsSafe(taskListCopy, manager);
  }

  /**
   * Determine if the remaining Tasks in the given Task list can safely complete
   * given the current state of the Task list and ResourceManager.
   * 
   * @param tasks
   *          Current list of Tasks.
   * @param manager
   *          ResourceManager currently managing claims.
   * @return True if a safe route of claims is still available.
   */
  protected boolean claimingResourceIsSafe(List<Task> tasks,
      ResourceManager manager) {
    boolean found = false;
    boolean safe = false;

    for (Task t : tasks) {
      if (!t.isTerminated()) {
        found = true;
        boolean allResourcesPassed = true;
        for (Resource r : manager.getResources()) {
          if (t.getMaximum(r) - t.getCurrentClaim(r) > r.getAvailableUnits()) {
            allResourcesPassed = false;
            break;
          }
        }

        if (allResourcesPassed) {
          manager.releaseAll(t);
          t.terminate();
          safe = claimingResourceIsSafe(tasks, manager);
        }
      }
    }
    if (!found) {
      safe = true;
    }
    return safe;
  }
}
