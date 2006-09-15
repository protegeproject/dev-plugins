package edu.stanford.smi.protege.query;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.OntologyException;
import edu.stanford.smi.protege.exception.ProtegeError;
import edu.stanford.smi.protege.exception.ProtegeIOException;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Facet;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protege.model.Reference;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.model.query.QueryCallback;
import edu.stanford.smi.protege.model.query.SynchronizeQueryCallback;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.OWLRestrictionQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;
import edu.stanford.smi.protege.query.querytypes.OwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.PhoneticQuery;
import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.transaction.TransactionMonitor;

public class QueryNarrowFrameStore implements NarrowFrameStore {
  private static transient Logger log = Log.getLogger(QueryNarrowFrameStore.class);
  
  private Object kbLock;
  
  private NarrowFrameStore delegate;
  private String name;

  private PhoneticIndexer indexer;
  
  /*-----------------------------------------------------------
   * Query Narrow Frame Store support methods.
   */
  
  public QueryNarrowFrameStore(String name, NarrowFrameStore delegate, Set<Slot> searchableSlots, Object kbLock) {
    if (log.isLoggable(Level.FINE)) {
      log.fine("Constructing QueryNarrowFrameStore");
    }
    this.delegate = delegate;
    String path = ApplicationProperties.getApplicationDirectory().getAbsolutePath()
                    + File.separator + "lucene" + File.separator  + name;
    indexer = new PhoneticIndexer(searchableSlots, delegate, path, kbLock);
    this.kbLock = kbLock;
  }
  
  public void indexOntologies() {
    indexer.indexOntologies();
  }
  

  /*---------------------------------------------------------------------
   * executeQuery methods
   */
  
  public void executeQuery(final Query query, final QueryCallback qc) {
    new Thread() {
      public void run() {
        try {
          Set<Frame> results = executeQuery(query);
          qc.provideQueryResults(results);
        } catch (OntologyException oe) {
          qc.handleError(oe);
        } catch (ProtegeIOException ioe) {
          qc.handleError(ioe);
        } catch (Throwable  t) {
          qc.handleError(new ProtegeError(t));
        }
      }
    }.start();
  }
  
  public Set<Frame> executeQuery(Query query) throws OntologyException, ProtegeIOException {
    if (query instanceof PhoneticQuery) {
      return executeQuery((PhoneticQuery) query);
    }
    else if (query instanceof OWLRestrictionQuery) {
      return executeQuery((OWLRestrictionQuery) query);
    }
    else if (query instanceof OwnSlotValueQuery) {
      return  executeQuery((OwnSlotValueQuery) query);
    }
    else if (query instanceof AndQuery) {
      return executeQuery((AndQuery) query);
    }
    else if (query instanceof OrQuery) {
      return executeQuery((OrQuery) query);
    }
    else {
      SynchronizeQueryCallback qc = new SynchronizeQueryCallback(kbLock);
      synchronized (kbLock) {
        delegate.executeQuery(query, qc);
      }
      return qc.waitForResults();
    }
  }
  
  public Set<Frame> executeQuery(PhoneticQuery query) {
    try {
      return indexer.executeQuery(query);
    } catch (IOException ioe) {
      Log.getLogger().log(Level.WARNING, "Search failed", ioe);
      return null;
    } 
  }
  
  public Set<Frame> executeQuery(OWLRestrictionQuery query) {
    Query innerQuery = query.getInnerQuery();
    Set<Frame> frames = executeQuery(innerQuery);
    Set<Frame> results = new HashSet<Frame>();
    for (Frame frame : frames) {
      if (frame instanceof Cls) {
        results.addAll(query.executeQueryBasedOnQueryResult((Cls) frame, getDelegate()));
      }
    }
    return results;
  }
  
  public Set<Frame> executeQuery(OwnSlotValueQuery query) {
    return delegate.getMatchingFrames(query.getSlot(), null, false, query.getExpr(), -1);
  }
  
  public Set<Frame> executeQuery(AndQuery query) {
    Collection<Query> conjuncts = query.getConjuncts();
    if (conjuncts.isEmpty()) {
      return delegate.getFrames();
    }
    Iterator<Query> conjunctIterator = conjuncts.iterator();
    Query conjunct = conjunctIterator.next();
    Set<Frame> results = executeQuery(conjunct);
    while (conjunctIterator.hasNext()) {
      conjunct = conjunctIterator.next();
      results.retainAll(executeQuery(conjunct));
    }
    return results;
  }
  
  public Set<Frame> executeQuery(OrQuery query) {
    Set<Frame> results = new HashSet<Frame>();
    for (Query q : query.getDisjuncts()) {
      results.addAll(executeQuery(q));
    }
    return results;
  }
  
 
  /*---------------------------------------------------------------------
   *  Common Narrow Frame Store Functions (excepting executeQuery)
   */

  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public NarrowFrameStore getDelegate() {
    return delegate;
  }

  public FrameID generateFrameID() {
    return delegate.generateFrameID();
  }

  public int getFrameCount() {
    return delegate.getFrameCount();
  }

  public int getClsCount() {
    return delegate.getClsCount();
  }

  public int getSlotCount() {
    return delegate.getSlotCount();
  }

  public int getFacetCount() {
    return delegate.getFacetCount();
  }

  public int getSimpleInstanceCount() {
    return delegate.getSimpleInstanceCount();
  }

  public Set<Frame> getFrames() {
    return delegate.getFrames();
  }

  public Frame getFrame(FrameID id) {
    return delegate.getFrame(id);
  }

  public List getValues(Frame frame, Slot slot, Facet facet, boolean isTemplate) {
    return delegate.getValues(frame, slot, facet, isTemplate);
  }

  public int getValuesCount(Frame frame, Slot slot, Facet facet,
                            boolean isTemplate) {
    return delegate.getValuesCount(frame, slot, facet, isTemplate);
  }

  public void addValues(Frame frame, Slot slot, Facet facet,
                        boolean isTemplate, Collection values) {
    if (log.isLoggable(Level.FINE)) {
      log.fine("addValues");
    }
    delegate.addValues(frame, slot, facet, isTemplate, values);
    if (facet == null && !isTemplate) {
      indexer.addValues(frame, slot, values);
    }
  }

  public void moveValue(Frame frame, Slot slot, Facet facet,
                        boolean isTemplate, int from, int to) {
    delegate.moveValue(frame, slot, facet, isTemplate, from, to);
  }

  public void removeValue(Frame frame, Slot slot, Facet facet,
                          boolean isTemplate, Object value) {
    if (log.isLoggable(Level.FINE)) {
      log.fine("Remove  Value");
    }
    delegate.removeValue(frame, slot, facet, isTemplate, value);
    if (facet == null && !isTemplate) {
      indexer.removeValue(frame, slot, value);
    }
  }

  public void setValues(Frame frame, Slot slot, Facet facet,
                        boolean isTemplate, Collection values) {
    if (log.isLoggable(Level.FINE)) {
      log.fine("setValues");
    }
    delegate.setValues(frame, slot, facet, isTemplate, values);
    if (facet == null && !isTemplate) {
      indexer.removeValues(frame, slot);
      indexer.addValues(frame, slot, values);
    }
  }

  public Set<Frame> getFrames(Slot slot, Facet facet, boolean isTemplate,
                              Object value) {
    return delegate.getFrames(slot, facet, isTemplate, value);
  }

  public Set<Frame> getFramesWithAnyValue(Slot slot, Facet facet,
                                          boolean isTemplate) {
    return delegate.getFramesWithAnyValue(slot, facet, isTemplate);
  }

  public Set<Frame> getMatchingFrames(Slot slot, Facet facet,
                                      boolean isTemplate, String value,
                                      int maxMatches) {
    return delegate.getMatchingFrames(slot, facet, isTemplate, value, maxMatches);
  }

  public Set<Reference> getReferences(Object value) {
    return delegate.getReferences(value);
  }

  public Set<Reference> getMatchingReferences(String value, int maxMatches) {
    return delegate.getMatchingReferences(value, maxMatches);
  }

  public void deleteFrame(Frame frame) {
    if (log.isLoggable(Level.FINE)) {
      log.fine("deleteFrame ");
    }
    delegate.deleteFrame(frame);
    indexer.removeValues(frame);
  }

  public void close() {
    delegate.close();
  }

  public Set getClosure(Frame frame, Slot slot, Facet facet, boolean isTemplate) {
    return delegate.getClosure(frame, slot, facet, isTemplate);
  }

  public void replaceFrame(Frame frame) {
    delegate.replaceFrame(frame);
  }

  public boolean beginTransaction(String name) {
    return delegate.beginTransaction(name);
  }

  public boolean commitTransaction() {
    return delegate.commitTransaction();
  }

  public boolean rollbackTransaction() {
    return delegate.rollbackTransaction();
  }

  public TransactionMonitor getTransactionStatusMonitor() {
    return delegate.getTransactionStatusMonitor();
  }

}
