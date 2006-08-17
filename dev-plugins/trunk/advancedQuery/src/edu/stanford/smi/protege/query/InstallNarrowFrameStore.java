package edu.stanford.smi.protege.query;

import java.util.HashSet;
import java.util.Set;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.framestore.SimpleFrameStore;
import edu.stanford.smi.protege.util.ProtegeJob;

public class InstallNarrowFrameStore extends ProtegeJob {
  
  public InstallNarrowFrameStore(KnowledgeBase kb) {
    super(kb);
  }

  @Override
  public Boolean run() throws ProtegeException {
    DefaultKnowledgeBase kb = (DefaultKnowledgeBase) getKnowledgeBase();
    SimpleFrameStore fs = (SimpleFrameStore) kb.getTerminalFrameStore();
    NarrowFrameStore nfs = fs.getHelper();
    Object annotation = kb.getFrame("owl:AnnotationProperty");
    Set<Cls> slotClasses = null;
    if (annotation != null && annotation instanceof Cls) {
      slotClasses = new HashSet<Cls>();
      slotClasses.add((Cls) annotation);
    }
    QueryNarrowFrameStore qnfs = new QueryNarrowFrameStore(nfs, slotClasses);
    fs.setHelper(qnfs);
    return new Boolean(true);
  }

}
