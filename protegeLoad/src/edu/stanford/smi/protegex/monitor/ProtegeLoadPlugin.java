package edu.stanford.smi.protegex.monitor;

import java.awt.Color;
import java.awt.Dimension;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;

import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.framestore.AbstractFrameStoreInvocationHandler;
import edu.stanford.smi.protege.model.framestore.FrameStore;
import edu.stanford.smi.protege.model.framestore.FrameStoreManager;
import edu.stanford.smi.protege.plugin.ProjectPluginAdapter;
import edu.stanford.smi.protege.ui.ProjectMenuBar;
import edu.stanford.smi.protege.ui.ProjectToolBar;
import edu.stanford.smi.protege.ui.ProjectView;
import edu.stanford.smi.protege.util.ComponentFactory;


public class ProtegeLoadPlugin extends ProjectPluginAdapter {
    
    private Dimension buttonSize = new Dimension(15, ComponentFactory.LARGE_BUTTON_HEIGHT);
    private static Map<Project, ServerMonitor> monitors = new HashMap<Project, ServerMonitor>();

    
    /**
     * Called after the view has been added to the screen
     */
    public void afterShow(ProjectView view, ProjectToolBar toolBar, ProjectMenuBar menuBar) {
        Project p = view.getProject();

        if (p.isMultiUserServer()) {
            return;  // probably can't get here?
        }
        DefaultKnowledgeBase kb = (DefaultKnowledgeBase) p.getKnowledgeBase();
        if (p.isMultiUserClient()) {
            synchronized (monitors) {
                monitors.put(p, new ServerMonitor(toolBar, kb));
            }
        }
        AbstractButton monitorButton = addMonitorButton(toolBar);
        installFrameStore(kb, monitorButton);
    }
    
    public void beforeClose(Project p) {
        synchronized (monitors) {
            ServerMonitor monitor = monitors.get(p);
            if (monitor != null) {
                monitor.dispose();
            }
        }
        DefaultKnowledgeBase kb = (DefaultKnowledgeBase) p.getKnowledgeBase();
        FrameStoreManager fsm = kb.getFrameStoreManager();
        FrameStore fs = fsm.getFrameStoreFromClass(LoadMonitorFrameStore.class);
        LoadMonitorFrameStore fsi = (LoadMonitorFrameStore) Proxy.getInvocationHandler(fs);
        fsi.dispose();
    }
    
    private AbstractButton addMonitorButton(ProjectToolBar toolBar) {
        JMenuItem monitorButton = new JMenuItem();
        monitorButton.setPreferredSize(buttonSize);
        monitorButton.setMinimumSize(buttonSize);
        monitorButton.setMaximumSize(buttonSize);
        monitorButton.setFocusable(false);
        monitorButton.setBackground(Color.WHITE);
        monitorButton.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        toolBar.add(monitorButton);
        return monitorButton;
    }
    
    private void installFrameStore(DefaultKnowledgeBase kb, AbstractButton monitorButton) {
        FrameStore fs = AbstractFrameStoreInvocationHandler.newInstance(LoadMonitorFrameStore.class, kb);
        
        LoadMonitorFrameStore fsi = (LoadMonitorFrameStore) Proxy.getInvocationHandler(fs);
        fsi.setButton(monitorButton);

        FrameStore head = kb.getHeadFrameStore();
        fs.setDelegate(head.getDelegate());
        head.setDelegate(fs);
    }
    

}
