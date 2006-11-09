package edu.stanford.smi.protege.query.update;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import edu.stanford.smi.protege.util.Log;

public class IndexOperationRunner implements Runnable {
  private Object lock = new Object();
  private List<IndexOperation> operations = new ArrayList<IndexOperation>();

  public void run() {
    while (true) {
      IndexOperation operation;
      synchronized (lock) {
        while (operations.isEmpty()) {
          try {
            lock.wait();
          } catch (InterruptedException e) {
            Log.getLogger().log(Level.WARNING, "Exception caught waiting to update lucene index");
          }
        }
        operation = operations.remove(0);
      }
      operation.execute();
    }
    
  }
  
  public void add(IndexOperation update) {
    synchronized (lock) {
      operations.add(update);
      lock.notifyAll();
    }
  }
  
  public void addFirst(IndexOperation operation) {
    synchronized (lock) {
      operations.add(0, operation);
      lock.notifyAll();
    }
  }
  
  public void flushAndReplace(IndexOperation operation) {
    synchronized (lock) {
      for (IndexOperation op : operations) {
        op.flush();
      }
      operations = new ArrayList<IndexOperation>();
      operations.add(operation);
      lock.notifyAll();
    }
  }
  
  public void waitForOperationToComplete(IndexOperation operation) {
    synchronized (lock) {
      while (operations.contains(operation)) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
          Log.getLogger().log(Level.WARNING, "Exception wating", e);
        }
      }
    }
  }

}
