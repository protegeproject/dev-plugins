package uk.ac.man.cs.mig.coode.protege.id;

import edu.stanford.smi.protege.util.PropertyList;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class Preferences {
    public static String ENABLED_PROPERTY="auto.id.enabled";
    public static String UNIQUE_PROPERTY="auto.id.unique";
    public static String PREFIX_PROPERTY="auto.id.prefix";
    public static String DIGITS_PROPERTY="auto.id.digits";
    
    private boolean enabled;
    private boolean uniqueId = true;
    private String prefix = "ID";
    private int digits = 7;
    
    
    public Preferences(OWLModel model) {
        PropertyList sources = model.getProject().getSources();
        Boolean flag = sources.getBoolean(ENABLED_PROPERTY);
        enabled = (flag != null && flag);
        if (enabled) {
            uniqueId = sources.getBoolean(UNIQUE_PROPERTY);
            prefix = sources.getString(PREFIX_PROPERTY);
            digits = sources.getInteger(DIGITS_PROPERTY);
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public boolean isUniqueId() {
        return uniqueId;
    }
    public void setUniqueId(boolean uniqueId) {
        this.uniqueId = uniqueId;
    }
    public String getPrefix() {
        return prefix;
    }
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    public int getDigits() {
        return digits;
    }
    public void setDigits(int digits) {
        this.digits = digits;
    }
    
    public void save(OWLModel model) {
        PropertyList sources = model.getProject().getSources();
        sources.setBoolean(ENABLED_PROPERTY, enabled);
        sources.setBoolean(UNIQUE_PROPERTY, uniqueId);
        sources.setString(PREFIX_PROPERTY, prefix);
        sources.setInteger(DIGITS_PROPERTY, digits);
    }

}
