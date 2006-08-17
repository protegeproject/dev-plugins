package edu.stanford.smi.protege.query;

import java.util.HashSet;
import java.util.Set;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.framestore.SimpleFrameStore;
import edu.stanford.smi.protege.util.ProtegeJob;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFNames;

public class InstallNarrowFrameStore extends ProtegeJob {
  public final static String RDF_LABEL = "rdf:label";
  public final static String RDF_COMMENT = "rdf:comment";
  
  public InstallNarrowFrameStore(KnowledgeBase kb) {
    super(kb);
  }

  @Override
  public Boolean run() throws ProtegeException {
    DefaultKnowledgeBase kb = (DefaultKnowledgeBase) getKnowledgeBase();
    SimpleFrameStore fs = (SimpleFrameStore) kb.getTerminalFrameStore();
    NarrowFrameStore nfs = fs.getHelper();
    
    QueryNarrowFrameStore qnfs = new QueryNarrowFrameStore(nfs, getSearchableSlots());
    fs.setHelper(qnfs);
    return new Boolean(true);
  }
  
  @SuppressWarnings("unchecked")
  public Set<Slot> getSearchableSlots() {
    DefaultKnowledgeBase kb = (DefaultKnowledgeBase) getKnowledgeBase();
    Set<Slot> slots = new HashSet<Slot>();
    if (kb instanceof OWLModel) {
      OWLModel owl = (OWLModel) kb;
      slots.addAll(owl.getOWLAnnotationProperties());
      slots.add((Slot) owl.createRDFSNamedClass(RDF_LABEL));
      slots.add((Slot) owl.createRDFSNamedClass(RDF_COMMENT));
      return slots;
    } else {
      slots.addAll(kb.getSlots());
    }
    return slots;
  }

}
