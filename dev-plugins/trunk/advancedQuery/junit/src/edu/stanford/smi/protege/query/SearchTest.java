package edu.stanford.smi.protege.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.OWLRestrictionQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;
import edu.stanford.smi.protege.query.querytypes.OwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.PhoneticQuery;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLClass;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFProperty;

public class SearchTest extends TestCase {
  private static transient Logger log  = Log.getLogger(SearchTest.class);
  
  /* ----------------------------------------------------------------------
   * Utility functions
   */
  
  @SuppressWarnings("unchecked")
  public static OWLModel getOWLModel() {
    List errors = new ArrayList();  
    Project project = new Project("advancedQuery/junit/projects/Pizza.pprj", errors);
    checkErrors(errors);
    OWLModel om = (OWLModel) project.getKnowledgeBase();
    Set<Slot> slots = (Set<Slot>) new InstallNarrowFrameStore(om).execute();
    new IndexOntologies(om).execute();
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
  
  public static void checkSearch(OWLModel om, Query query, String frameName, boolean succeed) {
    boolean found = false;
    for (Frame frame : om.executeQuery(query)) {
      assertTrue(frame.getName().equals(frameName));
      found = true;
    }
    assertTrue(succeed == found);
  }
  
  public static void checkPhoneticSearch(OWLModel om, Slot slot, String search, String frameName, boolean succeed) {
    checkSearch(om, new PhoneticQuery(slot, search), frameName, succeed);
  }
  
  /* ---------------------------------------------------------------------
   * Tests
   */
  
  public static void testOrQuery() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("testOrQuery");
    }
    OWLModel om = getOWLModel();
    RDFProperty comment = om.getRDFProperty("rdfs:comment");
    RDFProperty label   = om.getRDFProperty("rdfs:label");
    List<Query> queries = new ArrayList<Query>();
    Query q1 = new OwnSlotValueQuery(comment, "*at least 1 cheese*");
    queries.add(q1);
    Query q2 = new PhoneticQuery(label, "BaseEspzza");
    queries.add(q2);
    Query q = new OrQuery(queries);
    Set<Frame> frames = om.executeQuery(q);
    assertEquals(2, frames.size());
    Frame deepBase = om.getOWLNamedClass("DeepPanBase");
    assertTrue(frames.contains(deepBase));
    Frame cheesey = om.getOWLNamedClass("CheeseyPizza");
    assertTrue(frames.contains(cheesey));

    queries = new ArrayList<Query>();
    Query q3 = new PhoneticQuery(label, "BaseEsprzza");
    queries.add(q1);
    queries.add(q3);
    q = new OrQuery(queries);
    frames = om.executeQuery(q);
    assertEquals(2, frames.size());
    assertTrue(frames.contains(cheesey));
    
    queries = new ArrayList<Query>();
    queries.add(q3);
    queries.add(q1);
    q = new OrQuery(queries);
    frames = om.executeQuery(q);
    assertEquals(2, frames.size());
    assertTrue(frames.contains(cheesey));
  }

  public static void testBasicSearch() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("basic search test");
    }
    OWLModel om = getOWLModel();
    checkPhoneticSearch(om, om.getRDFProperty("rdfs:comment"), "A class to demonstrate mistakes made", "IceCream", true);
    checkPhoneticSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate mistakes made", "IceCream", true);
    checkPhoneticSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate mistaques made", "IceCream", true);
    checkPhoneticSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate missedakes made", "IceCream", true);
    checkPhoneticSearch(om, om.getRDFProperty("rdfs:comment"), "A Klass to demonstrate mistaches made", "IceCream", false);
    Log.getLogger().info("Done");
  }
  
  public static void testDelete() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("delete phonetic search test");
    }
    OWLModel om = getOWLModel();
    om.getOWLNamedClass("IceCream").delete();
    checkPhoneticSearch(om, om.getRDFProperty("rdfs:comment"), "A class to demonstrate mistakes made", "IceCream", false);
  }
  
  public static void testSetValues() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("set values search test");
    }
    OWLModel om = getOWLModel();
    OWLClass iceCream = om.getOWLNamedClass("IceCream");
    RDFProperty comment = om.getRDFProperty("rdfs:comment");
    iceCream.setPropertyValue(comment, "This is a real klass.  Don't make derogatory comments.");
    checkPhoneticSearch(om, comment, "class derogatory", "IceCream", true);
  }
  
  public static void testAddValues() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("add values search test");
    }
    OWLModel om = getOWLModel();
    OWLClass iceCream = om.getOWLNamedClass("IceCream");
    RDFProperty comment = om.getRDFProperty("rdfs:comment");
    iceCream.addPropertyValue(comment, "But this class doesn't fit well with pizza.  And the comment wasn't derogatory.");
    checkPhoneticSearch(om, comment, "duznt derogatory", "IceCream", true);
  }
  
  
  @SuppressWarnings("deprecation")
  public static void testOWLRestriction() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("owl restriction test (#1)");
    }
    OWLModel om = getOWLModel();
    Slot nameSlot = om.getSystemFrames().getNameSlot();
    OWLProperty property = om.getOWLProperty("hasTopping");
    OWLRestrictionQuery oquery = new OWLRestrictionQuery(om, property, new PhoneticQuery(nameSlot, "CheeseTopping"));
    checkSearch(om, oquery, "CheeseyPizza", true);
  }
  

  public static void testOwnSlotValue() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("own slot value");
    }
    OWLModel om = getOWLModel();
    RDFProperty comment = om.getRDFProperty("rdfs:comment");
    OwnSlotValueQuery query = new OwnSlotValueQuery(comment, "*Countries can only be either*");
    checkSearch(om, query, "Country", true);
  }
  
  public static void testAndQuery() {
    if (log.isLoggable(Level.FINE)) {
      log.fine("and query");
    }
    OWLModel om = getOWLModel();
    RDFProperty comment = om.getRDFProperty("rdfs:comment");
    RDFProperty label   = om.getRDFProperty("rdfs:label");
    
    List<Query> queries = new ArrayList<Query>();
    Query q1 = new PhoneticQuery(label, "PizzaComQueijo");
    queries.add(q1);
    // Here is a tricky point - the actual comment slot for cheesey pizza starts with "~#en " 
    // to represent the English language.
    Query q2 = new OwnSlotValueQuery(comment, "*Any pizza*");
    queries.add(q2);
    Query q = new AndQuery(queries);
    OWLClass cheesey = om.getOWLNamedClass("CheeseyPizza");
    Set<Frame> frames = om.executeQuery(q);
    assertEquals(1, frames.size());
    assertTrue(frames.contains(cheesey));
  }
}
