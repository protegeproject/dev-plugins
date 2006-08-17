package edu.stanford.smi.protege.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;


import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Facet;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Model;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.MergingNarrowFrameStore;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.server.RemoteSession;
import edu.stanford.smi.protege.server.Server;
import edu.stanford.smi.protege.server.Session;
import edu.stanford.smi.protege.server.framestore.ServerFrameStore;
import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protege.util.Log;

public class PhoneticIndexer implements Runnable {
  private enum Status {
    INDEXING, READY, DOWN
  };
  
  private String indexPath = ApplicationProperties.getApplicationDirectory().getAbsolutePath() + "/lucene";
  
  private Object lock = new Object();
  private NarrowFrameStore delegate;
  private Status status = Status.INDEXING;
  
  
  private Set<Cls> parentSlots;
  
  private static final String FRAME_FIELD    = "frame";
  private static final String SLOT_FIELD     = "slot";
  private static final String CONTENTS_FIELD = "contents";
  
  public PhoneticIndexer(Set<Cls> parentSlots, NarrowFrameStore delegate) {
    this.parentSlots = parentSlots;
    this.delegate = delegate;
  }

  public void run() {
    try {
      RemoteSession session = new Session(null, null);
      ServerFrameStore.recordCallNoCheck(session);
      long start = System.currentTimeMillis();
      Log.getLogger().info("Started indexing ontology");
      indexOntologies();
      Log.getLogger().info("Finished indexing ontology (" 
                           + ((System.currentTimeMillis() - start)/1000) + " seconds)");
    } catch (Throwable t) {
      Log.getLogger().log(Level.WARNING, "Exception caught in index thread", t);
    }
  }

  
  @SuppressWarnings("unchecked")
  private void indexOntologies() {
    IndexWriter myWriter;
    try {
      myWriter = new IndexWriter(indexPath,
                                 new StandardAnalyzer(),
                                 true);
    } catch (IOException e) {
      Log.getLogger().warning("Could not index ontologies because of I/O Error" + e);
      status = Status.DOWN;
      return;
    }
    Collection<NarrowFrameStore> dataSources = getDataSources();
    if (dataSources == null) {
      Log.getLogger().warning("Could not index ontologies - phonetic search will fail");
      status = Status.DOWN;
      return;
    }
    for (NarrowFrameStore nfs : dataSources) {
      for (Frame frame : nfs.getFrames()) {
        for (Slot slot : getSearchableSlots()) {
          for (Object value : delegate.getValues(frame, slot, null, false)) {
            if (!(value instanceof String)) {
              continue;
            }
            String content = (String) value;
            Document doc = new Document();
            doc.add(new Field(FRAME_FIELD, "" + frame.getFrameID().getLocalPart(), 
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
  }
  
  private String getValueString(NarrowFrameStore nfs, Frame frame, Slot slot) {
    String result = null;
    List values = nfs.getValues(frame, slot, (Facet) null, false);
    if (values == null) {
      return null;
    }
    for (Object value : values) {
      if (value instanceof String) {
        if (result == null) {
          result = "";
        }
        result = result + " " + value;
      }
    }
    return result;
  }
  
  private Collection<NarrowFrameStore> getDataSources() {
    Collection<NarrowFrameStore> dataSources = new ArrayList<NarrowFrameStore>();
    MergingNarrowFrameStore mnfs = null;
    NarrowFrameStore nfs;
    for (nfs = delegate; 
         nfs != null && !(nfs instanceof MergingNarrowFrameStore);
         nfs = nfs.getDelegate()) {
      ;
    }
    if (nfs == null) {
      return null;
    }
    mnfs = (MergingNarrowFrameStore) nfs;
    for (NarrowFrameStore anfs : mnfs.getAvailableFrameStores()) {
      NarrowFrameStore bottom = anfs;
      while (bottom.getDelegate() != null) {
        bottom = bottom.getDelegate();
      }
      dataSources.add(bottom);
    }
    return dataSources;
  }
  
  
  @SuppressWarnings("unchecked")
  private Set<Slot> getSearchableSlots() {
    Slot directSubClsesSlot   = (Slot) delegate.getFrame(Model.SlotID.DIRECT_SUBCLASSES);
    Slot directInstancesSlot  = (Slot) delegate.getFrame(Model.SlotID.DIRECT_INSTANCES);
    Set<Slot> searchableSlots = new HashSet<Slot>();
    for (Cls parentSlot : parentSlots) {
      Collection slotClses = delegate.getClosure(parentSlot, directSubClsesSlot, null, false);
      slotClses.add(parentSlot);
      for (Object slotCls : slotClses) {
        if (slotCls instanceof Cls) {
          searchableSlots.addAll(delegate.getClosure((Cls) slotCls, directInstancesSlot, null, false));
        }
      }
    }
    return searchableSlots;
  }

}
