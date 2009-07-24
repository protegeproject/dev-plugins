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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.ui.widget.AbstractTabWidget;

public class DarkMatterDetector extends AbstractTabWidget {
    private static final long serialVersionUID = 5762527226593760925L;
    private Logger log = Log.getLogger(getClass());
    
    private JLabel statusContainer;

    private JList unreferenced;
    private DefaultListModel listModel;
    private JButton start;
    private JButton stop;
    private JButton clean;
    
    private Future<Integer> job;
    private ExecutorService executor;

    public void initialize() {
        setLabel("Dark Matter Plugin");
        setLayout(new BorderLayout());
        add(createStatus(), BorderLayout.PAGE_START);
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
    
    
    private JComponent createStatus() {
        JPanel panel = new  JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        statusContainer = new JLabel("Dark Matter Plugin Started");
        panel.add(statusContainer);
        return panel;
    }
    
    private JComponent createList() {
        unreferenced = new JList();
        listModel = new DefaultListModel();
        unreferenced.setModel(listModel);
        return new JScrollPane(unreferenced);
    }
    
    private JPanel createButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        start = new JButton("Start");
        start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatus("Starting search for unreferenced anonymous resources");
                Callable<Integer> call = new FindUnreferencedSwing(getOWLModel(), DarkMatterDetector.this, "Searching for unreferenced anonymous resources") {
                  @Override
                    protected void onFind(RDFResource resource) {
                      listModel.addElement(resource.getBrowserText());
                    }
                  @Override
                    protected void onFinish() {
                      jobDone();
                      super.onFinish();
                      setStatus("Search complete");
                    }
                };
                listModel.clear();
                job = executor.submit(call);
                jobStarted();
            }
        });  
        panel.add(start);
        
        clean = new JButton("Clean");
        clean.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatus("Starting deletion of unreferenced anonymous resources");
                Callable<Integer> call = new FindUnreferencedSwing(getOWLModel(), DarkMatterDetector.this, "Deleting  unreferenced anonymous resources") {
                  @Override
                    protected void onFind(RDFResource resource) {
                      listModel.addElement("Deleting " + resource.getBrowserText());
                      resource.delete();
                    }
                  @Override
                    protected void onFinish() {
                      jobDone();
                      super.onFinish();
                      setStatus("Deletions complete");
                    }
                };
                listModel.clear();
                job = executor.submit(call);
                jobStarted();
            }
        });  
        panel.add(clean);
        
        stop = new JButton("Stop");
        stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (job != null) {
                    job.cancel(true);
                }
            }
        });
        stop.setEnabled(false);
        panel.add(stop);

        return panel;
    }
    
    private void setStatus(String status) {
        log.info(status);
        statusContainer.setText(status);
    }
    
    private void jobStarted() {
        start.setEnabled(false);
        clean.setEnabled(false);
        stop.setEnabled(true);
    }
    
    private void jobDone() {
        start.setEnabled(true);
        clean.setEnabled(true);
        stop.setEnabled(false);
        job = null;
    }

    
}
