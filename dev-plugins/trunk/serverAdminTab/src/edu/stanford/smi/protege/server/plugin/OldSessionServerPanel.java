package edu.stanford.smi.protege.server.plugin;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.server.RemoteSession;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.framestore.background.FrameCalculatorStats;
import edu.stanford.smi.protege.server.job.KillUserSessionJob;
import edu.stanford.smi.protege.server.metaproject.MetaProjectConstants;
import edu.stanford.smi.protege.util.AllowableAction;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.SelectableTable;


public class OldSessionServerPanel extends AbstractServerPanel {

	private boolean debugServerCheck = false;

	private UserInfoTable userInfo;
	private SelectableTable userInfoTable;

	public OldSessionServerPanel(KnowledgeBase kb) {
		super(kb);
	}

	@Override
	protected JComponent createCenterComponent() {
		  userInfo = new UserInfoTable();
		  userInfoTable = new SelectableTable();
		  userInfoTable.setModel(userInfo);

		  LabeledComponent lc = new LabeledComponent("Live sessions", new JScrollPane(userInfoTable), true);
		  lc.addHeaderButton(getKillSessionAction());

		  refresh();
		  return lc;
	}


	private AllowableAction getKillSessionAction() {
		return new AllowableAction("Kill session", Icons.getCancelIcon(), userInfoTable) {

			public void actionPerformed(ActionEvent arg0) {
				int row = userInfoTable.getSelectedRow();
				RemoteSession session = userInfo.getSession(row);

	             int kill =
	                  JOptionPane.showConfirmDialog(userInfoTable, "Kill session for user " + session.getUserName() +
	                                                "? This user may lose work as a result of this",
	                                                "Confirm Kill Session",
	                                                JOptionPane.OK_CANCEL_OPTION);
	              if (kill == JOptionPane.OK_OPTION ) {
	                  new KillUserSessionJob(session, getKnowledgeBase()).execute();
	                  refresh();
	              }

			}

			@Override
			public boolean isAllowed() {
				int row = userInfoTable.getSelectedRow();
				RemoteSession session = userInfo.getSession(row);
				return !debugServerCheck && !isKillAllowed(session);
			}

		};
	}

	@Override
	public void refresh() {
		 RemoteClientFrameStore clientFS = getRemoteClientFrameStore();
		 FrameCalculatorStats serverStats = clientFS.getServerStats();
		 userInfo.setUserInfo(clientFS.getUserInfo(), serverStats);
	}

	 private boolean isKillAllowed(RemoteSession session) {
	      KnowledgeBase kb = getKnowledgeBase();
	      String me = kb.getUserName();
	      return RemoteClientFrameStore.isOperationAllowed(kb, MetaProjectConstants.OPERATION_KILL_OTHER_USER_SESSION) ||
	                  me != null && me.equals(session.getUserName());
	  }

}
