package edu.stanford.smi.protege.server.plugin;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;

public abstract class AbstractServerPanel extends JPanel {

	private KnowledgeBase kb;
	private JComponent centerComponent;
	private JComponent footerComponent;

	public AbstractServerPanel(KnowledgeBase kb) {
		this.kb = kb;
		init();
	}

	protected void init() {
		setLayout(new BorderLayout());

		add(centerComponent = createCenterComponent(), BorderLayout.CENTER);
		add(footerComponent = createFooterComponent(), BorderLayout.SOUTH);

		addRefreshButton();
	}


	protected JComponent createFooterComponent() {
		return new JPanel();
	}

	protected JComponent createCenterComponent() {
		return new JPanel();
	}

	protected void addRefreshButton() {
		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				refresh();
			}
		});
		footerComponent.add(refreshButton);
	}

	public void refresh() {}

	public KnowledgeBase getKnowledgeBase() {
		return kb;
	}

	public JComponent getCenterComponent() {
		return centerComponent;
	}

	public JComponent getFooterComponent() {
		return footerComponent;
	}

	public RemoteClientFrameStore getRemoteClientFrameStore() {
		return (RemoteClientFrameStore) ((DefaultKnowledgeBase)kb).getTerminalFrameStore();
	}

}
