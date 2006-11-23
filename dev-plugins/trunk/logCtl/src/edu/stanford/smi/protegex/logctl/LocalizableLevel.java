package edu.stanford.smi.protegex.logctl;

import java.io.Serializable;
import java.util.logging.Level;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Localizable;

public class LocalizableLevel implements Serializable, Localizable {
    private static final long serialVersionUID = -4010738032456177067L;
    
    private Level level;
    
    public LocalizableLevel(Level level) {
        this.level = level;
    }
    
    public Level getLevel() {
        return level;
    }

    public void localize(KnowledgeBase kb) {
        ;
    }

}
