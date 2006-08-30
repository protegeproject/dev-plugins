package edu.stanford.smi.protege.query.querytypes;

import java.util.Collection;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;

public class AndQuery implements Query {
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


}
