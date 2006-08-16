package edu.stanford.smi.protege.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Facet;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protege.model.Model;
import edu.stanford.smi.protege.model.Reference;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.MergingNarrowFrameStore;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.transaction.TransactionMonitor;

public class QueryNarrowFrameStore implements NarrowFrameStore {
  private IndexWriter writer;
  private NarrowFrameStore delegate;
  private String name;
  private Set<Cls> parentSlots;
  
  private static final String FRAME_FIELD    = "frame";
  private static final String SLOT_FIELD     = "slot";
  private static final String CONTENTS_FIELD = "contents";

  
  /*-----------------------------------------------------------
   * Query Narrow Frame Store support methods.
   */
  
  public QueryNarrowFrameStore(NarrowFrameStore delegate, Set<Cls> parentSlots) {
    this.delegate = delegate;
    if (parentSlots == null) {
      this.parentSlots = new HashSet<Cls>();
      this.parentSlots.add((Cls) delegate.getFrame(Model.ClsID.STANDARD_SLOT));
    }
    this.parentSlots = parentSlots;
    new Thread() {
      public void run() {
        indexOntologies();
      }
    };
  }
  
  @SuppressWarnings("unchecked")
  private boolean indexOntologies() {
    IndexWriter myWriter;
    try {
      myWriter = new IndexWriter(ApplicationProperties.getApplicationDirectory().getAbsolutePath() + "/luceneIndex",
                                             new StandardAnalyzer(),
                                             true);
    } catch (IOException e) {
      Log.getLogger().warning("Could not index ontologies because of I/O Error" + e);
      return false;
    }
    Collection<NarrowFrameStore> dataSources = getDataSources();
    if (dataSources == null) {
      Log.getLogger().warning("Could not index ontologies - phonetic search will fail");
      return false;
    }
    for (NarrowFrameStore nfs : dataSources) {
      for (Frame frame : nfs.getFrames()) {
        for (Slot slot : getSearchableSlots()) {
          String content = getValueString(nfs, frame, slot);
          if (content == null) {
            continue;
          }
          Document doc = new Document();
          doc.add(new Field(FRAME_FIELD, "" + frame.getFrameID().getLocalPart(), 
                            Field.Store.YES, Field.Index.UN_TOKENIZED));
          doc.add(new Field(SLOT_FIELD, "" + slot.getFrameID().getLocalPart(),
                            Field.Store.YES, Field.Index.UN_TOKENIZED));
          doc.add(new Field(CONTENTS_FIELD, content, Field.Store.YES, Field.Index.TOKENIZED));
          try {
            writer.addDocument(doc);
          } catch (IOException e) {
            Log.getLogger().warning("Could not add frame slot value to searchable indicies - search will fail");
            return false;
          }
        }
      }
    }
    synchronized (this) {
      writer = myWriter;
    }
    return true;
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
  
  
  private Set<Slot> getSearchableSlots() {
    Slot directSubClsesSlot   = (Slot) delegate.getFrame(Model.SlotID.DIRECT_SUBCLASSES);
    Slot directInstancesSlot  = (Slot) delegate.getFrame(Model.SlotID.DIRECT_INSTANCES);
    Set<Slot> searchableSlots = new HashSet<Slot>();
    for (Cls parentSlot : parentSlots) {
      for (Object slotCls : delegate.getClosure(parentSlot, directSubClsesSlot, null, false)) {
        if (slotCls instanceof Cls) {
          searchableSlots.addAll(delegate.getClosure((Cls) slotCls, directInstancesSlot, null, false));
        }
      }
    }
    return searchableSlots;
  }

  /*---------------------------------------------------------------------
   *  Common Narrow Frame Store Functions
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
    delegate.addValues(frame, slot, facet, isTemplate, values);
  }

  public void moveValue(Frame frame, Slot slot, Facet facet,
                        boolean isTemplate, int from, int to) {
    delegate.moveValue(frame, slot, facet, isTemplate, from, to);
  }

  public void removeValue(Frame frame, Slot slot, Facet facet,
                          boolean isTemplate, Object value) {
    delegate.removeValue(frame, slot, facet, isTemplate, value);
  }

  public void setValues(Frame frame, Slot slot, Facet facet,
                        boolean isTemplate, Collection values) {
    delegate.setValues(frame, slot, facet, isTemplate, values);
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

  public Set<Frame> executeQuery(Query query) {
    if (query instanceof PhoneticQuery) {
      
    }
    return delegate.executeQuery(query);
  }

  public void deleteFrame(Frame frame) {
    delegate.deleteFrame(frame);
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
