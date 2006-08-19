package edu.stanford.smi.protege.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class SearchTest extends TestCase {
  
  public static void checkErrors(List errors) {
    for (Object error : errors) {
      if (error instanceof Throwable) {
        ((Throwable) error).printStackTrace();
      } else {
        System.out.println("Error = " + error);
      }
    }
  }

  /**
   * @param args
   */
  public static void test01() {   
    List errors = new ArrayList();  
    Project project = new Project("advancedQuery/junit/projects/Pizza.pprj", errors);
    checkErrors(errors);
    OWLModel om = (OWLModel) project.getKnowledgeBase();
    new InstallNarrowFrameStore(om).execute();
    
    PhoneticQuery pq = new PhoneticQuery(null, "A class to demonstrate mistakes made");
    for (Frame frame : om.getHeadFrameStore().executeQuery(pq)) {
      System.out.println("Found frame = " + frame);
    }
    System.out.println("Done");
  }
}
