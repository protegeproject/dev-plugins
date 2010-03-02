package uk.ac.man.cs.mig.coode.protege.id;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.FrameStoreAdapter;
import edu.stanford.smi.protege.ui.ProjectManager;
import edu.stanford.smi.protege.ui.SetDisplaySlotPanel;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.ModalDialog;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.model.impl.AbstractOWLModel;
import edu.stanford.smi.protegex.owl.ui.widget.OWLUI;

/**
 * @author Nick Drummond, Medical Informatics Group, University of Manchester
 *         03-Nov-2005
 */
public class IdFrameStore extends FrameStoreAdapter{
    private static Logger log = Log.getLogger(IdFrameStore.class);

    private long current = 0;

    private String lastUnallocatedName;
    private boolean unique;
    private String ns;
    private String prefix;
    private int digits;
    private Slot displaySlot;
    
    public static void setAutoIdPreferences(OWLModel owlModel, Preferences preferences) {
        IdFrameStore frameStore = owlModel.getFrameStoreManager().getFrameStoreFromClass(IdFrameStore.class);
        if (frameStore != null) {
            owlModel.getFrameStoreManager().removeFrameStore(frameStore);
        }
        if (preferences.isEnabled()) {
            frameStore = new IdFrameStore(owlModel, preferences);
            owlModel.getFrameStoreManager().insertFrameStore(frameStore, 2);
        }
    }

    private IdFrameStore(OWLModel owlModel, Preferences preferences) {
        ns = owlModel.getNamespaceManager().getDefaultNamespace();
        if (ns == null) {
            ns = owlModel.getDefaultOWLOntology().getName();
            if (!ns.endsWith("/")) {
                ns = ns + "#";
            }
        }
        prefix = preferences.getPrefix();
        unique = preferences.isUniqueId();
        digits = preferences.getDigits();
        
        displaySlot = OWLUI.getCommonBrowserSlot(owlModel);
    }
    
    private void showSetDisplayNameDialog(Cls cls) {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(new JLabel("Display label for class: "));
        JTextField nameField = new JTextField();
        panel.add(nameField);
        Dimension d = new JLabel("My Name for a class").getPreferredSize();
        nameField.setPreferredSize(new Dimension((int) d.getWidth(), (int) (1.5 *  d.getHeight())));
        ModalDialog.showDialog(ProjectManager.getProjectManager().getCurrentProjectView(), 
                               panel, 
                               "Set Display Label", 
                               ModalDialog.MODE_CLOSE);
        String displayName = nameField.getText();
        if (displaySlot != null && displayName != null && displayName.length() != 0) {
            super.setDirectOwnSlotValues(cls, displaySlot, Collections.singleton(displayName));
        }
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
        if (EventQueue.isDispatchThread() &&
                (id.getName() == null || id.getName().equals(lastUnallocatedName))) {
            id = generateNextId();
            cls = super.createCls(id, directTypes, directSuperclasses, loadDefaults);
            showSetDisplayNameDialog(cls);
        }
        else {
            cls = super.createCls(id, directTypes, directSuperclasses, loadDefaults);
        }
        return cls;
    }
    

    private FrameID generateNextId() {
        String id;
        if (!unique) {
            do{
                String i = "" + current++;
                while (i.length() < digits) {
                    i = "0" + i;
                }
                id = ns + prefix + i;
            } while (null != super.getFrame(id));
        }
        else {
            id = ns + prefix + UUID.randomUUID().toString();
        }
        return new FrameID(id);
    }
}
