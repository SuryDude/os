/**
 * 
 */
package com.vicfryzel.os3;

/**
 * @author vic
 * 
 */
public class BankerAllocator extends FIFOAllocator {

  /**
   * (non-Javadoc)
   * 
   * @see com.vicfryzel.os3.Allocator#Allocator()
   */
  public BankerAllocator(String input) {
    super(input);
  }

  /**
   * (non-Javadoc)
   * 
   * @see com.vicfryzel.os3.Allocator#handleInitiates()
   */
  @Override
  public void handleInitiates() {
    for (Task t : getTasksByCurrentType(Activity.Type.INITIATE)) {
      logger.info("Initiating " + t.getId());
      Activity current = t.getNextActivity();
      Resource requested = manager.getResourceByType(current.getResourceType());
      if (requested.getUnits() < current.getResources()) {
        StringBuilder b = new StringBuilder();
        b.append("Banker aborts task ").append(t.getId())
            .append(" before run begins:\n").append("claim for resource ")
            .append(requested.getType()).append(" (")
            .append(current.getResources())
            .append(") exceeds number of units present (")
            .append(requested.getUnits()).append(")");
        out.println(b.toString());
        abortTask(t);
      } else {
        t.setMaximum(requested, current.getResources());
        tasksToAdvance.add(t);
        t.incrementTotalTime();
        tasks.set(t.getIndex(), t);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.vicfryzel.os3.FIFOAllocator#handleRequest()
   */
  @Override
  protected void handleRequest(Task t) {
    Activity current = t.getNextActivity();
    logger.info("Request from task " + t.getId() + " for "
        + current.getResources() + " units of resource "
        + current.getResourceType());

    Resource r = manager.getResourceByType(current.getResourceType());
    if (t.getCurrentClaim(r) + current.getResources() > t.getMaximum(r)) {
      abortTask(t);
    } else {
      if (manager.claimingResourceIsSafe(tasks, t, current.getResourceType(),
          current.getResources())
          && manager
              .claim(t, current.getResourceType(), current.getResources())) {
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
  }

  /*
   * Do nothing to handle deadlocks, as Banker does not encounter them.
   * 
   * @see com.vicfryzel.os3.Allocator#handleDeadlocks()
   */
  @Override
  public void handleDeadlocks() {
    // Do nothing because Banker does not have deadlocks
  }
}
