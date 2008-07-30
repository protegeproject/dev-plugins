package edu.stanford.smi.protege.server.plugin;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.framestore.RemoteClientStats;
import edu.stanford.smi.protege.server.framestore.background.FrameCalculatorStats;
import edu.stanford.smi.protege.util.transaction.TransactionIsolationLevel;

public class StatsServerPanel extends AbstractServerPanel {

	private static final long serialVersionUID = 7877705769030877408L;

	private UserInfoTable userInfo;
	private JTable userInfoTable;

	private JTextField clientCacheText;
	private JTextField clientClosureCacheText;
	private JTextField roundTripText;
	private JTextField serverSpeedText;
	private JTextField txLevelText;

	public StatsServerPanel(KnowledgeBase kb) {
		super(kb);
	}

	@Override
	protected JComponent createCenterComponent() {
		JComponent comp = createTextArea();
		refresh();
		return comp;
	}

	@Override
	protected JComponent createFooterComponent() {
		JComponent footComp = super.createFooterComponent();
		footComp.add(createClearCacheButton());
		return footComp;
	}

	private void layoutUI() {
		setLayout(new BorderLayout());

		add(createTextArea(), BorderLayout.NORTH);

		userInfo = new UserInfoTable();
		userInfoTable = new JTable();
		userInfoTable.setModel(userInfo);
		add(new JScrollPane(userInfoTable), BorderLayout.CENTER);
	}

	private JPanel createTextArea() {
		int col2size = 8;
		JPanel wrappingPanel = new JPanel(new GridBagLayout());

		JPanel statsPanel = new JPanel(new GridLayout(5,2));
		statsPanel.setBorder(BorderFactory.createTitledBorder("Caching"));

		statsPanel.add(new JLabel("Client Cache Hit rate:"));
		clientCacheText = createOutputTextField(col2size);
		statsPanel.add(clientCacheText);

		statsPanel.add(new JLabel("Client Closure Cache rate:"));
		clientClosureCacheText = createOutputTextField(col2size);
		statsPanel.add(clientClosureCacheText);

		statsPanel.add(new JLabel("Estimated round trip time:"));
		roundTripText = createOutputTextField(col2size);
		statsPanel.add(roundTripText);

		statsPanel.add(new JLabel("Milliseconds to calculate Frame Cache:"));
		serverSpeedText = createOutputTextField(col2size);
		statsPanel.add(serverSpeedText);

		statsPanel.add(new JLabel("Transaction Isolation Level:"));
		txLevelText = createOutputTextField(col2size);
		statsPanel.add(txLevelText);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		wrappingPanel.add(statsPanel,c);

		return wrappingPanel;

	}


	private JButton createClearCacheButton() {
		JButton clearCacheButton = new JButton("Clear Client Cache");
		clearCacheButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				RemoteClientFrameStore client = getRemoteClientFrameStore();
				client.flushCache();
				refresh();
			}
		});
		return clearCacheButton;
	}

	private JTextField createOutputTextField(int size) {
		JTextField field = new JTextField(size);
		field.setEnabled(false);
		field.setHorizontalAlignment(SwingConstants.LEFT);
		return field;
	}

	@Override
	public void refresh() {
		RemoteClientFrameStore client = getRemoteClientFrameStore();
		RemoteClientStats clientStats = client.getClientStats();
		long startTime = System.currentTimeMillis();
		FrameCalculatorStats serverStats = client.getServerStats();
		long interval = System.currentTimeMillis() - startTime;

		int total = clientStats.getCacheHits() + clientStats.getCacheMisses();
		if (total != 0)  {
			float rate = (float) 100 * (float) clientStats.getCacheHits() / total;
			clientCacheText.setText("" + rate);
		} else {
			clientCacheText.setText("0/0");
		}

		total = clientStats.getClosureCacheHits() + clientStats.getClosureCacheMisses();
		if (total != 0) {
			float rate = (float) 100 * (float) clientStats.getClosureCacheHits() / total;
			clientClosureCacheText.setText("" + rate);
		} else {
			clientClosureCacheText.setText("Closure Caching not started\n");
		}

		roundTripText.setText("" + interval);

		serverSpeedText.setText("" + serverStats.getPrecalculateTime());

		TransactionIsolationLevel level = client.getTransactionIsolationLevel();
		txLevelText.setText(level == null ? "error" : level.toString());
	}

}
