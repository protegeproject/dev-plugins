package edu.stanford.smi.protegex.load;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {
    private LoadService service;
    
    public Client() throws MalformedURLException, RemoteException, NotBoundException {
        service = (LoadService) Naming.lookup(ProtegeLoadPlugin.url);
    }
    
    
    public void run() {
        System.out.println("Retrieved remote stats = " + service.getLoad());
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            new Client().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
