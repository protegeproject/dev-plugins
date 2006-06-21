package edu.stanford.smi.protege.server.plugin;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.framestore.RemoteClientStats;
import edu.stanford.smi.protege.server.framestore.background.FrameCalculatorStats;
import edu.stanford.smi.protege.widget.AbstractTabWidget;

// an example tab
public class ServerStatsPlugin extends AbstractTabWidget {
  private UserInfoTable userInfo;
  private JTable userInfoTable;
  private boolean infoAreaWritten = false;
  private JTextArea infoArea;

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
    setLayout(new GridBagLayout());
    
    infoArea = new JTextArea();
    infoArea.setRows(4);
    infoArea.setColumns(80);
    add(infoArea);
    
    userInfo = new UserInfoTable();
    userInfoTable = new JTable();
    userInfoTable.setModel(userInfo);
    add(new JScrollPane(userInfoTable));

    add(refreshButton);
    add(clearCacheButton);
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
  
  private void refresh() {
    RemoteClientFrameStore client = getRemoteClientFrameStore();
    RemoteClientStats clientStats = client.getClientStats();
    long startTime = System.currentTimeMillis();
    FrameCalculatorStats serverStats = client.getServerStats();
    long interval = System.currentTimeMillis() - startTime;
    
    if (infoAreaWritten) {
      infoArea.replaceRange(null, 0, 3);
    }
    int total = clientStats.getCacheHits() + clientStats.getCacheMisses();
    if (total != 0)  {
      float rate = ((float) 100) * ((float) clientStats.getCacheHits()) / ((float) total);
      infoArea.append("Client Cache Hit rate: " + rate + "\n");
    } else {
      infoArea.append("Caching not started\n");
    }
    
    total = clientStats.getClosureCacheHits() + clientStats.getClosureCacheMisses();
    if (total != 0) {
      float rate = ((float) 100) * ((float) clientStats.getClosureCacheHits()) / ((float) total);
      infoArea.append("Client Closure Cache Hit rate: " + rate + "\n");
    } else {
      infoArea.append("Closure Caching not started\n");
    }
    
    infoArea.append("Server is taking " + serverStats.getPrecalculateTime() 
                    + "ms to pre-cache a frame\n");
    infoArea.append("Round trip = " + interval + " ms");
    infoAreaWritten = true;
    
    userInfo.setUserInfo(client.getUserInfo(), serverStats);

  }
  
  private JTextField createOutputTextField(int size) {
    JTextField field = new JTextField(size);
    field.setEnabled(false);
    field.setHorizontalAlignment(SwingConstants.LEFT);
    return field;
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
