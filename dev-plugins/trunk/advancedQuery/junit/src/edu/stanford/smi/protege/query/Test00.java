package edu.stanford.smi.protege.query;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protegex.owl.jena.JenaKnowledgeBaseFactory;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.jena.parser.ProtegeOWLParser;
import edu.stanford.smi.protegex.owl.model.OWLClass;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class Test00 {
  
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
    JenaKnowledgeBaseFactory jkbf = new JenaKnowledgeBaseFactory();
    Project project = new Project(null, errors);
    JenaKnowledgeBaseFactory.setOWLFileName(project.getSources(), "file:/Users/tredmond/Desktop/foo");
    checkErrors(errors);
    project.setKnowledgeBaseFactory(jkbf);
    project.createDomainKnowledgeBase(jkbf, errors, false);
    checkErrors(errors);
    OWLModel om = (OWLModel) project.getKnowledgeBase();
    String prefix = om.getNamespaceManager().getDefaultNamespace();
    System.out.println("namespace prefix = " + prefix);
    try {
      ProtegeOWLParser.addImport((JenaOWLModel) om, 
                                 new URI("http://www.co-ode.org/ontologies/pizza/pizza_20041007.owl"), 
                                 "pizza");
    } catch (Exception e) {
      e.printStackTrace();
    }
    OWLClass pizza = om.getOWLNamedClass("pizza:Pizza");
    System.out.println("class = " + pizza);
    for (Object sub : pizza.getSubclasses(false)) {
      System.out.println("Subclass = " + sub);
    }
    System.out.println("pizza instance = " +  pizza.createInstance(prefix + ":pizzaToDeliver"));
    project.save(errors);
  }
}
