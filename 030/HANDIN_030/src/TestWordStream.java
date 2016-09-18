// Week 3
// sestoft@itu.dk * 2015-09-09

import java.io.BufferedReader;
import java.io.File;
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



public class TestWordStream {
  public static void main(String[] args) {
	File f = new File("C:\\Users\\SusieAgerholm\\Desktop\\PCPP\\HANDIN3\\words");  
    String filename = f.getName();
	//Exercise 3.1.1
    System.out.println("Exercise 3.3.1.: Antal ord i filen: ");
    System.out.println(readWords(filename).count());
    System.out.println("Exercise 3.3.2.: De første 100 ord: ");
	//printFirst100Words(readWords(filename));
	System.out.println("Exercise 3.3.3.: Alle ord med 22 bogstaver eller flere: ");
	//printAllWordsWith22LettersOrMore(readWords(filename));
	System.out.println("Exercise 3.3.4.: Tilfældigt ord med 22 bogstaver eller flere: ");
	//printAnyWordWith22LettersOrMore(readWords(filename));
	System.out.println("Exercise 3.3.5.: Print alle palindromer - sequential impl.: ");
	//printAllPalindromesSequential(readWords(filename));
	System.out.println("Exercise 3.3.6.: Print alle palindromer - parallel impl.: ");
	//printAllPalindromesParallel(readWords(filename));
	System.out.println("Exercise 3.3.7.: Print min, max og average");
	System.out.println("Min. antal bogstaver i et ord: "); 
	//mapToStringLengthAndPrintMin(readWords(filename));
	System.out.println("Max. antal bogstaver i et ord: ");
	//mapToStringLengthAndPrintMax(readWords(filename));
	System.out.println("Gennemsnitligt antal bogstaver i et ord: ");
	//mapToStringLengthAndPrintAverage(readWords(filename));
	System.out.println("Exercise 3.3.8.: Grupper ord efter længde: "); 
	//groupByStringLengthAndPrintGroups(readWords(filename));
	System.out.println("Exercise 3.3.9.: Grupper ord efter længde: "); 
	//mapToTreeMapAndPrintFirst100(readWords(filename));
	System.out.println("Exercise 3.3.10.: Tæl antal gange bogstavet 'e' er brugt: "); 
	//countNoOfTimesALetterIsUsed(readWords(filename));
	System.out.println("Exercise 3.3.11.: Tæl antal anagrammer: "); 
	groupAllAnagramsSequentialImpl(readWords(filename));	
	//REMEMBER TO CLOSE READER AT SOME POINT!!!
	
	//isPalindrome("Otto");
  }
  //Exercise 3.3.1 : DONE
  public static Stream<String> readWords(String filename) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(filename));
      // TO DO: Implement properly
	  return reader.lines(); 
    } catch (IOException exn) { 
      return Stream.<String>empty();
    }
  }
  
  //Exercise 3.3.2 : DONE
  public static void printFirst100Words(Stream<String> s) {
	s.limit(100).forEach(System.out::println);  
  }
  
  //Exercise 3.3.3 : DONE
  public static void printAllWordsWith22LettersOrMore(Stream<String> s) {
	s.filter(x -> x.length() >= 22).forEach(System.out::println);  
  }
  //Exercise 3.3.4 : DONE
  public static void printAnyWordWith22LettersOrMore(Stream<String> s) {
	System.out.println(s.filter(x -> x.length() >= 22).findAny());
  }
  
  //Exercise 3.3.5: : DONE
  public static void printAllPalindromesSequential(Stream<String> s) {
	System.out.println(convertTime(System.currentTimeMillis()));
	s.filter(x -> isPalindrome(x)).forEach(System.out::println);
	System.out.println(convertTime(System.currentTimeMillis()));
  }
  
  //Exercise 3.3.6 : DONE
  //Difference between sequential and parallel implementation is very small -
  //aroung 10-20 ms (probably due to overhead)
   public static void printAllPalindromesParallel(Stream<String> s) {
	System.out.println(convertTime(System.currentTimeMillis()));   
	s.parallel().filter(x -> isPalindrome(x)).forEach(System.out::println);
	System.out.println(convertTime(System.currentTimeMillis()));
  }
  
  //Exercise 3.3.7 : DONE
  //Hopefully these were not expected to be solve within one method...
   public static void mapToStringLengthAndPrintMin(Stream<String> s) {
	System.out.println(s.map(x -> x.length()).min((p1, p2) -> Integer.compare(p1, p2)).get());
   }
  
   public static void mapToStringLengthAndPrintMax(Stream<String> s) {
	System.out.println(s.map(x -> x.length()).max((p1, p2) -> Integer.compare(p1, p2)).get());
   }
  
   public static void mapToStringLengthAndPrintAverage(Stream<String> s) {
	System.out.println(s.collect(Collectors.averagingInt(x -> ((String) x).length())));
   }
  
   //Exercise 3.3.8 : DONE... OPTIONAL AS OVERRIDE OF GROUPING?
   public static void groupByStringLengthAndPrintGroups(Stream<String> s) {
	   Map<Integer, List<String>> m = s.collect(Collectors.groupingBy(x -> x.length())); 
	   for( Integer my_i : m.keySet() ) {
		   System.out.println("No words in this group: " + m.get(my_i).size());
		   System.out.println("				Iterating over group..." + my_i);
				 List<String> my_list = m.get(my_i);
				 for (String ss : my_list) {
					 System.out.println(ss);
				 }
		} 
   }
  
	//Exercise 3.3.9 : DONE
   public static void mapToTreeMapAndPrintFirst100(Stream<String> s) {
		s.map(x -> letters(x)).limit(100).forEach(System.out::println);
   }
  
  //Exercise 3.3.10 DONE 
	public static void countNoOfTimesALetterIsUsed(Stream<String> s) {
		System.out.println(s.reduce(0, (sum, x) -> sum += countNoLetterE(x), (sum1, sum2) -> sum1 + sum2));
   }
  //Exercise 3.3.11 MISSING
  public static void groupAllAnagramsSequentialImpl(Stream<String> s) {
	  System.out.println(s.collect(Collectors.groupingBy(x -> letters((String)x))).size()); 
	  //System.out.println(s.collect(Collectors.groupingBy(x -> x.length()));
  }
  
  //Exercise 3.3.12 MISSING 
  public static void groupAllAnagramsParallelImpl() {
	  
  }
  
  //public static void isAnagram();
  
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
		}
		//Increment key if needed...
		else {
			count = res.get(key);
			added = count + 1;
			
			res.put(key, added);	
		}
	}
	System.out.println("Word:" + s);
	
    return res;
  }
  public static int countNoLetterE(String s) {
	  // TO DO: Implement properly
	  int s_length = s.length();
	  int count = 0;
	  for (int i = 0; i <= s_length - 1; i++ ) {
		//Hent den aktuelle count value på denne key
		Character key = new Character(s.toLowerCase().charAt(i));
		//Initialize key if needed
			if (key == 'e') {
				count++;
			}
	  }
	 //System.out.println("Antal bogstavet 'e' i ordet " + s); 
	 return count;
  }	  
  
  /*public static Map<Character,Integer> anagrams(String s1, String s2) {
	 if(letters(s1).equals(letters(s2))) {
		 return Map<Character,Integer>;
	 }
	 else {
		 return false;
	 }
	 
  }*/
}
