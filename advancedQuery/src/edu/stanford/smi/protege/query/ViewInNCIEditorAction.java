package edu.stanford.smi.protege.query;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Icon;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.ui.ProjectManager;
import edu.stanford.smi.protege.ui.ProjectView;
import edu.stanford.smi.protege.util.Selectable;
import edu.stanford.smi.protege.util.ViewAction;
import edu.stanford.smi.protege.widget.TabWidget;

/**
 * This action is used to show the NCIEditTab and select a {@link Cls} in the tree.
 * It will only be enabled if at least on {@link Cls} is selected.
 *
 * @author Chris Callendar
 * @date 25-Sep-06
 */
public class ViewInNCIEditorAction extends ViewAction {

	private static final String NCITAB = "gov.nih.nci.protegex.edit.NCIEditTab";

    protected ViewInNCIEditorAction(Selectable selectable) {
        super(selectable);
    }
    
    protected ViewInNCIEditorAction(String text, Selectable selectable) {
        super(text, selectable);
    }
    
	protected ViewInNCIEditorAction(String text, Selectable selectable, Icon icon) {
		super(text, selectable, icon);
	}

    public void onView(Object o) {
    	if (!(o instanceof Cls))
    		return;

		final ProjectView projectView = ProjectManager.getProjectManager().getCurrentProjectView();
		if (projectView == null) 
			return;
		
		TabWidget tab = (TabWidget) projectView.getTabByClassName(NCITAB);
		if (tab != null) {
			// show the NCI tab
			projectView.setSelectedTab(tab);
			
			// select the Cls in the Class Browser tree
			
			// use reflection to remove dependency on NCI code
			try {
				Method getClassPanelMethod = tab.getClass().getMethod("getClassPanel", new Class[0]);
				Object clsPanel = getClassPanelMethod.invoke(tab, new Object[0]);
				Method setSelectedClsMethod = clsPanel.getClass().getMethod("setSelectedCls", new Class[] { Cls.class });
				setSelectedClsMethod.invoke(clsPanel, new Object[] { o });
			} catch (Throwable t) {
				System.err.println("Warning - couldn't view the selected Cls in the NCIEditTab");
				//t.printStackTrace();
			}
			
			// dependency on NCI code (would have to update manifest file)
			/*
			try {
				if (tab instanceof NCIEditTab) {
					NCIEditTab nciTab = (NCIEditTab) tab;
					nciTab.getClassPanel().setSelectedCls((Cls) o);
				}
			} catch (Throwable e) {
				System.err.println("Warning - couldn't view the selected Cls in the NCIEditTab");
				//e.printStackTrace();
			}
			*/
		}
	}
    
    /** Only allow if a Cls is selected. */
    @Override
    public void onSelectionChange() {
    	Collection col = getSelection();
    	boolean allowed = false;
    	for (Iterator iter = col.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof Cls) {
				allowed = true;
				break;
			}
		}
    	setAllowed(allowed);
    }

}
