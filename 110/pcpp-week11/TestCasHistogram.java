import java.util.concurrent.atomic.AtomicInteger;
import java.sql.Timestamp;
import java.util.concurrent.CyclicBarrier;

class TestCasHistogram {
  public static void main(String[] args) {
    countPrimeFactorsWithCasHistogram();
	//simpleFunctionalTest();
  }
  
  private static void simpleFunctionalTest() {
	final Histogram total = new CasHistogram(30);
	total.increment(1);
	System.out.println(total.getCount(1));
	total.getAndClear(1);
	System.out.println(total.getCount(1));
	
	
	  
	  
  }  
  private static void countPrimeFactorsWithCasHistogram() {
	Timestamp ts_start, ts_stop;  
    final Histogram total = new CasHistogram(30);
	//final Histogram histogram = new StmHistogram(30);
    final int range = 4_000_000;
	//create bins
    final int threadCount = 10, perThread = range / threadCount;
    final CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1), 
      stopBarrier = startBarrier;
    final Thread[] threads = new Thread[threadCount];
    final Histogram histogram = new CasHistogram(30);
    ts_start = new Timestamp(System.currentTimeMillis());
    for (int t=0; t<threadCount; t++) { //create a bin for every thread
      final int from = perThread * t, 
                  to = (t+1 == threadCount) ? range : perThread * (t+1); //If last thread : to = range, else start of next threads range...
        
      threads[t] = //start new thread
      
	  
      new Thread(() -> { 
          	  
	      try { startBarrier.await(); } catch (Exception exn) { }  //wait for all other threads to be ready to start
	      
	      for (int p=from; p<to; p++) {//thread run through all # in bins range
	    	  
			  histogram.increment(countFactors(p));
			  //System.out.println("Calling increment on bin: " + countFactors(p));
			  //System.out.println("This bin" + countFactors(p) + "has currently at count of: " + histogram.getCount(countFactors(p)));
	      	  //System.out.print("*");
		  }	  
	      try { stopBarrier.await(); } catch (Exception exn) { } //wait
	    });
        threads[t].start(); //start thread
    }
    try { 
    	
    startBarrier.await(); 
    ts_start = new Timestamp(System.currentTimeMillis());	
	System.out.println("ALL THREADS START...time is : " + ts_start.toString());
    for(int i = 0; i < 200; i++) {
    	try {
			Thread.sleep(30);
			total.transferBins(histogram);
			//total.transferBins(total);
			//System.out.println("Making dump of total...time is : " + System.nanoTime());
			//dump(total);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
	dump(total);
    
    } catch (Exception exn) { }
    try { stopBarrier.await(); 
    ts_stop = new Timestamp(System.currentTimeMillis());	
    System.out.println("ALL THREADS START...time is : " + ts_start.toString());
	System.out.println("ALL THREADS STOP...time is : " + ts_stop.toString());
    } catch (Exception exn) { }
    
    //dump(total); //make dump of histogram
    //getHistBins(total); //make dump of bins
    
    //dump(histogram); //make dump of histogram
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


class CasHistogram implements Histogram {
	private final AtomicInteger[] counts;

    public CasHistogram(int span) {
	this.counts = new AtomicInteger[span];  
	for (int i = 0; i < span; i++) {
		counts[i] = new AtomicInteger(0);
	}
  }

  public void increment(int bin) {
	int old, new_int;
	do {
		old = getCount(bin);
		new_int = old + 1;
	}
	while(!counts[bin].compareAndSet(old, new_int));
  }

  public int getCount(int bin) {
	return counts[bin].get(); 
  }

  public int getSpan() {
	return counts.length; 
  }

  public int[] getBins() {
	int[] temp = new int[counts.length]; 
	for (int bin = 0; bin < counts.length; bin++) {
		temp[bin] = getCount(bin);		
	}
	return temp;
  }

  public int getAndClear(int bin) {
    int old, new_int;
	do {
		old = getCount(bin);
		new_int = 0;
	}
	while(!counts[bin].compareAndSet(old, new_int));
	return old;
  }

  public void transferBins(Histogram hist) {
	int old_aggr, new_aggr;
	for(int bin = 0; bin < counts.length; bin++) { //problem: other thread may call transferBins on the same Histogram = inconsistent state!!!
		do {
			old_aggr = getCount(bin); 
			new_aggr = old_aggr + hist.getAndClear(bin);
		}
		while(!counts[bin].compareAndSet(old_aggr, new_aggr));
	}
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

class MyAtomicInteger {
  private int value;    // Visibility ensured by locking

  // Model implementation of compareAndSet to illustrate its meaning.
  // In reality, compareAndSet is not implemented using locks; the
  // opposite is usually the case.  
  public synchronized boolean compareAndSet(int oldValue, int newValue) {
    if (this.value == oldValue) {
      this.value = newValue;
      return true;
    } else
      return false;
  }

  public synchronized int get() { 
    return this.value;
  }

  public int addAndGet(int delta) {
    int oldValue, newValue;
    do {
      oldValue = get();
      newValue = oldValue + delta;
    } while (!compareAndSet(oldValue, newValue));
    return newValue;
  }

  public int getAndAdd(int delta) {
    int oldValue, newValue;
    do {
      oldValue = get();
      newValue = oldValue + delta;
    } while (!compareAndSet(oldValue, newValue));
    return oldValue;
  }

  public int incrementAndGet() {
    return addAndGet(1);
  }

  public int decrementAndGet() {
    return addAndGet(-1);
  }

  public int getAndSet(int newValue) {
    int oldValue;
    do { 
      oldValue = get();
    } while (!compareAndSet(oldValue, newValue));
    return oldValue;
  }
}


