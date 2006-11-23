package edu.stanford.smi.protegex.logctl;

import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.util.ProtegeJob;

public class GetLevelJob extends ProtegeJob {
    private static final long serialVersionUID = 2404422840444879187L;
    private String location;
    
    public GetLevelJob(KnowledgeBase kb, String location) {
        super(kb);
        this.location = location;
    }

    @Override
    public Object run() throws ProtegeException {
        return new LocalizableLevel(Logger.getLogger(location).getLevel());
    }

}
