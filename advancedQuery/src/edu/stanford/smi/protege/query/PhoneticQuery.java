package edu.stanford.smi.protege.query;

import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.query.Query;

public class PhoneticQuery implements Query {

  private Slot slot;
  private String expr;
  
  public PhoneticQuery(Slot slot, String expr) {
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
