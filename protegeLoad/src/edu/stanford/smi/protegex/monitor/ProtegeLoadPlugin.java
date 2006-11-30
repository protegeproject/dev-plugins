package edu.stanford.smi.protegex.monitor;

import java.awt.Color;
import java.awt.Dimension;
import java.lang.reflect.Proxy;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;

import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.framestore.AbstractFrameStoreInvocationHandler;
import edu.stanford.smi.protege.model.framestore.FrameStore;
import edu.stanford.smi.protege.plugin.ProjectPluginAdapter;
import edu.stanford.smi.protege.ui.ProjectMenuBar;
import edu.stanford.smi.protege.ui.ProjectToolBar;
import edu.stanford.smi.protege.ui.ProjectView;
import edu.stanford.smi.protege.util.ComponentFactory;


public class ProtegeLoadPlugin extends ProjectPluginAdapter {
    
    private Dimension buttonSize = new Dimension(40, ComponentFactory.LARGE_BUTTON_HEIGHT);

    /**
     * Called after the view has been added to the screen
     */
    public void afterShow(ProjectView view, ProjectToolBar toolBar, ProjectMenuBar menuBar) {
        Project p = view.getProject();
        DefaultKnowledgeBase kb = (DefaultKnowledgeBase) p.getKnowledgeBase();
        AbstractButton monitorButton = addMonitorButton(toolBar);
        FrameStore fs = AbstractFrameStoreInvocationHandler.newInstance(LoadMonitorFrameStore.class, kb);
        
        LoadMonitorFrameStore fsi = (LoadMonitorFrameStore) Proxy.getInvocationHandler(fs);
        fsi.setButton(monitorButton);

        FrameStore head = kb.getHeadFrameStore();
        fs.setDelegate(head.getDelegate());
        head.setDelegate(fs);
    }
    
    
    private AbstractButton addMonitorButton(ProjectToolBar toolBar) {
        JMenuItem monitorButton = new JMenuItem();
        monitorButton.setPreferredSize(buttonSize);
        monitorButton.setMinimumSize(buttonSize);
        monitorButton.setMaximumSize(buttonSize);
        monitorButton.setFocusable(false);
        monitorButton.setBackground(Color.BLUE);
        monitorButton.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        toolBar.add(monitorButton);
        return monitorButton;
    }
   

}
