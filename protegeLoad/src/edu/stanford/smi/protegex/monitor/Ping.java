package edu.stanford.smi.protegex.monitor;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.util.ProtegeJob;

public class Ping extends ProtegeJob {
    private static final long serialVersionUID = 2734727595517299830L;

    public Ping(KnowledgeBase kb) {
        super(kb);
    }
    
    @Override
    public Object run() throws ProtegeException {
        return null;
    }

}
