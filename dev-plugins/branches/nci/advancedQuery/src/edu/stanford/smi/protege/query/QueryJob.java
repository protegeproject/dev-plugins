package edu.stanford.smi.protege.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.stanford.smi.protegex.owl.model.Deprecatable;
import edu.stanford.smi.protegex.owl.model.OWLClass;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.model.RDFResource;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.framestore.FrameStoreManager;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.server.framestore.FrameCalculatorFrameStore;
import edu.stanford.smi.protege.server.framestore.background.FrameCalculator;
import edu.stanford.smi.protege.util.ProtegeJob;

public class QueryJob extends ProtegeJob {
	private static final long serialVersionUID = 8985897197800156441L;
	
	private FrameCalculator frameCalculator;
	
	private Query q;

	public QueryJob(KnowledgeBase kb, Query q) {
		super(kb);
		this.q = q;
	}

	@Override
	public Object run() throws ProtegeException {
	    lookupFrameCalculator();
	    disableCaching();
	    try {
	        Set<Frame> frames = getKnowledgeBase().executeQuery(q);
	        List<NamedFrame> namedFrames = new ArrayList<NamedFrame>();
	        for (Frame frame : frames) {
                    boolean deprecated = false;
                    if (frame instanceof Deprecatable) {
 		        deprecated = ((Deprecatable) frame).isDeprecated();
 		    }
	            namedFrames.add(new NamedFrame(frame.getBrowserText(), deprecated, frame));
	        }
	        Collections.sort(namedFrames);
	        return namedFrames;
	    } finally {
	        enableCaching();
	    }
	}
	
	public void lookupFrameCalculator() {
	    if (frameCalculator == null) {
	        KnowledgeBase kb = getKnowledgeBase();
	        FrameStoreManager fsm = ((DefaultKnowledgeBase) kb).getFrameStoreManager();
	        FrameCalculatorFrameStore fcfs = (FrameCalculatorFrameStore) fsm.getFrameStoreFromClass(FrameCalculatorFrameStore.class);
	        if (fcfs != null) frameCalculator = fcfs.getFrameCalculator();
	    }
	}
	
	public void disableCaching() {
	    if (frameCalculator != null) {
	        frameCalculator.setCachingEnabled(false);
	    }
	}

	public void enableCaching() {
	    if (frameCalculator != null) {
	        frameCalculator.setCachingEnabled(true);
	    }
	}
	
}
