package edu.stanford.smi.protege.query.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.stanford.smi.protege.model.BrowserSlotPattern;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.ValueType;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.InvalidQueryException;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;
import edu.stanford.smi.protege.query.querytypes.OwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.PhoneticQuery;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.ui.DisplayUtilities;
import edu.stanford.smi.protege.util.Assert;
import edu.stanford.smi.protege.util.JNumberTextField;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.ModalDialog;
import edu.stanford.smi.protege.util.ModalDialog.CloseCallback;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.ui.ProtegeUI;

/**
 * Holds the query items - the selected {@link Cls}, {@link Slot}, type, and expression.
 * 
 * @author Chris Callendar
 */
public class QueryComponent extends JPanel {
	
	private static final String EXACT_MATCH = "exact match";
	private static final String CONTAINS = "contains";
	private static final String STARTS_WITH = "starts with";
	private static final String ENDS_WITH = "ends with";
	private static final String SOUNDS_LIKE = "sounds like";
	private static final String IS = "is";
	private static final String GREATER_THAN = "greater than";
	private static final String LESS_THAN = "less than";
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	private static final String[] NULL = { "" };
	
	private Map<ValueType, String[]> typesMap;
	private Map<ValueType, QueryLabeledComponent> typesToComponentMap;
	
	private KnowledgeBase kb;
	private Collection<Slot> slots;
	private Collection<Slot> allSlots;

	private static final QueryLabeledComponent NULL_COMPONENT = getBlankComponent();
	
	protected QueryListComponent selectCls = null;
	protected QueryListComponent selectSlot = null;
	protected LabeledComponent typesComponent = null;
	private QueryLabeledComponent valueComponent = NULL_COMPONENT;

	private JComboBox cmbTypes = null;
	private DefaultComboBoxModel cmbModelTypes = null;
	private final DefaultComboBoxModel cmbModelBoolean = new DefaultComboBoxModel(new String[] { TRUE, FALSE });
	private DefaultComboBoxModel cmbModelSymbol = new DefaultComboBoxModel(NULL);
	private JPanel pnlQueryComponents;
	private ValueType currentValueType = null;
	protected final Slot defaultSlot;
	
	// this flag must be set to true for view (slot/cls/instance) actions to appear
	private boolean allowViewActions = false;
	
	/**
	 * Initializes this component with the given {@link KnowledgeBase} and selectable {@link Slot}s.
	 * The Name Slot (:NAME) is used as the default Slot.
	 * @param slots the available slots (which the user can choose from)
	 */
	public QueryComponent(KnowledgeBase kb, Collection<Slot> slots) {
		this(kb, slots, kb.getNameSlot());
	}
	
	/**
	 * Initializes this component with the given {@link KnowledgeBase} and selectable {@link Slot}s.
	 * The defaultSlot is used <b>IF</b> if is contained in the slots collection.
	 * @param slots the available slots (which the user can choose from)
	 * @param defaultSlot the slot to display by default	
	 */
	public QueryComponent(KnowledgeBase kb, Collection<Slot> slots, Slot defaultSlot) {
		this(kb, slots, defaultSlot, "");
	}
	
	/**
	 * Initializes this component with the given {@link KnowledgeBase} and selectable {@link Slot}s.
	 * The defaultSlot is used <b>IF</b> if is contained in the slots collection.
	 * @param slots the available slots (which the user can choose from)
	 * @param defaultSlot the slot to display by default
	 * @param value the default value to be searched
	 */
	public QueryComponent(KnowledgeBase kb, Collection<Slot> slots, Slot defaultSlot, String value) {
		this.kb = kb;
		this.slots = slots;
		this.allSlots = slots;
		this.defaultSlot = defaultSlot;
		this.typesMap = new HashMap<ValueType, String[]>(15);
		this.typesToComponentMap = new HashMap<ValueType, QueryLabeledComponent>(15);
					
		initialize();
		
		if (value != null && value.length() > 0 ) {
			((JTextField)this.valueComponent.getCenterComponent()).setText(value);
		} 
	}
	
	protected KnowledgeBase getKnowledgeBase() {
		return kb;
	}
	
	/**
	 * Gets the query for this component.  If the query is invalid an exception is thrown.
	 * @return Query 
	 * @throws InvalidQueryException if the query is invalid - missing a slot of expression value
	 */
	public final Query getQuery() throws InvalidQueryException {
		Slot slot = (Slot) selectSlot.getObject();
		if (slot == null) {
			JOptionPane.showMessageDialog(this, "Please choose a slot", "Choose a slot", JOptionPane.ERROR_MESSAGE);
			selectSlot.focus();
			throw new InvalidQueryException("A slot value is required");
		}

		return getQueryForType(slot, slot.getValueType());
	}
	
	protected Query getQueryForType(Slot slot, ValueType type) throws InvalidQueryException {
		Query q = null;
		String expr = getExpression();
		if ((expr == null) || (expr.length() == 0)) {
			JOptionPane.showMessageDialog(this, "Please enter an expression", "Enter an expression", JOptionPane.ERROR_MESSAGE);
			if (valueComponent != null) {
				valueComponent.focus();
			}
			throw new InvalidQueryException("An expression is required");
		}
		
		if (ValueType.ANY.equals(type) || ValueType.STRING.equals(type)) {
			q = getStringQuery(slot, expr);
		} else if (ValueType.BOOLEAN.equals(type) || ValueType.SYMBOL.equals(type) ||
				   ValueType.INTEGER.equals(type) || ValueType.FLOAT.equals(type)) {
			// TODO this doesn't work
			q = new OwnSlotValueQuery(slot, expr);
		} else if (ValueType.CLS.equals(type) || ValueType.INSTANCE.equals(type)) {
			// TODO what should go here?
			q = new OwnSlotValueQuery(slot, expr);
		}
		return q;
	}

	private String getExpression() {
		String expr = "";
		if (valueComponent != null) {
			Object obj = valueComponent.getValue();
			if (obj != null) {
				expr = obj.toString();
			}
		}
		return expr;
	}
	
	private Query getStringQuery(Slot slot, String expr) throws InvalidQueryException {
		Query q;
		String type = (String) getTypesComboBox().getSelectedItem();
		
		if (SOUNDS_LIKE.equals(type)) {
			q = new PhoneticQuery(slot, getExpression());
		} else {
			boolean startsWith = STARTS_WITH.equals(type) || CONTAINS.equals(type);
			boolean endsWith = ENDS_WITH.equals(type) || CONTAINS.equals(type);
			if (startsWith && !expr.endsWith("*")) {
				expr = expr + "*";
			}
			if (endsWith && !expr.startsWith("*")) {
				expr = "*" + expr;
			}
			//System.out.println("Searching for '" + expr + "'...");
			q = new OwnSlotValueQuery(slot, expr);
		}
		return q;
	}
	
	protected void initialize() {
		initializeTypes();
		initializeTypesToComponents();
		
		setLayout(new BorderLayout(0, 0));
		setDimensions();
		
		add(getQueryComponentsPanel(), BorderLayout.CENTER);

		// set the default slot (e.g. :NAME)
		if ((defaultSlot != null) && slots.contains(defaultSlot)) {
			getSelectSlotComponent().setObject(defaultSlot);
		} else if (slots.size() > 0) {
			getSelectSlotComponent().setObject(slots.iterator().next());
		} else {
			// if all else fails - use the :NAME slot
			// but what is :NAME isn't in the list of available slots?!?
			getSelectSlotComponent().setObject(kb.getNameSlot());
		}
	}

	/**
	 * Creates a map of {@link ValueType} objects to {@link QueryLabeledComponent} values, one of which 
	 * is shown as the value component to let the user enter a String, number, or to select an Cls or Instance.
	 */
	protected void initializeTypesToComponents() {
		typesToComponentMap.clear();
		QueryListComponent clsComp = new QueryListComponent("", kb);
		clsComp.setActions(createViewAction(clsComp, "View Cls", Icons.getViewClsIcon()),
						   createSelectClsAction(clsComp, "Select Cls", Icons.getAddClsIcon()),
						   createRemoveAction(clsComp, "Remove Cls", Icons.getRemoveClsIcon()));
		typesToComponentMap.put(ValueType.CLS, clsComp);

		QueryListComponent instComp = new QueryListComponent("", kb);
		instComp.setActions(createViewAction(instComp, "View Instance", Icons.getViewInstanceIcon()),
							createSelectInstanceAction(instComp, "Select Instance", Icons.getAddInstanceIcon()),
							createRemoveAction(instComp, "Remove Instance", Icons.getRemoveInstanceIcon()));
		typesToComponentMap.put(ValueType.INSTANCE, instComp);
				
		QueryLabeledComponent stringComp = new QueryLabeledComponent("String", new JTextField());
		typesToComponentMap.put(ValueType.ANY, stringComp);
		typesToComponentMap.put(ValueType.BOOLEAN, new QueryLabeledComponent("Boolean", new JComboBox(cmbModelBoolean)));
		typesToComponentMap.put(ValueType.FLOAT, new QueryLabeledComponent("Float", new JNumberTextField(0, false)));
		typesToComponentMap.put(ValueType.INTEGER, new QueryLabeledComponent("Integer", new JNumberTextField(0, true)));
		typesToComponentMap.put(ValueType.STRING, stringComp);
		typesToComponentMap.put(ValueType.SYMBOL, new QueryLabeledComponent("Symbol", new JComboBox(cmbModelSymbol)));
	}

	/**
	 * Creates a map of {@link ValueType} objects to String[] values used in the types {@link JComboBox}.
	 */
	protected void initializeTypes() {
		String[] string = { CONTAINS, STARTS_WITH, ENDS_WITH, EXACT_MATCH, SOUNDS_LIKE };	// any, string
		String[] number = { IS, GREATER_THAN, LESS_THAN };	// integer, float
		String[] contains = { CONTAINS };	// instance, class
		String[] is = { IS };	// symbol, boolean
		typesMap.clear();
		typesMap.put(ValueType.ANY, string);
		typesMap.put(ValueType.BOOLEAN, is);
		typesMap.put(ValueType.CLS, contains);
		typesMap.put(ValueType.FLOAT, number);
		typesMap.put(ValueType.INSTANCE, contains);
		typesMap.put(ValueType.INTEGER, number);
		typesMap.put(ValueType.STRING, string);
		typesMap.put(ValueType.SYMBOL, is);
	}
	
	/**
	 * Initializes and returns the query components panel.
	 */
	protected JPanel getQueryComponentsPanel() {
		if (pnlQueryComponents == null) {
			pnlQueryComponents = new JPanel(new GridLayout(1, /* 4 */ 3, 5, 0));
			/* pnlQueryComponents.add(getSelectClsComponent()); */
			pnlQueryComponents.add(getSelectSlotComponent());
			pnlQueryComponents.add(getTypesComponent());
			pnlQueryComponents.add(getValueComponent());
		}
		return pnlQueryComponents;
	}
	
	/**
	 * Gets the current {@link QueryLabeledComponent}.
	 */
	protected QueryLabeledComponent getValueComponent() {
		// initialized to a blank component
		return valueComponent;
	}
	
	/**
	 * Initializes and returns the types {@link LabeledComponent}.
	 */
	protected Component getTypesComponent() {
		if (typesComponent == null) {
			typesComponent = new LabeledComponent("", getTypesComboBox(), false);
		}
		return typesComponent;
	}
	
	/**
	 * Returns a new blank {@link QueryLabeledComponent}.
	 */
	protected static final QueryLabeledComponent getBlankComponent() {
		return new QueryLabeledComponent("", new JLabel(""));
	}
	
	/**
	 * Creates a new {@link QueryListComponent} which lets the user choose a {@link Cls}.
	 */
	protected QueryListComponent getSelectClsComponent() {
		if (selectCls == null) {
			selectCls = new QueryListComponent("Class", kb);
			selectCls.setActions(createViewAction(selectCls, "View Cls", Icons.getViewClsIcon()),
								 createSelectClsAction(selectCls, "Select Cls", Icons.getAddClsIcon()),
								 createRemoveAction(selectCls, "Remove Cls", Icons.getRemoveClsIcon()));
			selectCls.addListener(clsListener);
		}
		return selectCls;
	}
	
	/**
	 * Creates a new {@link QueryListComponent} which lets the user choose a {@link Slot}.
	 */
	protected QueryListComponent getSelectSlotComponent() {
		if (selectSlot == null) {
			selectSlot = new QueryListComponent("Slot", kb);
			selectSlot.setActions(createViewAction(selectSlot, "View Slot", Icons.getViewSlotIcon()), 
								  createSelectSlotAction(selectSlot, "Select Slot", Icons.getAddSlotIcon()),
								  createRemoveAction(selectSlot, "Remove Slot", Icons.getRemoveSlotIcon()));
			selectSlot.addListener(slotListener);
		}
		return selectSlot;
	}
	
	protected void setDimensions() {
		setMinimumSize(new Dimension(100, 56));
		//setPreferredSize(new Dimension(500, 56));
		setMaximumSize(new Dimension(2000, 56));
	}
	
	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		// pass the bgcolor 
		if (pnlQueryComponents != null) {
			if (selectCls != null) {
				selectCls.setBackground(bg);
			}
			if (selectSlot != null) {
				selectSlot.setBackground(bg);
			}
			if (typesComponent != null) {
				typesComponent.setBackground(bg);
			}
			if (valueComponent != null) {
				valueComponent.setBackground(bg);
			}
		}
	}
	
	private JComboBox getTypesComboBox() {
		if (cmbTypes == null) {
			cmbTypes = new JComboBox(getTypesModel());
			cmbTypes.setSelectedIndex(0);
		}
		return cmbTypes;
	}
	
	private DefaultComboBoxModel getTypesModel() {
		if (cmbModelTypes == null) {
			cmbModelTypes = new DefaultComboBoxModel();
			cmbModelTypes.addElement("");
		}
		return cmbModelTypes;
	}
		
	/** 
	 * Creates an action used to view a Frame.  Initially disabled.
	 * Returns null if allowViewActions is false. 
	 */
	protected Action createViewAction(final QueryListComponent comp, String name, Icon icon) {
        Action action = null;
        if (allowViewActions) {
	        action = new AbstractAction(name, icon) {
	            public void actionPerformed(ActionEvent event) {
	            	comp.viewObject();
	            }
	        };
	        action.setEnabled(false);
        }
        return action;
    }
    
	/**
	 * Creates an action that will display a popup dialog that lets the user choose a cls.
	 */
	protected Action createSelectClsAction(final QueryListComponent comp, final String name, Icon icon) {
		return new AbstractAction(name, icon) {
			public void actionPerformed(ActionEvent e) {
				Cls cls = DisplayUtilities.pickCls(comp, kb, kb.getRootClses(), name);
				// if the user pressed cancel then cls will be null
				if (cls != null) {
					comp.setObject(cls);
				}
			}
		};
	}
	
	/**
	 * Creates an action that will popup up a dialog letting the user choose an instance 
	 * (or possibility a cls if the project is an OWL project).
	 */
	protected Action createSelectInstanceAction(final QueryListComponent comp, final String name, Icon icon) {
		return new AbstractAction(name, icon) {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				Slot slot = (Slot) selectSlot.getObject();
				Collection clses = (slot == null ? kb.getRootClses() : slot.getAllowedClses());
				Instance inst = null;
				// For OWL projects - allow the user to select an instance OR a class
				if (kb instanceof OWLModel) {
					if (clses.isEmpty()) {
						clses = kb.getRootClses();
					}
					// using this resource chooser lets the user choose an instance or a cls
					// in the NCI there are no instances but some slots are still of value type 'Instance'
					inst = ProtegeUI.getSelectionDialogFactory().selectResourceByType(comp, (OWLModel) kb, clses);
				} else {
					// this only lets users choose an instance
					inst = DisplayUtilities.pickInstance(comp, clses);
				}
				// if the user pressed cancel then inst will be null
				if (inst != null) {
					comp.setObject(inst);
				}
			}
		};
	}	
	
	/** Creates an action used to select a value for the QueryListComponent.  Initially enabled. */
	protected Action createSelectSlotAction(final QueryListComponent comp, final String name, Icon icon) {
		return new AbstractAction(name, icon) {
			public void actionPerformed(ActionEvent e) {
				Slot slot = DisplayUtilities.pickSlot(comp, slots, name);
				// if the user pressed cancel then slot will be null
				if (slot != null) {
					comp.setObject(slot);
				}
			}
		};
	}	
	
	/** Creates an action used to remove a value from the QueryListComponent.  Initially disabled. */
	protected Action createRemoveAction(final QueryListComponent comp, String name, Icon icon) {
        Action action = new AbstractAction(name, icon) {
            public void actionPerformed(ActionEvent event) {
                comp.clearObject();
            }
        };
        action.setEnabled(false);
        return action;
    }

    /**
     * Resets this component - clears the fields.
     */
	public void reset() {
		removeAll();		
		initialize();
		revalidate();
		repaint();
	}
	
	/**
	 * Displays a QueryComponent inside a {@link ModalDialog} letting the user create a single query.
	 * At the moment {@link AndQuery} and {@link OrQuery} are not allowed.
	 * @param slots	the allowed slots
	 * @param defaultSlot the slot to display by default (must be contained in the slots Collection)
	 * @param parent the parent component
	 * @param title the title for the dialog
	 * @return the {@link Query} or null
	 */
    public static Query showQueryDialog(KnowledgeBase kb, Collection<Slot> slots, Slot defaultSlot, Component parent, String title) {
    	// This method is not used at the moment
    	Query query = null;
    	final QueryComponent comp = new QueryComponent(kb, slots, defaultSlot);

    	// we need to get the query this way because when the dialog closes all the children components
    	// of QueryComponent are disposed which prevents us from getting the query afterwards
    	final Query[] queryHolder = { null };
    	CloseCallback callback = new CloseCallback() {
    		public boolean canClose(int result) {
    			try {
					queryHolder[0] = comp.getQuery();
				} catch (InvalidQueryException e) {
					return false;
				}
    			return true;
    		}
    	};
    	
        int result = ModalDialog.showDialog(parent, comp, title, ModalDialog.MODE_OK_CANCEL, callback);
        switch (result) {
            case ModalDialog.OPTION_OK:
                query = queryHolder[0];
                break;
            case ModalDialog.OPTION_CANCEL:
                break;
            default:
                Assert.fail("bad result: " + result);
                break;
        }
        return query;
    }
    
	/** Listens for when the user selects a Cls and updates the slots accordingly. */
	private QueryListComponentListener clsListener = new QueryListComponentListener() {
		public void valueChanged(Object value) {
			if (value == null) {
				slots = allSlots;
			} else if (value instanceof Cls) {
				Cls cls = (Cls) value;
				slots = cls.getTemplateSlots();
				// use the default slot if one exists
				BrowserSlotPattern pattern = cls.getDirectBrowserSlotPattern();
				if (pattern != null) {
					selectSlot.setObject(pattern.getFirstSlot());
				} else {
					// check if the current slot is valid and reset if not
					Slot slot = (Slot) selectSlot.getObject();
					if ((slot != null) && !slots.contains(slot)) {
						selectSlot.setObject(kb.getNameSlot());
					}
				}
			}
		}
	};

	/** Listens for when a slot is selected and updates the types combobox and value component. */
	private QueryListComponentListener slotListener = new QueryListComponentListener() {
		@SuppressWarnings("unchecked")
		public void valueChanged(Object value) {
			DefaultComboBoxModel model = getTypesModel();
			// remove the old value component
			pnlQueryComponents.remove(valueComponent);
			if (value == null) {
				// reset the types and value components
				model.removeAllElements();
				model.addElement("");
				valueComponent = NULL_COMPONENT;
				currentValueType = null;
			} else if (value instanceof Slot) {
				Slot slot = (Slot) value;
				// update the types combobox (if different from current type)
				ValueType type = slot.getValueType();
				if (!type.equals(currentValueType)) {
					currentValueType = type;
					
					String[] types = typesMap.get(type);
					model.removeAllElements();
					for (int i = 0; i < types.length; i++) {
						model.addElement(types[i]);
					}
	
					// load the symbol values
					if (ValueType.SYMBOL.equals(type)) {
						cmbModelSymbol.removeAllElements();
						for (Iterator iter = slot.getAllowedValues().iterator(); iter.hasNext(); ) {
							cmbModelSymbol.addElement(iter.next());
						}
					}
	
					// get the new value component
					valueComponent = typesToComponentMap.get(type);
					valueComponent.reset();
				}
			}
			valueComponent.setBackground(pnlQueryComponents.getBackground());
			pnlQueryComponents.add(valueComponent);
			pnlQueryComponents.revalidate();
			pnlQueryComponents.repaint();
		}
	};
	    
}
