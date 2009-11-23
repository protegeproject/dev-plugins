package uk.ac.man.cs.mig.coode.protege.id;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protegex.owl.model.NamespaceUtil;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLOntology;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.impl.OWLUtil;
import edu.stanford.smi.protegex.owl.ui.ResourceRenderer;
import edu.stanford.smi.protegex.owl.ui.widget.OWLUI;

public class PreferencesPanel extends JPanel {
    private JCheckBox           enabled;
    private JRadioButton        uniqueId;
    private JRadioButton        sequentialId;
    private JTextField          prefixField;
    private JFormattedTextField digitCountField;
    private JComboBox renderingPropertyBox;

    
    private Preferences preferences;
    
    public PreferencesPanel(OWLModel owlModel) {
        preferences = new Preferences(owlModel);
        
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);
        
        enabled = new JCheckBox("Enabled?");
        enabled.setSelected(preferences.isEnabled());
        enabled.setAlignmentX(Component.LEFT_ALIGNMENT);
        enabled.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                updateEnabled();
            }
        });
        add(enabled);
        
        ButtonGroup uniqueIdGroup = new ButtonGroup();
        uniqueId = new JRadioButton("Unique id");
        uniqueIdGroup.add(uniqueId);
        uniqueId.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                updateEnabled();
            }
        });
        add(uniqueId);
        
        sequentialId = new JRadioButton("Readable, sequential ids");
        sequentialId.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                updateEnabled();
            }
        });
        uniqueIdGroup.add(sequentialId);
        add(sequentialId);
        if (preferences.isUniqueId()) {
            uniqueId.setSelected(true);
        }
        else {
            sequentialId.setSelected(true);
        }
        
        uniqueId.setSelected(preferences.isUniqueId());
        
        JPanel prefixPanel = new JPanel();
        prefixPanel.add(new JLabel("Prefix: "));
        prefixField = new JTextField("ID");
        prefixField.setPreferredSize(new JTextField("A long id").getPreferredSize());
        prefixField.setText(preferences.getPrefix());
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
        digitCountField.setValue(preferences.getDigits());
        digitCountPanel.add(digitCountField);
        add(digitCountPanel);
        
        add(getRenderingPanel(owlModel));
    }
    
    private JComponent getRenderingPanel(OWLModel owlModel) {
        JPanel localSettingsPanel = new JPanel();
        localSettingsPanel.setLayout(new BoxLayout(localSettingsPanel, BoxLayout.PAGE_AXIS));
        localSettingsPanel.setBorder(BorderFactory.createTitledBorder("Local Settings"));
        localSettingsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        localSettingsPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));
        
        renderingPropertyBox = makePropertyComboBox(OWLUI.getCommonBrowserSlot(owlModel));
        renderingPropertyBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        localSettingsPanel.add(new LabeledComponent("Render entities in this ontology (" + getOntologyName(owlModel) + ") using property: ", renderingPropertyBox));
        localSettingsPanel.setToolTipText("The local setting will apply only to the current ontology and it will be saved in the pprj file.");
        
        return localSettingsPanel;
    }
    
    @SuppressWarnings("deprecation")
    private JComboBox makePropertyComboBox(Slot defaultSlot) {
        OWLModel owlModel = (OWLModel) defaultSlot.getKnowledgeBase();
        TreeSet<RDFProperty> properties = new TreeSet<RDFProperty>(owlModel.getOWLAnnotationProperties());
        properties.remove(owlModel.getRDFSLabelProperty());
        Slot[] propertyArray = new Slot[properties.size() + 2];
        int counter = 0;
        propertyArray[counter++] = owlModel.getNameSlot();
        propertyArray[counter++] = owlModel.getRDFSLabelProperty();
        for (Slot p : properties)  {
            propertyArray[counter++] = p;
        }

        JComboBox combo = new JComboBox(propertyArray);
        combo.setRenderer(new ComboBoxRenderer());
        if (defaultSlot != null && 
                (properties.contains(defaultSlot) ||
                        defaultSlot.equals(owlModel.getNameSlot()) ||
                        defaultSlot.equals(owlModel.getRDFSLabelProperty()))) {
            combo.setSelectedItem(defaultSlot);
        }
        else {
            combo.setSelectedIndex(-1);
        }
        return combo;
    }
    
    private String getOntologyName(OWLModel owlModel) {
        OWLOntology displayedOntology =  OWLUtil.getActiveOntology(owlModel);
        return NamespaceUtil.getLocalName(displayedOntology.getName());
    }
    
    private class ComboBoxRenderer extends ResourceRenderer {
        private static final long serialVersionUID = -964343376627678218L;

        @Override
        protected void loadSlot(Slot slot) {
            OWLModel owlModel = (OWLModel) slot.getKnowledgeBase();
            if (slot.equals(owlModel.getSystemFrames().getNameSlot())) {
                addText("rdf:id");
            }
            else {
                super.loadSlot(slot);
            }
        }  
    }
    
    private void updateEnabled() {
        if (!enabled.isSelected()) {
            uniqueId.setEnabled(false);
            sequentialId.setEnabled(false);
            prefixField.setEnabled(false);
            digitCountField.setEnabled(false);
        }
        else if (uniqueId.isSelected()) {
            uniqueId.setEnabled(true);
            sequentialId.setEnabled(true);
            prefixField.setEnabled(true);
            digitCountField.setEnabled(false);
        }
        else {
            uniqueId.setEnabled(true);
            sequentialId.setEnabled(true);
            prefixField.setEnabled(true);
            digitCountField.setEnabled(true);
        }
    }
    
    public void save(OWLModel owlModel) {
        preferences.setEnabled(enabled.isSelected());
        preferences.setUniqueId(uniqueId.isSelected());
        preferences.setPrefix(prefixField.getText());
        preferences.setDigits((Integer) digitCountField.getValue());
        preferences.save(owlModel);
        IdFrameStore.setAutoIdPreferences(owlModel, preferences);
        Slot slot = (Slot) renderingPropertyBox.getSelectedItem();
        OWLUI.setCommonBrowserSlot(owlModel, slot);
    }

}
