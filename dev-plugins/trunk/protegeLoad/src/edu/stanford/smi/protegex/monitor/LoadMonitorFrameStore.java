package edu.stanford.smi.protegex.monitor;

import java.awt.Color;
import java.awt.Rectangle;
import java.lang.reflect.Method;

import javax.swing.AbstractButton;

import edu.stanford.smi.protege.model.framestore.AbstractFrameStoreInvocationHandler;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.model.query.QueryCallback;

public class LoadMonitorFrameStore extends AbstractFrameStoreInvocationHandler {
    AbstractButton monitorButton;
    int calls = 0;
    long duration = 0;

    @Override
    protected void executeQuery(Query q, QueryCallback qc) {
        busy();
        getDelegate().executeQuery(q, qc);
        idle();  // fix thhis later...
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
        monitorButton.setBackground(Color.RED);
        Rectangle r = monitorButton.getBounds();
        r.x=0;
        r.y=0;
        monitorButton.paintImmediately(r);
        calls++;
    }
    
    private void idle() {
        monitorButton.setBackground(Color.WHITE);
        Rectangle r = monitorButton.getBounds();
        r.x=0;
        r.y=0;
        monitorButton.paintImmediately(r);
    }
    
    public void setButton(AbstractButton monitorButton) {
        this.monitorButton = monitorButton;
    }

}
