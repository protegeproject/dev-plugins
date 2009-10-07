package uk.ac.man.cs.mig.coode.protege.id;

import java.util.Collection;
import java.util.logging.Logger;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protege.model.framestore.FrameStoreAdapter;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.impl.AbstractOWLModel;

/**
 * @author Nick Drummond, Medical Informatics Group, University of Manchester
 *         03-Nov-2005
 */
public class IdFrameStore extends FrameStoreAdapter{
    private static Logger log = Log.getLogger(IdFrameStore.class);

    private long current = 0;

    private OWLModel owlModel;
    private String lastUnallocatedName;
    private String prefix;

    public IdFrameStore(OWLModel owlModel, String prefix) {
        this.owlModel = owlModel;
        this.prefix = prefix;
    }

    @Override
    public Frame getFrame(String name) {
        Frame f = super.getFrame(name);
        if (f == null) {
            reverseEngineerCreateNewResourceName(name);
        }
        return f;
    }
    
    /*
     * This is obnoxious but I really don't like the idea of always ignoring the 
     * name in the createCls call.
     */
    private void reverseEngineerCreateNewResourceName(String name) {
        try {  // to reverse engineer the createNewResourceName detecting unspecified names 
            if (name != null) {
                int i = name.indexOf(AbstractOWLModel.DEFAULT_CLASS_NAME);
                if (i > 0 && name.length() > i + AbstractOWLModel.DEFAULT_CLASS_NAME.length() + 1) {
                    String counterName = name.substring(i + AbstractOWLModel.DEFAULT_CLASS_NAME.length() + 1);
                    try {
                        Integer.parseInt(counterName);
                        lastUnallocatedName = name;
                    }
                    catch (NumberFormatException nfe) {
                        ;
                    }
                }
            }
        }
        catch (Throwable t) {
            log.warning("Id frame store malfunctioned, continuing...");
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Cls createCls(FrameID id, Collection directTypes,
                         Collection directSuperclasses, boolean loadDefaults) {
        Cls cls;
        if (id.getName() == null || id.getName().equals(lastUnallocatedName)) {
            id = generateNextId();
            cls = super.createCls(id, directTypes, directSuperclasses, loadDefaults);
        }
        else {
            cls = super.createCls(id, directTypes, directSuperclasses, loadDefaults);
        }
        return cls;
    }
    

    private FrameID generateNextId() {
        String id;
        do{
            id = prefix + current++;
        } while (null != owlModel.getOWLNamedClass(id));
        return new FrameID(id);
    }
}
