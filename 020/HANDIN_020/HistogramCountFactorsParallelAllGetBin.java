import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;    
import java.util.Collections;    

public class HistogramCountFactorsParallelAllGetBin {
  public static void main(String[] args) {
    final int range = 50;    
    System.out.println("Counting factors with 10 parallel threads and creating Histogram from distribution...");
    countFactorsParallelN(range, 10);
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
  private static void countFactorsParallelN(int range, int threadCount) {
    
    //final Histogram histogram = new Histogram2(50);
    final Histogram histogram = new Histogram3(50);
    //final Histogram histogram = new Histogram4(50);
    
    final int perThread = range / threadCount;
    Thread[] threads = new Thread[threadCount];
    
    for (int t=0; t<threadCount; t++) {
      final int from = perThread * t, to = (t+1==threadCount) ? range : perThread * (t+1); 
      threads[t] = new Thread(() -> { 
	     for (int i=from; i<to; i++)
        histogram.increment(countFactors(i)); 
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
	threads[t].join();
    } catch (InterruptedException exn) { }
    ThreadSafeHistogram.dump(histogram);
    
    ArrayList my = histogram.getMyBins();
    my.add(51, new AtomicInteger(5));
  } 
} 

class ThreadSafeHistogram {
  
  //INVARIANT: Will only be called sequentially
  public static void dump(Histogram histogram) {
    int totalCount = 0;
    for (int bin=0; bin<histogram.getSpan(); bin++) {
      System.out.printf("%4d: %9d%n", bin, histogram.getCount(bin));
      totalCount += histogram.getCount(bin);
    }
    System.out.printf("      %9d%n", totalCount);
  }
}

interface Histogram {
  public void increment(int bin);
  public int getCount(int bin);
  public int getSpan();
  public int[] getBins();
}

class Histogram2 implements Histogram {
  //make sure reference to counts aray cannot be altered
  private final int[] counts;
  public Histogram2(int span) {
    //creating array of bins 
    this.counts = new int[span];
  }
  //synchronized read-modify-write operation
  public synchronized void increment(int bin) {
    counts[bin] = counts[bin] + 1;
  }
  //must be synchronized in order to prevent invalid reads 
  public synchronized int getCount(int bin) {
    return counts[bin];
  }
  //need not be synchronized since only read - final declaration ensures 
  //that counts array remains the same as initialized
  public int getSpan() {
    return counts.length;
  }
  
  //The length of an array is immutable in java, so client cannot alter eg. length,
  //but still not safe to let counts array escape since client would be able to 
  //circumvent synchronization on getCount and increment methods. Method returns 
  //a copy of counts = a snapshot. Guess it would have to be synchronized since
  //it is a read of shared state it should not be possible to make a copy while 
  //others may write to it - can lead to inconsistent view of data...I cannot see
  //that it is possible to make final the primitive values of an array???  
  public synchronized int[] getBins() {
    int[] countsCopy = Arrays.copyOf(counts, 50);
    return countsCopy;
  }
}


class Histogram3 implements Histogram {
  //make sure reference to counts aray cannot be altered
  private final AtomicInteger[] counts;
  public Histogram3(int span) {
    //creating array of bins and initialize AtomicInteger objects in each slot... 
    this.counts = new AtomicInteger[span];  
    for (int j= 0; j < counts.length; j++) {
      counts[j] = new AtomicInteger();
    }
    
  }
  //synchronized read-modify-write operation
  public void increment(int bin) {
    synchronized(this.counts[bin]) {
      this.counts[bin].getAndIncrement();
    }
    
  }
  //must be synchronized in order to prevent invalid reads - also needed in 
  //Atomic Integer version
  public int getCount(int bin) {
    return counts[bin].get();
  }
  //need not be synchronized since only read - final declaration ensures 
  //that counts array remains the same as initialized
  public int getSpan() {
    return counts.length;
  }
  
  //If method would allow returning objects, it would be possible to return a 
  //live view of counts array in the form of an unmodifiable list... 
  public int[] getBins() {  
    synchronized(this.counts) {
      int[] bins = new int[counts.length];     
      for (int i = 0; i < counts.length; i++) {
          bins[i] = counts[i].get();           
      }                                        
      return bins;                                   
    } 
  }                        
  
  public ArrayList getMyBins() {                                                                             
    //Creating 'live' view of counts array as unmodifiable list                                        
    List<AtomicInteger> list = new ArrayList<AtomicInteger>(Arrays.asList(counts));                    
    ArrayList unmodifiable = Collections.unmodifiableList(list);                                       
    return unmodifiable;                                                                                
  } 
}                                                                                                    
  
class Histogram4 implements Histogram {
  //make sure reference to counts aray cannot be altered
  private final AtomicIntegerArray counts;
  public Histogram4(int span) {
    //creating array of bins and initialize AtomicInteger objects in each slot... 
    this.counts = new AtomicIntegerArray(50);  
    
  }
  //synchronized read-modify-write operation
  public void increment(int bin) {   
      this.counts.getAndIncrement(bin);    
  }
  //must be synchronized in order to prevent invalid reads - also needed in 
  //Atomic Integer version
  public int getCount(int bin) {
    return counts.get(bin);
  }
  //need not be synchronized since only read - final declaration ensures 
  //that counts array remains the same as initialized
  public int getSpan() {
    return counts.length();
  }
  
  public int[] getBins() {    
    int[] hallo  = {1,2,3}; 
    return hallo;             
  }                           
} 