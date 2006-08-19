package edu.stanford.smi.protege.query.indexing;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

public interface IndexOperation {
  
  public void apply(IndexReader reader, IndexWriter writer);

}
