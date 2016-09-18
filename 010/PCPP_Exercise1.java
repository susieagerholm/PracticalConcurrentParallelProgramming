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

TestPrinter.java
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

3. 

TestPrinterSyncBlock.java
*/


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
Exercise 1.3:
1. Yes - loop continues, because new value set by main is not read by thread due 
to caching.

2. Yes, t terminates as expected. 

3. No, t does not terminate as expected. I guess with only a synchronized set 
method and an un-synchronized get methods the conditions for establishing a 
happened before relationship between invocations of the methods and subsequent 
flushing of values are not met. 

4. Yes t terminates a expected. Volatile keyword makes sure that every use of 
the variable is value field  is preceeded by flushing the cache so that value is 
read from main memory. Volatile is sufficient because t only reads from value - 
so the two threads do not both read and write... 

Exercise 1.4:
1. default: 00.07:38 min

2. 10 threads: 00.03.77 min

3. No. Expected count of 664579, but got 663765...

4. Yes, it will matter because two separate threads are reading and writing to 
the same variable. If this is not safeguarded by locking or other measures, 
lost updates are likely to occur in large datasets because the reading and 
writing of the two threads may interlock... 

*/

 
