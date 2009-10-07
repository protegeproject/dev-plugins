package uk.ac.man.cs.mig.coode.protege.id;

import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.ui.ProtegeUI;
import edu.stanford.smi.protegex.owl.ui.actions.AbstractOWLModelAction;
import edu.stanford.smi.protegex.owl.ui.actions.OWLModelActionConstants;
import edu.stanford.smi.protegex.owl.ui.dialogs.ModalDialogFactory;

public class PreferencesAction extends AbstractOWLModelAction {


    public String getIconFileName() {
        return "AutoId Preferences";
    }


    public String getMenubarPath() {
        return OWL_MENU + PATH_SEPARATOR + OWLModelActionConstants.PREFERENCES_GROUP;
    }


    public String getName() {
        return "AutoId Preferences...";
    }


    public String getToolbarPath() {
        return null;
    }


    public void run(OWLModel owlModel) {
        PreferencesPanel panel = new PreferencesPanel(owlModel);
        ProtegeUI.getModalDialogFactory().showDialog(ProtegeUI.getTopLevelContainer(owlModel.getProject()), panel,
                "AutoId Preferences", ModalDialogFactory.MODE_CLOSE);
    }
}
