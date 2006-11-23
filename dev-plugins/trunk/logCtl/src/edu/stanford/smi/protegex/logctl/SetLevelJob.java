package edu.stanford.smi.protegex.logctl;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.util.ProtegeJob;

public class SetLevelJob extends ProtegeJob {
    private static final long serialVersionUID = -4419544989916872812L;
    
    private String location;
    private Level level;
    
    public SetLevelJob(KnowledgeBase kb, String location, Level level) {
        super(kb);
        this.location = location;
        this.level = level;
    }

    @Override
    public Object run() throws ProtegeException {
        Logger.getLogger(location).setLevel(level);
        return null;
    }

}
