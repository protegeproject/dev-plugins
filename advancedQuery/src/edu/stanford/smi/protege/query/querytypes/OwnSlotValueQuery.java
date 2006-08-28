package edu.stanford.smi.protege.query.querytypes;

import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.query.Query;

public class OwnSlotValueQuery implements Query {
  private Slot slot;
  private String expr;
  
  public OwnSlotValueQuery(Slot slot, String expr) {
    this.slot = slot;
    this.expr = expr;
  }

  public String getExpr() {
    return expr;
  }

  public Slot getSlot() {
    return slot;
  }

}
