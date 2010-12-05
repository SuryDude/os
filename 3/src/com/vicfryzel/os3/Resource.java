// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os3;

/**
 * Represents a single Resource that is available for allocation.
 */
public class Resource {
  public class ResourceException extends Exception {
    private static final long serialVersionUID = 1L;

    public ResourceException(String m) {
      super(m);
    }
  }

  protected int type;
  protected int units;
  protected int availableUnits;

  /**
   * Create a new Resource.
   * 
   * @param type
   *          Type of this Resource.
   * @param units
   *          Number of units this Resource has.
   */
  public Resource(int type, int units) {
    this.type = type;
    this.units = units;
    this.availableUnits = units;
  }

  /**
   * Create a clone of another Resource.
   * 
   * @param other
   *          Resource to clone.
   */
  public Resource(Resource other) {
    this.type = other.type;
    this.units = other.units;
    this.availableUnits = other.availableUnits;
  }

  /**
   * @return Type of this Resource.
   */
  public int getType() {
    return type;
  }

  /**
   * @return Total number of units this resource has.
   */
  public int getUnits() {
    return units;
  }

  /**
   * @return Number of units this resource currently has available.
   */
  public int getAvailableUnits() {
    return availableUnits;
  }

  /**
   * Claim the given number of units from this Resource.
   * 
   * @param numUnits
   *          Number of units to claim.
   * @return New number of available units.
   * @throws ResourceException
   *           Thrown if claim cannot be made.
   */
  public int claim(int numUnits) throws ResourceException {
    if (numUnits > getAvailableUnits()) {
      throw new ResourceException("Tried to claim " + numUnits
          + " units, but there are only " + getAvailableUnits() + " available.");
    }
    if (numUnits > getUnits()) {
      throw new ResourceException("Tried to claim " + numUnits
          + " units, but there are only " + getUnits() + " total.");
    }
    return availableUnits -= numUnits;
  }

  /**
   * Release the given number of units back to this Resource.
   * 
   * @param numUnits
   *          Number of units to release.
   * @return New number of available units.
   * @throws ResourceException
   *           Thrown if release cannot be made.
   */
  public int release(int numUnits) throws ResourceException {
    if (numUnits > getUnits() || numUnits + getAvailableUnits() > getUnits()) {
      throw new ResourceException("Tried to release " + numUnits
          + " units, but there is a max of " + getUnits());
    }
    return availableUnits += numUnits;
  }

  /**
   * @return String representation of this Resource in its current state.
   */
  public String toString() {
    return "Resource " + type + ": " + getAvailableUnits() + "/" + getUnits();
  }

  /**
   * @param other
   *          Object to compare for equality.
   * @return True if this Resource is equivalent to the given Object.
   */
  public boolean equals(Object other) {
    return getType() == ((Resource) other).getType();
  }
}
