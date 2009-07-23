package edu.stanford.bmir.protegex.dark.matter;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.ui.widget.AbstractTabWidget;

public class DarkMatterDetector extends AbstractTabWidget {
    private static final long serialVersionUID = 5762527226593760925L;
    private Logger log = Log.getLogger(getClass());

    private JList unreferenced;
    private DefaultListModel listModel;
    private JButton start;
    private JButton clean;
    private JButton stop;
    
    private Future<Integer> job;
    private ExecutorService executor;

    public void initialize() {
        setLabel("Dark Matter Plugin");
        setLayout(new BorderLayout());
        add(createList(), BorderLayout.CENTER);
        add(createButtons(), BorderLayout.PAGE_END);
        executor = Executors.newCachedThreadPool();
    }
    
    @Override
    public void dispose() {
        if (job != null) {
            job.cancel(true);
        }
        super.dispose();
    }
    
    private JPanel createButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        start = new JButton("Start");
        start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                log.info("Starting search for unreferenced anonymous classes");
                Callable<Integer> call = new FindUnreferencedResources(getOWLModel()) {
                  @Override
                    protected void onFind(RDFResource resource) {
                      listModel.addElement(resource.getBrowserText());
                    }
                  @Override
                    protected void onFinish() {
                      jobDone();
                    }
                };
                listModel.clear();
                job = executor.submit(call);
                jobStarted();
            }
        });  
        panel.add(start);
        
        
        stop = new JButton("Stop");
        stop.setEnabled(false);
        stop.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               job.cancel(true);
            } 
        });
        panel.add(stop);
        return panel;
    }
    
    private JComponent createList() {
        unreferenced = new JList();
        listModel = new DefaultListModel();
        unreferenced.setModel(listModel);
        return new JScrollPane(unreferenced);
    }
    
    private void jobStarted() {
        start.setEnabled(false);
        stop.setEnabled(true);
    }
    
    private void jobDone() {
        start.setEnabled(true);
        stop.setEnabled(false);
        job = null;
    }

    
}
