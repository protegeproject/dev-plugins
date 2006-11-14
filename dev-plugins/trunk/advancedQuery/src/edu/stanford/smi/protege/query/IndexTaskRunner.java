package edu.stanford.smi.protege.query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import edu.stanford.smi.protege.util.FutureTask;
import edu.stanford.smi.protege.util.Log;

public class IndexTaskRunner {
    private List<FutureTask> tasks = new ArrayList<FutureTask>();
    private Thread th;
    private Object lock = new Object();
    private Runnable cleanUpTask;
    
    public void setCleanUpTask(Runnable task) {
        cleanUpTask = task;
    }

    public void addTask(FutureTask task) {
        synchronized (lock) {
            tasks.add(task);
            lock.notifyAll();
        }
    }
    
    public void addTaskFirst(FutureTask task) {
        synchronized (lock) {
            tasks.add(0, task);
            lock.notifyAll();
        }
    }
    
    public void restartQueue(FutureTask task) {
        synchronized (lock) {
            for (FutureTask cancelledTask : tasks) {
                cancelledTask.cancel(false);
            }
            tasks.clear();
            if (task != null) {
                tasks.add(task);
                lock.notifyAll();
            }
        }
    }
    
    public boolean isEmpty() {
        synchronized (lock) {
            return tasks.isEmpty();
        }
    }
    
    public void dispose() {
        restartQueue(null);
        th = null;
    }

    public void startBackgroundThread() {
        if (th != null) {
            return;
        }
        th = new Thread(new Runnable() {
            @SuppressWarnings("unchecked")
            public void run() {
                while (true) {
                    if (th != Thread.currentThread()) return;
                    FutureTask task;
                    synchronized (lock) {
                        while (tasks.isEmpty()) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                Log.getLogger().log(Level.WARNING, "Unexpected Interrupt", e);
                            }
                        }
                        task = tasks.remove(0);
                    }
                    if (task.preRun()) {
                        try {
                            task.run();
                        } catch (Throwable t) {
                            task.setException(t);
                        } finally {
                            if (!task.isDone()) {
                                task.set(null);
                            }
                        }
                    }
                    boolean finishUp;
                    synchronized (lock) {
                        finishUp = tasks.isEmpty() && cleanUpTask != null;
                    }
                    if (finishUp) {
                        cleanUpTask.run();
                        cleanUpTask = null;
                    }
                }
            }
        }, "Lucene Reindexing thread");
        th.start();
    }
}
