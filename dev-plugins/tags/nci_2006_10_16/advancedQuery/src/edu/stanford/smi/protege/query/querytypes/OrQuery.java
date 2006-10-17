package edu.stanford.smi.protege.query.querytypes;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;

public class OrQuery implements Query, Serializable {
  
  Collection<Query> disjuncts;

  public OrQuery(Collection<Query> disjuncts) {
    this.disjuncts = disjuncts;
  }

  public Collection<Query> getDisjuncts() {
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
