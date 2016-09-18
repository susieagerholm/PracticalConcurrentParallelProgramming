/*

Exercise 1.1:

1. Without the synchronization keyword the values generated will differ from the 
expected result of 10.000.000 due to the lost update problem (aka. threads read 
and increment the same value...)

2. Approximately 70% of all runs return the correct count. The software is still 
faulty = unreliable. The conditions of the current test are just not well suited 
to disclose this fact.

3. I did not think, it would make a difference - just small change in syntax, not 
in the evaluation of the program. 

But it actually made a difference with much smaller counts as results if you put 
in counter++; - guess the evaluation of the two statements must be different even 
if they have the same semantic...

4. After both threads terminate the result should be 0. Both the increment and 
the decrement method must be synchronized in order to reach correct result,
because they are still operating on the same value - so the lost update problem 
still applies...

5.
i)    8.753.377
ii)      17.401
iii)     -7.711
iv)           0

Exercise 1.2:
1.
*/

import java.io.IOException;

public class TestPrinter {
  public static void main(String[] args) throws IOException {
    final Printer p = new Printer();
    Thread t1 = new Thread(() -> {
	  while (true)		// Forever call print
	  p.print();
      });
    
    Thread t2 = new Thread(() -> {
	  while (true)		// Forever call print
	  p.print();
      });
    t1.start(); t2.start();
    try { t1.join(); t2.join(); }
    catch (InterruptedException exn) { 
      System.out.println("Some thread was interrupted");
    }
    
  }
}

class Printer {
  public synchronized void print() {
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

/*
Scenario:
t1 enters the print method and prints dash, and goes to sleep
t2 enters the print methods and pints dash, and goes to sleep
t1 or t2 moves on to printing the pipe...  

 
2. If the print method is synchronized, t2 will be prevented from entering the 
print method once t1 has entered. t2 will only be allowed to enter the method,
once t1 has exited it.

3. */
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
    //syncronized block
    synchronized(this.class) {
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

/*
Exercise 3.1:
1. 



 
