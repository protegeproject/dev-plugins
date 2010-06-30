package simulator.robots;

import java.util.Properties;

import simulator.AbstractRobot;
import edu.stanford.smi.protege.util.FrameWithBrowserText;
import edu.stanford.smi.protege.util.LazyTreeModel;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.ui.cls.ClassTreeWithBrowserTextNode;
import edu.stanford.smi.protegex.owl.ui.cls.ClassTreeWithBrowserTextRoot;

public class ClassTreeWithBrowserTextRobot extends AbstractRobot {
    
    public ClassTreeWithBrowserTextRobot(Properties p) {
        super(p);
    }

    @Override
    public void run() {
        OWLModel om = (OWLModel) getKnowledgeBase();
        OWLNamedClass c = chooseClass(om);
        FrameWithBrowserText cwt = new FrameWithBrowserText(c, c.getBrowserText());
        ClassTreeWithBrowserTextRoot root = new ClassTreeWithBrowserTextRoot(om.getOWLThingClass(), false);
        root.setModel(new LazyTreeModel(root));
        ClassTreeWithBrowserTextNode node = new ClassTreeWithBrowserTextNode(root, cwt);
        node.children();
    }

}
