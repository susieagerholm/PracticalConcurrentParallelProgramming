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
import org.multiverse.stms.gamma.transactionalobjects.GammaTxnInteger;

import java.sql.Timestamp;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CyclicBarrier;

class TestStmHistogram {
  public static void main(String[] args) {
    countPrimeFactorsWithStmHistogram();
  }

  private static void countPrimeFactorsWithStmHistogram() {
	Timestamp ts_start, ts_stop;  
	final Histogram total = new StmHistogram(30);
	//final Histogram histogram = new StmHistogram(30);
    final int range = 4_000_000;
	//create bins
    final int threadCount = 10, perThread = range / threadCount;
    final CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1), 
      stopBarrier = startBarrier;
    final Thread[] threads = new Thread[threadCount];
    final Histogram histogram = new StmHistogram(30);
    ts_start = new Timestamp(System.currentTimeMillis());
    for (int t=0; t<threadCount; t++) { //create a bin for every thread
      final int from = perThread * t, 
                  to = (t+1 == threadCount) ? range : perThread * (t+1); //If last thread : to = range, else start of next threads range...
        
      threads[t] = //start new thread
      
      new Thread(() -> { 
          	  
	      try { startBarrier.await(); } catch (Exception exn) { }  //wait for all other threads to be ready to start
	      
	      for (int p=from; p<to; p++) //thread run through all # in bins range
	    	  histogram.increment(countFactors(p));
	      	  System.out.print("*");
	      try { stopBarrier.await(); } catch (Exception exn) { } //wait
	    });
        threads[t].start(); //start thread
    }
    try { 
    	
    startBarrier.await(); 
    ts_start = new Timestamp(System.currentTimeMillis());	
	//System.out.println("ALL THREADS START...time is : " + ts_start.toString());
    for(int i = 0; i < 200; i++) {
    	try {
			Thread.sleep(30);
			total.transferBins(histogram);
			total.transferBins(total);
			System.out.println("Making dump of total...time is : " + System.nanoTime());
			dump(total);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
	
    
    } catch (Exception exn) { }
    try { stopBarrier.await(); 
    ts_stop = new Timestamp(System.currentTimeMillis());	
    System.out.println("ALL THREADS START...time is : " + ts_start.toString());
	System.out.println("ALL THREADS STOP...time is : " + ts_stop.toString());
    } catch (Exception exn) { }
    
    dump(total); //make dump of histogram
    //getHistBins(total); //make dump of bins
    
    dump(histogram); //make dump of histogram
    //getHistBins(histogram); //make dump of bins
    //System.out.println("Get and clear: " + histogram.getAndClear(4));
    //System.out.println("Get after clear: " + histogram.getCount(4));
  }

  public static void dump(Histogram histogram) {
    int totalCount = 0;
    for (int bin=0; bin<histogram.getSpan(); bin++) {
      System.out.printf("%4d: %9d%n", bin, histogram.getCount(bin));
      totalCount += histogram.getCount(bin);
    }
    System.out.printf("      %9d%n", totalCount);
  }
  
  public static void getHistBins(Histogram histogram) {
	  int[] temp = histogram.getBins();
	  for(int k = 0; k < temp.length; k++) {
		  System.out.println("# i array: " + k + " har antal: " + temp[k]);
	  }
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
	
	for (int i = 0; i < counts.length; i++) {
		counts[i] = new GammaTxnInteger(0);
	}
  }

  //10.2.1
  public void increment(int bin) {
	  atomic(() -> {
			counts[bin].increment();    
	  }); 
  }

  public int getCount(int bin) {
	  return atomic(() -> counts[bin].get()); 
  }

  public int getSpan() {
	  //final : length cannot be altered 
	  return counts.length;
  }

  //10.2.3 
  public int[] getBins() {
	  return atomic(() -> {
		int[] temp = new int[counts.length];
		for(int j = 0; j < temp.length; j++) {
			temp[j] = counts[j].get();
		}
	  return temp;
	  });
  }

//10.2.4
  public int getAndClear(int bin) {
    return atomic(() -> {
		int temp = getCount(bin);
		counts[bin].set(0);
		return temp;
	});
	
  }

//10.2.5
  public void transferBins(Histogram hist) { //transfer local counts to global - periodic updates instead of one final
	  for(int bin = 0; bin < counts.length; bin++) {
		  final int bin_final = bin;
		  atomic(() -> {
			  int aggregate = getCount(bin_final); 
			  aggregate += hist.getAndClear(bin_final);
			  counts[bin_final].set(aggregate);
		  });
	  }
  }
}

