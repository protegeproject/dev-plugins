package edu.stanford.smi.protege.query;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.logging.Level;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.util.Log;

public class QueryNarrowFrameStoreHandler implements InvocationHandler {
  private static Method setNameMethod;
  private static Method getNameMethod;
  private static Method getDelegateMethod;
  private static Method executeQueryMethod;

  private NarrowFrameStore delegate;
  private String name;
  
  public QueryNarrowFrameStoreHandler(NarrowFrameStore delegate) {
    if (setNameMethod == null) {
      Class cls = NarrowFrameStore.class;
      try { // not in static block so exception won't get thrown unless invoked.
        setNameMethod = cls.getMethod("setName", new Class[] {String.class});
        getNameMethod = cls.getMethod("getName", new Class[] {});
        getDelegateMethod = cls.getMethod("getDelegate", new Class[] {});
        executeQueryMethod = cls.getMethod("executeQuery", new Class[] { Query.class });
      } catch (NoSuchMethodException nsme) {
        Log.getLogger().log(Level.SEVERE, "Developer Error - method signatures need to be updated in " 
                               + QueryNarrowFrameStoreHandler.class);
      }
    }
    this.delegate = delegate;
  }

  public Object invoke(Object o, Method method, Object[] args)
      throws Throwable {
    if (method.equals(setNameMethod)) {
      name = (String) args[0];
      return null;
    } else if (method.equals(getNameMethod)) {
      return name;
    } else if (method.equals(getDelegateMethod)) {
      return delegate;
    } else if (method.equals(executeQueryMethod)) {
      return executeQuery((Query) args[0]);
    } else {
      return method.invoke(delegate, args);
    }
  }
  
  private Set<Frame> executeQuery(Query q) {
    Log.getLogger().info("Executing Query " + q);
    return null;
  }
  
  public NarrowFrameStore getNarrowFrameStore() {
    return (NarrowFrameStore) Proxy.newProxyInstance(NarrowFrameStore.class.getClassLoader(),
                                                     new Class[] {NarrowFrameStore.class},
                                                     this);
  }

}
