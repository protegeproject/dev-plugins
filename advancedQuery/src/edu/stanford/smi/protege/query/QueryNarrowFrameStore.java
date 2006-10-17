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

import edu.stanford.smi.protege.exception.ModificationException;
import edu.stanford.smi.protege.exception.OntologyException;
import edu.stanford.smi.protege.exception.ProtegeError;
import edu.stanford.smi.protege.exception.ProtegeIOException;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Facet;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protege.model.KnowledgeBase;
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
import edu.stanford.smi.protege.util.SimpleStringMatcher;
import edu.stanford.smi.protege.util.transaction.TransactionMonitor;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class QueryNarrowFrameStore implements NarrowFrameStore {
  private static transient Logger log = Log.getLogger(QueryNarrowFrameStore.class);
  
  private Object kbLock;
  
  private NarrowFrameStore delegate;
  private String name;

  private boolean useStdLucene  = false;
  private StdIndexer      stdIndexer;
  private PhoneticIndexer phoneticIndexer;
  
  private boolean indexingInProgress = false;
  private boolean indexHasProblems = false;
  
  /*-----------------------------------------------------------
   * Query Narrow Frame Store support methods.
   */
  
  public QueryNarrowFrameStore(String name, NarrowFrameStore delegate, Set<Slot> searchableSlots, KnowledgeBase kb) {
    if (log.isLoggable(Level.FINE)) {
      log.fine("Constructing QueryNarrowFrameStore");
    }
    this.delegate = delegate;
    String path = ApplicationProperties.getApplicationDirectory().getAbsolutePath()
                    + File.separator + "lucene" + File.separator  + name;
    phoneticIndexer = new PhoneticIndexer(searchableSlots, delegate, path + File.separator + "phonetic", kb);
    if (useStdLucene) {
      stdIndexer = new StdIndexer(searchableSlots, delegate,      path + File.separator + "standard", kb);
    }
    if (kb instanceof OWLModel) {
      phoneticIndexer.setOWLMode(true);
      if (useStdLucene) {
        stdIndexer.setOWLMode(true);
      }
    }
    this.kbLock = kb;
  }
  
  public void indexOntologies() {
    synchronized (kbLock) {
      indexHasProblems = false;
      indexingInProgress = true;
    }
    try {
      phoneticIndexer.indexOntologies();
      if (useStdLucene) {
        stdIndexer.indexOntologies();
      }
    } finally {
      synchronized (kbLock) {
        indexingInProgress = false;
      }
    }
  }
  
  public void checkWriteable() {
    synchronized (kbLock) {
      if (indexingInProgress) {
        throw new ModificationException("Server project in read-only mode: Indexing in Progress");
      }
    }
  }
  

  /*---------------------------------------------------------------------
   * executeQuery methods
   */
  
  public void executeQuery(final Query query, final QueryCallback qc) throws OntologyException, ProtegeIOException {
    synchronized (kbLock) {
      if (indexingInProgress) {
        throw new ProtegeIOException("Lucene Indicies not ready yet: Indexing in progress");
      }
    }
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
  
  public Set<Frame> executeQuery(PhoneticQuery query) throws ProtegeIOException {
    try {
      return phoneticIndexer.executeQuery(query);
    } catch (IOException ioe) {
      Log.getLogger().log(Level.WARNING, "Search failed", ioe);
      throw new ProtegeIOException(ioe);
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
  
  public Set<Frame> executeQuery(OwnSlotValueQuery query) throws ProtegeIOException {
    if  (useStdLucene) {
      try {
        return stdIndexer.executeQuery(query);
      } catch (IOException ioe) {
        Log.getLogger().log(Level.WARNING, "Search failed", ioe);
        throw new ProtegeIOException(ioe);
      } 
    } else {
      String searchString = query.getExpr();
      if (searchString.startsWith("*")) {
        return delegate.getMatchingFrames(query.getSlot(), null, false, searchString, -1);
      }
      else {
        SimpleStringMatcher matcher = new SimpleStringMatcher(searchString);
        Set<Frame> frames = delegate.getMatchingFrames(query.getSlot(), null, false, 
                                                       "*" + searchString, -1);
        Set<Frame> results = new HashSet<Frame>();
        for (Frame frame : frames)  {
          boolean found = false;
          for (Object o : delegate.getValues(frame, query.getSlot(), null, false)) {
            if (o instanceof String && matcher.isMatch((String) o)) {
              found = true;
              break;
            }
          }
          if (found) {
            results.add(frame);
          }
        }
        return results;
      }
    }
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
  
  public void handleErrorDuringNormalOperations(Throwable t) {
    if (!indexHasProblems) {
      indexHasProblems = true;
      Log.getLogger().log(Level.WARNING, "Problem found updating index", t);
      Log.getLogger().warning("This will not interfere with normal operations but consider reindexing");
      Log.getLogger().warning("Future messages will go to debug logging");
    } else {
      if (log.isLoggable(Level.FINE)) {
        log.log(Level.FINE, "Exception caught updating index", t);
      }
    }
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
                        boolean isTemplate, Collection values) throws ProtegeIOException {
    checkWriteable();
    if (log.isLoggable(Level.FINE)) {
      log.fine("addValues");
    }
    delegate.addValues(frame, slot, facet, isTemplate, values);
    try {
      if (facet == null && !isTemplate) {
        phoneticIndexer.addValues(frame, slot, values);
        if (useStdLucene) {
          stdIndexer.addValues(frame, slot, values);
        }
      }
    } catch (Exception e) {
      handleErrorDuringNormalOperations(e);
    }
  }

  public void moveValue(Frame frame, Slot slot, Facet facet,
                        boolean isTemplate, int from, int to) throws ProtegeIOException {
    checkWriteable();
    delegate.moveValue(frame, slot, facet, isTemplate, from, to);
  }

  public void removeValue(Frame frame, Slot slot, Facet facet,
                          boolean isTemplate, Object value) throws ProtegeIOException {
    checkWriteable();
    if (log.isLoggable(Level.FINE)) {
      log.fine("Remove  Value");
    }
    delegate.removeValue(frame, slot, facet, isTemplate, value);
    try {
      if (facet == null && !isTemplate) {
        phoneticIndexer.removeValue(frame, slot, value);
        if (useStdLucene) {
          stdIndexer.removeValue(frame, slot, value);
        }
      }
    } catch (Exception e) {
      handleErrorDuringNormalOperations(e);
    }
  }

  public void setValues(Frame frame, Slot slot, Facet facet,
                        boolean isTemplate, Collection values) throws ProtegeIOException {
    checkWriteable();
    if (log.isLoggable(Level.FINE)) {
      log.fine("setValues");
    }
    delegate.setValues(frame, slot, facet, isTemplate, values);
    try {
      if (facet == null && !isTemplate) {
        phoneticIndexer.removeValues(frame, slot);
        phoneticIndexer.addValues(frame, slot, values);
        if (useStdLucene) {
          stdIndexer.removeValues(frame, slot);
          stdIndexer.addValues(frame, slot, values);
        }
      }
    } catch (Exception e) {
      handleErrorDuringNormalOperations(e);
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

  public void deleteFrame(Frame frame) throws ProtegeIOException {
    checkWriteable();
    if (log.isLoggable(Level.FINE)) {
      log.fine("deleteFrame ");
    }
    delegate.deleteFrame(frame);
    try {
      phoneticIndexer.removeValues(frame);
      if (useStdLucene) {
        stdIndexer.removeValues(frame);
      }
    } catch (Exception e) {
      handleErrorDuringNormalOperations(e);
    }
  }

  public void close() {
    delegate.close();
  }

  public Set getClosure(Frame frame, Slot slot, Facet facet, boolean isTemplate) {
    return delegate.getClosure(frame, slot, facet, isTemplate);
  }

  public void replaceFrame(Frame frame) throws ProtegeIOException {
    checkWriteable();
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
