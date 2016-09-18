// For week 10
// sestoft@itu.dk * 2014-11-12, 2015-10-14

// Compile and run like this:
//   javac -cp ~/lib/multiverse-core-0.7.0.jar TestStmMap.java
//   java -cp ~/lib/multiverse-core-0.7.0.jar:. TestStmMap

// For the Multiverse library:
import org.multiverse.api.LockMode;
import org.multiverse.api.Txn;
import org.multiverse.api.references.*;
import org.multiverse.api.StmUtils;
import org.multiverse.api.callables.TxnVoidCallable;
import org.multiverse.stms.gamma.transactionalobjects.GammaTxnRef;

import static org.multiverse.api.StmUtils.*;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntToDoubleFunction;

public class TestStmMap {
  public static void main(String[] args) {
    SystemInfo();
    testAllMaps(); 
    //exerciseAllMaps();
  }

  // TO BE HANDED OUT
  private static double exerciseMap(int threadCount, int perThread, int range, 
                                    final OurMap<Integer, String> map) {
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(new Runnable() { public void run() {
        Random random = new Random(37 * myThread + 78);
        for (int i=0; i<perThread; i++) {
          Integer key = random.nextInt(range);
          if (!map.containsKey(key)) {
            // Add key with probability 60%
            if (random.nextDouble() < 0.60) 
              map.put(key, Integer.toString(key));
          } 
          else // Remove key with probability 2% and reinsert
            if (random.nextDouble() < 0.02) {
              map.remove(key);
              map.putIfAbsent(key, Integer.toString(key));
            }
        }
        final AtomicInteger ai = new AtomicInteger();
        map.forEach(new Consumer<Integer,String>() { 
            public void accept(Integer k, String v) {
              ai.getAndIncrement();
        }});
        // System.out.println(ai.intValue() + " " + map.size());
      }});
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    // map.reallocateBuckets();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
    return map.size();
  }

  private static void exerciseAllMaps() {
    final int bucketCount = 100_000, threadCount = 16;
    final int iterations = 1_600_000, perThread = iterations / threadCount;
    final int range = 100_000;
    System.out.println(Mark7(String.format("%-21s %d", "StmMap", threadCount),
          i -> exerciseMap(threadCount, perThread, range,
                           new StmMap<Integer,String>(bucketCount))));
  }

  // Very basic sequential functional test of a hash map.  You must
  // run with assertions enabled for this to work, as in 
  //   java -ea TestStmMapSolution
  private static void testMap(final OurMap<Integer, String> map) {
	assert map.size() == 0;
    assert !map.containsKey(117);
    assert !map.containsKey(-2);
    assert map.get(117) == null;
    assert map.put(117, "A") == null;
    assert map.containsKey(117);
    assert map.get(117).equals("A");
    assert map.put(17, "B") == null;
    assert map.size() == 2;
    assert map.containsKey(17);
    assert map.get(117).equals("A");
    assert map.get(17).equals("B");
    assert map.put(117, "C").equals("A");
    assert map.containsKey(117);
    assert map.get(117).equals("C");
    assert map.size() == 2;
    map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));
    assert map.remove(117).equals("C");
    assert !map.containsKey(117);
    assert map.get(117) == null;
    assert map.size() == 1;
    assert map.putIfAbsent(17, "D").equals("B");
    assert map.get(17).equals("B");
    assert map.size() == 1;
    assert map.containsKey(17);
    assert map.putIfAbsent(217, "E") == null;
    assert map.get(217).equals("E");
    assert map.size() == 2;
    assert map.containsKey(217);
    assert map.putIfAbsent(34, "F") == null;
    map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));
    // map.reallocateBuckets();
    assert map.size() == 3;
    assert map.get(17).equals("B") && map.containsKey(17);
    assert map.get(217).equals("E") && map.containsKey(217);
    assert map.get(34).equals("F") && map.containsKey(34);
    map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));    
    // map.reallocateBuckets();
    assert map.size() == 3;
    assert map.get(17).equals("B") && map.containsKey(17);
    assert map.get(217).equals("E") && map.containsKey(217);
    assert map.get(34).equals("F") && map.containsKey(34);
    map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));  
  }

  private static void testAllMaps() {
    testMap(new StmMap<Integer,String>(25));
  }

  // --- Benchmarking infrastructure ---

  // NB: Modified to show microseconds instead of nanoseconds

  public static double Mark7(String msg, IntToDoubleFunction f) {
    int n = 10, count = 1, totalCount = 0;
    double dummy = 0.0, runningTime = 0.0, st = 0.0, sst = 0.0;
    do { 
      count *= 2;
      st = sst = 0.0;
      for (int j=0; j<n; j++) {
        Timer t = new Timer();
        for (int i=0; i<count; i++) 
          dummy += f.applyAsDouble(i);
        runningTime = t.check();
        double time = runningTime * 1e6 / count; // microseconds
        st += time; 
        sst += time * time;
        totalCount += count;
      }
    } while (runningTime < 0.25 && count < Integer.MAX_VALUE/2);
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%-25s %15.1f us %10.2f %10d%n", msg, mean, sdev, count);
    return dummy / totalCount;
  }

  public static void SystemInfo() {
    System.out.printf("# OS:   %s; %s; %s%n", 
                      System.getProperty("os.name"), 
                      System.getProperty("os.version"), 
                      System.getProperty("os.arch"));
    System.out.printf("# JVM:  %s; %s%n", 
                      System.getProperty("java.vendor"), 
                      System.getProperty("java.version"));
    // The processor identifier works only on MS Windows:
    System.out.printf("# CPU:  %s; %d \"cores\"%n", 
                      System.getenv("PROCESSOR_IDENTIFIER"),
                      Runtime.getRuntime().availableProcessors());
    java.util.Date now = new java.util.Date();
    System.out.printf("# Date: %s%n", 
      new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(now));
  }
}

// Crude wall clock timing utility, measuring time in seconds
   
class Timer {
  private long start, spent = 0;
  public Timer() { play(); }
  public double check() { return (System.nanoTime()-start+spent)/1e9; }
  public void pause() { spent += System.nanoTime()-start; }
  public void play() { start = System.nanoTime(); }
}

interface Consumer<K,V> {
  void accept(K k, V v);
}

interface OurMap<K,V> {
  boolean containsKey(K k);
  V get(K k);
  V put(K k, V v);
  V putIfAbsent(K k, V v);
  V remove(K k);
  int size();
  void forEach(Consumer<K,V> consumer);
  // void reallocateBuckets();
}

// ----------------------------------------------------------------------
// A hash map that permits thread-safe concurrent operations, based on
// software transactional memory.

class StmMap<K,V> implements OurMap<K,V> {
  private final TxnRef<TxnRef<ItemNode<K,V>>[]> buckets;
  private final TxnInteger cachedSize;  

  public StmMap(int bucketCount) {
    final TxnRef<ItemNode<K,V>>[] buckets = makeBuckets(bucketCount);
    this.buckets = StmUtils.<TxnRef<ItemNode<K,V>>[]>newTxnRef(buckets);
    this.cachedSize = newTxnInteger();
  }

  @SuppressWarnings("unchecked") 
  private static <K,V> TxnRef<ItemNode<K,V>>[] makeBuckets(int size) {
    // Java's @$#@?!! type system requires "unsafe" cast here:
    final TxnRef<ItemNode<K,V>>[] buckets = (TxnRef<ItemNode<K,V>>[])new TxnRef[size];
    for (int hash=0; hash<buckets.length; hash++)
      buckets[hash] = StmUtils.<ItemNode<K,V>>newTxnRef();
    return buckets;
  }

  // Protect against poor hash functions and make non-negative
  private static <K> int getHash(K k) {
    final int kh = k.hashCode();
    return (kh ^ (kh >>> 16)) & 0x7FFFFFFF;  
  }

  // Return true if key k is in map, else false
  public boolean containsKey(K k) {
    return atomic(() -> { 
        final TxnRef<ItemNode<K,V>>[] bs = buckets.get();
        final int h = getHash(k), hash = h % bs.length; 
        boolean keyPresent = ItemNode.search(bs[hash].get(), k, null);
        return keyPresent;
      });
  }
  //10.3.1
  // Return value v associated with key k, or null
  public V get(K k) {
	  return atomic(() -> { 
	    final TxnRef<ItemNode<K,V>>[] bs = buckets.get();  //local reference to 'old' buckets, not influenced by concurrent reallocate, that might update bucket ref 
	    final int h = getHash(k), hash = h % bs.length; 
	    Holder<V> holder = new Holder<V>();
	    if(ItemNode.search(bs[hash].get(), k, holder)) {
	      	return holder.get();
	    }
	    else {
	    	return null;
	    }
	 });
  }

  public int size() {
	//System.out.println("SIZE IS NOW: " + cachedSize.atomicGet());  
	return cachedSize.atomicGet();
  }

  // Put v at key k, or update if already present.  
  //NOT UPDATING SIZE!!!
  public V put(K k, V v) {
	return atomic(() -> {
		 final TxnRef<ItemNode<K,V>>[] bs = buckets.get();
		 final int h = getHash(k), hash = h % bs.length;
		 final Holder<V> old = new Holder<V>();
		 final ItemNode<K,V> node = bs[hash].get(); //get start of relevant bucket /linked list
		 
		 ItemNode<K,V> deleted = ItemNode.delete(node, k, old); //find out where to attach node
		 ItemNode<K,V> newNode = new ItemNode<K,V>(k, v, deleted); 
		 bs[hash].set(newNode);
		 if(deleted == node) { //linked list was not altered 
			 cachedSize.increment();
			 System.out.println("PUT WITH NEW KEY = INCREMENT");
		 }
		 else {
			 System.out.println("PUT WITH OLD KEY = NO INCREMENT" );
		 }
		 return old.get();	      
	});   
  }

  // Put v at key k only if absent.  
  public V putIfAbsent(K k, V v) {
	  return atomic(() -> {
		 final TxnRef<ItemNode<K,V>>[] bs = buckets.get();
		 final int h = getHash(k), hash = h % bs.length;
		 final Holder<V> old = new Holder<V>();
		 final ItemNode<K,V> node = bs[hash].get(); //get start of relevant bucket /linked list
		 boolean found = ItemNode.search(node, k, old);
		 if(!found) {
			 ItemNode<K,V> deleted = ItemNode.delete(node, k, old); //find out where to attach node
			 ItemNode<K,V> newNode = new ItemNode<K,V>(k, v, deleted); 
			 bs[hash].set(newNode); 
			 System.out.println("PUT_IF_ABSENT METHOD: New node added: Key = " + newNode.getKeyValue() + ". Value = " + newNode.getValue());
			 cachedSize.increment();
			 System.out.println("FRA PUT_IF_ABSENT: CACHED SIZE ER NU" + cachedSize.get());
		 }
		 else {
			 System.out.println("PUT_IF_ABSENT: NODE WITH KEY= " + k + " AND VALUE= " + v + " NOT ADDED. KEY ALREADY EXISTS!!!");
		 }
		 return old.get();	      
	 });   
  }

  // Remove and return the value at key k if any, else return null
  //NOT UPDATING SIZE!!
  public V remove(K k) {
	  return atomic(() -> {
		  final TxnRef<ItemNode<K,V>>[] bs = buckets.get();
		  final int h = getHash(k), hash = h % bs.length;
		  Holder<V> old = new Holder<V>();
		  final ItemNode<K,V> node = bs[hash].get(); //get start of relevant bucket /linked list
		  ItemNode<K,V> deleted = ItemNode.delete(node, k, old);
		  //System.out.println("DELETED ER AF TYPE: " + deleted.getClass());
		  //System.out.println("BS[HASH] ER AF TYPE: " + bs[hash]);
		  if (deleted != node) {
			  bs[hash].set(deleted);
			  System.out.println("REMOVE METHOD: Node deleted: Key = " + deleted.getKeyValue() + ". Value = " + deleted.getValue());
			  cachedSize.decrement();
		  }
		  return old.get();
	});  
  }

  // Iterate over the hashmap's entries one bucket at a time.  Since a
  // reallocate does not affect the old buckets table, and item node
  // lists are immutable, only visibility is needed, no transactions.
  // This is good, because calling a consumer inside an atomic seems
  // suspicious.
  public void forEach(Consumer<K,V> consumer) {
	  System.out.println("PRINT LISTEN....");
	  final TxnRef<ItemNode<K,V>>[] bs = atomic(() -> buckets.get()); //peger på txnRef i buckets
	  for (int i=0; i < bs.length; i++) {
		  final int j = i; 
		  ItemNode<K,V> node = atomic(() -> bs[j].get());
		  while (node != null) {
			consumer.accept(node.k, node.v);
			if(node.next != null) {
				System.out.println("NODE" + node.k + " -> NEXT NODE: KEY=" + node.next.getKeyValue() + " - VALUE=" + node.next.getValue());
			}
			else {
				System.out.println("NODE" + node.k + " -> NEXT NODE: NULL");
			}
			
			node = node.next;
			
	  }
  }	  
}
  // public void reallocateBuckets() { 
  //   throw new RuntimeException("Not implemented");
  // }
  

  
  static class ItemNode<K,V> { //HUSK: ItemNode is immutable!!! 
    private final K k;
    private final V v;
    private final ItemNode<K,V> next;
    
    public ItemNode(K k, V v, ItemNode<K,V> next) {
      this.k = k;
      this.v = v;
      this.next = next;
    }
    
    public K getKeyValue() {
    	return k;
    }
    
    public V getValue() {
    	return v;
    }
    
    public void printYourself() {
    	System.out.println("KEY= " + this.k + " - VALUE=" + this.v + " - >");
    	if(this.next != null) {
    		next.printYourself();
    	}
    	
    }

    // These work on immutable data only, no synchronization needed.

    public static <K,V> boolean search(ItemNode<K,V> node, K k, Holder<V> old) {
      while (node != null) //if node is there 
        if (k.equals(node.k)) { //if node is sought for node
          if (old != null) //if old is not null 
            old.set(node.v); //set old to nodes value
          return true; //return true = found 
        } else //if node is not sought for
          node = node.next; //run on nodes next neighbour
      return false;
    }
    
    
    public static <K,V> ItemNode<K,V> delete(ItemNode<K,V> node, K k, Holder<V> old) {
      if (node == null) //this bucket / linked list is empty or we reached end of linked list
        return null; //return null for empty list or sought node not found
      else if (k.equals(node.k)) { //node is sought for node
     	  old.set(node.v); //set holder to nodes value
        return node.next; //return nodes next neighbour
      } else { //if node is there, but not sought for node
        final ItemNode<K,V> newNode = delete(node.next, k, old); //create newNode to hold result from recursive call to delete
        if (newNode == node.next) { //can both be null, end of linked list
        	
          return node; //return node?? 
        }  
        else
        	//System.out.println("RETURNING NEW NODE WITH REF TO NEWNODE ..." + k);
          return new ItemNode<K,V>(node.k, node.v, newNode);
      }
    }
  }
  
  // Object to hold a "by reference" parameter.  For use only on a
  // single thread, so no need for "volatile" or synchronization.

  static class Holder<V> {
    private V value;
    public V get() { 
      return value; 
    }
    public void set(V value) { 
      this.value = value;
    }
  }
}
