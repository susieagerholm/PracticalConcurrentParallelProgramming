import java.io.IOException;

public class TestPrinterSyncBlock {
  public static void main(String[] args) throws IOException {
    final Printer p = new Printer();
    Thread t1 = new Thread(() -> {
	  while (true)		// Forever call print
	  Printer.print();
      });
    
    Thread t2 = new Thread(() -> {
	  while (true)		// Forever call print
	  Printer.print();
      });
    t1.start(); t2.start();
    try { t1.join(); t2.join(); }
    catch (InterruptedException exn) { 
      System.out.println("Some thread was interrupted");
    }
    
  }
}

class Printer {
  public static void print() {
    synchronized(Printer.class) {
    System.out.print("-");
    
    try {
      Thread.sleep(50); 
    }
    catch (InterruptedException exn) {
      exn.printStackTrace();
    }
    System.out.print("|");
    }
  }
}