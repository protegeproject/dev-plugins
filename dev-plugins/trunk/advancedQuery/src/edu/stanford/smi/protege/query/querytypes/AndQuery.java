package edu.stanford.smi.protege.query.querytypes;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;

public class AndQuery implements Query, Serializable {
  public Collection<Query> conjuncts;
  
  
  public AndQuery(Collection<Query> conjuncts) {
    this.conjuncts = conjuncts;
  }

  public Collection<Query> getConjuncts() {
    return conjuncts;
  }

  public void localize(KnowledgeBase kb) {
    for (Query q : conjuncts) {
      q.localize(kb);
    }
  }

  @Override
  public String toString() {
	StringBuffer buffer = new StringBuffer();
	for (Iterator iter = conjuncts.iterator(); iter.hasNext();) {
	  Query query = (Query) iter.next();
	  buffer.append(query.toString());
	}
	return "AndQuery { " + buffer.toString() + " }";
  }
  
}
