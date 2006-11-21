package edu.stanford.smi.protege.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Model;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.util.FutureTask;
import edu.stanford.smi.protege.util.Log;

public abstract class CoreIndexer {
  private transient static final Logger log = Log.getLogger(CoreIndexer.class);
  
  private enum Status {
    INDEXING, READY, DOWN
  };
  
  private Object kbLock;
  
  private String indexPath;
  
  private Analyzer analyzer;
  private NarrowFrameStore delegate;
  private Status status = Status.INDEXING;
  
  private Set<Slot> searchableSlots;
  
  boolean owlMode = false;
  
  private Slot nameSlot;
  
  private static final String FRAME_NAME                = "frameName";
  private static final String SLOT_NAME                 = "slotName";
  private static final String CONTENTS_FIELD            = "contents";
  
  private IndexTaskRunner indexRunner = new IndexTaskRunner();

  
  public CoreIndexer(Set<Slot> searchableSlots, 
                     NarrowFrameStore delegate, 
                     String path, 
                     Object kbLock) {
    this.searchableSlots = searchableSlots;
    this.delegate = delegate;
    this.kbLock = kbLock;
    this.indexPath  = path;
    analyzer = createAnalyzer();
    nameSlot = (Slot) delegate.getFrame(Model.SlotID.NAME);
    indexRunner.startBackgroundThread();
  }
  
  public void dispose() {
      indexRunner.dispose();
  }
  
  public void setOWLMode(boolean owlMode) {
    this.owlMode = owlMode;
  }
  
  protected abstract Analyzer createAnalyzer();

  private IndexWriter openWriter(boolean create) throws IOException {
    return new IndexWriter(getIndexPath(), analyzer, create);
  }
  
  private String getIndexPath() {
    return indexPath;
  }
  
  @SuppressWarnings("unchecked")
  public void indexOntologies() throws ProtegeException {
      FutureTask indexTask = new FutureTask() {
          public void run() {
              boolean errorsFound = false;
              long start = System.currentTimeMillis();
              Log.getLogger().info("Started indexing ontology with " + searchableSlots.size() + " searchable slots");
              IndexWriter myWriter = null;
              try {
                  myWriter = openWriter(true);
                  Set<Frame> frames;
                  synchronized (kbLock) {
                      frames = delegate.getFrames();
                  }
                  for (Frame frame : frames) {
                    errorsFound = errorsFound || !addFrame(myWriter, frame);
                  }
                  myWriter.optimize();
                  status = Status.READY;
                  Log.getLogger().info("Finished indexing ontology (" 
                                       + ((System.currentTimeMillis() - start)/1000) + " seconds)");
              } catch (IOException ioe) {
                  died(ioe);
                  errorsFound = true;
              } finally {
                  forceClose(myWriter);
              }
              if (errorsFound) {  // ToDo - do this *much* better
                  throw new ProtegeException("Errors Found - see console log for details");
              }
          }
      };
      indexRunner.addTask(indexTask);
      try {
          indexTask.get();
      } catch (ExecutionException ee) {
          throw new RuntimeException(ee);
      } catch (InterruptedException interrupt) {
          throw new RuntimeException(interrupt);
      }
  }
  
  private boolean addFrame(IndexWriter writer, Frame frame) {
      boolean errorsFound = false;
      for (Slot slot : searchableSlots) {
          try {
              List values;
              synchronized (kbLock) {
                  values = delegate.getValues(frame, slot, null, false);
              }
              for (Object value : values) {
                  if (!(value instanceof String)) {
                      continue;
                  }
                  addUpdate(writer, frame, slot, (String) value);
              }
          } catch (Exception e) {
              Log.getLogger().log(Level.WARNING, "Exception caught indexing ontologies", e);
              Log.getLogger().warning("continuing...");
              errorsFound = true;
          }
      }
      return !errorsFound;
  }
  
  protected void addUpdate(IndexWriter writer, Frame frame, Slot slot, String value) throws IOException {
    if (owlMode && value.startsWith("~#")) {
      value = value.substring(5);
    }
    if (status == Status.DOWN || !searchableSlots.contains(slot)) {
      return;
    }
    Document doc = new Document();
    doc.add(new Field(FRAME_NAME, getFrameName(frame), 
                      Field.Store.YES, Field.Index.UN_TOKENIZED));
    doc.add(new Field(SLOT_NAME, getFrameName(slot),
                      Field.Store.YES, Field.Index.UN_TOKENIZED));
    doc.add(new Field(CONTENTS_FIELD, value, Field.Store.YES, Field.Index.TOKENIZED));
    writer.addDocument(doc);
  }

  @SuppressWarnings("unchecked")
  public Set<Frame> executeQuery(final Slot slot, final String expr) throws IOException {
      FutureTask queryTask = new FutureTask() {
          public void run() {
              if (status == Status.DOWN) {
                  set(null);
              }
              Searcher searcher = null;
              Set<Frame> results = new HashSet<Frame>();
              try {
                  Query luceneQuery = generateLuceneQuery(slot, expr);
                  searcher = new IndexSearcher(getIndexPath());
                  Hits hits = searcher.search(luceneQuery);
                  for (int i = 0; i < hits.length(); i++) {
                      Document doc = hits.doc(i);
                      String frameName = doc.get(FRAME_NAME);
                      synchronized (kbLock) {
                          results.add(getFrameByName(frameName));
                      }
                  }
              } catch (IOException ioe) {
                  setException(ioe);
              } finally {
                  forceClose(searcher);
              }
              set(results);
          }
      };

      try {
          indexRunner.addTask(queryTask);
          return (Set<Frame>) queryTask.get();
      } catch (InterruptedException e) {
          throw new RuntimeException(e);
      } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause != null && cause instanceof IOException) {
              throw (IOException) cause;
          }
          else {
              throw new RuntimeException(e);
          }
      }
  }
  
  protected Query generateLuceneQuery(Slot slot, String expr) throws IOException {
    String contents    = "" + expr;
    BooleanQuery query = new  BooleanQuery();
    
    TokenStream ts = analyzer.tokenStream(CONTENTS_FIELD, new StringReader(contents));
    Token tok;
    while ((tok = ts.next()) != null) {
      Term term = new Term(CONTENTS_FIELD, tok.termText());
      query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    }
    Term term = new Term(SLOT_NAME, getFrameName(slot));
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    return query;
  }
  
  protected Query generateLuceneQuery(String fname) throws IOException {
    BooleanQuery query  = new  BooleanQuery();
    
    Term term;
    term = new Term(FRAME_NAME, fname);
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    
    return query;
  }
  
  protected Query generateLuceneQuery(Frame frame, Slot slot) throws IOException {
    BooleanQuery query  = new  BooleanQuery();
    
    Term term;
    term = new Term(FRAME_NAME, getFrameName(frame));
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    term = new Term(SLOT_NAME, getFrameName(slot));
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    return query;
  }
  
  private void died(IOException ioe) {
    Log.getLogger().warning("Search index update failed " + ioe);
    Log.getLogger().warning("This exception will not interfere with normal (non-query) operations");
    Log.getLogger().warning("suggest reindexing to get the lucene indicies back");
    status = Status.DOWN;
  }
  
  private void forceClose(Searcher searcher) {
    try {
      if (searcher != null) {
        searcher.close();
      }
    } catch (IOException ioe) {
      Log.getLogger().log(Level.WARNING, "Exception caught closing files involved during search", ioe);
    }
  }
  
  private void forceClose(IndexWriter writer) {
    try {
      if (writer != null) {
        writer.close();
      }
    } catch (IOException ioe) {
      Log.getLogger().log(Level.WARNING, "Exception caught closing files involved in lucene indicies", ioe);
    }
  }
  
  private String getFrameName(Frame frame) {
    Collection values = delegate.getValues(frame, nameSlot, null, false);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return (String) values.iterator().next();
  }
  
  private Frame getFrameByName(String name) {
    Set<Frame> frames = delegate.getFrames(nameSlot, null, false, name);
    return frames.iterator().next();
  }
  
  private boolean isAnonymous(Frame frame) {
    if (!owlMode) {
      return false;
    }
    
    String name = getFrameName(frame);
    if (name == null) {
      return true;
    }
    return name.startsWith("@");
  }
  
  private void installOptimizeTask() {
      indexRunner.setCleanUpTask(new Runnable() {
          public void run() {
              try {
                  IndexWriter myWriter = openWriter(false);
                  myWriter.optimize();
                  myWriter.close();
              } catch (Exception e) {
                  log.info("Could not optimize the lucene index - " + e);
              }
          }
      });
  }
  
  
  /* --------------------------------------------------------------------------
   * Update Utilities for the Narrow Frame Store
   */
  
  public void addValues(final Frame frame, final Slot slot, final Collection values) { 
      indexRunner.addTask(new FutureTask() {
          public void run() {
              if (status == Status.DOWN || !searchableSlots.contains(slot) || isAnonymous(frame)) {
                  return;
              }
              IndexWriter writer = null;
              try {
                  long start = System.currentTimeMillis();
                  writer = openWriter(false);
                  if (slot.equals(nameSlot)) {
                      addFrame(writer, frame);
                      return;
                  }
                  for (Object value : values) {
                      if (value instanceof String) {
                          addUpdate(writer, frame, slot, (String) value);
                      }
                  }
                  if (log.isLoggable(Level.FINE)) {
                      log.fine("updated " + values.size() + " values in " + (System.currentTimeMillis() - start) + "ms");
                  }
              } catch (IOException ioe) {
                  died(ioe);
              } catch (Throwable t) {
                  Log.getLogger().warning("Error during indexing" + t);
              } finally {
                  forceClose(writer);
              }
          }
      });
      installOptimizeTask();
  }
  
  public void removeValue(final Frame frame, final Slot slot, final Object value) {
      indexRunner.addTask(new FutureTask() {
          public void run() {
              if (status == Status.DOWN || !searchableSlots.contains(slot) || !(value instanceof String)) {
                  return;
              }
              IndexSearcher searcher = null;
              try {
                  long start = System.currentTimeMillis();
                  searcher = new IndexSearcher(getIndexPath());
                  Hits hits = searcher.search(generateLuceneQuery(frame, slot));
                  for (int i = 0; i < hits.length(); i++) {
                      Document doc = hits.doc(i);
                      if (doc.get(CONTENTS_FIELD).equals(value)) {
                          searcher.getIndexReader().deleteDocument(hits.id(i));
                          break;
                      }
                  }
                  if (log.isLoggable(Level.FINE)) {
                      log.fine("remove value operation took " + (System.currentTimeMillis() - start) + "ms");
                  }
              } catch (IOException ioe) {
                  died(ioe);
              } catch (Throwable t) {
                  Log.getLogger().warning("Exception caught during indexing" + t);
              } finally {
                  forceClose(searcher);
              }
          }
      });
      installOptimizeTask();
  }
  
  public void removeValues(final Frame frame, final Slot slot) {
      indexRunner.addTask(new FutureTask() {
          public void run() {
              if (status == Status.DOWN || !searchableSlots.contains(slot)) {
                  return;
              }
              IndexSearcher searcher = null;
              try {
                  long start = System.currentTimeMillis();
                  searcher = new IndexSearcher(getIndexPath());
                  Hits hits;
                  if (slot.equals(nameSlot)) {
                      String fname = getFrameName(frame);
                      hits = searcher.search(generateLuceneQuery(fname));
                  }
                  else {
                      hits = searcher.search(generateLuceneQuery(frame, slot));
                  }
                  for (int i = 0; i < hits.length(); i++) {
                      searcher.getIndexReader().deleteDocument(hits.id(i));
                  }
                  if (log.isLoggable(Level.FINE)) {
                      log.fine("remove values operation took " + (System.currentTimeMillis() - start) + "ms");
                  }
              } catch (IOException ioe) {
                  died(ioe);
              } catch (Throwable t) {
                  Log.getLogger().warning("Exception caught during indexing " + t);
              } finally {
                  forceClose(searcher);
              }
          }
      });
      installOptimizeTask();
  }
  
  public void removeValues(final String fname) {
      indexRunner.addTask(new FutureTask() {
          public void run() {
              if (status == Status.DOWN) {
                  return;
              }
              IndexSearcher searcher = null;
              try {
                  long start = System.currentTimeMillis();
                  searcher = new IndexSearcher(getIndexPath());
                  Hits hits = searcher.search(generateLuceneQuery(fname));
                  for (int i = 0; i < hits.length(); i++) {
                      searcher.getIndexReader().deleteDocument(hits.id(i));
                  }
                  if (log.isLoggable(Level.FINE)) {
                      log.fine("remove values operation took " + (System.currentTimeMillis() - start) + "ms");
                  }
              } catch (IOException ioe) {
                  died(ioe);
              } catch (Throwable t) {
                  Log.getLogger().warning("Exception caught during indexing " + t);
              } finally {
                  forceClose(searcher);
              }
          }
      });
      installOptimizeTask();
  }

}
