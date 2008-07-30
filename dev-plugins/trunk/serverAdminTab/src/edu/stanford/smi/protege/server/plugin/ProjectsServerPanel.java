package edu.stanford.smi.protege.server.plugin;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.server.RemoteProjectUtil;
import edu.stanford.smi.protege.server.RemoteServer;
import edu.stanford.smi.protege.server.RemoteSession;
import edu.stanford.smi.protege.server.ServerProject.ProjectStatus;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.metaproject.MetaProjectConstants;
import edu.stanford.smi.protege.util.AllowableAction;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.ModalDialog;
import edu.stanford.smi.protege.util.SelectableTable;


public class ProjectsServerPanel extends AbstractServerPanel {

	private DefaultTableModel tableModel;
	private List<String> remoteProjects;
	private AllowableAction stopProjectAction;
	private AllowableAction startProjectAction;

	public ProjectsServerPanel(KnowledgeBase kb) {
		super(kb);
	}

	@Override
	protected JComponent createCenterComponent() {
		remoteProjects = new ArrayList<String>();
		SelectableTable projectsTable = ComponentFactory.createSelectableTable(null);
		projectsTable.setAutoCreateColumnsFromModel(true);
		projectsTable.setModel(createTableModel());

		LabeledComponent lc = new LabeledComponent("Remote projects", new JScrollPane(projectsTable), true);
		lc.addHeaderButton(getStopProjectAction(projectsTable));
		lc.addHeaderButton(getStartProjectAction(projectsTable));

		return lc;
	}


	private TableModel createTableModel() {
		tableModel = new DefaultTableModel();
		tableModel.addColumn("Project");
		tableModel.addColumn("Status");
		tableModel.addColumn("Sessions");

		fillTableModel();

		return tableModel;
	}


	private void fillTableModel() {
		tableModel.setRowCount(0);
		remoteProjects.clear();

		Map<String, Collection<RemoteSession>> projectSessionsMap = getProjectSessionMap();
		for (String project : projectSessionsMap.keySet()) {
			remoteProjects.add(project);
			Collection<RemoteSession> sessions = projectSessionsMap.get(project);

			ProjectStatus status = RemoteProjectUtil.getProjectStatus(getKnowledgeBase(), project);

			tableModel.addRow(new Object[] {project, status, sessions});
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


	private AllowableAction getStopProjectAction(final SelectableTable table) {
		stopProjectAction = new AllowableAction("Stop project", Icons.getCancelIcon(), table) {

			public void actionPerformed(ActionEvent arg0) {
				int row = table.getSelectedRow();
				String project = remoteProjects.get(row);
				ShutDownPanel shutDownPanel = new ShutDownPanel(project);
				int opt = ModalDialog.showDialog(ProjectsServerPanel.this, shutDownPanel,
						"Shut down project " + project, ModalDialog.MODE_OK_CANCEL);
				if (opt == ModalDialog.OPTION_OK) {
					//TODO: fix by tim!
					RemoteProjectUtil.shutdownProject(getKnowledgeBase().getProject(), shutDownPanel.getShutdownInSec());
				}
			}

			@Override
			public void onSelectionChange() {
				int row = table.getSelectedRow();
				if (row < 0) {
					this.setAllowed(false);
					return;
				}
				this.setAllowed(isStopAllowed(row));
			}
		};

		return stopProjectAction;
	}

	private AllowableAction getStartProjectAction(final SelectableTable table) {
		startProjectAction = new AllowableAction("Start project", Icons.getOkIcon(), table) {

			public void actionPerformed(ActionEvent arg0) {
				int row = table.getSelectedRow();
				String project = remoteProjects.get(row);

				int kill =
					JOptionPane.showConfirmDialog(ProjectsServerPanel.this, "Start project " + project +
							"?",
							"Confirm start project",
							JOptionPane.OK_CANCEL_OPTION);
				if (kill == JOptionPane.OK_OPTION ) {
					//new edu.stanford.smi.protege.server.job.KillUserSessionJob(session, getKnowledgeBase()).execute();
					refresh();
				}

			}

			@Override
			public void onSelectionChange() {
				int row = table.getSelectedRow();
				if (row < 0) {
					this.setAllowed(false);
					return;
				}
				this.setAllowed(isStartAllowed(row));
			}
		};

		return startProjectAction;
	}


	@Override
	public void refresh() {
		fillTableModel();
		tableModel.fireTableDataChanged();
	}

	private ProjectStatus getStatus(int row) {
		return (ProjectStatus) tableModel.getValueAt(row, 1);
	}


	private boolean isStopAllowed(int row) {
		//TODO - just for dedugging
		//return getStatus(row).equals(ProjectStatus.READY) &&
			return RemoteClientFrameStore.isOperationAllowed(getKnowledgeBase(), MetaProjectConstants.OPERATION_STOP_REMOTE_PROJECT);
	}

	private boolean isStartAllowed(int row) {
		return getStatus(row).equals(ProjectStatus.CLOSED_FOR_MAINTENANCE) &&
			RemoteClientFrameStore.isOperationAllowed(getKnowledgeBase(), MetaProjectConstants.OPERATION_START_REMOTE_PROJECT);
	}

	class ShutDownPanel extends JPanel {
		private JTextField minsTextField = new JTextField(5);

		ShutDownPanel(String project){
			 SpringLayout layout = new SpringLayout();
			 setLayout(layout);
			 JLabel shLabel = new JLabel("Shutdown time (in minutes):");
			 layout.putConstraint(SpringLayout.WEST, shLabel, 5, SpringLayout.WEST, this);
			 layout.putConstraint(SpringLayout.NORTH, shLabel, 5, SpringLayout.NORTH, this);
			 layout.putConstraint(SpringLayout.WEST, minsTextField, 5, SpringLayout.EAST, shLabel);
			 layout.putConstraint(SpringLayout.NORTH, minsTextField, 3,  SpringLayout.NORTH, this);
			 minsTextField.setText("10");
			 JLabel confLabel = new JLabel("<html><b>Are you sure you want to shut down project " + project + "?</b><br>" +
			 		"This will affect all the users connected currently to this project.</html>");
			 layout.putConstraint(SpringLayout.WEST, confLabel, 5, SpringLayout.WEST, this);
			 layout.putConstraint(SpringLayout.NORTH, confLabel, 20, SpringLayout.SOUTH, shLabel);
			 layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, confLabel);
			 layout.putConstraint(SpringLayout.SOUTH, this, 5, SpringLayout.SOUTH, confLabel);

			 add(shLabel);
			 add(minsTextField);
			 add(confLabel);
		}

		int getShutdownInSec() {
			String str = minsTextField.getText().trim();
			return (int) (Float.valueOf(str) * 60);
		}

	}

}
