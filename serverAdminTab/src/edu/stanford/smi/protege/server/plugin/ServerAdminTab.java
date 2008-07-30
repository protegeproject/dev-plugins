package edu.stanford.smi.protege.server.plugin;

import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.resource.ResourceKey;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.metaproject.MetaProjectConstants;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.widget.AbstractTabWidget;

public class ServerAdminTab extends AbstractTabWidget {

	private static final long serialVersionUID = -8915505640982212662L;

	private JTabbedPane tabbedPane;

	public void initialize() {
		setLabel("Server Admin");
		setIcon(Icons.getIcon(new ResourceKey("configure")));
		tabbedPane = ComponentFactory.createTabbedPane(true);

		tabbedPane.addTab("Stats", createStatsTab());
		tabbedPane.addTab("Sessions", createSessionsTab());
		tabbedPane.addTab("Projects", createProjectsTab());

		add(tabbedPane);

	}


	private JPanel createStatsTab() {
		return new StatsServerPanel(getKnowledgeBase());
	}

	private JPanel createSessionsTab() {
		return new SessionServerPanel(getKnowledgeBase());
	}

	private JPanel createProjectsTab() {
		return new ProjectsServerPanel(getKnowledgeBase());
	}


	public static boolean isSuitable(Project project, Collection errors) {
		if (!project.isMultiUserClient()) {
			errors.add("Can be used only in multi-user client mode.");
			return false;
		}
		if (!RemoteClientFrameStore.isOperationAllowed(project.getKnowledgeBase(), MetaProjectConstants.OPERATION_CONFIGURE_SERVER)) {
			errors.add("No priviledges to configure server");
			return false;
		}
		return true;
	}
}
