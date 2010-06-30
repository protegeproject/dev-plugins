package simulator.robots;

import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;

import simulator.AbstractRobot;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.model.RDFResource;


public class ReaderRobot extends AbstractRobot {
    private static final Logger log = Log.getLogger(ReaderRobot.class);
    
    public static final String DISPLAY_CLASS_INTERVAL_PROPERTY="display.result.interval";
    private long displayResultsInterval = 0;
    private long startReportingInterval;
    
    public ReaderRobot(Properties p) {
        super(p);
        try {
            displayResultsInterval = Integer.parseInt(getProperty(DISPLAY_CLASS_INTERVAL_PROPERTY));
        }
        catch (Throwable t) {
            displayResultsInterval = 0;
        }
        startReportingInterval = System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        OWLModel om = (OWLModel) getKnowledgeBase();
        OWLNamedClass c = chooseClass(om);
        Collection equivs = c.getEquivalentClasses();
        String equivBrowserText = null;
        if (equivs != null && !equivs.isEmpty()) {
            for (Object o : equivs) {
                if (o instanceof Frame) {
                    equivBrowserText = ((Frame) o).getBrowserText();
                }
            }
        }
        Collection supers = c.getSuperclasses(false);
        String superBrowserText = null;
        if (supers != null && !supers.isEmpty()) {
            for (Object o : supers) {
                if (o instanceof RDFResource && ((RDFResource) o).isAnonymous()) {
                    superBrowserText = ((RDFResource) o).getBrowserText();
                }
            }
        }
        long now = System.currentTimeMillis();
        if (displayResultsInterval != 0 && (now - startReportingInterval > displayResultsInterval)) {
            log.info("Found class " + c);
            if (equivBrowserText != null) {
                log.info("Defined!");
            }
            else if (superBrowserText != null) {
                log.info("Has necessary and sufficient!");
            }
            startReportingInterval = now;
        }
    }
    
}