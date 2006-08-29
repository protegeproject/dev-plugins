package edu.stanford.smi.protege.query.querytypes;

import java.util.Collection;

import edu.stanford.smi.protege.model.query.Query;

public class AndQuery implements Query {
  public Collection<Query> conjuncts;
  
  
  public AndQuery(Collection<Query> conjuncts) {
    this.conjuncts = conjuncts;
  }

  public Collection<Query> getConjuncts() {
    return conjuncts;
  }


}
