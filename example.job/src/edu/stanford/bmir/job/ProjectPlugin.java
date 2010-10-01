package edu.stanford.bmir.job;

import java.util.Collection;
import java.util.Random;
import java.util.logging.Logger;

import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.plugin.ProjectPluginAdapter;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;

public class ProjectPlugin extends ProjectPluginAdapter {
    public static final Logger LOGGER = Log.getLogger(ProjectPlugin.class);
    
    private Random r = new Random();

    @Override
    public void afterLoad(Project p) {
        if (p.getKnowledgeBase() instanceof OWLModel && !p.isMultiUserServer()) {
            OWLModel model = (OWLModel) p.getKnowledgeBase();
            OWLNamedClass c = chooseClass(model, .5f);
            MyJob job = new MyJob(model, c);
            job.execute();
        }
    }
    
    public OWLNamedClass chooseClass(OWLModel om, float descentProbability) {
        int depth = 0;
        OWLNamedClass top = om.getOWLThingClass();
        do {
            Collection subclasses  = top.getSubclasses(false);
            int count = 0;
            for (Object o : subclasses) {
                if (realOWLNamedClass(o)) {
                    count++;
                }
            }
            if (count == 0) {
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
        } while (r.nextFloat() < descentProbability);
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
    
    
}
