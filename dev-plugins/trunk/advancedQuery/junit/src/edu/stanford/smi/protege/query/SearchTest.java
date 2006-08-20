package edu.stanford.smi.protege.query;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class SearchTest extends TestCase {
  
  /* ----------------------------------------------------------------------
   * Utility functions
   */
  
  public static OWLModel getOWLModel() {
    List errors = new ArrayList();  
    Project project = new Project("advancedQuery/junit/projects/Pizza.pprj", errors);
    checkErrors(errors);
    return (OWLModel) project.getKnowledgeBase();
  }
  
  public static void checkErrors(List errors) {
    for (Object error : errors) {
      if (error instanceof Throwable) {
        ((Throwable) error).printStackTrace();
      } else {
        System.out.println("Error = " + error);
      }
    }
  }
  
  public static void checkSearch(OWLModel om, Slot slot, String search, String frameName, boolean succeed) {
    boolean found = false;
    PhoneticQuery pq = new PhoneticQuery(slot, search);
    for (Frame frame : om.getHeadFrameStore().executeQuery(pq)) {
      assertTrue(frame.getName().equals(frameName));
      found = true;
    }
    assertTrue(succeed == found);
  }
  
  /* ---------------------------------------------------------------------
   * Tests
   */


  public static void testBasicSearch() {   
    OWLModel om = getOWLModel();
    new InstallNarrowFrameStore(om).execute();
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A class to demonstrate mistakes made", "IceCream", true);
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate mistakes made", "IceCream", true);
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate mistaques made", "IceCream", true);
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate missedakes made", "IceCream", true);
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate mistaches made", "IceCream", false);
    Log.getLogger().info("Done");
  }
  
  public static void testDelete() {
    OWLModel om = getOWLModel();
    new InstallNarrowFrameStore(om).execute();
    om.getOWLNamedClass("IceCream").delete();
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A class to demonstrate mistakes made", "IceCream", true);
  }
  

}
