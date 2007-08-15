package edu.stanford.smi.protege.query.querytypes;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.util.LocalizeUtils;

public class OwnSlotValueQuery implements VisitableQuery {
  private Slot slot;
  private String expr;
  private int maxMatches;
  
  public OwnSlotValueQuery(Slot slot, String expr, int maxMatches) {
    this.slot = slot;
    this.expr = expr;
    this.maxMatches = maxMatches;
  }
  
  public void accept(QueryVisitor visitor) {
      visitor.visit(this);
  }

  public String getExpr() {
    return expr;
  }

  public Slot getSlot() {
    return slot;
  }
  
  public int getMaxMatches() {
      return maxMatches;
  }

  public void localize(KnowledgeBase kb) {
    LocalizeUtils.localize(slot, kb);
  }
  
  @Override
  public String toString() {
	return "OwnSlotValueQuery: slot=" + (slot != null ? slot.getBrowserText() : "null") + ", expression=" + expr;
  }

}
