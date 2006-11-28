package edu.stanford.smi.protegex.load;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.logging.Level;

import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.plugin.ProjectPluginAdapter;
import edu.stanford.smi.protege.util.Log;

public class ProtegeLoadPlugin extends ProjectPluginAdapter {
    public final static String url = "//localhost/ProtegeLoad";
    
    private static boolean serviceRunning = false;
    private static LoadService service = new  LoadServiceImpl();
    static {
        try {
            Naming.rebind(url, service);
            serviceRunning = true;
        } catch (RemoteException e) {
            Log.getLogger().warning("Unable to start load service" + e);
        } catch (MalformedURLException e) {
            Log.getLogger().log(Level.SEVERE, "Programmer error", e);
        }
    }
    

    public void afterCreate(Project p) {
        // TODO Auto-generated method stub

    }

    public void afterLoad(Project p) {
        // TODO Auto-generated method stub

    }

    
    /**
     * Called before the close operation for a project
     */
    public void beforeClose(Project p) {
        
    }
    
    public void dispose() {
        // TODO Auto-generated method stub

    }

}
