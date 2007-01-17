package edu.stanford.smi.protege.query;

import java.awt.Cursor;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.ui.ProjectView;
import edu.stanford.smi.protege.util.Selectable;
import edu.stanford.smi.protege.widget.TabWidget;

/**
 * This action is used to show the {@link EditDialog}.
 * It will only be enabled if at least one {@link Cls} is selected.
 *
 * @author Chris Callendar
 * @date 25-Sep-06
 */
public class NCIEditAction extends NCIViewAction {

	private static final String EDITDIALOG = "gov.nih.nci.protegex.edit.EditDialog";

	public NCIEditAction(String text, Selectable selectable, Icon icon) {
		super(text, selectable, icon);
	}

	/**
	 * Determines if the needed NCI classes are available.
	 */
	public static boolean isValid() {
		boolean valid = true;
		try {
			Class.forName(NCITAB);
			Class.forName(EDITDIALOG);
		} catch (Throwable t) {
			valid = false;
		}
		return valid;
	}

    protected void performAction(final TabWidget tab, final ProjectView projectView, final Cls cls) {
		final Cursor oldCursor = projectView.getCursor();
		projectView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		// run this later to let the tab change occur
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// use reflection to remove dependency on NCI code
				try {
					Class.forName(EDITDIALOG).getConstructors()[0].newInstance(tab, cls);
				} catch (Throwable t) {
					System.err.println("Warning - couldn't open the EditDialog for " + cls);
					t.printStackTrace();
				}
									
				// dependency on NCI code
				/*
				try {
		        	new EditDialog((NCIEditTab) tab, cls);
				} catch (Throwable t) {
					System.err.println("Warning - couldn't open the EditDialog for " + cls);
				}
				*/
				
				projectView.setCursor(oldCursor);
			}
		});
	}
    
}
