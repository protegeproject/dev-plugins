package simulator.robots;

import java.util.Properties;
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
        OWLModel om = (OWLModel) getKnowledgeBase();
        OWLNamedClass c = chooseClass(om);
        FrameWithBrowserText cwt = new FrameWithBrowserText(c, c.getBrowserText());
        ClassTreeWithBrowserTextRoot root = new ClassTreeWithBrowserTextRoot(om.getOWLThingClass(), false);
        root.setModel(new LazyTreeModel(root));
        ClassTreeWithBrowserTextNode node = new ClassTreeWithBrowserTextNode(root, cwt);
        node.children();
        long interval = System.currentTimeMillis() - start;
        if (slowOperation != 0 && interval > slowOperation) {
            logger.info("Getting Children of " + c + " took " + interval);
        }
    }

}
