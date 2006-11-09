package edu.stanford.smi.protege.query.update;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class IndexOperation<V> implements Future<V>, Runnable {
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

  public V get() throws InterruptedException, ExecutionException {
    return get(0, TimeUnit.MILLISECONDS);
  }

  public synchronized V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
    long mstimeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
    while (!done) {
      if (timeout == 0) {
        this.wait();
      }
      else {
        this.wait(mstimeout);
      }
    }
    if (isCancelled()) {
      throw new CancellationException("Operation cancelled");
    }
    else if (t != null) {
      throw new ExecutionException(t);
    }
    return result;
  }

  public synchronized boolean isCancelled() {
    return cancelled;
  }

  public synchronized boolean isDone() {
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
