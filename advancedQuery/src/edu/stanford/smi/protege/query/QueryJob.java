package edu.stanford.smi.protege.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.util.ProtegeJob;

public class QueryJob extends ProtegeJob {
	private static final long serialVersionUID = 8985897197800156441L;
	
	private Query q;

	public QueryJob(KnowledgeBase kb, Query q) {
		super(kb);
		this.q = q;
	}

	@Override
	public Object run() throws ProtegeException {
		Set<Frame> frames = getKnowledgeBase().executeQuery(q);
		List<NamedFrame> namedFrames = new ArrayList<NamedFrame>();
		for (Frame frame : frames) {
			namedFrames.add(new NamedFrame(frame.getBrowserText(), frame));
		}
		Collections.sort(namedFrames);
		return namedFrames;
	}

}
