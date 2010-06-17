package simulator.robots;

import java.util.Properties;

import simulator.AbstractRobot;

import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.model.OWLObjectProperty;
import edu.stanford.smi.protegex.owl.model.OWLSomeValuesFrom;

public class WriterRobot extends AbstractRobot {

    public WriterRobot(Properties p) {
        super(p);
    }
    
    public void run() {
        OWLModel om = (OWLModel) getKnowledgeBase();
        boolean success = false;
        OWLNamedClass cls1 = chooseClass(om);
        OWLNamedClass cls2 = chooseClass(om);
        OWLObjectProperty p = chooseObjectProperty(om);
        while (!success) {
            try {
                om.beginTransaction("test transaction");
                OWLSomeValuesFrom restriction = om.createOWLSomeValuesFrom();
                restriction.setOnProperty(p);
                restriction.setFiller(cls2);
                cls1.addEquivalentClass(restriction);
                cls1.removeEquivalentClass(restriction);
                om.commitTransaction();
                success = true;
            }
            catch (Exception e) {
                om.rollbackTransaction();
                System.out.println("Transaction restarting " + e);
            }
        }
    }
}
