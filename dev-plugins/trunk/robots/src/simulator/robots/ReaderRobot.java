package simulator.robots;

import java.util.Properties;
import java.util.logging.Logger;

import simulator.AbstractRobot;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;


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

    @Override
    public void run() {
        OWLModel om = (OWLModel) getKnowledgeBase();
        OWLNamedClass c = chooseClass(om);
        long now = System.currentTimeMillis();
        if (displayResultsInterval != 0 && (now - startReportingInterval > displayResultsInterval)) {
            log.info("Found class " + c);
            startReportingInterval = now;
        }
    }
    
}