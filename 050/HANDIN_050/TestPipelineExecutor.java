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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.ExecutionException;

public class TestPipelineExecutor {
  private static final ExecutorService executor = Executors.newWorkStealingPool(6); 
  private static List<Future<?>> futures = new ArrayList<Future<?>>();
  
  private static final BlockingQueue<String> urls = new OneItemQueue<String>();
  private static final BlockingQueue<Webpage> pages = new OneItemQueue<Webpage>();
  private static final BlockingQueue<Link> refPairs = new OneItemQueue<Link>();
  private static final BlockingQueue<Link> uniqueLinks = new OneItemQueue<Link>();	
	
  
  public static void main(String[] args) {
	TestPipelineExecutor exe = new TestPipelineExecutor();
	exe.run();		
  }
  
  private static void run() {
	final String[] my_urls = {
    "http://www.itu.dk", 
	"http://www.di.ku.dk", 
	"http://www.miele.de",
    "http://www.microsoft.com", 
	"http://www.amazon.com", 
	"http://www.dr.dk",
    "http://www.vg.no", 
	"http://www.tv2.dk", 
	"http://www.google.com",
    "http://www.ing.dk", 
	"http://www.dtu.dk", 
	"http://www.bbc.co.uk"
    };
	
    for (int i=0; i<my_urls.length; i++) {
	  System.out.println("Before submit to executor!!"); 	
	  final int j = i; 	
	  futures.add(executor.submit(() ->  {
		 urls.put(my_urls[j]);
	  }));	
	  System.out.println(j + "  made it past submit!!" + " No of futures in arr: " + futures.size());
	}
	try {
      for (Future<?> fut : futures)
		  
		//System.out.println("waiting for fut?" + fut.isDone());  
		System.out.println("Get res from fut: " + fut.get());		
   /* } catch (InterruptedException exn) { 
      System.out.println("Interrupted: " + exn);*/
    } catch (Exception exn) { 
      throw new RuntimeException(exn.getCause()); 
    }
	System.out.println("method run to conclusion!!!");
  }
}


class UrlProducer {
  private final BlockingQueue<String> output;	
  
  public UrlProducer(BlockingQueue<String> output) {
    this.output = output;
  }
	
  public void run() { 
    for (int i=0; i<urls.length; i++)
      //output.put(urls[i]);
	  futures.add(executor.submit(() -> output.put(urls[i])));	
	}
  
	//System.out.println(output.size());

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
      //      System.out.println("PageGetter: " + url);
      try { 
        String contents = getPage(url, 200);
        output.put(new Webpage(url, contents));
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
      //      System.out.println("LinkScanner: " + page.url);
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
	private final BlockingQueue<Link> unique_output;
	private final HashSet<Link> unique = new HashSet<Link>();
	
	public Uniquifier(BlockingQueue<Link> input, BlockingQueue<Link> output){
		this.input = input;
		this.output = output;
	}
	
	public void run() { 
    while (true) {
      Link l = input.take();
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
      //      System.out.println("LinkPrinter: " + link.from);
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
}

class OneItemQueue<T> implements BlockingQueue<T> {
  private T item;
  private boolean full = false;

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
    synchronized (this) {
      while (!full) {
        try { this.wait(); } 
        catch (InterruptedException exn) { }
      }
      full = false;
      this.notifyAll();
      return item;
    }
  }
}
