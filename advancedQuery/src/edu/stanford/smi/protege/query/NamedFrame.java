package edu.stanford.smi.protege.query;

import java.io.Serializable;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Localizable;
import edu.stanford.smi.protege.util.LocalizeUtils;

public class NamedFrame implements Serializable, Comparable<NamedFrame>, Localizable {
	private static final long serialVersionUID = -4865927039683192271L;
	private String browserText;
	private Frame frame;
	private boolean deprecated;
	
	public NamedFrame(String browserText, boolean deprecated, Frame frame) {
		super();
		this.browserText = browserText;
		this.deprecated = deprecated;
		this.frame = frame;
	}

	public String getBrowserText() {
		return browserText;
	}
    
    public boolean isDeprecated() {
        return deprecated;
    }
    
	public Frame getFrame() {
		return frame;
	}


	public int compareTo(NamedFrame o) {
		return browserText.toLowerCase().compareTo(o.getBrowserText().toLowerCase());
	}

	public void localize(KnowledgeBase kb) {
		LocalizeUtils.localize(frame, kb);
	}
}
