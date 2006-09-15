package edu.stanford.smi.protege.query;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;
import edu.stanford.smi.protege.query.ui.QueryComponent;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.metaproject.Operation;
import edu.stanford.smi.protege.server.metaproject.impl.OperationImpl;
import edu.stanford.smi.protege.ui.FrameRenderer;
import edu.stanford.smi.protege.ui.ListFinder;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.DoubleClickActionAdapter;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.ListPanel;
import edu.stanford.smi.protege.util.ListPanelComponent;
import edu.stanford.smi.protege.util.ListPanelListener;
import edu.stanford.smi.protege.util.JProgressButton;
import edu.stanford.smi.protege.util.SelectableList;
import edu.stanford.smi.protege.util.ViewAction;
import edu.stanford.smi.protege.widget.AbstractTabWidget;
import edu.stanford.smi.protege.widget.TabWidget;

/**
 * {@link TabWidget} for doing advanced queries.
 *
 * @author Timothy Redmond, Chris Callendar
 * @date 15-Aug-06
 */
public class AdvancedQueryPlugin extends AbstractTabWidget {

	private static final long serialVersionUID = -5589620508506925170L;

	public static final Operation INDEX_OPERATION = new OperationImpl("Generate Lucene Indicies");

	private Collection<Slot> slots;
	
	private KnowledgeBase kb;
	private ListPanel queriesListPanel;
	private SelectableList lstResults;
	private JRadioButton btnAndQuery;
	private JRadioButton btnOrQuery;
	private JPanel pnlQueryBottom;
	private boolean canIndex = false;
	
	public AdvancedQueryPlugin() {
		super();
		
		this.slots = new ArrayList<Slot>(0);
	}
	
	public void initialize() {
		this.kb = getKnowledgeBase();
		InstallNarrowFrameStore frameStore = new InstallNarrowFrameStore(kb);
		this.slots = frameStore.getSearchableSlots();
		this.canIndex = RemoteClientFrameStore.isOperationAllowed(kb, INDEX_OPERATION);
		frameStore.execute();

		// initialize UI 
		setLabel("Advanced Query Tab");
		setIcon(Icons.getAddQueryLibraryIcon());
        setLayout(new BorderLayout());
        
        // add UI components
		createGUI();
		// add the default first query component
		addQueryComponent();
	}
	
	private void createGUI() {
		JPanel pnlLeft = new JPanel(new BorderLayout(5, 5));
		LabeledComponent lcLeft = new LabeledComponent("Query", pnlLeft, true);
		if (canIndex) {
			addIndexButton(lcLeft);
		}

		lcLeft.addHeaderButton(getAddQueryAction());
		pnlLeft.add(new JScrollPane(getQueryList()), BorderLayout.CENTER);
		pnlLeft.add(getQueryBottomPanel(), BorderLayout.SOUTH);		

		SelectableList lst = getResultsList();
		LabeledComponent lcRight = new LabeledComponent("Search Results", new JScrollPane(lst), true);
		lcRight.addHeaderButton(getViewAction());
		lcRight.setFooterComponent(new ListFinder(lst, "Find Instance"));

		JSplitPane splitter = ComponentFactory.createLeftRightSplitPane();
		splitter.setLeftComponent(lcLeft);
		splitter.setRightComponent(lcRight);
        add(splitter, BorderLayout.CENTER);
	}

	/**
	 * Adds a button to the LabeledComponent which when clicked will create an index of the ontology.
	 */
	private void addIndexButton(LabeledComponent lc) {
		//final ImageIcon icon = ComponentUtilities.loadImageIcon(AdvancedQueryPlugin.class, "ui/index.gif");
		final JProgressButton btnProgress = new JProgressButton();
		Action action = new AbstractAction("Index Ontology") {
			public void actionPerformed(ActionEvent buttonPushed) {
				btnProgress.showProgressBar("Indexing...");
				new Thread(new Runnable() {
					public void run() {
						new IndexOntologies(kb).execute();
						btnProgress.hideProgressBar();
					}
				}).start();
		    }
		};
		btnProgress.setAction(action);
		// hack - add a fake button to get at the parent
		JButton btnTemp = lc.addHeaderButton(action);
		btnTemp.getParent().add(btnProgress);
		btnTemp.getParent().remove(btnTemp);	// now remove the fake button
		
		btnProgress.setToolTipText("Create the index for this ontology");
		btnProgress.setPreferredSize(new Dimension(120, 26));
		btnProgress.setFont(btnProgress.getFont().deriveFont(Font.BOLD));
		btnProgress.setForeground(Color.darkGray);
	}
	
	private ListPanel getQueryList() {
		if (queriesListPanel == null) {
			queriesListPanel = new ListPanel();
			queriesListPanel.addListener(new ListPanelListener() {
				public void panelAdded(JPanel panel, ListPanel listPanel) {}
				public void panelRemoved(JPanel comp, ListPanel listPanel) {
					// always have one query component
					if (listPanel.getPanelCount() == 0) {
						addQueryComponent();
					}
				}
			});
		}
		return queriesListPanel;
	}

	private JPanel getQueryBottomPanel() {
		if (pnlQueryBottom == null) {
			pnlQueryBottom = new JPanel();
			pnlQueryBottom.setLayout(new BoxLayout(pnlQueryBottom, BoxLayout.LINE_AXIS));
			pnlQueryBottom.setPreferredSize(new Dimension(500, 28));
			
			pnlQueryBottom.add(new JButton(getAddQueryAction()));
			pnlQueryBottom.add(Box.createRigidArea(new Dimension(4, 0)));
			JButton btn = new JButton(new AbstractAction("Clear", Icons.getClearIcon(false, false)) {
				public void actionPerformed(ActionEvent e) {
					clearComponents();
				}
			});
			pnlQueryBottom.add(btn);
			pnlQueryBottom.add(Box.createRigidArea(new Dimension(8, 0)));
			
			btnAndQuery = new JRadioButton("Match All  ", false);
			btnOrQuery = new JRadioButton("Match Any  ", true);
			pnlQueryBottom.add(btnAndQuery);
			pnlQueryBottom.add(btnOrQuery);
			ButtonGroup group = new ButtonGroup();
			group.add(btnAndQuery);
			group.add(btnOrQuery);
			
			pnlQueryBottom.add(Box.createHorizontalGlue());
			
			final JButton btnSearch = new JButton(new AbstractAction("Search", Icons.getFindIcon()) {
				public void actionPerformed(ActionEvent e) {
					doSearch();
				}
			});
			pnlQueryBottom.add(btnSearch);

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (getRootPane() != null) {
						getRootPane().setDefaultButton(btnSearch);
					}
				}
			});
		}			
		return pnlQueryBottom;
	}

	private SelectableList getResultsList() {
		if (lstResults == null) {
	        lstResults = ComponentFactory.createSelectableList(null, false);
	        lstResults.setCellRenderer(new FrameRenderer());
	        lstResults.addMouseListener(new DoubleClickActionAdapter(getViewAction()));
		}
		return lstResults;
	}

	private ViewAction getViewAction() {
		return new ViewAction("View Instance", lstResults, Icons.getViewInstanceIcon()) {
		    public void onView(Object o) {
		        kb.getProject().show((Instance) o);
		    }
		};
	}
	
	private Action getAddQueryAction() {
		return new AbstractAction("Add Query", Icons.getAddQueryLibraryIcon()) {
			public void actionPerformed(ActionEvent e) {
				addQueryComponent();
			}
		};
	}
	
	private void addQueryComponent() {
		QueryComponent qc = new QueryComponent(kb, slots);
		ListPanelComponent comp = new ListPanelComponent(queriesListPanel, qc);
		queriesListPanel.addPanel(comp);
	}
	
	private void clearComponents() {
		queriesListPanel.removeAllPanels();
		addQueryComponent();
		queriesListPanel.repaint();
	}
	
	private void doSearch() {
		Query query = null;
		Collection<JPanel> panels = queriesListPanel.getPanels();
		int size = panels.size();
		if (size == 1) {
			ListPanelComponent comp = (ListPanelComponent) panels.iterator().next();
			QueryComponent qc = (QueryComponent) comp.getMainPanel();
			query = qc.getQuery();
		} else if (size > 1) {
			Collection<Query> queries = new ArrayList<Query>(size);
			for (Iterator iter = panels.iterator(); iter.hasNext(); ) {
				ListPanelComponent comp = (ListPanelComponent) iter.next();
				QueryComponent qc = (QueryComponent) comp.getMainPanel();
				queries.add(qc.getQuery());
			}
			if (btnAndQuery.isSelected()) {
				query = new AndQuery(queries);
			} else {
				query = new OrQuery(queries);
			}
		}
		if (query != null) {
			doQuery(query);
		}
	}

	private void doQuery(Query q) {
		Set<Frame> results = kb.executeQuery(q);
		if ((results == null) || (results.size() == 0)) {
			lstResults.setListData(new String[] { "No results found." });
		} else {
			lstResults.setListData(new Vector<Frame>(results));
		}
	}
	
}
