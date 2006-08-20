package edu.stanford.smi.protege.query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLClass;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;

public class SearchTest extends TestCase {
  private static transient Logger log  = Log.getLogger(SearchTest.class);
  
  /* ----------------------------------------------------------------------
   * Utility functions
   */
  
  public static OWLModel getOWLModel() {
    List errors = new ArrayList();  
    Project project = new Project("advancedQuery/junit/projects/Pizza.pprj", errors);
    checkErrors(errors);
    OWLModel om = (OWLModel) project.getKnowledgeBase();
    new InstallNarrowFrameStore(om).execute();
    return om;
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
    if (log.isLoggable(Level.FINE)) {
      log.fine("basic search test");
    }
    OWLModel om = getOWLModel();
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A class to demonstrate mistakes made", "IceCream", true);
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate mistakes made", "IceCream", true);
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate mistaques made", "IceCream", true);
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate missedakes made", "IceCream", true);
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate mistaches made", "IceCream", false);
    Log.getLogger().info("Done");
  }
  
  public static void testDelete() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("delete phonetic search test");
    }
    OWLModel om = getOWLModel();
    om.getOWLNamedClass("IceCream").delete();
    checkSearch(om, om.getRDFProperty("rdfs:comment"), "A class to demonstrate mistakes made", "IceCream", false);
  }
  
  public static void testSetValues() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("set values search test");
    }
    OWLModel om = getOWLModel();
    OWLClass iceCream = om.getOWLNamedClass("IceCream");
    RDFProperty comment = om.getRDFProperty("rdfs:comment");
    iceCream.setPropertyValue(comment, "This is a real klass.  Don't make derogatory comments.");
    checkSearch(om, comment, "class derogatory", "IceCream", true);
  }
  
  public static void testAddValues() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("add values search test");
    }
    OWLModel om = getOWLModel();
    OWLClass iceCream = om.getOWLNamedClass("IceCream");
    RDFProperty comment = om.getRDFProperty("rdfs:comment");
    iceCream.addPropertyValue(comment, "But this class doesn't fit well with pizza.  And the comment wasn't derogatory.");
    checkSearch(om, comment, "duznt derogatory", "IceCream", true);
  }

}
