package edu.stanford.bmir.job;

import java.util.logging.Logger;

import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.server.RemoteProjectManager;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class Main {
    public final static Logger LOGGER = Log.getLogger(Main.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        RemoteProjectManager rpm = RemoteProjectManager.getInstance();
        Project p = rpm.getProject("localhost:5100", "Timothy Redmond", "troglodyte", "Pizza", true);
        OWLModel model = (OWLModel) p.getKnowledgeBase();
        int count = (Integer) (new MyJob(model, model.getOWLNamedClass("CheeseyPizza")).execute());
        LOGGER.info("The server tells me that CheeseyPizza has " + count + " subclasses.");
    }

}
