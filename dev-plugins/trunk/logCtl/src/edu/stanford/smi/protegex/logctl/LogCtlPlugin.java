package edu.stanford.smi.protegex.logctl;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.stanford.smi.protege.widget.AbstractTabWidget;

public class LogCtlPlugin extends AbstractTabWidget {
    private static final long serialVersionUID = 1028676900020977636L;

    private final static Level[] levels = { Level.SEVERE, Level.WARNING, Level.INFO,
                                            Level.CONFIG, Level.FINE, Level.FINER,
                                            Level.FINEST };
    
    private JTextField locationField;
    private JComboBox levelDropDown;
    private JButton putButton;
    private JButton getButton;
    

    public void initialize() {
        // initialize UI 
        setLabel("Logger Control");
        setLayout(new BorderLayout());
        // setLayout(new GridLayout(3,2));
        JPanel panel = new JPanel(new GridLayout(3,2));
        panel.add(new JTextArea("Class/Password: "));
        locationField = new JTextField(132);
        panel.add(locationField);
        
        panel.add(new JTextArea("Level: "));
        levelDropDown = new JComboBox();
        for (Level level : levels) {
            levelDropDown.addItem(level);
        }
        panel.add(levelDropDown);
        putButton = new JButton("SetLevel");
        putButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                String location = locationField.getText();
                Level level = (Level) levelDropDown.getSelectedItem();
                Logger log = Logger.getLogger(location);
                log.setLevel(level);
            }
            
        });
        panel.add(putButton);
        getButton = new JButton("getLevel");
        getButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                String location = locationField.getText();
                Logger log = Logger.getLogger(location);
                levelDropDown.setSelectedItem(log.getLevel());
            }
            
        });
        panel.add(getButton);
        add(panel, BorderLayout.CENTER);
    }

}
