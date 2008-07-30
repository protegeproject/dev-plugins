package edu.stanford.smi.protege.server.plugin;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.server.RemoteServer;
import edu.stanford.smi.protege.server.RemoteSession;
import edu.stanford.smi.protege.server.Session;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.metaproject.MetaProjectConstants;
import edu.stanford.smi.protege.util.AllowableAction;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.SelectableTable;


public class SessionServerPanel extends AbstractServerPanel {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss, yyyy.MM.dd");

	private DefaultTableModel tableModel;
	private List<RemoteSession> remoteSessions;
	private AllowableAction killSessionAction;

	public SessionServerPanel(KnowledgeBase kb) {
		super(kb);
	}

	@Override
	protected JComponent createCenterComponent() {
		remoteSessions = new ArrayList<RemoteSession>();
		SelectableTable sessionsTable = ComponentFactory.createSelectableTable(null);
		sessionsTable.setAutoCreateColumnsFromModel(true);
		sessionsTable.setModel(createTableModel());

		LabeledComponent lc = new LabeledComponent("Live sessions", new JScrollPane(sessionsTable), true);
		lc.addHeaderButton(getKillSessionAction(sessionsTable));

		return lc;
	}



	private TableModel createTableModel() {
		tableModel = new DefaultTableModel();
		tableModel.addColumn("Id");
		tableModel.addColumn("Project");
		tableModel.addColumn("User name");
		tableModel.addColumn("User IP");
		tableModel.addColumn("Login time");

		fillTableModel();

		return tableModel;
	}

	private void fillTableModel() {
		tableModel.setRowCount(0);
		remoteSessions.clear();

		RemoteSession mySession = getRemoteClientFrameStore().getSession();

		Map<String, Collection<RemoteSession>> projectSessionsMap = getProjectSessionMap();
		for (String project : projectSessionsMap.keySet()) {
			Collection<RemoteSession> sessions = projectSessionsMap.get(project);
			for (RemoteSession session : sessions) {
				remoteSessions.add(session);

				String loginTime = "(unknown)";
				String sessionId = "(unknown)";

				if (session instanceof Session) {
					Date date = new Date(((Session) session).getStartTime());
					loginTime = dateFormat.format(date);
					sessionId = Integer.toString(((Session)session).getId());

					if (session.equals(mySession)) {
						sessionId = "*" + sessionId;
					}
				}
				tableModel.addRow(new Object[] {sessionId, project, session.getUserName(), session.getUserIpAddress(), loginTime});
			}
		}
	}


	private Map<String, Collection<RemoteSession>> getProjectSessionMap() {
		Map<String, Collection<RemoteSession>> map = new HashMap<String, Collection<RemoteSession>>();

		RemoteServer server = getRemoteClientFrameStore().getRemoteServer();
		RemoteSession session = getRemoteClientFrameStore().getSession();

		try {
			Collection<String> projects = server.getAvailableProjectNames(session);
			for (String project : projects) {
				Collection<RemoteSession> sessions = server.getCurrentSessions(project, session);
				if (sessions != null && sessions.size() > 0) {
					map.put(project, sessions);
				}
			}
		} catch (RemoteException e) {
			Log.getLogger().log(Level.WARNING, "Error at getting remote projects and sessions", e);
		}

		return map;
	}


	private AllowableAction getKillSessionAction(final SelectableTable table) {
		killSessionAction = new AllowableAction("Kill session", Icons.getCancelIcon(), table) {

			public void actionPerformed(ActionEvent arg0) {
				int row = table.getSelectedRow();
				RemoteSession session = remoteSessions.get(row);

				int kill =
					JOptionPane.showConfirmDialog(SessionServerPanel.this, "Kill session for user " + session.getUserName() +
							"? This user may lose work as a result of this",
							"Confirm Kill Session",
							JOptionPane.OK_CANCEL_OPTION);
				if (kill == JOptionPane.OK_OPTION ) {
					new edu.stanford.smi.protege.server.job.KillUserSessionJob(session, getKnowledgeBase()).execute();
					refresh();
				}

			}

			@Override
			public void onSelectionChange() {
				int row = table.getSelectedRow();
				if (row < 0) {
					return;
				}
				RemoteSession session = remoteSessions.get(row);
				this.setAllowed(isKillAllowed(session));
			}
		};

		return killSessionAction;
	}


	@Override
	public void refresh() {
		fillTableModel();
		tableModel.fireTableDataChanged();
	}

	private boolean isKillAllowed(RemoteSession session) {
		KnowledgeBase kb = getKnowledgeBase();
		String me = kb.getUserName();
		return RemoteClientFrameStore.isOperationAllowed(kb, MetaProjectConstants.OPERATION_KILL_OTHER_USER_SESSION) ||
		me != null && me.equals(session.getUserName());
	}

}
