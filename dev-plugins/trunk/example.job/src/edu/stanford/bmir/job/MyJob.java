package edu.stanford.bmir.job;

import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.util.LocalizeUtils;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.ProtegeJob;
import edu.stanford.smi.protegex.owl.model.OWLClass;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class MyJob extends ProtegeJob {
    private static final long serialVersionUID = 6634896991711144325L;

    public static final Logger LOGGER = Log.getLogger(MyJob.class);
    
    private OWLClass cls;

    public MyJob(OWLModel model, OWLClass cls) {
        super(model);
        this.cls = cls;
    }
    
    public Object run() throws ProtegeException {
        boolean onServer = getKnowledgeBase().getProject().isMultiUserServer();
        int count = cls.getSubclassCount();
        if (onServer)  {
            LOGGER.info("Client with example job plugin installed has contacted the server asking about a class: " + cls.getBrowserText());
            LOGGER.info("This class has " + count + " subclasses.  Passing the information back to the client.");
        }
        else {
            LOGGER.info("Protege client is running standalone.  Executing job on client");
        }
        return count;
    }
    
    @Override
    public OWLModel getKnowledgeBase() {
        return (OWLModel) super.getKnowledgeBase();
    }
    
    
    @Override
    public void localize(KnowledgeBase kb) {
        super.localize(kb);
        LocalizeUtils.localize(cls, kb);
    }

}
