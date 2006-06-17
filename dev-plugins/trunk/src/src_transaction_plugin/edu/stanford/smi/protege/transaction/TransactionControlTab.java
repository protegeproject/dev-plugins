package edu.stanford.smi.protege.transaction;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;

import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.ui.ProjectManager;
import edu.stanford.smi.protege.util.LazyTreeRoot;
import edu.stanford.smi.protege.util.ModalDialog;
import edu.stanford.smi.protege.util.SelectableTree;
import edu.stanford.smi.protege.widget.AbstractTabWidget;

public class TransactionControlTab extends AbstractTabWidget {
	
	private int transCounter;
	private int nestedTransCount;
	
	private JTextArea infoArea;
	private JTextField nestedTransField;
	private KnowledgeBase kb;
	private JButton startTransactionButton;
	private JButton commitTransactionButton;
	private JButton rollbackTransactionButton;
	
	public void initialize() {		
		setLabel("TransactionControlTab");
		
		kb = getKnowledgeBase();
		
		startTransactionButton = new JButton("Start Transaction");
		startTransactionButton.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent arg0) {
				startTransaction();
			}
		});
		
		commitTransactionButton = new JButton("Commit Transaction");
		commitTransactionButton.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent arg0) {
				commitTransactionAction();
			};
		});
		
		rollbackTransactionButton = new JButton("Rollback Transaction");
		rollbackTransactionButton.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent arg0) {
				rollbackTransactionAction();
			};
		});
		
		JButton refreshTreesButton = new JButton("Refresh trees");
		refreshTreesButton.addActionListener(new ActionListener() {		
			public void actionPerformed(ActionEvent arg0) {		
				refreshTrees();
			}		
		});
		
		JButton forceActivateButton = new JButton("Activate Buttons");
		forceActivateButton.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent arg0) {
				updateEditableButtons(true);
			}
		});
		
		JPanel p1 = new JPanel(new GridBagLayout());
		p1.setSize(50,50);
		p1.add(refreshTreesButton);
		
		nestedTransField = new JTextField(10);
		nestedTransField.setEditable(false);
				
		JPanel buttosPanel = new JPanel(new GridBagLayout());
		buttosPanel.add(startTransactionButton);
		buttosPanel.add(commitTransactionButton);
		buttosPanel.add(rollbackTransactionButton);
		buttosPanel.add(new JLabel(" Nested transaction level "));
		buttosPanel.add(nestedTransField);
		buttosPanel.add(new JLabel("     "));
		buttosPanel.add(forceActivateButton);
		
		infoArea = new JTextArea("Info Area\n");
		JScrollPane sp = new JScrollPane(infoArea);		
		
		BorderLayout borderlayout = new BorderLayout();		
		borderlayout.setVgap(20);
		setLayout(borderlayout);
		
		add(buttosPanel,BorderLayout.NORTH);
		add(p1, BorderLayout.SOUTH);
		add(sp, BorderLayout.CENTER);
		
		updateEditableButtons();
	}

	private void refreshTrees() {	
		
		for (Iterator iter = ProjectManager.getProjectManager().getCurrentProjectView().getTabs().iterator(); iter.hasNext();) {
			Component tabWidget = (Component) iter.next();
			if (tabWidget instanceof AbstractTabWidget) {
				JTree tabTree = ((AbstractTabWidget)tabWidget).getClsTree();
				try {
					LazyTreeRoot root = (LazyTreeRoot) ((SelectableTree)tabTree).getModel().getRoot();
					root.reload();
					tabTree.expandRow(0);					
				} catch (Exception e) {
					//System.out.println("Failed to reload tree for tab: " + tabWidget);
				}				
			}			
		}
		infoArea.append(new Date() + ": Refresh trees\n");
	}

	private void startTransaction() {
		boolean success = false;
				
		try {			
			success =kb.beginTransaction("Transaction " + transCounter);
				
			transCounter ++;
			nestedTransCount++;
		} catch (Exception e) {
			RuntimeException re = new RuntimeException();
			re.initCause(e);
			throw(re);
		}finally {				
			infoArea.append(new Date() + ": Transaction " + transCounter + " started ..." +(success ? "OK":"failed") + " ->  Nested level: "+ nestedTransCount + "\n");
			nestedTransField.setText(Integer.toString(nestedTransCount));
			updateEditableButtons();
		}		
	}

	private void commitTransactionAction() {
		boolean success = false;
		
		try {			
//			In protege-head the commitTranscation method is not in the KnowledgeBase interface
			success = ((DefaultKnowledgeBase)kb).commitTransaction();			
			
			nestedTransCount--;
		} catch (Exception e) {
			ModalDialog.showMessageDialog(this, "There was an error at committing the transaction." +
					"\nError message: " + e.getMessage()+
					"\nConsult console for further details.", "Error", ModalDialog.MODE_CLOSE);
			RuntimeException re = new RuntimeException();
			re.initCause(e);
			throw(re);
		}finally {				
			infoArea.append(new Date() + ":     Transaction commited " + "..." +(success ? "OK":"failed") + " ->  Nested level: "+ nestedTransCount + "\n");
			nestedTransField.setText(Integer.toString(nestedTransCount));
			updateEditableButtons();
		}
	}

	private void rollbackTransactionAction() {
		boolean success = false;
		
		try {
//			In protege-head the commitTranscation method is not in the KnowledgeBase interface
			success = ((DefaultKnowledgeBase)kb).rollbackTransaction();
			
			nestedTransCount--;
		} catch (Exception e) {
			ModalDialog.showMessageDialog(this, "There was an error at rolling back the transaction." +
					"\nError message: " + e.getMessage()+
					"\nConsult console for further details.", "Error", ModalDialog.MODE_CLOSE);
			RuntimeException re = new RuntimeException();
			re.initCause(e);
			throw(re);
		}finally {				
			infoArea.append(new Date() + ":     Transaction rolled back " + "..." +(success ? "OK":"failed") + " ->  Nested level: "+ nestedTransCount + "\n");
			nestedTransField.setText(Integer.toString(nestedTransCount));
			updateEditableButtons();
		}
	}

	private void updateEditableButtons() {
		if (nestedTransCount <= 0) 
			updateEditableButtons(false);
		else 
			updateEditableButtons(true);
				
	}

	private void updateEditableButtons(boolean active) {		
		commitTransactionButton.setEnabled(active);
		rollbackTransactionButton.setEnabled(active);				
	}

}
