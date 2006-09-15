package edu.stanford.smi.protege.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.Border;


/**
 * This class is meant to immitate a {@link JList} which contains {@link JPanel} objects 
 * that can be selected and moved up or down. 
 *
 * @author Chris Callendar
 * @date 13-Sep-06
 */
public class ListPanel extends JPanel {

	private static final Color COLOR_SELECTED = new Color(240, 230, 202);
	private static final Border BORDER_NOFOCUS = new CustomLineBorder(null, null, Color.lightGray, null, 1);
	private static final Border BORDER_FOCUS = BorderFactory.createLineBorder(new Color(245, 165, 16), 1);

	class MouseFocus extends MouseAdapter implements FocusListener {
		public void focusLost(FocusEvent e) {}
		public void focusGained(FocusEvent e) {
			if (e.getSource() instanceof JPanel) {
				focus((JPanel) e.getSource());
			}
		}
		public void mouseClicked(MouseEvent e) {
			if (e.getSource() instanceof JPanel) {
				focus((JPanel) e.getSource());
			}
		}
		private void focus(JPanel pnl) {
			if (pnl != focussedPnl) {
				if (focussedPnl != null) {
					ListPanel.unfocus(focussedPnl);
				}
				focussedPnl = pnl;
			}
			ListPanel.focus(focussedPnl);
		}
	}
	
	private ArrayList<ListPanelListener> listeners;
	private ArrayList<JPanel> panels = new ArrayList<JPanel>();
	private Box emptyBox = null;
	private MouseFocus listener = new MouseFocus();
	private JPanel focussedPnl = null;

	public ListPanel() {
		super();
		this.listeners = new ArrayList<ListPanelListener>(1);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Color.white);		
		add(getEmptyBox());
	}
	
	public void addListener(ListPanelListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(ListPanelListener listener) {
		this.listeners.remove(listener);
	}
	
	public void addPanel(JPanel pnl) {
		addPanel(pnl, panels.size());
	}

	public void addPanel(final JPanel pnl, int index) {
		remove(emptyBox);
		pnl.addMouseListener(listener);
		pnl.addFocusListener(listener);
		if (index <= panels.size()) {
			panels.add(index, pnl);
			add(pnl, index);
		} else {
			panels.add(pnl);
			add(pnl);
		}
		add(emptyBox);
		revalidate();
		firePanelAdded(pnl);
		pnl.setBorder(BORDER_NOFOCUS);
		pnl.setFocusable(true);
		ListPanel.addFocusListenerRecursively(pnl, new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				listener.focus(pnl);
			}
		});
	}
	
	protected void firePanelAdded(JPanel pnl) {
		for (Iterator<ListPanelListener> iter = listeners.iterator(); iter.hasNext(); ) {
			iter.next().panelAdded(pnl, this);
		}
	}

	protected void firePanelRemoved(JPanel pnl) {
		for (Iterator<ListPanelListener> iter = listeners.iterator(); iter.hasNext(); ) {
			iter.next().panelRemoved(pnl, this);
		}
	}
	
	public void removePanel(JPanel pnl) {
		remove(pnl);
		panels.remove(pnl);
		revalidate();
		repaint();
		pnl.removeMouseListener(listener);
		pnl.removeFocusListener(listener);
		firePanelRemoved(pnl);
		
		if (pnl == focussedPnl) {
			focussedPnl = null;
		}
	}

	public void removePanel(int index) {
		if ((index >= 0) && (index < panels.size())) {
			removePanel((JPanel)panels.get(index));
		}
	}
	

	public void removeAllPanels() {
		panels.clear();
		removeAll();
		focussedPnl = null;
	}		
	
	public void reloadPanels() {
		removeAll();
		for (int i = 0; i < panels.size(); i++) {
			add(panels.get(i));
		}
		revalidate();
		if (focussedPnl != null) {
			focussedPnl.requestFocus();
		}
	}
	
	private Box getEmptyBox() {
		if (emptyBox == null) {
			emptyBox = Box.createVerticalBox();
		    emptyBox.add(Box.createVerticalGlue());
		    emptyBox.add(Box.createVerticalStrut(250));
		}
		return emptyBox;
	}	
	
	private static void addFocusListenerRecursively(Container container, FocusListener listener) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component child = container.getComponent(i);
			child.addFocusListener(listener);
			if (child instanceof Container) {
				addFocusListenerRecursively((Container)child, listener);					
			}
		}
	}
	
	public static void removeFocusListenerRecursively(Container container, FocusListener listener) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component child = container.getComponent(i);
			child.removeFocusListener(listener);
			if (child instanceof Container) {
				removeFocusListenerRecursively((Container)child, listener);					
			}
		}
	}	
	
	private static void focus(JPanel pnl) {
		pnl.setBorder(BORDER_FOCUS);
		pnl.setBackground(COLOR_SELECTED);
	}
	
	private static void unfocus(JPanel pnl) {
		pnl.setBorder(BORDER_NOFOCUS);
		pnl.setBackground(Color.white);
	}

	public void moveSelectedUp() {
		JPanel pnl = focussedPnl;
		moveUp(pnl);
	}
	
	public void moveUp(JPanel pnl) {
		if (pnl != null) {
			int index = -1;
			for (int i = 0; i < panels.size(); i++) {
				if (pnl == panels.get(i)) {
					index = i;
					break;
				}
			}
			if (index > 0) {
				removePanel(pnl);
				addPanel(pnl, index - 1);
				pnl.requestFocus();
			}
		}
	}	

	public void moveSelectedDown() {
		JPanel pnl = focussedPnl;
		moveDown(pnl);
	}
	
	public void moveDown(JPanel pnl) {
		if (pnl != null) {
			int index = -1;
			for (int i = 0; i < panels.size(); i++) {
				if (pnl == panels.get(i)) {
					index = i;
					break;
				}
			}
			if ((index >= 0) && ((index + 1) < panels.size())) {
				removePanel(pnl);
				addPanel(pnl, index + 1);
				pnl.requestFocus();
			}
		}
	}	

	public int getPanelCount() {
		return panels.size();
	}

	public Collection<JPanel> getPanels() {
		return panels;
	}

}
