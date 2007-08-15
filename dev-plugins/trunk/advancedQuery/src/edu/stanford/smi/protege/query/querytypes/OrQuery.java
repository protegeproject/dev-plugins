package edu.stanford.smi.protege.query.querytypes;

import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;

public class OrQuery implements VisitableQuery {
  
  Collection<VisitableQuery> disjuncts;

  public OrQuery(Collection<VisitableQuery> disjuncts) {
    this.disjuncts = disjuncts;
  }
  
  public void accept(QueryVisitor visitor) {
      visitor.visit(this);
  }

  public Collection<VisitableQuery> getDisjuncts() {
    return disjuncts;
  }

  public void localize(KnowledgeBase kb) {
    for (Query q : disjuncts) {
      q.localize(kb);
    }
  }

  @Override
  public String toString() {
	StringBuffer buffer = new StringBuffer();
	for (Iterator iter = disjuncts.iterator(); iter.hasNext();) {
	  Query query = (Query) iter.next();
	  buffer.append(query.toString());
	}
	return "OrQuery { " + buffer.toString() + " }";
  }
  
}
