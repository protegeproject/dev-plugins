package edu.stanford.smi.protege.server.plugin;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.framestore.RemoteClientStats;
import edu.stanford.smi.protege.server.framestore.background.FrameCalculatorStats;
import edu.stanford.smi.protege.util.transaction.TransactionIsolationLevel;
import edu.stanford.smi.protege.widget.AbstractTabWidget;

// an example tab
public class ServerStatsPlugin extends AbstractTabWidget {

  /**
   * 
   */
  private static final long serialVersionUID = -7384785943184573895L;
  private UserInfoTable userInfo;
  private JTable userInfoTable;

  private JTextField clientCacheText;
  private JTextField clientClosureCacheText;
  private JTextField roundTripText;
  private JTextField serverSpeedText;
  private JTextField txLevelText;
  
  private JButton refreshButton;
  private JButton clearCacheButton;
  
  public void initialize() {
    setLabel("Server Stats");
    setIcon(Icons.getInstanceIcon());
    
    createRefreshButton();
    createClearCacheButton();
    layoutUI();
    refresh();
  }
  
  private void layoutUI() {
    setLayout(new BorderLayout());
    
    add(createTextArea(), BorderLayout.NORTH);
    
    userInfo = new UserInfoTable();
    userInfoTable = new JTable();
    userInfoTable.setModel(userInfo);
    add(new JScrollPane(userInfoTable), BorderLayout.CENTER);

    JPanel buttonArea = new JPanel(new GridLayout(1,2));
    buttonArea.add(refreshButton);
    buttonArea.add(clearCacheButton);
    add(buttonArea, BorderLayout.SOUTH);
  }
  
  private JPanel createTextArea() {
    int col2size = 8;
    JPanel textArea = new JPanel(new GridLayout(5,2));
    
    textArea.add(new JLabel("Client Cache Hit rate:"));
    clientCacheText = createOutputTextField(col2size);
    textArea.add(clientCacheText);
    
    textArea.add(new JLabel("Client Closure Cache rate:"));
    clientClosureCacheText = createOutputTextField(col2size);
    textArea.add(clientClosureCacheText);
    
    textArea.add(new JLabel("Estimated round trip time:"));
    roundTripText = createOutputTextField(col2size);
    textArea.add(roundTripText);
    
    textArea.add(new JLabel("Milliseconds to calculate Frame Cache"));
    serverSpeedText = createOutputTextField(col2size);
    textArea.add(serverSpeedText);
    
    textArea.add(new JLabel("Transaction Isolation Level:"));
    txLevelText = createOutputTextField(col2size);
    textArea.add(txLevelText);
    
    return textArea;
  }
  
  private void createRefreshButton() {
    refreshButton = new JButton("Refresh Server Stats");
    refreshButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        refresh();
      }
    });
  }
  
  private void createClearCacheButton() {
    clearCacheButton = new JButton("Clear Client Cache");
    clearCacheButton.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        RemoteClientFrameStore client = getRemoteClientFrameStore();
        client.flushCache();
        refresh();
      }
    });
  }
  
  private JTextField createOutputTextField(int size) {
    JTextField field = new JTextField(size);
    field.setEnabled(false);
    field.setHorizontalAlignment(SwingConstants.LEFT);
    return field;
  }
  
  private void refresh() {
    RemoteClientFrameStore client = getRemoteClientFrameStore();
    RemoteClientStats clientStats = client.getClientStats();
    long startTime = System.currentTimeMillis();
    FrameCalculatorStats serverStats = client.getServerStats();
    long interval = System.currentTimeMillis() - startTime;
    
    int total = clientStats.getCacheHits() + clientStats.getCacheMisses();
    if (total != 0)  {
      float rate = ((float) 100) * ((float) clientStats.getCacheHits()) / ((float) total);
      clientCacheText.setText("" + rate);
    } else {
      clientCacheText.setText("0/0");
    }
    
    total = clientStats.getClosureCacheHits() + clientStats.getClosureCacheMisses();
    if (total != 0) {
      float rate = ((float) 100) * ((float) clientStats.getClosureCacheHits()) / ((float) total);
      clientClosureCacheText.setText("" + rate);
    } else {
      clientClosureCacheText.setText("Closure Caching not started\n");
    }
    
    roundTripText.setText("" + interval);
    
    serverSpeedText.setText("" + serverStats.getPrecalculateTime());
    
    TransactionIsolationLevel level = client.getTransactionIsolationLevel();
    txLevelText.setText(level == null ? "error" : level.toString());
    
    userInfo.setUserInfo(client.getUserInfo(), serverStats);

  }
  
  public RemoteClientFrameStore getRemoteClientFrameStore() {
    DefaultKnowledgeBase kb = (DefaultKnowledgeBase) getProject().getKnowledgeBase();
    return (RemoteClientFrameStore) kb.getTerminalFrameStore();
  }
  
  public static boolean isSuitable(Project project, Collection errors) {
    KnowledgeBase kb = project.getKnowledgeBase();
    if (!(kb instanceof DefaultKnowledgeBase)) {
      errors.add("Knowledge base is not recognized");
      return false;
    }
    DefaultKnowledgeBase dkb = (DefaultKnowledgeBase) kb;
    boolean ret = dkb.getTerminalFrameStore() instanceof RemoteClientFrameStore;
    if (!ret) {
      errors.add("Not a client");
    }
    return ret;
  }
}
