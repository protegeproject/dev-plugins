package edu.stanford.smi.protegex.load;

import java.rmi.Remote;

public interface LoadService extends Remote {
    
    ProtegeLoadStats getLoad();

}
