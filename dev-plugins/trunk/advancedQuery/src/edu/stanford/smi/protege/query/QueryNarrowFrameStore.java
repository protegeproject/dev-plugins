package edu.stanford.smi.protege.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.smi.protege.model.Facet;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protege.model.Model;
import edu.stanford.smi.protege.model.Reference;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.MergingNarrowFrameStore;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.transaction.TransactionMonitor;

public class QueryNarrowFrameStore implements NarrowFrameStore {
  private NarrowFrameStore delegate;
  private String name;
  private Set<Slot> parentSlots = new HashSet<Slot>();

  
  public QueryNarrowFrameStore(NarrowFrameStore delegate) {
    this.delegate = delegate;
    
  }
  
  @SuppressWarnings("unchecked")
  private void indexOntologies() {
    Slot directSubSlots = (Slot) delegate.getFrame(Model.SlotID.DIRECT_SUBSLOTS);
    Set<Slot> slots = new HashSet<Slot>();
    for (Slot parent : parentSlots) {
      slots.addAll((Set<Slot>) delegate.getClosure(parent, directSubSlots, null, false));
    }
    Collection<NarrowFrameStore> dataSources = getDataSources();
    if (dataSources == null) {
      Log.getLogger().warning("Could not index ontologies - phonetic search will fail");
    }
    for (NarrowFrameStore nfs : dataSources) {
      for (Frame frame : nfs.getFrames()) {
        for (Slot slot : parentSlots) {
          
        }
      }
    }
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
