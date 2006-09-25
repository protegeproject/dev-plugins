package edu.stanford.smi.protege.query.ui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JPanel;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;
import edu.stanford.smi.protege.util.ListPanel;
import edu.stanford.smi.protege.util.ListPanelComponent;
import edu.stanford.smi.protegex.owl.model.OWLModel;

/**
 * A collection of useful methods when working with queries and {@link QueryComponent}s.
 *
 * @author Chris Callendar
 * @date 22-Sep-06
 */
public final class QueryUtil {

	/**
	 * Gets a {@link Query} from a {@link ListPanel}.   If any query is invalid then null is returned.
	 * @param listPanel the list panel
	 * @param andQuery if multiple queries exist then it will either create an {@link AndQuery} or an {@link OrQuery}.
	 * @return {@link Query} or null if no query was valid
	 */
	public static Query getQueryFromListPanel(ListPanel listPanel, boolean andQuery) {
		Query query = null;
		Collection<JPanel> panels = listPanel.getPanels();
		ArrayList<Query> queries = new ArrayList<Query>(panels.size());
		for (Iterator iter = panels.iterator(); iter.hasNext(); ) {
			ListPanelComponent comp = (ListPanelComponent) iter.next();
			QueryComponent qc = (QueryComponent) comp.getMainPanel();
			final Query q = qc.getQuery();
			
			// if q is null then this query is invalid and we must halt
			if (q == null) {
				queries.clear();
				break;
			}
			queries.add(q);
		}
		if (queries.size() == 1) {
			query = (Query) queries.get(0);
		} else if (queries.size() > 1) {
			query = (andQuery ? new AndQuery(queries) : new OrQuery(queries));
		}
		return query;
	}
	
	/**
	 * Adds a new query component to the {@link ListPanel}.
	 * @param kb the {@link KnowledgeBase} or {@link OWLModel}
	 * @param slots the allowed slots
	 * @param defaultSlot the slot to display by default (must be contained in the slots Collection)
	 * @param listPanel the {@link ListPanel} to add too
	 */
	public static void addQueryComponent(KnowledgeBase kb, Collection<Slot> slots, Slot defaultSlot, ListPanel listPanel) {
		QueryComponent qc = new QueryComponent(kb, slots, defaultSlot);
		ListPanelComponent comp = new ListPanelComponent(listPanel, qc, false, true);
		comp.setMinimumSize(new Dimension(60, 56));
		comp.setPreferredSize(new Dimension(500, 56));
		comp.setMaximumSize(new Dimension(5000, 56));
		comp.setRemoveActionToolTip("Remove query");
		listPanel.addPanel(comp);
	}	

	/**
	 * Adds a new OWL restriction query component to the {@link ListPanel}.
	 * @param model the {@link OWLModel}
	 * @param slots the allowed slots for the restriction queries
	 * @param defaultSlot the slot to display by default (must be contained in the slots Collection)
	 * @param listPanel the {@link ListPanel} to add too
	 */
	public static void addRestrictionQueryComponent(OWLModel model, Collection<Slot> slots, Slot defaultSlot, ListPanel listPanel) {
		OWLRestrictionQueryComponent qc = new OWLRestrictionQueryComponent(model, slots, defaultSlot);
		ListPanelComponent comp = new ListPanelComponent(listPanel, qc, false, true);
		comp.setMinimumSize(new Dimension(60, 100));
		comp.setPreferredSize(new Dimension(500, 200));
		comp.setMaximumSize(new Dimension(5000, 500));
		comp.setRemoveActionToolTip("Remove query");
		listPanel.addPanel(comp);
	}
	
}
