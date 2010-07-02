package simulator.robots;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import simulator.AbstractRobot;
import edu.stanford.smi.protege.util.FrameWithBrowserText;
import edu.stanford.smi.protege.util.LazyTreeModel;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.ui.cls.ClassTreeWithBrowserTextNode;
import edu.stanford.smi.protegex.owl.ui.cls.ClassTreeWithBrowserTextRoot;

public class ClassTreeWithBrowserTextRobot extends AbstractRobot {
    private static Logger logger = Log.getLogger(ClassTreeWithBrowserTextRobot.class);
    
    public static final String SLOW_OPERATION_PROPERTY="log.slow.operation";
    private long slowOperation = 0;
    
    public ClassTreeWithBrowserTextRobot(Properties p) {
        super(p);
        try {
            slowOperation = Integer.parseInt(getProperty(SLOW_OPERATION_PROPERTY));
        }
        catch (Throwable t) {
            slowOperation = 0;
        }
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        MonitorThreadPerformanceRunnable monitor = null;
        if (slowOperation  != 0) {
        	monitor = new MonitorThreadPerformanceRunnable(Thread.currentThread());
        	new Thread(monitor, "Monitoring GetChildren performance").start();
        }
        
        OWLNamedClass c = doTheActualWork();
        	
        long interval = System.currentTimeMillis() - start;
        if (monitor != null) {
        	monitor.setDone(true);
        }
        if (slowOperation != 0 && interval > slowOperation) {
            logger.info("Getting Children of " + c + " took " + interval);
        }
    }
    
    private OWLNamedClass doTheActualWork() {
        OWLModel om = (OWLModel) getKnowledgeBase();
        OWLNamedClass c = chooseClass(om);
        FrameWithBrowserText cwt = new FrameWithBrowserText(c, c.getBrowserText());
        ClassTreeWithBrowserTextRoot root = new ClassTreeWithBrowserTextRoot(om.getOWLThingClass(), false);
        root.setModel(new LazyTreeModel(root));
        ClassTreeWithBrowserTextNode node = new ClassTreeWithBrowserTextNode(root, cwt);
        node.children();
        return c;
    }
    
    private class MonitorThreadPerformanceRunnable implements Runnable {
    	private boolean done = false;
    	private Thread toMonitor;
    	
    	public MonitorThreadPerformanceRunnable(Thread threadToMonitor) {
    		toMonitor = threadToMonitor;
    	}
    	
    	public void run() {
    		synchronized (this) {
    			try {
    				wait(slowOperation);
    			}
    			catch (InterruptedException ie) {
    				logger.log(Level.WARNING, "shouldn't", ie);
    			}
    			if (!done) {
    	            logger.info("Getting Children is taking a long time for thread " + toMonitor);
    			}
    		}
    	}
    	
    	public synchronized void setDone(boolean done) {
			this.done = done;
			if (done) {
				notifyAll();
			}
		}
    }

}
