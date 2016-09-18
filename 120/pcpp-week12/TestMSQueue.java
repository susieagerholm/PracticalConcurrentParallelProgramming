// For week 12
// sestoft@itu.dk * 2014-11-16

// Unbounded list-based lock-free queue by Michael and Scott 1996 (who
// call it non-blocking).

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;


public class TestMSQueue extends Tests {
  public static void main(String[] args) throws Exception {
		//sequentialTest(new MSQueue<Integer>());
		//parallelTest(new MSQueue<Integer>());
		//Benchmark.Mark7("scalability", TestMSQueue::scalability);
		scalabilityTest(new MSQueue<Integer>(), 8);
		
  }
  
  private static void sequentialTest(UnboundedQueue<Integer> ubq) throws Exception {
    System.out.printf("%nSequential test: %s", ubq.getClass());    
	//enque full - unbounded, does not exit!!
    //deque from empty - return null
	assertNull((Integer)ubq.dequeue());
	//enqueue items
	ubq.enqueue(7); ubq.enqueue(9); ubq.enqueue(13); 
    //deques items - make sure proper FIFO order...
	assertEquals(ubq.dequeue(), 7);
    assertEquals(ubq.dequeue(), 9);
    assertEquals(ubq.dequeue(), 13);
	//make sure queue is empty and returning null correctly!
    assertNull((Integer)ubq.dequeue());
    System.out.println("... passed");
  }
  
  private static void parallelTest(MSQueue<Integer> ubq) throws Exception {
    System.out.printf("%nParallel test: %s", ubq.getClass()); 
    final ExecutorService pool = Executors.newCachedThreadPool();
    new PutTakeTest(ubq, 17, 100000).test(pool); 
	//new PutTakeTest(ubq, 17, 1000).test(pool); 
	pool.shutdown();
    System.out.println("... passed");      
  }
  
  private static void scalabilityTest(MSQueue<Integer> ubq, int threadCount_max) throws Exception {
	System.out.printf("%nParallel test: %s", ubq.getClass()); 
    final ExecutorService pool = Executors.newCachedThreadPool();
    //new PutTakeTest(ubq, 4, 100000).test(pool); 
	for (int i = 1; i <= threadCount_max; i++) {
		new PutTakeTest(ubq, i, 10000000).test(pool); 
	}
	//new PutTakeTest(ubq, 4, 10000).test(pool); 
	pool.shutdown();
    System.out.println("... passed");    
  }
  
}


interface UnboundedQueue<T> {
  void enqueue(T item);
  T dequeue();
}

// ------------------------------------------------------------
// Unbounded lock-based queue with sentinel (dummy) node

class LockingQueue<T> implements UnboundedQueue<T> {  
  // Invariants:
  // The node referred by tail is reachable from head.
  // If non-empty then head != tail, 
  //    and tail points to last item, and head.next to first item.
  // If empty then head == tail.

  private static class Node<T> {
    final T item;
    Node<T> next;
    
    public Node(T item, Node<T> next) {
      this.item = item;
      this.next = next;
    }
  }

  private Node<T> head, tail;

  public LockingQueue() {
    head = tail = new Node<T>(null, null);
  }
  
  public synchronized void enqueue(T item) { // at tail
    Node<T> node = new Node<T>(item, null);
    tail.next = node; //add new node to end of queue
    tail = node; //update tail reference points to new last
  }

  public synchronized T dequeue() {     // from head
    if (head.next == null) //is queue empty?
      return null;
    Node<T> first = head; 
    head = first.next; //get first item in queue
    return head.item; //return first item
  }
}


// ------------------------------------------------------------
// Unbounded lock-free queue (non-blocking in M&S terminology), 
// using CAS and AtomicReference

// This creates one AtomicReference object for each Node object.  The
// next MSQueueRefl class further below uses one-time reflection to
// create an AtomicReferenceFieldUpdater, thereby avoiding this extra
// object.  In practice the overhead of the extra object apparently
// does not matter much.

class MSQueue<T> implements UnboundedQueue<T> {
  private final AtomicReference<Node<T>> head, tail;

  public MSQueue() {
    Node<T> dummy = new Node<T>(null, null);
    head = new AtomicReference<Node<T>>(dummy); //initial value is dummy
    tail = new AtomicReference<Node<T>>(dummy); //initial value is dummy
  }

  public void enqueue(T item) { // at tail
    Node<T> node = new Node<T>(item, null);
    while (true) {
      Node<T> last = tail.get(), next = last.next.get();
      if (last == tail.get()) {         // E7
        if (next == null)  {
          // In quiescent state, try inserting new node
          if (last.next.compareAndSet(next, node)) { // E9
            // Insertion succeeded, try advancing tail
            tail.compareAndSet(last, node);
            return;
          }
        } else 
          // Queue in intermediate state, advance tail
          tail.compareAndSet(last, next);
      }
    }
  }

  public T dequeue() { // from head
    while (true) {
      Node<T> first = head.get(), last = tail.get(), next = first.next.get(); // D3
      if (first == head.get()) {       // D5
        if (first == last) {
          if (next == null)
            return null;
          else
            tail.compareAndSet(last, next);
        } else {
          T result = next.item;
		  if (head.compareAndSet(first, next)) // D13
            return result;
        }
      }
    }
  }

  private static class Node<T> {
    final T item;
    final AtomicReference<Node<T>> next;

    public Node(T item, Node<T> next) {
      this.item = item;
      this.next = new AtomicReference<Node<T>>(next);
    }
  }
}


// --------------------------------------------------
// Lock-free queue, using CAS and reflection on field Node.next

class MSQueueRefl<T> implements UnboundedQueue<T> {
  private final AtomicReference<Node<T>> head, tail;

  public MSQueueRefl() {
    // Essential to NOT make dummy a field as in Goetz p. 334, that
    // would cause a memory management disaster, huge space leak:
    Node<T> dummy = new Node<T>(null, null);
    head = new AtomicReference<Node<T>>(dummy);
    tail = new AtomicReference<Node<T>>(dummy);
  }

  @SuppressWarnings("unchecked") 
  // Java's @$#@?!! generics type system: abominable unsafe double type cast
  private final AtomicReferenceFieldUpdater<Node<T>, Node<T>> nextUpdater 
    = AtomicReferenceFieldUpdater.newUpdater((Class<Node<T>>)(Class<?>)(Node.class), 
                                             (Class<Node<T>>)(Class<?>)(Node.class), 
                                             "next");    

  public void enqueue(T item) { // at tail
    Node<T> node = new Node<T>(item, null);
    while (true) {
      Node<T> last = tail.get(), next = last.next;
      if (last != tail.get()) {         // E7
        if (next == null)  {  //our last is stil last node - we are in quiescent mode
          // In quiescent state, try inserting new node
          if (nextUpdater.compareAndSet(last, next, node)) {
            // Insertion succeeded, try advancing tail
            tail.compareAndSet(last, node);
            return;
          }
        } else {
          // Queue in intermediate state, advance tail
          tail.compareAndSet(last, next);
        }
      }
	  else 
		  System.out.println("GOT CANCELLED BY E7!!"); 
    }
  }
  
  public T dequeue() { // from head
    while (true) {
      Node<T> first = head.get(), last = tail.get(), next = first.next;
      if (first == head.get()) {        // D5
        if (first == last) {
          if (next == null)
            return null;
          else
            tail.compareAndSet(last, next);
        } else {
          T result = next.item;
          if (head.compareAndSet(first, next)) {
            return result;
          }
        }
      }
	  else
		System.out.println("GOT CANCELLED BY D5!!");
    }
  }

  private static class Node<T> {
    final T item;
    volatile Node<T> next;

    public Node(T item, Node<T> next) {
      this.item = item;
      this.next = next;
    }
  }
}

class Tests {
  public static void assertEquals(int x, int y) throws Exception {
    if (x != y) 
      throw new Exception(String.format("ERROR: %d not equal to %d%n", x, y));
  }

  public static void assertTrue(boolean b) throws Exception {
    if (!b) 
      throw new Exception(String.format("ERROR: assertTrue"));
  }
  
  public static void assertNull(Integer z) throws Exception {
    if (z != null) 
      throw new Exception(String.format("ERROR: assertNull (QUEUE IS NOT EMPTY!!)"));
  }
}

class PutTakeTest extends Tests {
  // We could use one CyclicBarrier for both starting and stopping,
  // precisely because it is cyclic, but the code becomes clearer by
  // separating them:
  protected CyclicBarrier startBarrier, stopBarrier;
  protected final MSQueue<Integer> ubq;
  protected final int nTrials, nPairs;
  protected final AtomicInteger putSum = new AtomicInteger(0);
  protected final AtomicInteger takeSum = new AtomicInteger(0);

  public PutTakeTest(MSQueue<Integer> ubq, int npairs, int ntrials) {
    this.ubq = ubq;
    this.nTrials = ntrials;
    this.nPairs = npairs;
    this.startBarrier = new CyclicBarrier(npairs * 2 + 1);
    this.stopBarrier = new CyclicBarrier(npairs * 2 + 1);
  }
  
  void test(ExecutorService pool) {
    try {
      for (int i = 0; i < nPairs; i++) {
        pool.execute(new ProducerPrime());
        pool.execute(new ConsumerPrime());
      }      
      startBarrier.await(); // wait for all threads to be ready
	  //System.out.println("TEST STARTER...");
	  Timer my_timer = new Timer();
      stopBarrier.await();  // wait for all threads to finish      
	  System.out.println("Pairs: " + nPairs + " - Trials: " + nTrials + "\n" + "...time to execute in nano secs:  " + my_timer.check());
      assertNull(ubq.dequeue());
      assertEquals(putSum.get(), takeSum.get());
	  //System.out.println("PRODUCER: " + putSum.get());
	   //System.out.println("CONSUMER: " + takeSum.get());
    } catch (Exception e) {
		
      throw new RuntimeException(e);
    }
  }

  class Producer implements Runnable {
    public void run() {
      try {
        Random random = new Random();
        int sum = 0;
        startBarrier.await();
        for (int i = nTrials; i > 0; --i) {
		  //System.out.println("PRODUCER" + sum);	
          int item = random.nextInt();
		  ubq.enqueue(item);
          sum += item;
        }
		System.out.println("PRODUCER IS DONE: " + sum);
        putSum.getAndAdd(sum);
        stopBarrier.await();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  class Consumer implements Runnable {
	
    public void run() {
      try {
        startBarrier.await();
        int sum = 0;
		int counter = 0;
        do {
			Integer item = ubq.dequeue();
			if (item != null) {
				//System.out.println("CONSUMER: " + sum + "- COUNTER IS " + counter);
				sum += item;
				counter += 1;	
			}
		}
		while(counter < nTrials);
		System.out.println("CONSUMER IS DONE: " + counter);
		takeSum.getAndAdd(sum);
        stopBarrier.await();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  class ProducerPrime implements Runnable {
    public void run() {
      try {
        Random random = new Random();
        int sum = 0;
        startBarrier.await();
        for (int i = 0; i < nTrials; i++) {
		  //System.out.println("PRODUCER" + sum);	
          if(isPrime(i)) {
			ubq.enqueue(i);
			sum += i;  
		  }
        }
		//System.out.println("PRODUCER IS DONE: " + sum);
        putSum.getAndAdd(sum);
        stopBarrier.await();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
	private boolean isPrime(int n) {
		int k = 2;
		while (k * k <= n && n % k != 0)
		k++;
		return n >= 2 && k * k > n;
    }
  }
  
  class ConsumerPrime implements Runnable {
	
    public void run() {
      try {
        startBarrier.await();
        int sum = 0;
		int counter = 0;
		int primeCount = isPrimeCounter(nTrials);
        do {
			Integer item = ubq.dequeue();
			//System.out.println("CONSUMER sum is curr: " + sum);
			if (item != null && isPrime(item)) {
				//System.out.println("CONSUMER: " + sum + "- COUNTER IS " + counter);
				sum += item;
				counter++;	
			}
		}
		while(counter < primeCount);
		//System.out.println("COMSUMER IS DONE: " + counter);
		takeSum.getAndAdd(sum);
        stopBarrier.await();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
	
	private boolean isPrime(int n) {
		int k = 2;
		while (k * k <= n && n % k != 0)
		k++;
		return n >= 2 && k * k > n;
    }
	
	private int isPrimeCounter(int range) {
		int sum = 0;
		for (int i = 0; i <= range; i++) {
			if(isPrime(i)) {
				sum++;	
			}
		}
		return sum;
	}
  }
  
  class Timer {
  private long start, spent = 0;
  public Timer() { play(); }
  public double check() { return (System.nanoTime()-start+spent)/1e9; }
  public void pause() { spent += System.nanoTime()-start; }
  public void play() { start = System.nanoTime(); }
  }

}


