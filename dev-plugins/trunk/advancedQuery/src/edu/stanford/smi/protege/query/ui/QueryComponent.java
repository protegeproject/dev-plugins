package edu.stanford.smi.protege.query.ui;

import java.awt.BorderLayout;
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
import edu.stanford.smi.protege.query.querytypes.OwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.PhoneticQuery;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.ui.DisplayUtilities;
import edu.stanford.smi.protege.util.JNumberTextField;
import edu.stanford.smi.protege.util.LabeledComponent;

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
	
	private QueryListComponent selectCls;
	private QueryListComponent selectSlot;
	private JComboBox cmbTypes;
	private DefaultComboBoxModel cmbModelTypes;
	private final DefaultComboBoxModel cmbModelBoolean = new DefaultComboBoxModel(new String[] { TRUE, FALSE });
	private DefaultComboBoxModel cmbModelSymbol = new DefaultComboBoxModel(NULL);
	private static final QueryLabeledComponent NULL_COMPONENT = new QueryLabeledComponent("", new JLabel(""));
	private QueryLabeledComponent valueComponent = NULL_COMPONENT;
	private JPanel pnlQueryComponents;
	private ValueType currentValueType = null;
	
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
		public void valueChanged(Object value) {
			// remove the old value component
			pnlQueryComponents.remove(valueComponent);
			if (value == null) {
				// reset the types and value components
				cmbModelTypes.removeAllElements();
				cmbModelTypes.addElement("");
				valueComponent = NULL_COMPONENT;
				currentValueType = null;
			} else if (value instanceof Slot) {
				Slot slot = (Slot) value;
				// update the types combobox (if different from current type)
				ValueType type = slot.getValueType();
				if (!type.equals(currentValueType)) {
					currentValueType = type;
					
					String[] types = typesMap.get(type);
					cmbModelTypes.removeAllElements();
					for (int i = 0; i < types.length; i++) {
						cmbModelTypes.addElement(types[i]);
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
			pnlQueryComponents.add(valueComponent);
			pnlQueryComponents.revalidate();
			pnlQueryComponents.repaint();
		}
	};
	
	public QueryComponent(KnowledgeBase kb, Collection<Slot> slots) {
		this.kb = kb;
		this.slots = slots;
		this.allSlots = slots;
		this.typesMap = new HashMap<ValueType, String[]>(15);
		this.typesToComponentMap = new HashMap<ValueType, QueryLabeledComponent>(15);
		
		initialize();
	}
	
	private void initializeTypesToComponents() {
		typesToComponentMap.clear();
		QueryListComponent clsComp = new QueryListComponent("", kb);
		clsComp.setActions(createViewAction(clsComp, "View Cls", Icons.getViewClsIcon()),
						   createSelectClsAction(clsComp, "Select Cls", Icons.getAddClsIcon(), false),
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

	private void initializeTypes() {
		String[] string = { EXACT_MATCH, CONTAINS, STARTS_WITH, ENDS_WITH, SOUNDS_LIKE };	// any, string
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
	
	public Query getQuery() {
		Slot slot = (Slot) selectSlot.getObject();
		if (slot == null) {
			JOptionPane.showMessageDialog(this, "Please choose a slot", "Choose a slot", JOptionPane.ERROR_MESSAGE);
			selectSlot.focus();
			return null;
		}

		Query q = null;
		ValueType type = slot.getValueType();
		if (ValueType.ANY.equals(type) || ValueType.STRING.equals(type)) {
			q = getStringQuery(slot);
		} else if (ValueType.BOOLEAN.equals(type) || ValueType.SYMBOL.equals(type) ||
				   ValueType.INTEGER.equals(type) || ValueType.FLOAT.equals(type)) {
			q = new OwnSlotValueQuery(slot, getExpression());
		} else if (ValueType.CLS.equals(type) || ValueType.INSTANCE.equals(type)) {
			// TODO what should go here?
			q = new OwnSlotValueQuery(slot, getExpression());
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
	
	private Query getStringQuery(Slot slot) {
		Query q;
		String type = (String) cmbTypes.getSelectedItem();
		if (SOUNDS_LIKE.equals(type)) {
			q = new PhoneticQuery(slot, getExpression());
		} else {
			boolean startsWith = STARTS_WITH.equals(type) || CONTAINS.equals(type);
			boolean endsWith = ENDS_WITH.equals(type) || CONTAINS.equals(type);
			String expr = getExpression();
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
	
	private void initialize() {
		initializeTypes();
		initializeTypesToComponents();
		
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(500, 56));
		setMaximumSize(new Dimension(2000, 56));
		//setBackground(Color.white);
		
		// set the display name slot
		selectCls = new QueryListComponent("Class", kb);
		selectCls.setActions(createViewAction(selectCls, "View Cls", Icons.getViewClsIcon()),
							 createSelectClsAction(selectCls, "Select Cls", Icons.getAddClsIcon(), true),
							 createRemoveAction(selectCls, "Remove Cls", Icons.getRemoveClsIcon()));
		selectCls.addListener(clsListener);

		selectSlot = new QueryListComponent("Slot", kb);
		selectSlot.setActions(createViewAction(selectSlot, "View Slot", Icons.getViewSlotIcon()), 
							  createSelectSlotAction(selectSlot, "Select Slot", Icons.getAddSlotIcon()),
							  createRemoveAction(selectSlot, "Remove Slot", Icons.getRemoveSlotIcon()));
		selectSlot.addListener(slotListener);

		pnlQueryComponents = new JPanel(new GridLayout(1, 4, 5, 5));
		pnlQueryComponents.add(selectCls);
		pnlQueryComponents.add(selectSlot);
		pnlQueryComponents.add(new LabeledComponent("", getTypesComboBox(), false));
		pnlQueryComponents.add(valueComponent);
		add(pnlQueryComponents, BorderLayout.CENTER);

		// set the default slot (:NAME)
		selectSlot.setObject(kb.getNameSlot());
	}
	
	private JComboBox getTypesComboBox() {
		if (cmbTypes == null) {
			cmbModelTypes = new DefaultComboBoxModel();
			cmbModelTypes.addElement("");
			cmbTypes = new JComboBox(cmbModelTypes);
			cmbTypes.setSelectedIndex(0);
		}
		return cmbTypes;
	}
	   
	
	/** Creates an action used to view a Frame.  Initially disabled. */
	private Action createViewAction(final QueryListComponent comp, String name, Icon icon) {
        Action action = new AbstractAction(name, icon) {
            public void actionPerformed(ActionEvent event) {
            	comp.viewObject();
            }
        };
        action.setEnabled(false);
        return action;
    }
    
	private Action createSelectClsAction(final QueryListComponent comp, final String name, Icon icon, final boolean updateSlotComponent) {
		return new AbstractAction(name, icon) {
			public void actionPerformed(ActionEvent e) {
				Cls cls = DisplayUtilities.pickCls(comp, kb, kb.getRootClses(), name);
				comp.setObject(cls);
			}
		};
	}
	
	private Action createSelectInstanceAction(final QueryListComponent comp, final String name, Icon icon) {
		return new AbstractAction(name, icon) {
			public void actionPerformed(ActionEvent e) {
				Slot slot = (Slot) selectSlot.getObject();
				Collection clses = (slot == null ? kb.getRootClses() : slot.getAllowedClses());
				Instance inst = DisplayUtilities.pickInstance(comp, clses);
				comp.setObject(inst);
			}
		};
	}	
	
	/** Creates an action used to select a value for the QueryListComponent.  Initially enabled. */
	private Action createSelectSlotAction(final QueryListComponent comp, final String name, Icon icon) {
		return new AbstractAction(name, icon) {
			public void actionPerformed(ActionEvent e) {
				Slot slot = DisplayUtilities.pickSlot(comp, slots, name);
				comp.setObject(slot);
			}
		};
	}	
	
	/** Creates an action used to remove a value from the QueryListComponent.  Initially disabled. */
    private Action createRemoveAction(final QueryListComponent comp, String name, Icon icon) {
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
    
}
