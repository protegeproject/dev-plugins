package edu.stanford.bmir.protegex.dark.matter;

import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.ProgressMonitor;

import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;

public abstract class FindUnreferencedSwing extends FindUnreferencedResources {
    private static Logger log = Log.getLogger(FindUnreferencedSwing.class);
    private JComponent parent;
    private String description;
    private ProgressMonitor progressMonitor;

    public FindUnreferencedSwing(OWLModel owlModel, JComponent parent, String description) {
        super(owlModel);
        this.parent = parent;
        this.description = description;
    }

    @Override
    protected void setFramesToFinish(int count) {
        progressMonitor = new ProgressMonitor(parent, description, null, 0, count);
    }

    @Override
    protected void setProgress(int progress) {
        progressMonitor.setProgress(progress);
        if (progressMonitor.isCanceled()) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    protected void onFinish() {
        progressMonitor.close();
        progressMonitor = null;
    }
    
    public static void debugUnreferencedResources(OWLModel owlModel) {
        FindUnreferencedSwing finder = new FindUnreferencedSwing(owlModel, null, "Finding unreferenced resources") {
            @Override
            protected void onFind(RDFResource resource) {
                log.info("Found Resource: " + resource.getBrowserText());
            }
        };
        log.info("Finding Unreferenced Resource");
        finder.call();
    }



}
