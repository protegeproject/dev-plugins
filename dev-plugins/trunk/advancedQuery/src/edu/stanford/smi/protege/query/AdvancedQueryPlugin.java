package edu.stanford.smi.protege.query;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;
import edu.stanford.smi.protege.query.ui.QueryComponent;
import edu.stanford.smi.protege.query.ui.QueryUtil;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.metaproject.Operation;
import edu.stanford.smi.protege.server.metaproject.impl.OperationImpl;
import edu.stanford.smi.protege.ui.FrameRenderer;
import edu.stanford.smi.protege.ui.ListFinder;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.DoubleClickActionAdapter;
import edu.stanford.smi.protege.util.JProgressButton;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.ListPanel;
import edu.stanford.smi.protege.util.ListPanelListener;
import edu.stanford.smi.protege.util.SelectableList;
import edu.stanford.smi.protege.util.ViewAction;
import edu.stanford.smi.protege.widget.AbstractTabWidget;
import edu.stanford.smi.protege.widget.TabWidget;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.ui.icons.OWLIcons;
import edu.stanford.smi.protegex.owl.ui.icons.OverlayIcon;

/**
 * {@link TabWidget} for doing advanced queries.
 *
 * @author Timothy Redmond, Chris Callendar
 * @date 15-Aug-06
 */
public class AdvancedQueryPlugin extends AbstractTabWidget {

	private static final long serialVersionUID = -5589620508506925170L;

	public static final Operation INDEX_OPERATION = new OperationImpl("Generate Lucene Indicies");

	private KnowledgeBase kb;
	private Collection<Slot> slots;
	private boolean canIndex;
	private boolean isOWL;
	private Slot defaultSlot = null;
	
	private ListPanel queriesListPanel;
	private SelectableList lstResults;
	private JRadioButton btnAndQuery;
	private JRadioButton btnOrQuery;
	private JPanel pnlQueryBottom;
	
	public AdvancedQueryPlugin() {
		super();
		this.canIndex = false;
		this.isOWL = false;
		this.slots = Collections.emptySet();
	}
	
	/**
	 * Initializes this {@link TabWidget}.  Installs the {@link NarrowFrameStore} and initializes the UI.
	 */
	@SuppressWarnings("unchecked")
	public void initialize() {
		this.kb = getKnowledgeBase();
		InstallNarrowFrameStore frameStore = new InstallNarrowFrameStore(kb);
		this.canIndex = RemoteClientFrameStore.isOperationAllowed(kb, INDEX_OPERATION);
		this.slots = (Set) frameStore.execute();

		// determine if current project is an OWL project, if so collect OWLProperty values
		this.isOWL = (kb instanceof OWLModel);
		
        // TODO determine default slot, from properties file?
		this.defaultSlot = kb.getSlot("Preferred_Name");
        if (defaultSlot == null) {
        	defaultSlot = kb.getNameSlot();
        }
        
        // add UI components
		createGUI();
		// add the default first query component
		addQueryComponent();
	}
	
	/**
	 * Creates the GUI, initializing the components and adding them to the tab.
	 */
	private void createGUI() {
		setLabel("Advanced Query Tab");
		setIcon(ComponentUtilities.loadImageIcon(AdvancedQueryPlugin.class, "querytab.gif"));	// Icons.getQueryIcon(), Icons.getQueryExportIcon();
        setLayout(new BorderLayout());

        JPanel pnlLeft = new JPanel(new BorderLayout(5, 5));
		LabeledComponent lcLeft = new LabeledComponent("Query", pnlLeft, true);
		
		// only add if the user has permission to index the ontology
		if (canIndex) {
			addIndexButton(lcLeft);
		}

		JButton btn = lcLeft.addHeaderButton(getAddQueryAction());
		btn.setText("Add Query");
		Dimension dim = new Dimension(100, btn.getPreferredSize().height);
		btn.setPreferredSize(dim);
		btn.setMinimumSize(dim);
		btn.setMaximumSize(dim);
		if (isOWL) {
			btn = lcLeft.addHeaderButton(getAddRestrictionQueryAction());
			dim = new Dimension(124, btn.getPreferredSize().height);
			btn.setText("Add Restriction");
			btn.setPreferredSize(dim);
			btn.setMinimumSize(dim);
			btn.setMaximumSize(dim);
		}
		
		pnlLeft.add(new JScrollPane(getQueryList()), BorderLayout.CENTER);
		pnlLeft.add(getQueryBottomPanel(), BorderLayout.SOUTH);		

		SelectableList lst = getResultsList();
		LabeledComponent lcRight = new LabeledComponent("Search Results", new JScrollPane(lst), true);
		lcRight.addHeaderButton(getViewAction());
		lcRight.setFooterComponent(new ListFinder(lst, "Find Instance"));
		
		// Add action for showing in NCI Edit Tab
		if (isOWL) {
			// TODO different icon?
			lcRight.addHeaderButton(new ViewInNCIEditorAction("View Cls in the NCI Edit Tab", lstResults, Icons.getViewClsIcon()));
		}

		JSplitPane splitter = ComponentFactory.createLeftRightSplitPane();
		splitter.setLeftComponent(lcLeft);
		splitter.setRightComponent(lcRight);
        add(splitter, BorderLayout.CENTER);
	}

	/**
	 * Adds the "Index Ontology" button to the given LabeledComponent.
	 * When clicked it will create an index of the ontology and displays an infinitely looping
	 * {@link JProgressBar} inside the button.  Once the indexing is complete the progress bar disappears.
	 */
	private void addIndexButton(LabeledComponent lc) {
		//final ImageIcon icon = ComponentUtilities.loadImageIcon(AdvancedQueryPlugin.class, "ui/index.gif");
		final JProgressButton btnProgress = new JProgressButton();
		Action action = new AbstractAction("Index Ontology") {
			public void actionPerformed(ActionEvent buttonPushed) {
				btnProgress.showProgressBar("Indexing...");
				new Thread(new Runnable() {
					public void run() {
					  try {
						new IndexOntologies(kb).execute();
					  } finally {
						btnProgress.hideProgressBar();
					  }
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
	
	/**
	 * Initializes and returns the query {@link ListPanel}.  This is the panel that contains 
	 * all the queries (there will always be at least one query).
	 */
	private ListPanel getQueryList() {
		if (queriesListPanel == null) {
			queriesListPanel = new ListPanel(200, false);
			// ensure always one query component exists
			queriesListPanel.addListener(new ListPanelListener() {
				public void panelAdded(JPanel panel, ListPanel listPanel) {}
				public void panelRemoved(JPanel comp, ListPanel listPanel) {
					if (listPanel.getPanelCount() == 0) {
						addQueryComponent();
					}
				}
			});
		}
		return queriesListPanel;
	}

	
	/**
	 * Initializes and returns the bottom query panel which contains
	 * the "Add Query", "Clear" and "Search" buttons, as well as the 
	 * "Match All" and "Match Any" checkboxes.
	 */
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
			btn.setToolTipText("Remove all queries and start over");
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
		    	if (o instanceof Instance) {
			    	// TODO - display in NCI Editor?
			        kb.getProject().show((Instance)o);
				}
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
	
	private Action getAddRestrictionQueryAction() {
		Icon icon = new OverlayIcon(OWLIcons.getImageIcon(OWLIcons.OWL_RESTRICTION).getImage(), 5, 5, 
									OWLIcons.getImageIcon(OWLIcons.ADD_OVERLAY).getImage(), 15, 13, 15, 16);
		return new AbstractAction("Add a restriction query", icon) {
			public void actionPerformed(ActionEvent e) {
				QueryUtil.addRestrictionQueryComponent((OWLModel)kb, slots, defaultSlot, queriesListPanel);
			}
		};
	}	

	private void addQueryComponent() {
		QueryUtil.addQueryComponent(kb, slots, defaultSlot, queriesListPanel);
	}
	
	/**
	 * Removes all the query components and then adds one back as the starting query.
	 */
	private void clearComponents() {
		queriesListPanel.removeAllPanels();
		addQueryComponent();
		queriesListPanel.repaint();
	}

	
	/**
	 * Creates the {@link Query} object from all the {@link QueryComponent}s.
	 * If there are multiple queries then either an {@link AndQuery} or an {@link OrQuery} are used.  
	 * Passes the {@link Query} on to {@link AdvancedQueryPlugin#doQuery(Query)} if the query is valid.
	 */
	private void doSearch() {
		try {
			Query query = QueryUtil.getQueryFromListPanel(queriesListPanel, btnAndQuery.isSelected());
			doQuery(query);
		} catch (InvalidQueryException e) {
			System.err.println("Invalid query: " + e.getMessage());
		}
	}

	/**
	 * Executes the query using the {@link KnowledgeBase} and puts the results
	 * into the list. 
	 * @param q the query to perform
	 * @see KnowledgeBase#executeQuery(Query)
	 */
	private void doQuery(Query q) {
		Set<Frame> results = null;
		if (q != null) {
			results = kb.executeQuery(q);
		}
		if ((results == null) || (results.size() == 0)) {
			lstResults.setListData(new String[] { "No results found." });
		} else {
			lstResults.setListData(new Vector<Frame>(results));
		}
	}
	
}
