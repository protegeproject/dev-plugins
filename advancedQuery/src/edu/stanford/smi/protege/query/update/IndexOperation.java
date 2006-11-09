package edu.stanford.smi.protege.query.update;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class IndexOperation<V> implements Future<V> {
  private boolean cancelled = false;
  private boolean done      = false;
  private V result;
  private Throwable t;

  public synchronized boolean cancel(boolean mayInterruptIfRunning) {
    if (done) {
      return false;
    }
    cancelled = true;
    done = true;
    return true;
  }

  public synchronized V get() throws InterruptedException, ExecutionException {

  }

  public V get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
    while (!done) {
      this.wait();
    }
    if ()
    if (result != null)
    // TODO Auto-generated method stub
    return null;
  }

  public synchronized boolean isCancelled() {
    return cancelled;
  }

  public boolean isDone() {
    return done;
  }
  
  public synchronized void set(V result) {
    this.result = result;
    done = true;
  }
  
  public synchronized void setException(Throwable t) {
    this.t = t;
    done = true;
  }
  

}
