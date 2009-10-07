package uk.ac.man.cs.mig.coode.protege.id;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.Application;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.plugin.ProjectPluginAdapter;
import edu.stanford.smi.protege.ui.ProjectMenuBar;
import edu.stanford.smi.protege.ui.ProjectToolBar;
import edu.stanford.smi.protege.ui.ProjectView;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;

/**
 * @author Nick Drummond, Medical Informatics Group, University of Manchester
 *         03-Nov-2005
 */
public class AutoIdPlugin extends ProjectPluginAdapter{
    private static final Logger log = Log.getLogger(AutoIdPlugin.class);

    public void afterShow(ProjectView projectView, ProjectToolBar projectToolBar, ProjectMenuBar projectMenuBar) {

        KnowledgeBase kb = projectView.getProject().getKnowledgeBase();

        if (kb instanceof OWLModel) {
            OWLModel owlModel = (OWLModel) kb;

            InputStream propsStream = this.getClass().getResourceAsStream("/default.properties");
            Properties props = new Properties();
            try {
                props.load(propsStream);
                propsStream.close();

                String prefix = props.getProperty("id.prefix");
                String ns;
                if ((ns = owlModel.getNamespaceManager().getDefaultNamespace()) != null) {
                    prefix = ns + prefix;
                }
                else {
                    prefix = owlModel.getDefaultOWLOntology().getName() + "#" + prefix;
                }

                IdFrameStore fs = new IdFrameStore(owlModel, prefix);
                kb.insertFrameStore(fs);

                log.info("ACTIVATED AUTO ID prefix: " + prefix);
            }
            catch (IOException e) {
                log.log(Level.WARNING, "Exception caught", e);
            }
        }
    }

    public static void main(String[] args) {
        Application.main(args);
    }
}
