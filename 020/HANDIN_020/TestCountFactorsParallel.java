import java.util.concurrent.atomic.AtomicInteger;

public class TestCountFactorsParallel {
  public static void main(String[] args) {
    final int range = 5_000_000;
    System.out.printf("Counting factors with 10 parallel threads - result: %10d%n%n", countFactorsParallelN(range, 10));
  }

  //Count factors
  public static int countFactors(int p) {
    if (p < 2) 
      return 0;
    int factorCount = 1, k = 2;
    while (p >= k * k) {
      if (p % k == 0) {
	factorCount++;
	p /= k;
      } else 
	k++;
    }
    return factorCount;
  }


 //General parallel solution, using multiple threads
  private static int countFactorsParallelN(int range, int threadCount) {
    final int perThread = range / threadCount;
    //final MyAtomicInteger myInteger = new MyAtomicInteger();
    final AtomicInteger myInteger = new AtomicInteger();
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int from = perThread * t, to = (t+1==threadCount) ? range : perThread * (t+1); 
      threads[t] = new Thread(() -> {
	     for (int i=from; i<to; i++)
	       myInteger.addAndGet(countFactors(i));
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
	threads[t].join();
    } catch (InterruptedException exn) { }
    return myInteger.get();
  } 
} 

//MyAtomicInteger with implementation based on synchronization strategy
class MyAtomicInteger {
  private int count = 0;
  public synchronized void addAndGet(int value) {
    count = count + value;
  }
  public synchronized int get() { 
    return count; 
  }
}
