package edu.stanford.smi.protege.query;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.query.Query;

public class PhoneticQuery implements Query {
  private Frame frame;
  private Slot slot;
  private String expr;
  
  public PhoneticQuery(Frame frame, Slot slot, String expr) {
    this.frame = frame;
    this.slot = slot;
    this.expr = expr;
  }
  
  public String getExpr() {
    return expr;
  }
  public Frame getFrame() {
    return frame;
  }
  public Slot getSlot() {
    return slot;
  }

}
