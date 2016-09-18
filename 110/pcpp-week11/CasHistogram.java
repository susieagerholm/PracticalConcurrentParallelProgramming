class TestCasHistogram {
	private final AtomicInteger[] counts;

  public CasHistogram(int span) {
	this.counts = new int[span];  
  }

  public void increment(int bin) {
	
  }

  public int getCount(int bin) {
	return counts[bin].get(); 
  }

  public int getSpan() {
	 
  }

  public int[] getBins() {
    
  }

  public int getAndClear(int bin) {
    
  }

  public void transferBins(Histogram hist) {
    
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


