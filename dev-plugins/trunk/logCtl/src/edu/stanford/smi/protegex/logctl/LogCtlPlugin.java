package edu.stanford.smi.protegex.logctl;

// java - awt, swing
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

// java - util
import java.util.logging.Level;
import java.util.logging.Logger;

// protege
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.widget.AbstractTabWidget;

public class LogCtlPlugin extends AbstractTabWidget {
	private JButton getButton;
	private JButton putButton;
	private JComboBox levelDropDown;
	private JLabel classLabel;
	private JLabel levelLabel;
	private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	private JPanel mainPanel = new JPanel(new GridBagLayout());
	private JTextField locationField;

    private static final long serialVersionUID = 1028676900020977636L;
    private final static Level[] levels = { Level.SEVERE, Level.WARNING, Level.INFO,
                                            Level.CONFIG, Level.FINE, Level.FINER,
                                            Level.FINEST };

    public void initialize() {
		// Set the title for the tab widget.
        setLabel("Logger Control");

		// Initialize & layout components.
		GridBagConstraints c = new GridBagConstraints();

		// classLabel
		classLabel = new JLabel("Class/Password:");
		ComponentUtilities.setTitleLabelFont(classLabel);
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.insets = new Insets(40, 0, 0, 10); // top padding (top, left, bottom, right for Insets objects)
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_END;
		c.weightx = 0.0;
		c.weighty = 0.0;
		mainPanel.add(classLabel, c);

        // levelLabel
		levelLabel = new JLabel("Level:");
		ComponentUtilities.setTitleLabelFont(levelLabel);
		c.gridy = 1;
		c.insets = new Insets(10, 0, 0, 10);
		mainPanel.add(levelLabel, c);

		// locationField
		locationField = new JTextField(80);
		c.gridx = 1;
		c.gridy = 0;
		c.insets = new Insets(40, 0, 0, 0); // top padding
		c.anchor = GridBagConstraints.LINE_START;
		mainPanel.add(locationField, c);

		// levelDropDown
		levelDropDown = new JComboBox();
		initLevelsDropDown();
		c.gridy = 1;
		c.insets = new Insets(10, 0, 0, 0);
		mainPanel.add(levelDropDown, c);

		// buttons panel
		initButtons();
		buttonsPanel.add(getButton);
		buttonsPanel.add(putButton);
		c.gridy = 2;
		mainPanel.add(buttonsPanel, c);

		// Add main panel to tab plug-in.
		setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.PAGE_START);
	}

	private void initLevelsDropDown() {
		Dimension size = locationField.getPreferredSize();
		size.width = 150;
		levelDropDown.setPreferredSize(size);

		for (Level level : levels) {
			levelDropDown.addItem(level);
		}
	}

	private void initButtons() {
		getButton = new JButton("Get Level");
		getButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String location = locationField.getText();
			  	Logger log = Logger.getLogger(location);
			  	levelDropDown.setSelectedItem(log.getLevel());
		  	}
	  	});

		putButton = new JButton("Set Level");
		putButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String location = locationField.getText();
				Level level = (Level) levelDropDown.getSelectedItem();
				Logger log = Logger.getLogger(location);
				log.setLevel(level);
			}
		});
	}
}
