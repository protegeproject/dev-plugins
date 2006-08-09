package edu.stanford.smi.protege.query;

import edu.stanford.smi.protege.exception.ProtegeException;
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
    SimpleFrameStore fs = (SimpleFrameStore) ((DefaultKnowledgeBase) getKnowledgeBase()).getTerminalFrameStore();
    NarrowFrameStore nfs = fs.getHelper();
    QueryNarrowFrameStoreHandler qnfs = new QueryNarrowFrameStoreHandler(nfs);
    fs.setHelper(qnfs.getNarrowFrameStore());
    return new Boolean(true);
  }

}
