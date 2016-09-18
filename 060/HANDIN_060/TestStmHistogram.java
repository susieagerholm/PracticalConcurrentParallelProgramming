// For week 10
// sestoft@itu.dk * 2014-11-05, 2015-10-14

// Compile and run like this:
//   javac -cp ~/lib/multiverse-core-0.7.0.jar TestStmHistogram.java
//   java -cp ~/lib/multiverse-core-0.7.0.jar:. TestStmHistogram

// For the Multiverse library:
import org.multiverse.api.references.*;
import static org.multiverse.api.StmUtils.*;

// Multiverse locking:
import org.multiverse.api.LockMode;
import org.multiverse.api.Txn;
import org.multiverse.api.callables.TxnVoidCallable;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CyclicBarrier;

class TestStmHistogram {
  public static void main(String[] args) {
    countPrimeFactorsWithStmHistogram();
  }

  private static void countPrimeFactorsWithStmHistogram() {
    final Histogram histogram = new StmHistogram(30);
    final int range = 4_000_000;
	//create bins
    final int threadCount = 10, perThread = range / threadCount;
    final CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1), 
      stopBarrier = startBarrier;
    final Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) { //create a bin for every thread
      final int from = perThread * t, 
                  to = (t+1 == threadCount) ? range : perThread * (t+1); //If last thread : to = range, else start of next threads range...
        threads[t] = //start new thread
          new Thread(() -> { 
	      try { startBarrier.await(); } catch (Exception exn) { }  //wait for all other threads to be ready to start
	      for (int p=from; p<to; p++) //thread run  through all no in bins range
		histogram.increment(countFactors(p));
	      System.out.print("*");
	      try { stopBarrier.await(); } catch (Exception exn) { } //wait
	    });
        threads[t].start(); //start thread
    }
    try { startBarrier.await(); } catch (Exception exn) { }
    try { stopBarrier.await(); } catch (Exception exn) { }
    dump(histogram); //make dump of histogram
  }

  public static void dump(Histogram histogram) {
    int totalCount = 0;
    for (int bin=0; bin<histogram.getSpan(); bin++) {
      System.out.printf("%4d: %9d%n", bin, histogram.getCount(bin));
      totalCount += histogram.getCount(bin);
    }
    System.out.printf("      %9d%n", totalCount);
  }

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
}

interface Histogram {
  void increment(int bin);
  int getCount(int bin);
  int getSpan();
  int[] getBins();
  int getAndClear(int bin);
  void transferBins(Histogram hist);
}

class StmHistogram implements Histogram {
  private final TxnInteger[] counts;

  public StmHistogram(int span) {
	this.counts = new TxnInteger[span];  
    //throw new RuntimeException("Not implemented");
  }

  public void increment(int bin) {
	  atomic(() -> counts[bin].increment()); 
    //throw new RuntimeException("Not implemented");
  }

  public int getCount(int bin) {
	  return atomic(() -> counts[bin])); 
    //throw new RuntimeException("Not implemented");
  }

  public int getSpan() {
	  //final : length cannot be altered 
	  return counts.length;
    //throw new RuntimeException("Not implemented");
  }

  public int[] getBins() {
    throw new RuntimeException("Not implemented");
  }

  public int getAndClear(int bin) {
    return atomic(() -> {
		int temp = getCount(bin);
		counts[bin] = 0;
		return temp;
	});
	
	//throw new RuntimeException("Not implemented");
  }

  public void transferBins(Histogram hist) { //transfer local counts to global - periodic updates instead of one final
    throw new RuntimeException("Not implemented");
  }
}

