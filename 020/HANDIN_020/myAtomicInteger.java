import java.util.concurrent.atomic.*;

class MyAtomicInteger {
  private final AtomicInteger value = new AtomicInteger();
  
  public int addAndGet(int amount) {
    return value.addAndGet(amount);  
  } 
  
  public int get() {
    return value.get();  
  } 
  
  /* Testing increment possible evt. if final?
  public static void main(String[] args) {
    MyAtomicInteger myInteger = new MyAtomicInteger();
    System.out.println(myInteger.addAndGet(10));
    System.out.println(myInteger.addAndGet(11));
  }*/ 

}

//or

/*class myAtomicInteger {
  @GuardedBy("this") private int value;
  
  public synchronized int addAndGet(int amount) {
    value += amount;  
  } 
  
  public synchronized int get() {
    return value;  
  } 

}
  */
