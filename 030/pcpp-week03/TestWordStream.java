// Week 3
// sestoft@itu.dk * 2015-09-09

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.Format;
import java.util.*;



public class TestWordStream2 {
  public static void main(String[] args) {
    String filename = "words.txt";
    //System.out.println(readWords(filename).count());
	System.out.println("Test current exercise!!!: ");
	//printFirst100Words(readWords(filename));
	//printAllWordsWith22LettersOrMore(readWords(filename));
	//printAnyWordWith22LettersOrMore(readWords(filename));
	//printAllPalindromesSequential(readWords(filename));
	//printAllPalindromesParallel(readWords(filename));
	//mapToStringLengthAndPrintMin(readWords(filename));
	//mapToStringLengthAndPrintMax(readWords(filename));
	//mapToStringLengthAndPrintAverage(readWords(filename));
	//groupByStringLengthAndPrint(readWords(filename));
	//letters("HelLLLLLLLLLLLLLLLLLLLLLLLLLLLLlos");
	mapToTreeMapAndPrintFirst100(readWords(filename));
	
	
	
	//isPalindrome("Otto");
  }
  //Exercise 3.3.1 : KORREKT
  public static Stream<String> readWords(String filename) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(filename));
      // TO DO: Implement properly
	  return reader.lines(); 
    } catch (IOException exn) { 
      return Stream.<String>empty();
    }
  }
  
  //Exercise 3.3.2 : KORREKT
  public static void printFirst100Words(Stream<String> s) {
	s.limit(100).forEach(System.out::println);  
  }
  
  //Exercise 3.3.3 : KORREKT
  public static void printAllWordsWith22LettersOrMore(Stream<String> s) {
	s.filter(x -> x.length() >= 22).forEach(System.out::println);  
  }
  //Exercise 3.3.4 : KORREKT
  public static void printAnyWordWith22LettersOrMore(Stream<String> s) {
	System.out.println(s.filter(x -> x.length() >= 22).findAny());
  }
  
  //Exercise 3.3.5: : KORREKT
  public static void printAllPalindromesSequential(Stream<String> s) {
	System.out.println(convertTime(System.currentTimeMillis()));
	s.filter(x -> isPalindrome(x)).forEach(System.out::println);
	System.out.println(convertTime(System.currentTimeMillis()));
  }
  
  //Exercise 3.3.6 : KORREKT
  //Difference between sequential and parallel implementation is very small -
  //aroung 10-20 ms (probably due to overhead)
   public static void printAllPalindromesParallel(Stream<String> s) {
	System.out.println(convertTime(System.currentTimeMillis()));   
	s.parallel().filter(x -> isPalindrome(x)).forEach(System.out::println);
	System.out.println(convertTime(System.currentTimeMillis()));
  }
  
  //Exercise 3.3.7 : KORREKT?
  //Hopefully these were not expected to be solve within one method...
   public static void mapToStringLengthAndPrintMin(Stream<String> s) {
	System.out.println(s.map(x -> x.length()).min((p1, p2) -> Integer.compare(p1, p2)).get());
   }
  
   public static void mapToStringLengthAndPrintMax(Stream<String> s) {
	System.out.println(s.map(x -> x.length()).max((p1, p2) -> Integer.compare(p1, p2)).get());
   }
  
   public static void mapToStringLengthAndPrintAverage(Stream<String> s) {
	System.out.println(s.collect(Collectors.averagingInt(x -> x.length())));
   }
  
   /*Exercise 3.3.8 : IKKE KORREKT...
   public static void groupByStringLengthAndPrintGroups(Stream<String> s) {
	 Collectors	coll = s.collect(Collectors.groupingBy(x -> x.length()));
	 for (Collection<String> x : coll) {
		 for (String y : x) {
			 System.out.println(y);
		 }
	 } 	
   }*/
  
	//Exercise 3.3.9 : IKKE KORREKT
	public static void mapToTreeMapAndPrintFirst100(Stream<String> s) {
	System.out.println(s.map(x -> letters(x)));
   }
  
  //Exercise 3.3.10  
  //Exercise 3.3.11 
  //Exercise 3.3.12  
  
  public static String convertTime(long time){
    Date date = new Date(time);
    Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss.SSSZ");
    return format.format(date);
}

  public static boolean isPalindrome(String s) {
    // TO DO: Implement properly
	int no_loops = s.length() / 2;
	int limit = s.length() - 1;
    for(int i = 0; i < no_loops; i++) {
		if (s.toUpperCase().charAt(i) != s.toUpperCase().charAt(limit - i)) {
			return false;
		}
	}
	//System.out.println("PALINDROME FOUND!!!");
	return true; 
  }

  public static Map<Character,Integer> letters(String s) {
    Map<Character,Integer> res = new TreeMap<>();
    // TO DO: Implement properly
	int s_length = s.length();
	int count, added;
	for (int i = 0; i <= s_length - 1; i++ ) {
		//Hent den aktuelle count value på denne key
		Character key = new Character(s.toLowerCase().charAt(i));
		//Initialize key if needed
		if (res.get(key) == null) {
			res.put(key, 1);
			System.out.println("initializing " + key);
		}
		//Increment key if needed...
		else {
			count = res.get(key);
			System.out.println("Current count of Key " + key + ":" + count);
			added = count + 1;
			System.out.println("Current count of Key " + key + ":" + count);
			
			res.put(key, added);	
		}
	}
	//System.out.println(res.size());
	//System.out.println(res.entrySet());
    return res;
  }
}
