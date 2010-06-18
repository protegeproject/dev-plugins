package simulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.server.RemoteProjectManager;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.model.OWLObjectProperty;

public abstract class AbstractRobot implements Robot {
    private static Logger log = Log.getLogger(AbstractRobot.class);
    
    public final static String THREAD_COUNT_PROP = "threads";
    public final static String RUNS_PER_MEASUREMENT_PROP = "runsPerMeasurement";
    public final static String CLASS_PROP = "class";
    
    public final static String NAME_PROP = "name";
    public final static String PASSWORD_PROP = "password";

    public final static String DESCEND_PROBABILITY_PROP = "descend.probability";
    
    
    private static int counter = 0;
    private int mycount;
    
    private static Random r = new Random();
    

    private KnowledgeBase kb;
    private Properties properties;
    
    private List<Integer> depths = new ArrayList<Integer>();
    
    
    public AbstractRobot(Properties p) {
        this.properties = p;
        mycount = counter++;
    }
    
    public abstract void run();

    public void login(String hostname, int port, String projectName) {
        RemoteProjectManager rpm = RemoteProjectManager.getInstance();
        Project project = rpm.getProject(hostname + ":" + port, 
                                         properties.getProperty(NAME_PROP), 
                                         properties.getProperty(PASSWORD_PROP), 
                                         projectName, 
                                         true);
        kb = project.getKnowledgeBase();
    }
    
    public void logout() {
        reportStats();
        kb.dispose();
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public String toString() {
        return getProperty(NAME_PROP) + "<" + mycount + ">";
    }
    
    public KnowledgeBase getKnowledgeBase() {
    	return kb;
    }
    
    /* ------------------------------------------------------
     * Utilities
     */

    private void reportStats() {
        if (log.isLoggable(Level.FINE)) {
            log.fine("Reporting stats for robot = " + toString());
            if (depths.isEmpty()) return;
            int totalDepth = 0;
            for (int depth : depths) {
                totalDepth += depth;
            }
            log.fine("Average depth descended looking for classes = " +
                     totalDepth / depths.size());
        }
    }
    
    public OWLNamedClass chooseClass(OWLModel om) {
        int depth = 0;
        OWLNamedClass top = om.getOWLThingClass();
        float descentProbability = Float.parseFloat(getProperty(DESCEND_PROBABILITY_PROP));
        do {
            Collection subclasses  = top.getSubclasses(false);
            int count = 0;
            for (Object o : subclasses) {
                if (realOWLNamedClass(o)) {
                    count++;
                }
            }
            if (count == 0) {
                depths.add(depth);
                return top;
            }
            depth++;
            for (Object o : subclasses) {
                if (realOWLNamedClass(o)) {
                    count--;
                    if (count == 0) {
                        top = (OWLNamedClass) o;
                    } 
                    else if (r.nextInt(count) == 0) {
                        top = (OWLNamedClass) o;
                        break;
                    }
                }
            }
        } while (withProbability(descentProbability));
        depths.add(depth);
        return top;
    }
    
    private boolean realOWLNamedClass(Object o) {
        if (o instanceof OWLNamedClass) {
            OWLNamedClass cls =(OWLNamedClass) o;
            try {
                return !cls.isSystem();
            } catch (Exception e) {
                System.out.println("Frame with id = " + cls.getFrameID().getName() +
                                    " refused to give its name");
                return false;
            }
        }
        return false;
    }

    public OWLObjectProperty chooseObjectProperty(OWLModel  om) {
        int count = 0;
        for (Object o : om.getRDFProperties()) {
            if (o instanceof OWLObjectProperty) {
                count++;
            }
        }
        for (Object o : om.getRDFProperties()) {
            if (o instanceof OWLObjectProperty) {
                count--;
                if (count == 0) {
                    return (OWLObjectProperty) o;
                }
                else if (r.nextInt(count) == 0) {
                    return (OWLObjectProperty) o;
                }
            }
        }
        throw new RuntimeException("Programmer error");
    }
    
    public boolean withProbability(float  p) {
        return r.nextFloat() < p;
    }
}
