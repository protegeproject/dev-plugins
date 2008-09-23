package edu.stanford.smi.protegex.monitor;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.util.logging.Level;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.framestore.AbstractFrameStoreInvocationHandler;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.model.query.QueryCallback;
import edu.stanford.smi.protege.util.Log;

public class LoadMonitorFrameStore extends AbstractFrameStoreInvocationHandler {
    private AbstractButton monitorButton;
    private boolean enabled = true;
    


    private enum State {
        IDLE, RUNNING, AWAITING_IDLE
    }
    private State state = State.IDLE;
    private Thread timeoutThread;
    private long lastIdleTime = System.currentTimeMillis();
    private Object lock = new Object();
    
    public LoadMonitorFrameStore(KnowledgeBase kb) {
        timeoutThread = new Thread(new Runnable() {

            public void run() {
                boolean doNotify;
                while (true) {
                    doNotify = false;
                    synchronized (lock) {
                        try {
                            lock.wait(PluginProperties.getIdleTimeout());
                        } catch (InterruptedException e) {
                            Log.getLogger().log(Level.WARNING, 
                                                "Why am I getting interrupted?", e);
                        }
                        if (timeoutThread == null) {
                            return;
                        }
                        switch (state) {
                        case IDLE:
                        case RUNNING:
                            break;
                        case AWAITING_IDLE:
                            long now = System.currentTimeMillis();
                            doNotify = (now - lastIdleTime >= PluginProperties.getIdleTimeout());
                            if (doNotify) state = State.IDLE;
                            break;
                        default:
                            Log.getLogger().warning("Developer missed a case " + state);
                        }
                    }

                    if (doNotify) {
                        // It is possible but extremely unlikely that this color change will
                        // be overridden by the color change invoked by a previous transition 
                        // to the RUNNING state.  For this to happen it would have to take 
                        // longer than IDLE_TIMEOUT for the previous transition to the RUNNING
                        // state to repain the button.
                        // Can I safely synchronize invokeLater in the lock?
                        SwingUtilities.invokeLater(new Runnable() {
                           public void run() {
                               setButtonColor(Color.WHITE);
                           }
                        });
                    }
                }
            }
            
        }, "Load Monitor Set Idle Thread");
        timeoutThread.start();
    }
    
    public void setButton(AbstractButton monitorButton) {
        this.monitorButton = monitorButton;
    }
    
    public void dispose() {
        timeoutThread = null;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void executeQuery(Query q, QueryCallback qc) {
        busy();
        // Unclear that this should be busy until the callback
        // Other threads can make this go idle anyway.
        getDelegate().executeQuery(q, qc);
        idle();
    }

    @Override
    protected Object handleInvoke(Method method, Object[] args) {
        Object result;
        busy();
        result = invoke(method, args);
        idle();
        return result;
    }
    
    private void busy() {
        boolean doNotify = false;
        synchronized (lock) {
            doNotify = (state == State.IDLE);
            state = State.RUNNING;
        }
        if (doNotify) {
            // It is possible that this update will get overriden by the color change invoked
            // by a previous AWAITING_IDLE -> IDLE transition in a race condition.  Hard to 
            // avoid safely.  Can I safely synchronize invokeLater in the lock?
            if (EventQueue.isDispatchThread()) {
                setButtonColor(Color.RED);
            }
            else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setButtonColor(Color.RED);
                    }
                });
            }
        }
    }
    

    
    private void idle() {
        synchronized (lock) {
            state = State.AWAITING_IDLE;
            lastIdleTime = System.currentTimeMillis();
        }
    }
    

    /*
     * On my relatively fast machine, this call takes between .5ms and 2ms.
     * So it is a significant overhead if it occured on each knowledge base call.
     */
    private void setButtonColor(Color c) {
    	if (!enabled || monitorButton == null || monitorButton.getGraphics() == null ) return;
    	try {
    		Rectangle r = monitorButton.getBounds();
    		r.x=0;
    		r.y=0;
    		monitorButton.setBackground(c);
    		monitorButton.paint(monitorButton.getGraphics());
    		// monitorButton.paintImmediately(r);
    	}
    	catch (Throwable t) {
    		;
    	}
    }

}
