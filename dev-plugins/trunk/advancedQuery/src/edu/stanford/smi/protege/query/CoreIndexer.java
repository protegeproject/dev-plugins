package edu.stanford.smi.protege.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protege.model.Model;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
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
  public void indexOntologies() {
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
        for (Slot slot : searchableSlots) {
          List values;
          synchronized (kbLock) {
            values = delegate.getValues(frame, slot, null, false);
          }
          for (Object value : values) {
            if (!(value instanceof String)) {
              continue;
            }
            addUpdate(myWriter, frame, slot, (String) value);
          }
        }
      }
      myWriter.optimize();
      status = Status.READY;
      Log.getLogger().info("Finished indexing ontology (" 
                           + ((System.currentTimeMillis() - start)/1000) + " seconds)");
    } catch (IOException ioe) {
      died(ioe);
    } finally {
      forceClose(myWriter);
    }
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

  public Set<Frame> executeQuery(Slot slot, String expr) throws IOException {
    if (status == Status.DOWN) {
      return null;
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
    } finally {
      forceClose(searcher);
    }
    return results;
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
  
  protected Query generateLuceneQuery(Frame frame) throws IOException {
    BooleanQuery query  = new  BooleanQuery();
    
    Term term;
    term = new Term(FRAME_NAME, getFrameName(frame));
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
    Log.getLogger().log(Level.WARNING, "Search index update failed",ioe);
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
    return (String) values.iterator().next();
  }
  
  private Frame getFrameByName(String name) {
    Set<Frame> frames = delegate.getFrames(nameSlot, null, false, name);
    return frames.iterator().next();
  }
  
  
  /* --------------------------------------------------------------------------
   * Update Utilities for the Narrow Frame Store
   */
  
  public void addValues(Frame frame, Slot slot, Collection values) { 
    if (status == Status.DOWN || !searchableSlots.contains(slot)) {
      return;
    }
    IndexWriter writer = null;
    try {
      long start = System.currentTimeMillis();
      writer = openWriter(false);
      for (Object value : values) {
        if (value instanceof String) {
          addUpdate(writer, frame, slot, (String) value);
        }
      }
      writer.optimize();
      if (log.isLoggable(Level.FINE)) {
        log.fine("updated " + values.size() + " values in " + (System.currentTimeMillis() - start) + "ms");
      }
    } catch (IOException ioe) {
      died(ioe);
    } finally {
      forceClose(writer);
    }
  }
  
  public void removeValue(Frame frame, Slot slot, Object value) {
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
    } finally {
      forceClose(searcher);
    }
  }
  
  public void removeValues(Frame frame, Slot slot) {
    if (status == Status.DOWN || !searchableSlots.contains(slot)) {
      return;
    }
    IndexSearcher searcher = null;
    try {
      long start = System.currentTimeMillis();
      searcher = new IndexSearcher(getIndexPath());
      Hits hits = searcher.search(generateLuceneQuery(frame, slot));
      for (int i = 0; i < hits.length(); i++) {
        searcher.getIndexReader().deleteDocument(hits.id(i));
      }
      if (log.isLoggable(Level.FINE)) {
        log.fine("remove values operation took " + (System.currentTimeMillis() - start) + "ms");
      }
    } catch (IOException ioe) {
      died(ioe);
    } finally {
      forceClose(searcher);
    }
  }
  
  public void removeValues(Frame frame) {
    if (status == Status.DOWN) {
      return;
    }
    IndexSearcher searcher = null;
    try {
      long start = System.currentTimeMillis();
      searcher = new IndexSearcher(getIndexPath());
      Hits hits = searcher.search(generateLuceneQuery(frame));
      for (int i = 0; i < hits.length(); i++) {
        searcher.getIndexReader().deleteDocument(hits.id(i));
      }
      if (log.isLoggable(Level.FINE)) {
        log.fine("remove values operation took " + (System.currentTimeMillis() - start) + "ms");
      }
    } catch (IOException ioe) {
      died(ioe);
    } finally {
      forceClose(searcher);
    }
  }

}
