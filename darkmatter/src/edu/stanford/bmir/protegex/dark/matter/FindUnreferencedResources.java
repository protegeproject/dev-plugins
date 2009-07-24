package edu.stanford.bmir.protegex.dark.matter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.model.Reference;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;

public abstract class FindUnreferencedResources implements Callable<Integer> {
    private static Logger log = Log.getLogger(FindUnreferencedResources.class);
    private OWLModel model;
    private Set<RDFResource> traversed = new HashSet<RDFResource>();
    private int topLevelCount;
    
    public FindUnreferencedResources(OWLModel model) {
        this.model = model;
    }
    
    protected abstract void onFind(RDFResource resource);
    protected abstract void onFinish();
    protected abstract void setFramesToFinish(int count);
    protected abstract void setProgress(int progress);
    
    @SuppressWarnings({ "unchecked" })
    public Integer call() {
        try {
            Collection cpFrames = model.getRDFResources();
            traversed.clear();

            int progress = 0;
            int size = cpFrames.size();
            log.info("Searching through " + size + " frames");
            setFramesToFinish(size);
            for (Object o : cpFrames) {
                setProgress(progress++);
                if (Thread.interrupted()) {
                    break;
                }
                if (o instanceof RDFResource && ((RDFResource)  o).isAnonymous()) {
                    RDFResource  resource  = (RDFResource) o;
                    lookForContainingUnreferencedClass(resource);
                }
            }
            return topLevelCount;
        }
        catch (Throwable t) {
            log.log(Level.WARNING, "Exception caught in search thread", t);
            return topLevelCount;
        }
        finally {
            onFinish();
            log.info("Search for anonymous resources completed");
        }
    }
    
    public void lookForContainingUnreferencedClass(RDFResource resource) {
        if (traversed.contains(resource) || resource.isDeleted()) {
            if (log.isLoggable(Level.FINER)) {
                log.finer("Resource already searched or deleted");
            }
            return;
        }
        traversed.add(resource);
        boolean referencesFound = false;
        if (log.isLoggable(Level.FINER)) {
            log.finer("Examining resource " + resource.getBrowserText());
        }
        for (Object o : resource.getReferences()) {
            Reference reference = (Reference) o;
            if (!(reference.getFrame() instanceof  RDFResource)
                    || !(reference.getSlot() instanceof RDFProperty)
                    || reference.isTemplate()) {
                continue; // only do owl stuff;
            }
            else if (reference.getSlot().equals(model.getRDFSSubClassOfProperty()) 
                      || reference.getSlot().equals(model.getOWLEquivalentClassProperty())) {
                if (log.isLoggable(Level.FINER)) {
                   if (((RDFResource) reference.getFrame()).isAnonymous()) {
                       log.finer("Resource " + resource.getBrowserText() + "part of gci");
                   }
                   else {
                       log.finer("Resource " + resource.getBrowserText() 
                                    + " related to named class " + ((RDFResource)reference.getFrame()).getBrowserText());
                   }
                }
                return; // a gci or subclass statement is good
            }
            else {
                referencesFound = true;
                RDFResource referringResource = (RDFResource) reference.getFrame();
                if (log.isLoggable(Level.FINER)) {
                    log.finer("found reference using property: " + reference.getSlot().getBrowserText());
                    log.finer("referring resource: " + reference.getFrame().getBrowserText());
                }
                if  (referringResource.isAnonymous()) {
                    lookForContainingUnreferencedClass(referringResource);
                }
                else {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("Resource " + resource.getBrowserText() 
                                    + " directly referenced by " + referringResource.getBrowserText() 
                                    + " by  property " + reference.getSlot().getBrowserText());
                    }
                    return;  // doesn't happen?
                }
            }
        }
        if (resource.getRDFTypes().contains(model.getOWLAllDifferentClass())) {
            return;
        }
        if (!referencesFound) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Found resource " + resource.getBrowserText() + " not directly referenced by anyone");
            }
            onFind(resource);
            topLevelCount++;
        }
        return;
    }
}
