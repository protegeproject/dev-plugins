package edu.stanford.smi.protege.ui.internalKB;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import edu.stanford.smi.protege.event.KnowledgeBaseAdapter;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.WidgetDescriptor;
import edu.stanford.smi.protege.storage.clips.ClipsKnowledgeBaseFactory;
import edu.stanford.smi.protege.util.CollectionUtilities;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.LazyTreeNode;
import edu.stanford.smi.protege.util.LazyTreeRoot;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.PropertyList;
import edu.stanford.smi.protege.util.SelectableTree;
import edu.stanford.smi.protege.widget.AbstractTabWidget;
import edu.stanford.smi.protege.widget.ClsesAndInstancesTab;



public class InternalKBTab extends AbstractTabWidget {
	ClsesAndInstancesTab ciTab;
	Project internalPrj;
	JTextField frameCountWas;
	JTextField frameCountIs;
	
	
	public void initialize() {
		setLabel("InternalKBView");
		ciTab.initialize();
		ciTab.setEnabled(false);
		
		JSplitPane splitPane = ComponentFactory.createTopBottomSplitPane();
		
		JPanel buttonPanel = new JPanel();
		JButton refreshButton = new JButton("Refresh counter");
		refreshButton.addActionListener( new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				refreshDisplay();
			};
		});
				
		JButton cleanInternalKBButton = new JButton("Clean internal KB");
		cleanInternalKBButton.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				removeUnreferencedInstances(getProject().getInternalProjectKnowledgeBase());
				refreshDisplay();
			};			
		});
		cleanInternalKBButton.setToolTipText("This is dangerous to do!");
		
		frameCountWas = new JTextField(10);
		frameCountIs = new JTextField(10);
		
		frameCountIs.setEnabled(false);
		frameCountWas.setEnabled(false);
		
		String frameCount = String.valueOf((getProject().getInternalProjectKnowledgeBase().getFrameCount()));
		frameCountWas.setText(frameCount);
		frameCountIs.setText(frameCount);
		
		buttonPanel.add(refreshButton);
		buttonPanel.add(new JLabel(" Old frame count "));
		buttonPanel.add(frameCountWas);
		buttonPanel.add(new JLabel(" New frame count "));
		buttonPanel.add(frameCountIs);
		buttonPanel.add(cleanInternalKBButton);
		
		splitPane.setLeftComponent(ciTab);
		splitPane.setRightComponent(buttonPanel);
		add(splitPane);		
	
	}
	
	private void refreshDisplay() {
		refreshCountDisplay();
		//System.out.println(internalPrj.getKnowledgeBase().getFrameCount() + " " + getProject().getInternalProjectKnowledgeBase().getFrameCount());
	}

	private void refreshCountDisplay() {
		String was = frameCountIs.getText();
		
		frameCountWas.setText(was);
		frameCountIs.setText(String.valueOf(getProject().getInternalProjectKnowledgeBase().getFrameCount()));
	}
	
	public void setup(WidgetDescriptor descriptor, Project project) {
		ciTab = new ClsesAndInstancesTab();
		 
		createInternalPrj(project);
		//internalPrj = project;
		
		ciTab.setup(getDescriptor(),internalPrj);
		/*
		WidgetDescriptor wd = project.createWidgetDescriptor();
		wd.setName("InternalKBView");
		wd.setWidgetClassName("edu.stanford.smi.protege.widget.ClsesAndInstancesTab");
		wd.setVisible(true);
		ciTab.setup(wd, internalPrj);
		*/
		super.setup(descriptor, project);
		
	};
	
	private Project createInternalPrj(Project prj) {
		internalPrj = Project.createBuildProject(prj.getInternalProjectKnowledgeBase(), new ArrayList());
		internalPrj.setProjectFilePath("internalKB.pprj");
		
		PropertyList orig = prj.getSources();
		String clsFileName = ClipsKnowledgeBaseFactory.getClsesSourceFile(orig);
		String instFileName = ClipsKnowledgeBaseFactory.getInstancesSourceFile(orig);
		
		ClipsKnowledgeBaseFactory.setSourceFiles(internalPrj.getSources(), "internalKB.pont", "internalKB.pins");
		internalPrj.save(new ArrayList());
		
		internalPrj = Project.loadProjectFromFile("internalKB.pprj", new ArrayList());
		
		ClipsKnowledgeBaseFactory.setSourceFiles(orig, clsFileName, instFileName);
		return internalPrj;
	}
	
	private void removeUnreferencedInstances(KnowledgeBase kb) {
		Collection roots = kb.getCls("Project").getDirectInstances();
		
		Iterator i = kb.getUnreachableSimpleInstances(roots).iterator();
		while (i.hasNext()) {
			Instance instance = (Instance) i.next();
			 Log.getLogger().info("found unreachable instance: " + instance);
			if (instance.isEditable()) {
				kb.deleteInstance(instance);
			}
		}
	}

	
}