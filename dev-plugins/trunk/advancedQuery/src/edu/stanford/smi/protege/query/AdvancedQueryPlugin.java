package edu.stanford.smi.protege.query;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.metaproject.Operation;
import edu.stanford.smi.protege.server.metaproject.impl.OperationImpl;
import edu.stanford.smi.protege.widget.AbstractTabWidget;

public class AdvancedQueryPlugin extends AbstractTabWidget {

  private static final long serialVersionUID = -5589620508506925170L;
  
  public static final Operation INDEX_OPERATION = new OperationImpl("Generate Lucene Indicies");

  public void initialize() {
    final KnowledgeBase kb = getKnowledgeBase();
    setLabel("Advanced Query Tab");
    setIcon(Icons.getInstancesIcon());
    add(new JLabel("Query UI goes here"));
    if (RemoteClientFrameStore.isOperationAllowed(getKnowledgeBase(), INDEX_OPERATION)) {
      add(new JButton(new AbstractAction("Index Ontologies") {

        public void actionPerformed(ActionEvent buttonPushed) {
          new IndexOntologies(kb).execute();
        }
        
      }));
    }
    new InstallNarrowFrameStore(getKnowledgeBase()).execute();
  }
}
