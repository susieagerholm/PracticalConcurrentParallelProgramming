class ThreadSafeHistogram {
  public static void main(String[] args) {
    final Histogram histogram = new Histogram2(30);
    histogram.increment(7);
    histogram.increment(13);
    histogram.increment(7);
    histogram.increment(35);
    dump(histogram);
  }

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
}

class Histogram2 implements Histogram {
  //make sure reference to counts aray cannot be altered
  private int[] counts;
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
}
