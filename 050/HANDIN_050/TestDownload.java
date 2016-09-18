// For week 5
// sestoft@itu.dk * 2014-09-19

import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.concurrent.Future;



public class TestDownload {
  private static final ExecutorService executor //= Executors.newWorkStealingPool();
    = Executors.newCachedThreadPool();
  	
  private static final String[] urls = 
  { "http://www.itu.dk", "http://www.di.ku.dk", "http://www.miele.de",
    "http://www.microsoft.com", "http://www.amazon.com", "http://www.dr.dk",
    "http://www.vg.no", "http://www.tv2.dk", "http://www.google.com",
    "http://www.ing.dk", "http://www.dtu.dk", "http://www.eb.dk", 
    "http://www.nytimes.com", "http://www.guardian.co.uk", "http://www.lemonde.fr",   
    "http://www.welt.de", "http://www.dn.se", "http://www.heise.de", "http://www.wsj.com", 
    "http://www.bbc.co.uk", "http://www.dsb.dk", "http://www.bmw.com", "https://www.cia.gov" 
  };

  public static void main(String[] args) throws IOException {
	
	//Exercise 5.3.1
	/*String url = "http://www.miele.de";
    String page = getPage(url, 10);
	int page_size = page.length();
	System.out.println("Content " + page_size);
    System.out.printf("%-30s%n%s%n", url, page);*/
	
	//Exercise 5.3.2 + 5.3.3
	//System.out.printf("%6.1f ns%n", getPages(urls, 200));
	
	//Exercise 5.3.4
	getPagesParallel(urls);
	//getPagesParallel(urls);
	//getPagesParallel(urls);
	//getPagesParallel(urls);
	//getPagesParallel(urls);
	
	//Map<String, String> hm = getPages(urls, 10);
	//Map<String, String> hm = getPagesParallel(urls);
	 
  }
  
  private static void printMap(Map<String, String> hm) {
	 //Get a set of the entries
      Set set = hm.entrySet();
	  System.out.println(hm.size());
      // Get an iterator
      Iterator i = set.iterator();
		 while(i.hasNext()) {
         Map.Entry me = (Map.Entry)i.next();
         System.out.print(me.getKey() + ": ");
         System.out.println(me.getValue());
	  }	  
  }

  public static String getPage(String url, int maxLines) throws IOException {
    // This will close the streams after use (JLS 8 para 14.20.3):
    try (BufferedReader in 
         = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
      StringBuilder sb = new StringBuilder();
      for (int i=0; i<maxLines; i++) {
		  //System.out.println("inside for loop :" + i );
        String inputLine = in.readLine();
        if (inputLine == null) {
			//System.out.println("inputline is null");
          break;		
		}
        else
          sb.append(inputLine).append("\n");
      }
      return sb.toString();
    }
  }
  
  //Exercise 5.3.2
  public static Map<String, String> getPages(String[] myUrls, int maxLines) {
	final HashMap<String, String> myHashMap = new HashMap<String, String>();
    Timer t = new Timer();   
	for ( String url : myUrls ) {
		try {
			//fetch page
			String page = getPage(url, 10);
			// put url in map
			myHashMap.put(url, page);
		}
		catch (Exception e){
			e.printStackTrace();	
		}
	}
	double time = t.check() * 1e6 / myUrls.length;
	System.out.printf("%6.1f us%n", time); 
	System.out.println(myHashMap.size()); 
	return myHashMap;

	}
	
	//Exercise 5.3.4
	public static Map<String, String> getPagesParallel(String[] myUrls) {
		final HashMap<String, String> myHashMap = new HashMap<String, String>();
		Map<String, Future<String>> futures = new HashMap<String, Future<String>>(); 
		Timer t = new Timer(); 
		for ( String url : myUrls ) {
			//create future for exec
			final String url1 = url;
			futures.put(url, executor.submit( () -> getPage(url1, 200)
			));
		}
		try { 
			Iterator it = futures.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();
				Future fut = (Future)pair.getValue();
				String webpage = (String)fut.get();
				myHashMap.put((String)pair.getKey(), webpage);				
			}
		}
		catch (Exception exn) { 
				System.out.println(exn); 
		}  
	double time = t.check() * 1e6 / myUrls.length;
	System.out.printf("%6.1f us%n", time);
	System.out.println(myHashMap.size());
	
	//printMap(myHashMap);
	//return time;
	return myHashMap;		
	}
}
