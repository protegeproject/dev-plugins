package uk.ac.man.cs.mig.coode.protege.id;

import edu.stanford.smi.protegex.owl.model.OWLModel;

public class Preferences {
    private boolean enabled;
    private boolean uniqueId;
    private String prefix;
    private int digits;
    
    
    public Preferences(OWLModel model) {
        
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
    

}
