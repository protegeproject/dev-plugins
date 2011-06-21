package test;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class DummyWindowTestApplication {
	
	private Frame ctrl;
	
	private TextField fWidth;
	private TextField fHeight;
	private Checkbox cbAreDimensionsOfInterior;
	private TextField fTitle;
	private TextField fIconFileName;
	private JFileChooser fCh;
	
	public static void main(String[] args) {
		DummyWindowTestApplication app = new DummyWindowTestApplication();
		app.start();
	}
	
	private void start() {
		ctrl = new Frame("Create window");
		ctrl.setSize(new Dimension(450, 320));
		ctrl.setLayout(new BorderLayout());
		
		Panel pMain = new Panel();
		pMain.setLayout(new GridBagLayout());
		GridBagConstraints cLabel = new GridBagConstraints();
		cLabel.gridx = 0;
		cLabel.weightx = 0.20;
		cLabel.weighty = 1;
		cLabel.insets = new Insets(10, 20, 10, 10);
		cLabel.fill = GridBagConstraints.HORIZONTAL;
		GridBagConstraints cField = new GridBagConstraints();
		cField.gridx = 1;
		cField.weightx = 0.60;
		cField.weighty = 1;
		cField.insets = new Insets(10, 0, 10, 20);
		cField.fill = GridBagConstraints.HORIZONTAL;

		Label label = new Label("Width:");
		cLabel.gridy = 0;
		pMain.add(label, cLabel);
		fWidth = new TextField("250");
		cField.gridy = 0;
		pMain.add(fWidth, cField);
		
		cLabel.gridy = 1;
		pMain.add(new Label("Height:"), cLabel);
		fHeight = new TextField("200");
		cField.gridy = 1;
		pMain.add(fHeight, cField);
		
		cbAreDimensionsOfInterior = new Checkbox("Are these the dimensions of the window content?");
		cField.gridy = 2;
		cField.gridwidth = GridBagConstraints.REMAINDER;
		pMain.add(cbAreDimensionsOfInterior, cField);
		
		cLabel.gridy = 3;
		pMain.add(new Label("Title:"), cLabel);
		fTitle = new TextField("");
		cField.gridy = 3;
		pMain.add(fTitle, cField);

		cLabel.gridy = 4;
		cField.gridwidth = GridBagConstraints.RELATIVE;
		pMain.add(new Label("Icon:"), cLabel);
		fIconFileName = new TextField("");
		fIconFileName.setPreferredSize(new Dimension(150, 25));
		Button bSelectFile = getSelectFileButton();
		cField.gridy = 4;
		pMain.add(fIconFileName, cField);
		cField.gridx = 2;
		cField.weightx = 0.20;
		pMain.add(bSelectFile, cField);
		
		Panel pSouth = new Panel();
		pSouth.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 15));
		pSouth.setSize(new Dimension(200, 50));
		
		pSouth.add(getCreateWindowButton());
		pSouth.add(getCloseButton());
		
		ctrl.add(pMain, BorderLayout.CENTER);
		ctrl.add(pSouth, BorderLayout.SOUTH);
		ctrl.setLocation(300, 200);
		ctrl.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closeApplication();
			}
		});
		ctrl.setVisible(true);

	}
	
	private Button getCreateWindowButton() {
		Button createButton = new Button("Create window");
		createButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final Frame f = new Frame(fTitle.getText());
				int width = Integer.parseInt(fWidth.getText());
				int height = Integer.parseInt(fHeight.getText());
				f.setSize(width, height);
				f.setIconImage(getIconImage());
				
				f.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						f.dispose();
					}
				});
				f.setVisible(true);
				
				if (cbAreDimensionsOfInterior.getState()) {
					Insets insets = f.getInsets();
					f.setSize(f.getWidth() + (insets.left + insets.right),
							f.getHeight() + (insets.top + insets.bottom));
				}
			}
		});
		return createButton;
	}
	
	private Image getIconImage() {
		String iconFilename = fIconFileName.getText();
		File f;
		Image icon = null;
		if (iconFilename != null && iconFilename.trim().length() > 0 
				&& (f = new File(iconFilename)).exists()) {
			try {
				icon = ImageIO.read(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return icon;
	}
	
	private Button getCloseButton() {
		Button closeButton = new Button("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeApplication();
			}
		});
		return closeButton;
	}

	private void closeApplication() {
		ctrl.dispose();
		System.exit(0);
	}

	private Button getSelectFileButton() {
		Button selectFileButton = new Button("Select");
		selectFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final Frame f = new Frame("Select file...");
				f.setSize(500, 400);
				
				f.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						f.dispose();
					}
				});
				f.setVisible(true);
				
				fCh = new JFileChooser(fIconFileName.getText());
				fCh.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent actionEvent) {
						  JFileChooser theFileChooser = (JFileChooser) actionEvent.getSource();
					        String command = actionEvent.getActionCommand();
					        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
					          File selectedFile = theFileChooser.getSelectedFile();
					          fIconFileName.setText(selectedFile.toString());
					          f.dispose();
					        } else if (command.equals(JFileChooser.CANCEL_SELECTION)) {
					          f.dispose();
					        }					}
				});
				fCh.addChoosableFileFilter(new FileFilter() {
					@Override
					public String getDescription() {
						// ICO is not supported by the default Java ImageIO reader. 
						// One way to add support for .ICO files is to use a 3rd party library, 
						// such as http://www.vdburgh.net/2/f/files/ICOReader/
						//
						//return "*.ICO, *.GIF, *.JPG, *.PNG, *.BMP";
						
						return "*.GIF, *.JPG, *.PNG, *.BMP";
					}
					
					@Override
					public boolean accept(File f) {
						String fName = f.getName();
						if ( f.isDirectory()
							//|| fName.toUpperCase().endsWith(".ICO")	//see explanation above
							|| fName.toUpperCase().endsWith(".GIF")
							|| fName.toUpperCase().endsWith(".JPG")
							|| fName.toUpperCase().endsWith(".PNG")
							|| fName.toUpperCase().endsWith(".BMP")) {
							return true;
						}
						return false;
					}
				});
				f.add(fCh);
			}
		});
		return selectFileButton;
	}

}
