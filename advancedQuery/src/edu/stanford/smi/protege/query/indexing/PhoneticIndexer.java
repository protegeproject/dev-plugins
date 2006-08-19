package edu.stanford.smi.protege.query.indexing;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
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

import com.tangentum.phonetix.DoubleMetaphone;
import com.tangentum.phonetix.lucene.PhoneticAnalyzer;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.query.PhoneticQuery;
import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protege.util.Log;

public class PhoneticIndexer implements Runnable {
  private enum Status {
    INDEXING, READY, DOWN
  };
  
  private String indexPath = ApplicationProperties.getApplicationDirectory().getAbsolutePath() + "/lucene";
  
  private Analyzer analyzer = new PhoneticAnalyzer(new DoubleMetaphone(200));
  private NarrowFrameStore delegate;
  private Status status = Status.INDEXING;
  
  private Set<Slot> searchableSlots;
  
  private static final String FRAME_LOCAL_FIELD    = "frameLocal";
  private static final String FRAME_PROJECT_FIELD  = "frameProject";
  private static final String SLOT_FIELD     = "slot";
  private static final String CONTENTS_FIELD = "contents";
  
  public PhoneticIndexer(Set<Slot> searchableSlots, NarrowFrameStore delegate) {
    this.searchableSlots = searchableSlots;
    this.delegate = delegate;
  }

  public void run() {

  }

  
  @SuppressWarnings("unchecked")
  public void indexOntologies() {
    long start = System.currentTimeMillis();
    Log.getLogger().info("Started indexing ontology");
    IndexWriter myWriter;
    try {
      myWriter = new IndexWriter(indexPath,
                                 analyzer,
                                 true);
    } catch (IOException e) {
      Log.getLogger().warning("Could not index ontologies because of I/O Error" + e);
      status = Status.DOWN;
      return;
    }
    for (Frame frame : delegate.getFrames()) {
      for (Slot slot : searchableSlots) {
        for (Object value : delegate.getValues(frame, slot, null, false)) {
          if (!(value instanceof String)) {
            continue;
          }
          String content = (String) value;
          Document doc = new Document();
          FrameID fid = frame.getFrameID();
          doc.add(new Field(FRAME_LOCAL_FIELD, "" + fid.getLocalPart(), 
                            Field.Store.YES, Field.Index.UN_TOKENIZED));
          doc.add(new Field(FRAME_PROJECT_FIELD, "" + fid.getMemoryProjectPart(), 
                            Field.Store.YES, Field.Index.UN_TOKENIZED));
          doc.add(new Field(SLOT_FIELD, "" + slot.getFrameID().getLocalPart(),
                            Field.Store.YES, Field.Index.UN_TOKENIZED));
          doc.add(new Field(CONTENTS_FIELD, content, Field.Store.YES, Field.Index.TOKENIZED));
          doc.add(new Field("title", "Frame = " + frame + ", Slot = " + slot, 
                            Field.Store.YES, Field.Index.UN_TOKENIZED));
          try {
            myWriter.addDocument(doc);
          } catch (IOException e) {
            Log.getLogger().warning("Could not add frame slot value to searchable indicies - search will fail");
            status = Status.DOWN;  // could imagine a partial status here...
            return;
          }
        }
      }
    }
    try {
      myWriter.optimize();
      myWriter.close();
    } catch (IOException ioe) {
      Log.getLogger().log(Level.WARNING, "Exception closing writer", ioe);
      status = Status.DOWN;
      return;
    }
    status = Status.READY;
    Log.getLogger().info("Finished indexing ontology (" 
                         + ((System.currentTimeMillis() - start)/1000) + " seconds)");
  }
  
  public Set<Frame> executeQuery(PhoneticQuery query) throws IOException {
    Query luceneQuery = generateLuceneQuery(query)
    Searcher searcher = new IndexSearcher(indexPath);
    Hits hits = searcher.search(luceneQuery);
    Set<Frame> results = new HashSet<Frame>();
    for (int i = 0; i < hits.length(); i++) {
      Document doc = hits.doc(i);
      int frameLocal = Integer.parseInt(doc.get(FRAME_LOCAL_FIELD));
      int frameProject = Integer.parseInt(doc.get(FRAME_PROJECT_FIELD));
      results.add(delegate.getFrame(FrameID.createLocal(frameProject, frameLocal)));
    }
    return results;
  }
  
  public Query generateLuceneQuery(PhoneticQuery pq) throws IOException {
    String slot     = "" + pq.getSlot().getFrameID().getLocalPart();
    String contents = "" + pq.getExpr();
    BooleanQuery query = new  BooleanQuery();
    TokenStream ts = analyzer.tokenStream(CONTENTS_FIELD, new StringReader(contents));
    Token tok;
    while ((tok = ts.next()) != null) {
      Term term = new Term(CONTENTS_FIELD, tok.termText());
      query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    }
    Term term = new Term(SLOT_FIELD, slot);
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    return query;
  }
}
