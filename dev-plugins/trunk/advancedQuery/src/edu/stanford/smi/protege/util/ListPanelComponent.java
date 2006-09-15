package edu.stanford.smi.protege.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

/**
 * Holds a {@link JPanel} and adds 3 buttons on the right: move up, remove, and move down.
 *
 * @author Chris Callendar
 * @date 14-Sep-06
 */
public class ListPanelComponent extends JPanel {

	private ListPanel listPanel;
	private JPanel pnlMain;
	private JToolBar rightToolbar;
	
	private Action upAction;
	private Action downAction;
	private Action removeAction;

	public ListPanelComponent(ListPanel listPanel, JPanel mainPanel) {
		super(new BorderLayout(2, 0));
		this.listPanel = listPanel;
		this.pnlMain = mainPanel;
		
		initialize();
	}
	
	public JPanel getMainPanel() {
		return pnlMain;
	}

	private void initialize() {
		setPreferredSize(new Dimension(500, 56));
		setMaximumSize(new Dimension(5000, 56));
		
		add(pnlMain, BorderLayout.CENTER);
		add(getRightButtonsPanel(), BorderLayout.EAST);
	}
	
	public void setUpActionToolTip(String tt) {
		upAction.putValue(Action.SHORT_DESCRIPTION, tt);
	}

	public void setDownActionToolTip(String tt) {
		downAction.putValue(Action.SHORT_DESCRIPTION, tt);
	}

	public void setRemoveActionToolTip(String tt) {
		removeAction.putValue(Action.SHORT_DESCRIPTION, tt);
	}

	private JToolBar getRightButtonsPanel() {
		if (rightToolbar == null) {
			rightToolbar = new JToolBar(JToolBar.VERTICAL);
			rightToolbar.setOpaque(false);
			rightToolbar.setRollover(true);
			rightToolbar.setFloatable(false);
			rightToolbar.setBorderPainted(false);
			rightToolbar.setBorder(null);

			final Dimension dim = new Dimension(21, 56);
			rightToolbar.setPreferredSize(dim);
			rightToolbar.setMinimumSize(dim);
			rightToolbar.setMaximumSize(dim);
			
			upAction = new AbstractAction(null, getIcon("up.gif")) {
				public void actionPerformed(ActionEvent e) {
					listPanel.moveUp(ListPanelComponent.this);
				}
			};
			rightToolbar.add(addButton(upAction, "Move up"));

			removeAction = new AbstractAction(null, getIcon("remove.gif")) {
				public void actionPerformed(ActionEvent e) {
					listPanel.removePanel(ListPanelComponent.this);
				}
			};
			rightToolbar.add(addButton(removeAction, "Remove"));
			
			downAction = new AbstractAction(null, getIcon("down.gif")) {
				public void actionPerformed(ActionEvent e) {
					listPanel.moveDown(ListPanelComponent.this);
				}
			};
			rightToolbar.add(addButton(downAction, "Move down"));
		}
		return rightToolbar;
	}
	
	private JButton addButton(Action action, String text) {
		JButton btn = new JButton(action);
		btn.setText(null);
		btn.setToolTipText(text);
		btn.setOpaque(false);
        btn.setRolloverEnabled(true);
        final Dimension dim = new Dimension(19, 18);
		btn.setPreferredSize(dim);
		btn.setMaximumSize(dim);
		btn.setMinimumSize(dim);
		//btn.setBorder(null);
		return btn;
	}

	private Icon getIcon(String path) {
        URL url = ListPanelComponent.class.getResource(path);
        return (url != null ? new ImageIcon(url) : null);
	}

	public static void main(String[] args) {
		JDialog dlg = new JDialog();
		dlg.setModal(true);
		dlg.setTitle("Test");
		
		final ListPanel listPanel = new ListPanel();
		dlg.getContentPane().add(new JScrollPane(listPanel), BorderLayout.CENTER);
		
		Action action = new AbstractAction("Add") {
			public void actionPerformed(ActionEvent e) {
				JPanel pnl = new JPanel();
				pnl.add(new JLabel("Label #" + (listPanel.getPanelCount()+1)));
				ListPanelComponent comp = new ListPanelComponent(listPanel, pnl);
				listPanel.addPanel(comp);
			}
		};
		
		for (int i = 1; i < 4; i++) {
			action.actionPerformed(null);
		}
		
		JPanel pnl = new JPanel(new FlowLayout());
		dlg.getContentPane().add(pnl, BorderLayout.SOUTH);
		pnl.add(new JButton(action));
		
		dlg.setSize(550, 250);
		dlg.setVisible(true);
		System.exit(0);
	}
	
}
