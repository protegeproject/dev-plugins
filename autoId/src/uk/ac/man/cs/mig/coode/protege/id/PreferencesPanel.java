package uk.ac.man.cs.mig.coode.protege.id;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.stanford.smi.protegex.owl.model.OWLModel;

public class PreferencesPanel extends JPanel {
    public JCheckBox           enabled;
    public JCheckBox           uniqueId;
    public JTextField          prefixField;
    public JFormattedTextField digitCountField;
    
    
    public PreferencesPanel(OWLModel owlModel) {
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        
        setLayout(layout);
        
        enabled = new JCheckBox("Enabled?");
        enabled.setAlignmentX(Component.LEFT_ALIGNMENT);
        enabled.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                updateEnabled();
            }
        });
        add(enabled);
        
        uniqueId = new JCheckBox("Use unique Ids?");
        uniqueId.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                updateEnabled();
            }
        });
        add(uniqueId);
        
        JPanel prefixPanel = new JPanel();
        prefixPanel.add(new JLabel("Prefix: "));
        prefixField = new JTextField("ID");
        prefixField.setPreferredSize(new JTextField("A long id").getPreferredSize());
        prefixPanel.add(prefixField);
        add(prefixPanel);
        
        JPanel digitCountPanel = new JPanel();
        digitCountPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        digitCountPanel.setLayout(new FlowLayout());
        digitCountPanel.add(new JLabel("Number of Digits: "));
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumIntegerDigits(1);
        format.setMaximumIntegerDigits(2);
        digitCountField = new JFormattedTextField(format);
        double height = digitCountField.getPreferredSize().getHeight();
        double width  = new JLabel("999").getPreferredSize().getWidth();
        digitCountField.setPreferredSize(new Dimension((int) width, (int) height));
        digitCountField.setValue(7);
        digitCountPanel.add(digitCountField);
        add(digitCountPanel);
    }
    
    private void updateEnabled() {
        if (!enabled.isSelected()) {
            uniqueId.setEnabled(false);
            prefixField.setEnabled(false);
            digitCountField.setEnabled(false);
        }
        else if (!uniqueId.isSelected()) {
            uniqueId.setEnabled(true);
            prefixField.setEnabled(false);
            digitCountField.setEnabled(false);
        }
        else {
            uniqueId.setEnabled(true);
            prefixField.setEnabled(true);
            digitCountField.setEnabled(true);
        }
    }

}
