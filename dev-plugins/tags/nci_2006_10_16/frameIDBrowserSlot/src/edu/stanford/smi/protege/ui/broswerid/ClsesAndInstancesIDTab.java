package edu.stanford.smi.protege.ui.broswerid;
import java.awt.Container;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.ui.DirectInstancesList;
import edu.stanford.smi.protege.ui.FrameRenderer;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.SelectableList;
import edu.stanford.smi.protege.widget.ClsesAndInstancesTab;

public class ClsesAndInstancesIDTab extends ClsesAndInstancesTab {
	private final class FrameIDRenderer extends FrameRenderer {		
		@Override
		public void load(Object value) {
			super.load(value);
			if (value instanceof Frame)
				appendText(" id=" + ((Frame)value).getFrameID().getLocalPart());
		}
	}

	@Override
	public void initialize() {
		super.initialize();
		setLabel("OWLClasses+FrameID");
		getClsTree().setCellRenderer(new FrameIDRenderer());
		adjustDirectInstanceRenderer();		
	}
	
	
	private void adjustDirectInstanceRenderer() {	
		try {
			//ugly way of getting the instance list, because there are no getter methods in the superclass
			DirectInstancesList dirList = (DirectInstancesList)((Container)((Container)((Container)getComponent(0)).getComponent(2)).getComponent(1)).getComponent(1);
			((SelectableList)((DirectInstancesList) dirList).getSelectable()).setCellRenderer(new FrameIDRenderer());
			
		} catch (Exception e) {
			Log.getLogger().warning("Error at setting browser slot " + e.getMessage());			
		}
	}
	
}
