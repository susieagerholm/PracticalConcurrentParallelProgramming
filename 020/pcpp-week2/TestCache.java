// For week 2

// Code from Goetz et al 5.6, written by Brian Goetz and Tim Peierls.
// Modifications by sestoft@itu.dk * 2014-09-08

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.*;
import java.util.function.Function;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;


public class TestCache {
  public static void main(String[] args) throws InterruptedException {
    Computable<Long, long[]> factorizer = new Factorizer(),
    cachingFactorizer = new Memoizer5<Long,long[]>(factorizer);
    // cachingFactorizer = factorizer;
    
    //2.4.1
    exerciseFactorizer(factorizer);
    Factorizer myFact = (Factorizer)factorizer;
    System.out.println("Calls to myFact: " + myFact.getCount());
    
    
    //2.4.2 - 2.4.6
    /*Factorizer f = new Factorizer(); 
    exerciseFactorizer(new Memoizer3<Long,long[]>(f)); 
    System.out.println(f.getCount());*/

    
    /*long p = 71827636563813227L;

    print(factorizer.compute(p));
    
    long[] factors = cachingFactorizer.compute(p);
    print(factors);

    print(cachingFactorizer.compute(p));
    print(cachingFactorizer.compute(p));
    print(cachingFactorizer.compute(p));
    print(cachingFactorizer.compute(p));
    print(cachingFactorizer.compute(p));
    print(cachingFactorizer.compute(p));
    print(cachingFactorizer.compute(p)); */
  }

  private static void print(long[] arr) {
    for (long x : arr) 
      System.out.print(" " + x);
    System.out.println();
  }
  
 private static void printNow(long tid) {
    String now = new SimpleDateFormat("h:m:s:ms").format(new Timestamp(tid));
    System.out.println(now);
 } 
  
  
 private static void exerciseFactorizer(Computable<Long, long[]> f) { 
    long start_test =  System.currentTimeMillis();
    printNow(start_test);
    final int threadCount = 16; 
    final long start = 10_000_000_000L, range = 20_000L; 
    System.out.println("Aktuelle impl: " + f.getClass()); 
    
    final long from1 =  start, to1 = start + range - 1;
    
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      
      final long from2 = start+range+t*range/4; 
      final long to2 = from2+range; 
      threads[t] = new Thread(() -> { 
        try {
          for (long i=from1; i<to1; i++) {
            f.compute(i);
       }
          for (long i=from2; i<to2; i++) {
            f.compute(i);
       }
        } catch (Exception e) {
          e.printStackTrace();
        }
     
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
	threads[t].join();
    } catch (InterruptedException exn) { exn.printStackTrace(); }
    
    long stop_test = System.currentTimeMillis();
    printNow(stop_test);
    long dur = stop_test - start_test;
    System.out.println("approx dur in ms: " + dur);

    
    
  }   
  
   
}


// Interface that represents a function from A to V
interface Computable <A, V> {
  V compute(A arg) throws InterruptedException;
}


// Prime factorization is a function from Long to long[]
class Factorizer implements Computable<Long, long[]> {
  // For statistics only, count number of calls to factorizer:
  private final AtomicLong count = new AtomicLong(0);
  public long getCount() { return count.longValue(); }

  public long[] compute(Long wrappedP) {
    count.getAndIncrement();
    //System.out.println("We got here..." + getCount());
    long p = wrappedP;
    ArrayList<Long> factors = new ArrayList<Long>();
    long k = 2;
    while (p >= k * k) {
      if (p % k == 0) {
	factors.add(k);
	p /= k;
      } else 
	k++;
    }
    // Now k * k > p and no number in 2..k divides p
    factors.add(p);
    long[] result = new long[factors.size()];
    for (int i=0; i<result.length; i++) 
      result[i] = factors.get(i);
      //System.out.println("Content in result array?..." + result.length);
    return result;
  }
}


/**
 * Initial cache attempt using HashMap and synchronization; 
 * suffers from lack of parallelism due to coarse locking.  
 * From Goetz p. 103
 * @author Brian Goetz and Tim Peierls
 */
class Memoizer1 <A, V> implements Computable<A, V> {
  private final Map<A, V> cache = new HashMap<A, V>();
  private final Computable<A, V> c;
  
  public Memoizer1(Computable<A, V> c) { this.c = c; }

  public synchronized V compute(A arg) throws InterruptedException {
    V result = cache.get(arg);
    if (result == null) {
      result = c.compute(arg);
      cache.put(arg, result);
    }
    return result;
  }
}


/**
 * Memoizer2
 * Replacing HashMap with ConcurrentHashMap for better parallelism.
 * From Goetz p. 105
 * @author Brian Goetz and Tim Peierls
 */
class Memoizer2 <A, V> implements Computable<A, V> {
  private final Map<A, V> cache = new ConcurrentHashMap<A, V>();
  private final Computable<A, V> c;
  
  public Memoizer2(Computable<A, V> c) { this.c = c; }
  
  public V compute(A arg) throws InterruptedException {
    V result = cache.get(arg);
    if (result == null) {
      result = c.compute(arg);
      cache.put(arg, result);
    }
    return result;
  }
}


/**
 * Memoizer3 
 * Create a Future and register in cache immediately.
 * Calls: ft.run() -> eval.call() -> c.compute(arg)
 * From Goetz p. 106
 * @author Brian Goetz and Tim Peierls
 */
class Memoizer3<A, V> implements Computable<A, V> {
  private final Map<A, Future<V>> cache 
    = new ConcurrentHashMap<A, Future<V>>();
  private final Computable<A, V> c;
  
  public Memoizer3(Computable<A, V> c) { this.c = c; }
  
  public V compute(final A arg) throws InterruptedException {
    Future<V> f = cache.get(arg);
    if (f == null) {
      Callable<V> eval = new Callable<V>() {
	  public V call() throws InterruptedException {
	    return c.compute(arg);
      }};
      FutureTask<V> ft = new FutureTask<V>(eval);
      cache.put(arg, ft);
      f = ft;
      ft.run();
    }
    try { return f.get(); } 
    catch (ExecutionException e) { throw launderThrowable(e.getCause()); }  
  }

  public static RuntimeException launderThrowable(Throwable t) {
    if (t instanceof RuntimeException)
      return (RuntimeException) t;
    else if (t instanceof Error)
      throw (Error) t;
    else
      throw new IllegalStateException("Not unchecked", t);
  }
}


/**
 * Memoizer4, hybrid of variant of Goetz's Memoizer3 and Memoizer.  If
 * arg not in cache, create Future, then atomically putIfAbsent in
 * cache, then run on calling thread.
 */

class Memoizer4<A, V> implements Computable<A, V> {
  private final Map<A, Future<V>> cache 
    = new ConcurrentHashMap<A, Future<V>>();
  private final Computable<A, V> c;
  
  public Memoizer4(Computable<A, V> c) { this.c = c; }
  
  public V compute(final A arg) throws InterruptedException {
    Future<V> f = cache.get(arg);
    if (f == null) {
      Callable<V> eval = new Callable<V>() {
	  public V call() throws InterruptedException {
	    return c.compute(arg);
      }};
      FutureTask<V> ft = new FutureTask<V>(eval);
      f = cache.putIfAbsent(arg, ft);
      if (f == null) { 
	f = ft; 
	ft.run();
      }
    }
    try { return f.get(); } 
    catch (ExecutionException e) { throw launderThrowable(e.getCause()); }  
  }

  public static RuntimeException launderThrowable(Throwable t) {
    if (t instanceof RuntimeException)
      return (RuntimeException) t;
    else if (t instanceof Error)
      throw (Error) t;
    else
      throw new IllegalStateException("Not unchecked", t);
  }
}

/**
 * Memoizer5, modern variant of Memoizer4 using the new Java 8
 * computeIfAbsent.  Atomically test whether arg is in cache and if
 * not create Future and put it there, then run on calling thread.
 */

class Memoizer5<A, V> implements Computable<A, V> {
  private final Map<A, Future<V>> cache 
    = new ConcurrentHashMap<A, Future<V>>();
  private final Computable<A, V> c;
  
  public Memoizer5(Computable<A, V> c) { this.c = c; }
  
  public V compute(final A arg) throws InterruptedException {
    // AtomicReference is used as a simple assignable holder; no atomicity needed
    final AtomicReference<FutureTask<V>> ftr = new AtomicReference<FutureTask<V>>();
    Future<V> f = cache.computeIfAbsent(arg, new Function<A,Future<V>>() { 
	public Future<V> apply(final A arg) {
	  Callable<V> eval = new Callable<V>() {
	      public V call() throws InterruptedException {
		return c.compute(arg);
	      }};
	  ftr.set(new FutureTask<V>(eval));
	  return ftr.get();
	}});
    // Important to run() the future outside the computeIfAbsent():
    if (ftr.get() != null) 
      ftr.get().run();
    try { return f.get(); } 
    catch (ExecutionException e) { throw launderThrowable(e.getCause()); }  
  }

  public static RuntimeException launderThrowable(Throwable t) {
    if (t instanceof RuntimeException)
      return (RuntimeException) t;
    else if (t instanceof Error)
      throw (Error) t;
    else
      throw new IllegalStateException("Not unchecked", t);
  }
}


/**
 * Final implementation of Memoizer using cheap get() followed by
 * atomic putIfAbsent.
 * From Goetz p. 108
 * @author Brian Goetz and Tim Peierls
 */
class Memoizer <A, V> implements Computable<A, V> {
  private final ConcurrentMap<A, Future<V>> cache
    = new ConcurrentHashMap<A, Future<V>>();
  private final Computable<A, V> c;
  
  public Memoizer(Computable<A, V> c) { this.c = c; }

  public V compute(final A arg) throws InterruptedException {
    while (true) {
      Future<V> f = cache.get(arg);
      if (f == null) {
	Callable<V> eval = new Callable<V>() {
	    public V call() throws InterruptedException {
	      return c.compute(arg);
	    }
	  };
	FutureTask<V> ft = new FutureTask<V>(eval);
	f = cache.putIfAbsent(arg, ft);
	if (f == null) {
	  f = ft;
	  ft.run();
	}
      }
      try {
	return f.get();
      } catch (CancellationException e) {
	cache.remove(arg, f);
      } catch (ExecutionException e) {
	throw launderThrowable(e.getCause());
      }
    }
  }


  /**
   * Coerce a checked Throwable to an unchecked RuntimeException.
   
   * sestoft@itu.dk 2014-09-07: This method converts a Throwable
   * (which is a checked exception) into a RuntimeException (which is
   * an unchecked exception) or an IllegalStateException (which is a
   * subclass of RuntimeException and hence unchecked).  It is unclear
   * why RuntimeException and Error are treated differently; both are
   * unchecked.  A simpler (but grosser) approach is to simply throw a
   * new RuntimeException(t), thus wrapping the Throwable, but that
   * may lead to a RuntimeException containing a RuntimeException
   * which is a little strange.  The original
   * java.util.concurrent.ExecutionException that wrapped the
   * Throwable is itself checked and therefore needs to be caught and
   * turned into something less obnoxious.

   * @author Brian Goetz and Tim Peierls 
   */

  public static RuntimeException launderThrowable(Throwable t) {
    if (t instanceof RuntimeException)
      return (RuntimeException) t;
    else if (t instanceof Error)
      throw (Error) t;
    else
      throw new IllegalStateException("Not unchecked", t);
  }
}
