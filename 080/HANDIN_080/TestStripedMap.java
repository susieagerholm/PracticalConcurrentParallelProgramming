// For week 6 -- four incomplete implementations of concurrent hash maps
// sestoft@itu.dk * 2014-10-07, 2015-09-25

// Parts of the code are missing.  Your task in the exercises is to
// write the missing parts.

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.IntToDoubleFunction;
import java.util.function.Predicate;

public class TestStripedMap {
  public static void main(String[] args) {
    //SystemInfo();
	//testAllMaps();    // Must be run with: java -ea TestStripedMap 
    //exerciseAllMaps();
    //timeAllMaps();
	functionalTestOfMap();   
  }

  private static void timeAllMaps() {
    final int bucketCount = 100_000, lockCount = 32;
    for (int t=1; t<=32; t++) {
      final int threadCount = t;
      Mark7(String.format("%-21s %d", "SynchronizedMap", threadCount),
            i -> timeMap(threadCount, 
                         new SynchronizedMap<Integer,String>(bucketCount)));
      Mark7(String.format("%-21s %d", "StripedMap", threadCount),
            i -> timeMap(threadCount, 
                         new StripedMap<Integer,String>(bucketCount, lockCount)));
      Mark7(String.format("%-21s %d", "StripedWriteMap", threadCount), 
            i -> timeMap(threadCount, 
                         new StripedWriteMap<Integer,String>(lockCount, lockCount)));
      Mark7(String.format("%-21s %d", "WrapConcHashMap", threadCount),
            i -> timeMap(threadCount, 
                         new WrapConcurrentHashMap<Integer,String>()));
    }
  }

  // TO BE HANDED OUT
  private static double timeMap(int threadCount, final OurMap<Integer, String> map) {
    final int iterations = 5_000_000, perThread = iterations / threadCount;
    final int range = 200_000;
    return exerciseMap(threadCount, perThread, range, map);
  }

  // TO BE HANDED OUT
  private static double exerciseMap(int threadCount, int perThread, int range, 
                                    final OurMap<Integer, String> map) {
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(() -> {
        Random random = new Random(37 * myThread + 78);
        for (int i=0; i<perThread; i++) {
          Integer key = random.nextInt(range);
		  final int kh = key.hashCode(); 
		  final int kk = (kh ^ (kh >>> 16)) & 0x7FFFFFFF;
		  final int moduloooo = kk % 32;
		  //System.out.println("min hash vaerdi: " + moduloooo);
          if (!map.containsKey(key)) {
            // Add key with probability 60%
            if (random.nextDouble() < 0.60) 
              map.put(key, Integer.toString(key));
			  	
          } 
          else // Remove key with probability 2% and reinsert
            if (random.nextDouble() < 0.02) {
              map.remove(key);
              //System.out.println( "exercising putifabsent on " + key);
			  map.putIfAbsent(key, Integer.toString(key));
			  
            }
        }
        final AtomicInteger ai = new AtomicInteger();
        map.forEach(new Consumer<Integer,String>() { 
            public void accept(Integer k, String v) {
              ai.getAndIncrement();
        }});
         //System.out.println(ai.intValue() + " is atomic counter - map size is " + map.size());
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    map.reallocateBuckets();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
	//System.out.println( "after allocate - map size is " + map.size());
    return map.size();
  }

  private static void exerciseAllMaps() {
    final int bucketCount = 100_000, lockCount = 32, threadCount = 16;
    final int iterations = 1_600_000, perThread = iterations / threadCount;
    final int range = 100_000;
    System.out.println(Mark7(String.format("%-21s %d", "SynchronizedMap", threadCount),
      i -> exerciseMap(threadCount, perThread, range,
                       new SynchronizedMap<Integer,String>(bucketCount))));
    System.out.println(Mark7(String.format("%-21s %d", "StripedMap", threadCount),
      i -> exerciseMap(threadCount, perThread, range,
                       new StripedMap<Integer,String>(bucketCount, lockCount))));
    System.out.println(Mark7(String.format("%-21s %d", "StripedWriteMap", threadCount), 
      i -> exerciseMap(threadCount, perThread, range,
                       new StripedWriteMap<Integer,String>(lockCount, lockCount))));
	System.out.println(Mark7(String.format("%-21s %d", "WrapConcHashMap", threadCount),
      i -> exerciseMap(threadCount, perThread, range,
                       new WrapConcurrentHashMap<Integer,String>())));
  }
  
  // Very basic sequential functional test of a hash map.  You must
  // run with assertions enabled for this to work, as in 
  //   java -ea TestStripedMap
  private static void testMap(final OurMap<Integer, String> map) {
    System.out.printf("%n%s%n", map.getClass());
    assert map.size() == 0;
    assert !map.containsKey(117);
    assert !map.containsKey(-2);
    assert map.get(117) == null;
    assert map.put(117, "A") == null;
    assert map.containsKey(117);
    assert map.get(117).equals("A");
    assert map.put(17, "B") == null;
    //map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));//sus
    assert map.size() == 2;
    assert map.containsKey(17);
    assert map.get(117).equals("A");
    assert map.get(17).equals("B");
    assert map.put(117, "C").equals("A");
    assert map.containsKey(117);
    assert map.get(117).equals("C");
    assert map.size() == 2;
    //map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));
    assert map.remove(117).equals("C"); //remove second item in bucket
    assert !map.containsKey(117);
    assert map.get(117) == null;
    assert map.size() == 1;
    assert map.putIfAbsent(17, "D").equals("B");
    assert map.get(17).equals("B");
    assert map.size() == 1;
    assert map.containsKey(17);
    assert map.putIfAbsent(217, "E") == null;
    assert map.get(217).equals("E");
    //map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));//sus
    assert map.size() == 2;
    assert map.containsKey(217);
    assert map.putIfAbsent(34, "F") == null;
    //map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));
    map.reallocateBuckets();
    assert map.size() == 3;
    assert map.get(17).equals("B") && map.containsKey(17);
    assert map.get(217).equals("E") && map.containsKey(217);
    assert map.get(34).equals("F") && map.containsKey(34);
    //map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));    
    map.reallocateBuckets();
    assert map.size() == 3;
    assert map.get(17).equals("B") && map.containsKey(17);
    assert map.get(217).equals("E") && map.containsKey(217);
    assert map.get(34).equals("F") && map.containsKey(34);
    //map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));  
    assert map.remove(111) == null; //try to remove from bucket that does not contain any items
    assert map.remove(217).equals("E"); //try to remove first item in bucket
    assert map.put(317, "Z") == null;
    map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));
    assert map.remove(417) == null; //try to remove item with unknown key from bucket with items
    
  }

  private static void testAllMaps() {
    testMap(new SynchronizedMap<Integer,String>(25));
    testMap(new StripedMap<Integer,String>(25, 5));
    testMap(new StripedWriteMap<Integer,String>(25, 5));
    testMap(new WrapConcurrentHashMap<Integer,String>());
  }

//EXERCISE 8.1.2
public static void functionalTestOfMap() {
	OurMap<Integer, String> test3 = new WrapConcurrentHashMap<Integer,String>();
	OurMap<Integer, String> test2 = new SynchronizedMap<Integer, String>(77);
	OurMap<Integer, String> test1 = new StripedMap<Integer, String>(77, 7);
	OurMap<Integer, String> test = new StripedWriteMap<Integer, String>(77, 7);
	//SET UNIT UNDER TEST
	OurMap<Integer, String> UUT = test; 
	
	
	final int threadCount = 16;
	ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
	final CyclicBarrier barrier = new CyclicBarrier(threadCount + 1);
	//AtomicInteger counter = new AtomicInteger(0);
	AtomicIntegerArray count = new AtomicIntegerArray(threadCount);
	
	for(int i = 0; i < threadCount; i++) {
		final int l = i;
		executorService.execute(new Runnable() {
		    public void run() {
		    	try {
		    		//Threads wait until all ready
		    		barrier.await();
			    	//Every thread has own instance of randomizer to avoid synchronizing access to random, influencing tests...
			    	Random rand = new Random();
			    	// Run test 1000 times
			    	for (int j = 0; j < 1000; j++) {
			    		 int key = rand.nextInt(100);
			    		 //add thread no as first value
			    		 String val = l + " : " + Integer.toString(key);
			    		 String put = UUT.put(key,val);
			    		 if (put == null) { //only count as extra if put is adding new key/val
			    			 count.getAndIncrement(l);
			    			// System.out.println("INCREMENTING FROM PUT" + l);
			    		 }
			    		 if (put != null) {
			    			 //if original put was made by other thread, remove count from this thread
			    			 String all[] = put.split("\\s+");
			    			 count.getAndDecrement(Integer.valueOf(all[0]));
			    			// System.out.println("DECREMENTING FROM PUT" + l);
			    			//increment thread making put
			    			 count.getAndIncrement(l);
			    			// System.out.println("INCREMENTING FROM PUT" + l);
			    		 }
			    		 int key2 = rand.nextInt(100);
			    		 String val2 = l + " : " + Integer.toString(key2);
			    		 String put_if_absent = UUT.putIfAbsent(key2,val2);  
			    		 //if put_if_abs is successful, increment counter for this thread
			    		 if (put_if_absent == null) {
			    			 count.getAndIncrement(l);
			    			 //System.out.println("INCREMENTING FROM PUT IF ABSENT" + l);
			    			 //no need to decrement from any thread
			    		 }
			    		 //Check if buckets contain...
			    		 Integer myKey = rand.nextInt(100); 
			    		 if(UUT.containsKey(myKey) == true) {
			    			//System.out.println("Value collected: " + UUT.get(myKey));
			    		 }
			    		 String remove = UUT.remove(rand.nextInt(100));  			    		 
				    	 //if remove has found item to remove
			    		 if(remove != null ) {
				    		//decrement counter for thread who signed this item 
				    		String all[] = remove.split("\\s+");
				    		count.getAndDecrement(Integer.valueOf(all[0]));
				    		//System.out.println("DECREMENTING FROM REMOVE" + l);
				    	 }
			    	 
			    	}
				    barrier.await();
				   
			    }
		    	catch (Exception e) {
		    		e.printStackTrace();
		    	}
		    	 
		    }
		});

	}
	
	
	try {
		barrier.await(); //await start
		barrier.await(); //wait for threads to finish
	} catch (InterruptedException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (BrokenBarrierException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} 
	
	//EXERCISE 8.1.4
	System.out.println("EXERCISE 8.1.4");
	UUT.forEach((k, v) -> {
		String[] res = v.split("\\s+");
		final int value = Integer.parseInt(res[0]);
		int lowerBound = 0, myboolean;
		
		if(lowerBound <= value && value <= threadCount) { myboolean = 1;}
		else {myboolean = 0;}
		
		switch (myboolean) {
			case 0:
				System.out.println("ENTRY HAS WRONG SIGNATURE " + value);
				break;
			case 1:
				System.out.println("ENTRY WITH APPROVED SIGNATURE : ALL GOOD!!"  + value);
		}	
		
	});
	
	//System.out.println("ALL OPERATIONS HAS CREDIBLE THREAD SIGNATURE ");
	
	//Exercise 8.1.5
	System.out.println("EXERCISE 8.1.5");
	System.out.println("SIZE OF MAP: " + UUT.size());
	
	//get totals from count array
	int total = 0;
	for (int i = 0; i < count.length(); i++) {
		total += count.get(i);
	}
	
	System.out.println("COUNT FROM THREADS: " + total);
	
	
	//EXTRA TEST:_ GET PRINT FROM ALL BUCKETS
	int[] totals = new int[threadCount];
	UUT.forEach((k,v) -> {
		
		System.out.println("Key: " + k + " - value: " + v);
		String[] res = v.split("\\s+");
		int value = Integer.parseInt(res[0]);
		int old = totals[value];
		totals[value] = old + 1;
	});
	
	//further check - all individual counts...
	for (int i = 0; i < totals.length; i++) {
		System.out.println("# in place: " + i + ": " + totals[i]);
	}
	
	executorService.shutdown();
	
	
}
    
  // --- Benchmarking infrastructure ---

  private static class Timer {
    private long start, spent = 0;
    public Timer() { play(); }
    public double check() { return (System.nanoTime()-start+spent)/1e9; }
    public void pause() { spent += System.nanoTime()-start; }
    public void play() { start = System.nanoTime(); }
  }

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
  void reallocateBuckets();
}

// ----------------------------------------------------------------------
// A hashmap that permits thread-safe concurrent operations, similar
// to a synchronized version of HashMap<K,V>.

class SynchronizedMap<K,V> implements OurMap<K,V>  {
  // Synchronization policy: 
  //   buckets[hash] and cachedSize are guarded by this
  private ItemNode<K,V>[] buckets;
  private int cachedSize;
  
  public SynchronizedMap(int bucketCount) {
    this.buckets = makeBuckets(bucketCount);
  }

  @SuppressWarnings("unchecked") 
  private static <K,V> ItemNode<K,V>[] makeBuckets(int size) {
    // Java's @$#@?!! type system requires this unsafe cast    
    return (ItemNode<K,V>[])new ItemNode[size];
  }

  // Protect against poor hash functions and make non-negative
  private static <K> int getHash(K k) {
    final int kh = k.hashCode();
    return (kh ^ (kh >>> 16)) & 0x7FFFFFFF;  
  }

  // Return true if key k is in map, else false
  public synchronized boolean containsKey(K k) {
    final int h = getHash(k), hash = h % buckets.length;
   // System.out.println("key is " + k + " - h is " + h + "- hash is " + hash);
    return ItemNode.search(buckets[hash], k) != null;
  }

  // Return value v associated with key k, or null
  public synchronized V get(K k) {
    final int h = getHash(k), hash = h % buckets.length;
    ItemNode<K,V> node = ItemNode.search(buckets[hash], k);
    if (node != null) 
      return node.v;
    else
      return null;
  }

  public synchronized int size() {
    return cachedSize;
  }

  // Put v at key k, or update if already present 
  public synchronized V put(K k, V v) {
    final int h = getHash(k), hash = h % buckets.length;
    //System.out.println("PUT: Putting key: " + k + " in bucket # " + hash + "with value " + v );
    ItemNode<K,V> node = ItemNode.search(buckets[hash], k);
    if (node != null) {
      V old = node.v;
      node.v = v;
      return old;
    } else {
      buckets[hash] = new ItemNode<K,V>(k, v, buckets[hash]);
      cachedSize++;
      return null;
    }
  }

  // Put v at key k only if absent
  public synchronized V putIfAbsent(K k, V v) {
    final int h = getHash(k), hash = h % buckets.length;
    ItemNode<K,V> node = ItemNode.search(buckets[hash], k);
    if (node != null)
      return node.v;
    else {
      buckets[hash] = new ItemNode<K,V>(k, v, buckets[hash]);
    //  System.out.println("PUT_IF_ABS: Putting key: " + k + " in bucket # " + hash + "with value " + v);
      cachedSize++;
      return null;
    }
  }

  // Remove and return the value at key k if any, else return null
  public synchronized V remove(K k) {
    final int h = getHash(k), hash = h % buckets.length;
    ItemNode<K,V> prev = buckets[hash];
    if (prev == null) //is bucket empty?
      return null;
    else if (k.equals(prev.k)) {        // Delete first ItemNode
      V old = prev.v;
      cachedSize--;
      buckets[hash] = prev.next;
      return old;
    } else {                            // Search later ItemNodes
      while (prev.next != null && !k.equals(prev.next.k))
        prev = prev.next;
      // Now prev.next == null || k.equals(prev.next.k)
      if (prev.next != null) {  // Delete ItemNode prev.next
        V old = prev.next.v;
        cachedSize--; 
        prev.next = prev.next.next;
        return old;
      } else
        return null;
    }
  }

  // Iterate over the hashmap's entries one bucket at a time
  public synchronized void forEach(Consumer<K,V> consumer) {
    for (int hash=0; hash<buckets.length; hash++) {
      ItemNode<K,V> node = buckets[hash];
      while (node != null) {
    	//System.out.println("Bucket: " + hash + " my key is " + node.k + " and val is" + node.v);  
    	if(node.next != null) {
    		//System.out.println("I point to node with key " + node.next.k + " and val " + node.next.v );
    	}
        consumer.accept(node.k, node.v);
        node = node.next;
      }
    }
  }

  // Double bucket table size, rehash, and redistribute entries.

  public synchronized void reallocateBuckets() {
    final ItemNode<K,V>[] newBuckets = makeBuckets(2 * buckets.length);
    for (int hash=0; hash<buckets.length; hash++) {
      ItemNode<K,V> node = buckets[hash];
      while (node != null) {
        final int newHash = getHash(node.k) % newBuckets.length;
        ItemNode<K,V> next = node.next;
        node.next = newBuckets[newHash];
        newBuckets[newHash] = node;
        node = next;
      }
    }
    buckets = newBuckets;
  }

  static class ItemNode<K,V> {
    private final K k;
    private V v;
    private ItemNode<K,V> next;
    
    public ItemNode(K k, V v, ItemNode<K,V> next) {
      this.k = k;
      this.v = v;
      this.next = next;
    }

    public static <K,V> ItemNode<K,V> search(ItemNode<K,V> node, K k) {
      while (node != null && !k.equals(node.k))
        node = node.next;
      return node;
    }
  }
}

// ----------------------------------------------------------------------
// A hash map that permits thread-safe concurrent operations, using
// lock striping (intrinsic locks on Objects created for the purpose).

// NOT IMPLEMENTED: get, putIfAbsent, size, remove and forEach.

// The bucketCount must be a multiple of the number lockCount of
// stripes, so that h % lockCount == (h % bucketCount) % lockCount and
// so that h % lockCount is invariant under doubling the number of
// buckets in method reallocateBuckets.  Otherwise there is a risk of
// locking a stripe, only to have the relevant entry moved to a
// different stripe by an intervening call to reallocateBuckets.

class StripedMap<K,V> implements OurMap<K,V> {
  // Synchronization policy: 
  //   buckets[hash] is guarded by locks[hash%lockCount]
  //   sizes[stripe] is guarded by locks[stripe]
  private volatile ItemNode<K,V>[] buckets;
  private final int lockCount;
  private final Object[] locks;
  private final int[] sizes;

  public StripedMap(int bucketCount, int lockCount) {
    if (bucketCount % lockCount != 0)
      throw new RuntimeException("bucket count must be a multiple of stripe count");
    this.lockCount = lockCount;
    this.buckets = makeBuckets(bucketCount);
    this.locks = new Object[lockCount];
    this.sizes = new int[lockCount];
    for (int stripe=0; stripe<lockCount; stripe++) 
      this.locks[stripe] = new Object();
  }

  @SuppressWarnings("unchecked") 
  private static <K,V> ItemNode<K,V>[] makeBuckets(int size) {
    // Java's @$#@?!! type system requires this unsafe cast    
    return (ItemNode<K,V>[])new ItemNode[size];
  }

  // Protect against poor hash functions and make non-negative
  private static <K> int getHash(K k) {
    final int kh = k.hashCode();
    return (kh ^ (kh >>> 16)) & 0x7FFFFFFF;  
  }

  // Return true if key k is in map, else false
  public boolean containsKey(K k) {
    final int h = getHash(k), stripe = h % lockCount;
    synchronized (locks[stripe]) {
      final int hash = h % buckets.length;
      return ItemNode.search(buckets[hash], k) != null;
    }
  }

  // Return value v associated with key k, or null
  public V get(K k) {
    // TO DO: IMPLEMENT
	final int h = getHash(k), stripe = h % lockCount;
    synchronized (locks[stripe]) {
	   final int hash = h % buckets.length;
	   ItemNode<K, V> node = ItemNode.search(buckets[hash], k);
	   if (node != null) 
		return node.v;
	   else
		return null;
	}
	
  }

  public int size() {
	// TO DO: IMPLEMENT
	int result = 0;
	for (int stripe = 0; stripe < lockCount; stripe++) {
		synchronized (locks[stripe]) {
			result += sizes[stripe];
		}
	}	
	return result;
  }
  
  // Put v at key k, or update if already present 
  public V put(K k, V v) {
    final int h = getHash(k), stripe = h % lockCount;
    synchronized (this /*locks[stripe]*/) {
      final int hash = h % buckets.length;
      final ItemNode<K,V> node = ItemNode.search(buckets[hash], k);
      if (node != null) {
        V old = node.v;
        node.v = v;
        return old;
      } else {
        buckets[hash] = new ItemNode<K,V>(k, v, buckets[hash]);
        sizes[stripe]++;
        return null;
      }
    }
  }

  // Put v at key k only if absent
  public V putIfAbsent(K k, V v) {
    // TO DO: IMPLEMENT
	final int h = getHash(k), stripe = h % lockCount;
    synchronized (locks[stripe]) {
		final int hash = h % buckets.length;
	    final ItemNode<K,V> node = ItemNode.search(buckets[hash], k);
        if (node != null)
            return node.v;
        else {
            buckets[hash] = new ItemNode<K,V>(k, v, buckets[hash]);
			sizes[stripe]++;
            return null;
		}	
	}
  }

  // Remove and return the value at key k if any, else return null
  public V remove(K k) {
    // TO DO: IMPLEMENT
    final int h = getHash(k), hash = h % buckets.length, stripe = h % lockCount;
	synchronized (locks[stripe]) { 
		ItemNode<K,V> prev = buckets[hash];
		if (prev == null) return null;
		else if (k.equals(prev.k)) {        // Delete first ItemNode
			V old = prev.v;
			sizes[stripe]--;
			buckets[hash] = prev.next; //Set next to be the new first ItemNode
			return old;
		} 
		else {                            // Search later ItemNodes
			while (prev.next != null && !k.equals(prev.next.k))
			prev = prev.next;
			// Now prev.next == null || k.equals(prev.next.k)
			if (prev.next != null) {  // Delete ItemNode prev.next
				V old = prev.next.v;
				sizes[stripe]--;
				prev.next = prev.next.next;
				return old;
			} else
				return null;
			}
	}
  }

  // Iterate over the hashmap's entries one stripe at a time;
  // stripewise less locking and more concurrency.  An intervening
  // reallocateBuckets (cannot happen while holding the lock on a
  // stripe so no need to take a local copy bs of the buckets field)
  // may redistribute items between buckets but each item stays in the
  // same stripe.
  public void forEach(Consumer<K,V> consumer) {
    // TO DO: IMPLEMENT
	for (int stripe=0; stripe<lockCount; stripe++) { 
		//System.out.println("in loop med stripe no..." + stripe);
		synchronized (locks[stripe]) {
			for (int hash=stripe; hash<buckets.length; hash+=lockCount) { //iterate through buckets in this stripe
				ItemNode<K,V> node = buckets[hash];
				while (node != null) { //have we reached end of this bucket?
					int myStripe = getNodeStripe(node);
					//System.out.println("Stripe: "  + stripe + "antal regs paa stripe: " + sizes[stripe] + " Key: " + node.k + " value: " + node.v + " this node belongs to: " + myStripe );
				consumer.accept(node.k, node.v);
				node = node.next;
				}
				
			}
		}
	}
  }	
  
  /*public void forEach(Consumer<K, V> consumer) {
	  lockAllAndThen(() -> {
			for (int hash=0; hash<buckets.length; hash++) {
			ItemNode<K,V> node = buckets[hash];
			while (node != null) {
			consumer.accept(node.k, node.v);
			node = node.next;
			}
		}
	});
  }*/
  
  private int getNodeStripe(ItemNode<K, V> myNode) {
	//if(myNode.k != 0) {
		return getHash(myNode.k) % lockCount;
	//}  
  }
  
  
  // First lock all stripes.  Then double bucket table size, rehash,
  // and redistribute entries.  Since the number of stripes does not
  // change, and since buckets.length is a multiple of lockCount, a
  // key that belongs to stripe s because (getHash(k) % N) %
  // lockCount == s will continue to belong to stripe s.  Hence the
  // sizes array need not be recomputed.

  public void reallocateBuckets() {
    lockAllAndThen(() -> {
        final ItemNode<K,V>[] newBuckets = makeBuckets(2 * buckets.length);
        for (int hash=0; hash<buckets.length; hash++) {
          ItemNode<K,V> node = buckets[hash];
          while (node != null) {
            final int newHash = getHash(node.k) % newBuckets.length;
            ItemNode<K,V> next = node.next;
            node.next = newBuckets[newHash];
            newBuckets[newHash] = node;
            node = next;
          }
        }
        buckets = newBuckets;
      });
  }
  
  // Lock all stripes, perform the action, then unlock all stripes
  private void lockAllAndThen(Runnable action) {
    lockAllAndThen(0, action);
  }

  private void lockAllAndThen(int nextStripe, Runnable action) {
    if (nextStripe >= lockCount)
      action.run();
    else 
      synchronized (locks[nextStripe]) {
        lockAllAndThen(nextStripe + 1, action);
      }
  }

  static class ItemNode<K,V> {
    private final K k;
    private V v;
    private ItemNode<K,V> next;
    
    public ItemNode(K k, V v, ItemNode<K,V> next) {
      this.k = k;
      this.v = v;
      this.next = next;
    }

    // Assumes locks[getHash(k) % lockCount] is held by the thread
    public static <K,V> ItemNode<K,V> search(ItemNode<K,V> node, K k) {
      while (node != null && !k.equals(node.k))
        node = node.next;
      return node;
    }
  }
}

// ----------------------------------------------------------------------
// A hashmap that permits thread-safe concurrent operations, using
// lock striping (intrinsic locks on Objects created for the purpose),
// and with immutable ItemNodes, so that reads do not need to lock at
// all, only need visibility of writes, which is ensured through the
// AtomicIntegerArray called sizes.

// NOT IMPLEMENTED: get, putIfAbsent, size, remove and forEach.

// The bucketCount must be a multiple of the number lockCount of
// stripes, so that h % lockCount == (h % bucketCount) % lockCount and
// so that h % lockCount is invariant under doubling the number of
// buckets in method reallocateBuckets.  Otherwise there is a risk of
// locking a stripe, only to have the relevant entry moved to a
// different stripe by an intervening call to reallocateBuckets.

class StripedWriteMap<K,V> implements OurMap<K,V> {
  // Synchronization policy: writing to
  //   buckets[hash] is guarded by locks[hash % lockCount]
  //   sizes[stripe] is guarded by locks[stripe]
  // Visibility of writes to reads is ensured by writes writing to
  // the stripe's size component (even if size does not change) and
  // reads reading from the stripe's size component.
  private volatile ItemNode<K,V>[] buckets;
  private final int lockCount;
  private final Object[] locks;
  private final AtomicIntegerArray sizes; 
  
  public StripedWriteMap(int bucketCount, int lockCount) {
    if (bucketCount % lockCount != 0)
      throw new RuntimeException("bucket count must be a multiple of stripe count");
    this.lockCount = lockCount;
    this.buckets = makeBuckets(bucketCount);
    this.locks = new Object[lockCount];
    this.sizes = new AtomicIntegerArray(lockCount);
    
    for (int stripe=0; stripe<lockCount; stripe++) 
      this.locks[stripe] = new Object();
  }

  @SuppressWarnings("unchecked") 
  private static <K,V> ItemNode<K,V>[] makeBuckets(int size) {
    // Java's @$#@?!! type system requires "unsafe" cast here:
    return (ItemNode<K,V>[])new ItemNode[size];
  }

  // Protect against poor hash functions and make non-negative
  private static <K> int getHash(K k) {
    final int kh = k.hashCode();
    return (kh ^ (kh >>> 16)) & 0x7FFFFFFF;  
  }

  // Return true if key k is in map, else false
  public boolean containsKey(K k) {
    final ItemNode<K,V>[] bs = buckets;
    final int h = getHash(k), stripe = h % lockCount, hash = h % bs.length;
    // The sizes access is necessary for visibility of bs elements
    return sizes.get(stripe) != 0 && ItemNode.search(bs[hash], k, null);
  }

  // Return value v associated with key k, or null
  public V get(K k) {
    // TO DO: IMPLEMENT
    
	final ItemNode<K,V>[] bs = buckets; //make immutable copy of bucket list??
    final int h = getHash(k), stripe = h % lockCount, hash = h % bs.length;
	Holder<V> holder = new Holder<V>();
	if(ItemNode.search(bs[hash], k, holder))
		return holder.get();
	else
		return null;
  }

  public int size() {
    // TO DO: IMPLEMENT
	int res = 0;
	for (int i = 0; i < sizes.length(); i++) {
		res += sizes.get(i);
	}
    return res;
  }

  // Put v at key k, or update if already present.  The logic here has
  // become more contorted because we must not hold the stripe lock
  // when calling reallocateBuckets, otherwise there will be deadlock
  // when two threads working on different stripes try to reallocate
  // at the same time.
  public V put(K k, V v) {
    final int h = getHash(k), stripe = h % lockCount;
    final Holder<V> old = new Holder<V>();
    ItemNode<K,V>[] bs;
    int afterSize; 
    synchronized (locks[stripe]) {
      bs = buckets; //set bs = array of buckets
      final int hash = h % bs.length; //find hash value - modulo of hash and bucket length
      final ItemNode<K,V> node = bs[hash], //set node equal to old node on key, if any
        newNode = ItemNode.delete(node, k, old);
      bs[hash] = new ItemNode<K,V>(k, v, newNode);
      // Write for visibility; increment if k was not already in map
      afterSize = sizes.addAndGet(stripe, newNode == node ? 1 : 0);
    }
    if (afterSize * lockCount > bs.length)
      reallocateBuckets(bs);
    return old.get();
  }

  // Put v at key k only if absent.  
  public V putIfAbsent(K k, V v) {
    // TO DO: IMPLEMENT
    final int h = getHash(k), stripe = h % lockCount;
	final Holder<V> holder = new Holder<V>();
	holder.set(v);
	final ItemNode<K,V>[] bs; //= buckets;
	int afterSize; 
    synchronized (locks[stripe]) {
		bs = buckets;
		afterSize = sizes.get(stripe);
		final int hash = h % bs.length; //find relevant bucket
		ItemNode<K, V> node = bs[hash]; //find first node
		if (!ItemNode.search(node, k, holder)) {//hvis v ikke er til stede
            bs[hash] = new ItemNode<K,V>(k, v, bs[hash]);
			afterSize = sizes.addAndGet(stripe, 1);
			holder.set(null); //(V)holder;
			return holder.get();
	    }
	}
	if (afterSize * lockCount > bs.length)
			reallocateBuckets(bs);
	//System.out.println("PUT IF ABS RETURNS " + holder.get());
	return holder.get();
  }

  // Remove and return the value at key k if any, else return null
  public V remove(K k) {
    //return null;
	// TO DO: IMPLEMENT
    final int h = getHash(k), stripe = h % lockCount;
	final Holder<V> holder = new Holder<V>();
	final ItemNode<K,V>[] bs;
	//int afterSize;
	synchronized (locks[stripe]) {
		bs = buckets;
		final int hash = h % bs.length; //find relevant bucket 
		ItemNode<K, V> node = bs[hash]; //find first node
		ItemNode<K, V> deleted = ItemNode.delete(node, k, holder);
	    if (deleted!= null) {
			//afterSize = sizes.get(stripe);
			bs[hash] = deleted;
			sizes.decrementAndGet(stripe);
			return holder.get();
		}	
        else {
			return null;
	    }
	}
  }

  // Iterate over the hashmap's entries one stripe at a time.  
  public void forEach(Consumer<K,V> consumer) {
    // TO DO: IMPLEMENT
	final ItemNode<K,V>[] bs = buckets;
	for (int stripe=0; stripe < lockCount; stripe++) {
		synchronized (locks[stripe]) {
			for (int hash=stripe; hash<bs.length; hash+=lockCount) {
				ItemNode<K,V> node = buckets[hash];
				while (node != null) {
				consumer.accept(node.k, node.v);
				node = node.next;
				}
			}
		}
	}
  }
 
 
//implement alternative version of for each
  /*public void ForEach(Consumer <K,V> consumer) {
	// TO DO: IMPLEMENT
	final ItemNode<K,V>[] bs = buckets;
	for (int hash=0; hash<bs.length; hash++) {
		int hash = h % bs.length; 
		int stripe = h % lockCount;
		int my_no_buckets = sizes[stripe];
		synchronized (locks[stripe]) {
			ItemNode<K,V> node = buckets[hash];
			while (node != null) {
			consumer.accept(node.k, node.v);
			node = node.next;
			}
		}
	}	
  }*/
	


  // Now that reallocation happens internally, do not do it externally
  public void reallocateBuckets() { }

  // First lock all stripes.  Then double bucket table size, rehash,
  // and redistribute entries.  Since the number of stripes does not
  // change, and since buckets.length is a multiple of lockCount, a
  // key that belongs to stripe s because (getHash(k) % N) %
  // lockCount == s will continue to belong to stripe s.  Hence the
  // sizes array need not be recomputed.  

  // In any case, do not reallocate if the buckets field was updated
  // since the need for reallocation was discovered; this means that
  // another thread has already reallocated.  This happens very often
  // with 16 threads and a largish buckets table, size > 10,000.

  public void reallocateBuckets(final ItemNode<K,V>[] oldBuckets) {
    lockAllAndThen(() -> { 
        final ItemNode<K,V>[] bs = buckets;
        if (oldBuckets == bs) {
          // System.out.printf("Reallocating from %d buckets%n", bs.length);
          final ItemNode<K,V>[] newBuckets = makeBuckets(2 * bs.length);
          for (int hash=0; hash<bs.length; hash++) {
            ItemNode<K,V> node = bs[hash];
            while (node != null) {
              final int newHash = getHash(node.k) % newBuckets.length;
              newBuckets[newHash] 
                = new ItemNode<K,V>(node.k, node.v, newBuckets[newHash]);
              node = node.next;
            }
          }
          buckets = newBuckets; // Visibility: buckets field is volatile
        } 
      });
  }
  
  // Lock all stripes, perform action, then unlock all stripes
  private void lockAllAndThen(Runnable action) {
    lockAllAndThen(0, action);
  }

  private void lockAllAndThen(int nextStripe, Runnable action) {
    if (nextStripe >= lockCount)
      action.run();
    else 
      synchronized (locks[nextStripe]) {
        lockAllAndThen(nextStripe + 1, action);
      }
  }

  static class ItemNode<K,V> {
    private final K k;
    private final V v;
    private final ItemNode<K,V> next;
    
    public ItemNode(K k, V v, ItemNode<K,V> next) {
      this.k = k;
      this.v = v;
      this.next = next;
    }

    // These work on immutable data only, no synchronization needed.

    public static <K,V> boolean search(ItemNode<K,V> node, K k, Holder<V> old) {
      while (node != null) //hvis vi ikke har nået enden af node rækken
        if (k.equals(node.k)) { //hvis key er lig nodens key 
          if (old != null) //hvis værdien af old er forskellig fra null
            old.set(node.v); //sæt holder til værdien af v (by reference)
          return true;
        } else 
          node = node.next;
      return false;
    }
    
    public static <K,V> ItemNode<K,V> delete(ItemNode<K,V> node, K k, Holder<V> old) { //method returns the new node list after deleted key has been removed
      if (node == null) //tjek om der er node - ikke end of bucket
        return null; 
      else if (k.equals(node.k)) { //hvis node har efterspurgte key
        old.set(node.v); //Sæt holderen til at holde værdien af gl. key
        return node.next; //Returner næste node
      } else {
        final ItemNode<K,V> newNode = delete(node.next, k, old); //Recursive call: Send delete videre til næste node
        if (newNode == node.next) 
          return node;
        else 
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

// ----------------------------------------------------------------------
// A wrapper around the Java class library's sophisticated
// ConcurrentHashMap<K,V>, making it implement OurMap<K,V>

class WrapConcurrentHashMap<K,V> implements OurMap<K,V> {
  final ConcurrentHashMap<K,V> underlying = new ConcurrentHashMap<K,V>();

  public boolean containsKey(K k) {
    return underlying.containsKey(k);
  }

  public V get(K k) {
    return underlying.get(k);
  }

  public V put(K k, V v) {
    return underlying.put(k, v);
  }

  public V putIfAbsent(K k, V v) {
    return underlying.putIfAbsent(k, v);
  }
  
  public V remove(K k) {
    return underlying.remove(k);
  }

  public int size() {
    return underlying.size();
  }
  
  public void forEach(Consumer<K,V> consumer) {
    underlying.forEach((k,v) -> consumer.accept(k,v));
  }

  public void reallocateBuckets() { }
}


/*class PrettyPrintingMap<K, V> {
    private OurMap<K, V> map;

    public PrettyPrintingMap(OurMap<K, V> map) {
        this.map = map;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        //NEED TO IMPL OWN ITERATOR
        Iterator<Entry<K, V>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<K, V> entry = iter.next();
            sb.append(entry.getKey());
            sb.append('=').append('"');
            sb.append(entry.getValue());
            sb.append('"');
            if (iter.hasNext()) {
                sb.append(',').append(' ');
            }
        }
        return sb.toString();

    }
}*/
