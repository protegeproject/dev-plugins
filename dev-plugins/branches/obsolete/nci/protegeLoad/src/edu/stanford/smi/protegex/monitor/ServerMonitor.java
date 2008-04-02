package edu.stanford.smi.protegex.monitor;

import java.awt.Color;
import java.awt.Dimension;
import java.util.logging.Level;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.framestore.FrameStoreManager;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.ui.ProjectToolBar;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.Log;

public class ServerMonitor {
    private Dimension buttonSize = new Dimension(15, ComponentFactory.LARGE_BUTTON_HEIGHT);
    private enum PingState {
        IDLE, PING_IN_PROGRESS, SHUTDOWN
    }
    
    private PingState pingState = PingState.IDLE;
    private long pingStartTime = System.currentTimeMillis();
    private Object pingLock = new Object();
    
    public ServerMonitor(ProjectToolBar toolBar, KnowledgeBase kb) {
        if (!checkPing(kb)) {
            return;
        }
        JMenuItem serverUpButton = new JMenuItem();
        serverUpButton.setPreferredSize(buttonSize);
        serverUpButton.setMinimumSize(buttonSize);
        serverUpButton.setMaximumSize(buttonSize);
        serverUpButton.setFocusable(false);
        serverUpButton.setBackground(Color.GREEN);
        serverUpButton.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        toolBar.add(serverUpButton);
        new Thread(new ServerPing(kb), "Server Ping Thread").start();
        new Thread(new ServerPingMonitor(serverUpButton), "Ping Monitor Thread").start();       
    }
    
    public void dispose() {
        synchronized (pingLock) {
            pingState = PingState.SHUTDOWN;
        }
    }
    
    private void interrupted(InterruptedException e) {
        Log.getLogger().log(Level.WARNING, "Huh... What?", e);
    }

    private boolean checkPing(KnowledgeBase kb) {
        try {
            (new Ping(kb)).execute();
        }
        catch (Throwable t) {
            for (Throwable cause = t; cause != null ; cause = cause.getCause()) {
                if (cause instanceof ClassNotFoundException) {
                    Log.getLogger().info("Server does not support ping operation");
                    return false;
                }
            }
            Log.getLogger().log(Level.WARNING, "Exception found attempting ping to server", t);
            return false;
        }
        return true;
    }
    
    private class ServerPing implements Runnable {
        private KnowledgeBase kb;
        
        public ServerPing(KnowledgeBase kb) {
            this.kb = kb;
        }

        public void run() {
            while (true) {
                synchronized (pingLock) {
                    try {
                        pingLock.wait(PluginProperties.getServerPingInterval());
                    } catch (InterruptedException e) {
                        interrupted(e);
                    }
                    if (pingState == PingState.SHUTDOWN) return;
                    pingState = PingState.PING_IN_PROGRESS;
                    pingStartTime = System.currentTimeMillis();
                }
                (new Ping(kb)).execute();
                synchronized (pingLock) {
                    if (pingState == PingState.SHUTDOWN) return;
                    pingState = PingState.IDLE;
                }
            }
            
        }
        
    }
    
    private class ServerPingMonitor implements Runnable {
        private AbstractButton serverPingButton;
        private boolean serverUp = true;
        
        public ServerPingMonitor(AbstractButton serverPingButton) {
            this.serverPingButton = serverPingButton;
        }

        public void run() {
            while (true) {
                boolean doNotify = false;
                synchronized (pingLock) {
                    try {
                        pingLock.wait(PluginProperties.getServerPingInterval());
                    } catch (InterruptedException e) {
                        interrupted(e);
                    }
                    if (pingState == PingState.SHUTDOWN) return;
                    if (pingState == PingState.PING_IN_PROGRESS && 
                            System.currentTimeMillis() > pingStartTime + PluginProperties.getLatePingTimeout()) {
                        if (serverUp) doNotify = true;
                        serverUp = false;
                    }
                    else if (!serverUp) {
                        doNotify = true;
                        serverUp = true;
                    }
                }
                if (doNotify) {
                    if (serverUp) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                serverPingButton.setBackground(Color.GREEN);
                            }
                        });
                    }
                    else {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                serverPingButton.setBackground(Color.RED);
                            }
                        });
                    }
                }
            }
        }
    }
}
