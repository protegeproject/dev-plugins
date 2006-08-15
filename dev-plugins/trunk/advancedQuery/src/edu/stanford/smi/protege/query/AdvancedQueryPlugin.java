package edu.stanford.smi.protege.query;

import javax.swing.JLabel;

import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.widget.AbstractTabWidget;

public class AdvancedQueryPlugin extends AbstractTabWidget {

  private static final long serialVersionUID = -5589620508506925170L;

  public void initialize() {
    setLabel("Advanced Query Tab");
    setIcon(Icons.getInstancesIcon());
    add(new JLabel("Query UI goes here"));
    new InstallNarrowFrameStore(getKnowledgeBase()).execute();
  }
  


}
