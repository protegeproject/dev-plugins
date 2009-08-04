package edu.stanford.bmir.protegex.dark.matter;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protegex.owl.model.RDFResource;

public class DarkMatterTableModel extends AbstractTableModel {
    private List<FrameID> frames = new ArrayList<FrameID>();
    private List<String> messages = new ArrayList<String>();
    
    public enum Column {
        ID("Identifier"),
        MESSAGE("Info");
        
        private String name;

        private Column(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
    
    public void addInfo(RDFResource resource, String message) {
        frames.add(resource.getFrameID());
        messages.add(message);
        fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
    }
    
    public void clear()  {
        frames.clear();
        messages.clear();
        fireTableDataChanged();
    }
    

    public int getColumnCount() {
        return Column.values().length;
    }
    
    @Override
    public String getColumnName(int column) {
        return Column.values()[column].getName();
    }

    public int getRowCount() {
        return frames.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (Column.values()[columnIndex]) {
        case ID:
            return frames.get(rowIndex).getName();
        case MESSAGE:
            return messages.get(rowIndex);
        default:
            throw new IllegalStateException("Programmer error");
        }
    }

}
