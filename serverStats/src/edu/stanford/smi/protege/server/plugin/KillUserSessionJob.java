package edu.stanford.smi.protege.server.plugin;


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.server.RemoteSession;
import edu.stanford.smi.protege.server.Server;
import edu.stanford.smi.protege.server.ServerProject;
import edu.stanford.smi.protege.server.framestore.RemoteServerFrameStore;
import edu.stanford.smi.protege.server.framestore.ServerFrameStore;
import edu.stanford.smi.protege.server.framestore.ServerSessionLost;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.ProtegeJob;

public class KillUserSessionJob extends ProtegeJob {
    private RemoteSession sessionToKill;
    
    public KillUserSessionJob(RemoteSession session, KnowledgeBase kb) {
        super(kb);
        this.sessionToKill = session;
    }


    public Object run() throws ProtegeException {
        if (!isAllowed()) {
            return Boolean.FALSE;
        }
        boolean failures = false;
        Server server = Server.getInstance();
        Collection<ServerProject> projects = server.getCurrentProjects(sessionToKill);
        if (projects == null) {
            return Boolean.TRUE;
        }
        projects = new ArrayList<ServerProject>(projects);
        for (ServerProject project : projects) {
            try {
                project.close(sessionToKill);
            } catch (ServerSessionLost e) {
                Log.getLogger().log(Level.WARNING, "Could not close session", e);
                failures = true;
            }
        }
        return Boolean.valueOf(!failures);
    }

    private boolean isAllowed() {
        Project project = getKnowledgeBase().getProject();
        ServerProject serverProject = Server.getInstance().getServerProject(project);
        RemoteSession mySession = ServerFrameStore.getCurrentSession();
        RemoteServerFrameStore fs = serverProject.getDomainKbFrameStore(mySession);

        try {
            return sessionToKill.getUserName().equals(mySession.getUserName()) || 
                fs.getAllowedOperations(mySession).contains(ServerStatsPlugin.KILL_OTHER_USER_SESSION);
        } catch (RemoteException e) {
            Log.getLogger().log(Level.WARNING, "Caught Exception trying to check permissions", e);
            return false;
        }
    }
}
