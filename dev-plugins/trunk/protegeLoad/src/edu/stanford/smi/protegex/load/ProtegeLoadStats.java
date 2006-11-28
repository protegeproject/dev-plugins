package edu.stanford.smi.protegex.load;

import java.io.Serializable;
import java.util.Map;

import edu.stanford.smi.protege.server.framestore.background.FrameCalculatorStats;

public class ProtegeLoadStats implements Serializable {
    private static final long serialVersionUID = -4323439447102793636L;
    
    private float eventQueueLoad;
    private boolean onNetwork;
    private Map<String, FrameCalculatorStats> serverLoad;
    
    
    public float getEventQueueLoad() {
        return eventQueueLoad;
    }
    
    public void setEventQueueLoad(float eventQueueLoad) {
        this.eventQueueLoad = eventQueueLoad;
    }
    
    public boolean isOnNetwork() {
        return onNetwork;
    }
    
    public void setOnNetwork(boolean onNetwork) {
        this.onNetwork = onNetwork;
    }
    
    public Map<String, FrameCalculatorStats> getServerLoad() {
        return serverLoad;
    }
    
    public void setServerLoad(Map<String, FrameCalculatorStats> serverLoad) {
        this.serverLoad = serverLoad;
    }
}
