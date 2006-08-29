package edu.stanford.smi.protege.query.querytypes;

import java.util.Collection;

import edu.stanford.smi.protege.model.query.Query;

public class OrQuery implements Query {
  
  Collection<Query> disjuncts;

  public OrQuery(Collection<Query> disjuncts) {
    this.disjuncts = disjuncts;
  }

  public Collection<Query> getDisjuncts() {
    return disjuncts;
  }

}
