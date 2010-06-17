package simulator.robots;

import java.util.Properties;

import edu.stanford.smi.protegex.owl.model.OWLModel;
import simulator.AbstractRobot;


public class ReaderRobot extends AbstractRobot {
    
    public ReaderRobot(Properties p) {
        super(p);
    }

    @Override
    public void run() {
        OWLModel om = (OWLModel) getKnowledgeBase();
        chooseClass(om);
    }
    
}