// For week 5
// sestoft@itu.dk * 2014-09-23

// A pipeline of transformers connected by bounded queues.  Each
// transformer consumes items from its input queue and produces items
// on its output queue.

// This is illustrated by generating URLs, fetching the corresponding
// webpages, scanning the pages for links to other pages, and printing
// those links; using four threads connected by three queues:

// UrlProducer --(BlockingQueue<String>)--> 
// PageGetter  --(BlockingQueue<Webpage>)--> 
// LinkScanner --(BlockingQueue<Link>)--> 
// LinkPrinter


// For reading webpages
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

// For regular expressions
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import java.util.HashSet;
import java.util.Set;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;

import java.util.List;
import java.util.ArrayList;




public class TestPipeline {
  //final static ExecutorService executor = Executors.newWorkStealingPool(); 
  final static ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
	
  public static void main(String[] args) {
    //runAsThreads();
	runAsExecutor();
  }

  private static void runAsThreads() {
    final BlockingQueue<String> urls = new OneItemQueue<String>();
    final BlockingQueue<Webpage> pages = new OneItemQueue<Webpage>();
    final BlockingQueue<Link> refPairs = new OneItemQueue<Link>();
	final BlockingQueue<Link> uniqueLinks = new OneItemQueue<Link>();
    Thread t1 = new Thread(new UrlProducer(urls));
    Thread t2 = new Thread(new PageGetter(urls, pages));
    Thread t3 = new Thread(new LinkScanner(pages, refPairs));
	Thread t4 = new Thread(new Uniquifier(refPairs, uniqueLinks));
    Thread t5 = new Thread(new LinkPrinter(uniqueLinks));
	//Thread t4 = new Thread(new LinkPrinter(refPairs));
    t1.start(); t2.start(); t3.start(); t4.start(); t5.start(); 
  }
  
  private static void runAsExecutor() {
	ExecutorService executor = Executors.newWorkStealingPool();  
	final BlockingQueue<String> urls = new OneItemQueue<String>();
    final BlockingQueue<Webpage> pages = new OneItemQueue<Webpage>();
    final BlockingQueue<Link> refPairs = new OneItemQueue<Link>();
	final BlockingQueue<Link> uniqueLinks = new OneItemQueue<Link>();
	
	CompletionService<Future<?>> ecs = new ExecutorCompletionService<Future<?>>(executor);
	
	//IMPL WITH EXECUTOR.SUBMIT
	/*futures.add(executor.submit(() -> new UrlProducer(urls)));
	futures.add(executor.submit(() -> new PageGetter(urls, pages)));
	futures.add(executor.submit(() -> new LinkScanner(pages,refPairs)));
	futures.add(executor.submit(() -> new Uniquifier(refPairs, uniqueLinks)));
	futures.add(executor.submit(() -> new LinkPrinter(uniqueLinks)));*/
	
	//IMPL WITH EXECUTOR.EXECUTE
	executor.execute(new UrlProducer(urls));
	executor.execute(new PageGetter(urls, pages));
	executor.execute(new LinkScanner(pages, refPairs));
	executor.execute(new Uniquifier(refPairs, uniqueLinks));
	executor.execute(new LinkPrinter(uniqueLinks));
		
	try {
	 //executor.wait();
	  executor.awaitTermination(1000, TimeUnit.SECONDS);	
	  System.out.println(futures.get(0).get());
	  //if ( futures.get(0).get() instanceof UrlProducer) {
		//System.out.println("URL prod er returneret!! ");
	    
	  //}
	  
	  System.out.println("urls is full: " + urls.isFull());
		
	 //System.out.println(urls.take());	
	 /*for (Future<?> fut : futures) {
		fut.get();		
	  }*/
	} 
	//catch (InterruptedException exn) { 
     //System.out.println("Interrupted: " + exn);
    //} 
	catch (Exception exn) { 
      throw new RuntimeException(exn.getCause()); 
    }
	
	
  }
}

class UrlProducer implements Runnable {
  private final BlockingQueue<String> output;

  public UrlProducer(BlockingQueue<String> output) {
    this.output = output;
  }

  public void run() { 
    for (int i=0; i<urls.length; i++) {
		System.out.println("URL producer adding new url to urls");	
      output.put(urls[i]);

	}
	    }

  private static final String[] urls = 
  { "http://www.itu.dk", "http://www.di.ku.dk", "http://www.miele.de",
    "http://www.microsoft.com", "http://www.amazon.com", "http://www.dr.dk",
    "http://www.vg.no", "http://www.tv2.dk", "http://www.google.com",
    "http://www.ing.dk", "http://www.dtu.dk", "http://www.bbc.co.uk"
  };
}

class PageGetter implements Runnable {
  private final BlockingQueue<String> input;
  private final BlockingQueue<Webpage> output;

  public PageGetter(BlockingQueue<String> input, BlockingQueue<Webpage> output) {
    this.input = input;
    this.output = output;
  }

  public void run() { 
    while (true) {
      String url = input.take();
      System.out.println("PageGetter is runnning: " + url);
      try { 
        String contents = getPage(url, 200);
		System.out.println("PageGetter has received contents: " + url);
        output.put(new Webpage(url, contents));
		System.out.println("PageGetter has put contents in queue for linkscanner: " + url);
        
      } catch (IOException exn) { System.out.println(exn); }
    }
  }

  public static String getPage(String url, int maxLines) throws IOException {
    // This will close the streams after use (JLS 8 para 14.20.3):
    try (BufferedReader in 
         = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
      StringBuilder sb = new StringBuilder();
      for (int i=0; i<maxLines; i++) {
        String inputLine = in.readLine();
        if (inputLine == null)
          break;
        else
        sb.append(inputLine).append("\n");
      }
      return sb.toString();
    }
  }
}

class LinkScanner implements Runnable {
  private final BlockingQueue<Webpage> input;
  private final BlockingQueue<Link> output;

  public LinkScanner(BlockingQueue<Webpage> input, 
                     BlockingQueue<Link> output) {
    this.input = input;
    this.output = output;
  }

  private final static Pattern urlPattern 
    = Pattern.compile("a href=\"(\\p{Graph}*)\"");

  public void run() { 
    while (true) {
      Webpage page = input.take();
            System.out.println("LinkScanner is finding links on: " + page.url);
      // Extract links from the page's <a href="..."> anchors
      Matcher urlMatcher = urlPattern.matcher(page.contents);
      while (urlMatcher.find()) {
        String link = urlMatcher.group(1);
        output.put(new Link(page.url, link));
      }
    }
  }
}

class Uniquifier implements Runnable {
	private final BlockingQueue<Link> input;
	private final BlockingQueue<Link> output;
	private final HashSet<Link> unique = new HashSet<Link>();
	
	public Uniquifier(BlockingQueue<Link> input, BlockingQueue<Link> output){
		this.input = input;
		this.output = output;
	}
	
	public void run() { 
    while (true) {
      Link l = input.take();
	  System.out.println("Uniquifier is testing uniqueness of link " + l.from);
      if(unique.add(l)) output.put(l);
	}
  }
}

class LinkPrinter implements Runnable {
  private final BlockingQueue<Link> input;

  public LinkPrinter(BlockingQueue<Link> input) {
    this.input = input;
  }

  public void run() { 
    while (true) {
      Link link = input.take();
      System.out.println("LinkPrinter: " + link.from);
      System.out.printf("%s links to %s%n", link.from, link.to);
    }
  }
}


class Webpage {
  public final String url, contents;
  public Webpage(String url, String contents) {
    this.url = url;
    this.contents = contents;
  }
}

class Link {
  public final String from, to;
  public Link(String from, String to) {
    this.from = from;
    this.to = to;
  }

  // Override hashCode and equals so can be used in HashSet<Link>

  public int hashCode() {
    return (from == null ? 0 : from.hashCode()) * 37
         + (to == null ? 0 : to.hashCode());
  }

  public boolean equals(Object obj) {
    Link that = obj instanceof Link ? (Link)obj : null;
    return that != null 
      && (from == null ? that.from == null : from.equals(that.from))
      && (to == null ? that.to == null : to.equals(that.to));
  }
}

// Different from java.util.concurrent.BlockingQueue: Allows null
// items, and methods do not throw InterruptedException.

interface BlockingQueue<T> {
  void put(T item);
  T take();
  boolean isFull();
}

class OneItemQueue<T> implements BlockingQueue<T> {
  private T item;
  private boolean full = false;
  
  public boolean isFull() {
	  return full;
  }

  public void put(T item) {
  
    synchronized (this) {
      while (full) {
        try { this.wait(); } 
        catch (InterruptedException exn) { }
      }
      full = true;
      this.item = item;
      this.notifyAll();
    }
  }

  public T take() {
    System.out.println("trying to take from queue...");
	synchronized (this) {
      while (!full) {
        try { this.wait(); } 
        catch (InterruptedException exn) { }
      }
	  System.out.println("succeeded in taking from queue!!");
      full = false;
      this.notifyAll();
      return item;
    }
  }
}
