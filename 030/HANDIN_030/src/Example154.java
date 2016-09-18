// Example 154 from page 123 of Java Precisely third edition (The MIT Press 2016)
// Author: Peter Sestoft (sestoft@itu.dk)
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

class Example154 {
  public static void main(String[] args) { 
    FunList<Integer> empty = new FunList<>(null),
      list1 = cons(9, cons(13, cons(0, empty))),                  // 9 13 0       
      list2 = cons(7, list1),                                     // 7 9 13 0     
      list3 = cons(8, list1),                                     // 8 9 13 0     
      list4 = list1.insert(1, 12),                                // 9 12 13 0    
      list5 = list2.removeAt(3),                                  // 7 9 13       
      list6 = list5.reverse(),                                    // 13 9 7       
      list7 = list5.append(list5),                                // 7 9 13 7 9 13
	  list88 = list7.remove(7),
      list10 = list7.filter((n)-> n < 12),
      list11 = list7.removeFun(9);
      int counter = list88.count((n)-> n == 13);
    
	System.out.println(IntStream.range(0, 1_000_000).filter(i -> isPrime(i)).count());
	System.out.println("list 1: " + list1);
    System.out.println("list 2: " + list2);
    System.out.println("list 3: " + list3);
    System.out.println("list 4: " + list4);
    System.out.println("list 5: " + list5);
    System.out.println("list 6: " + list6);
    System.out.println("list 7: " + list7);
	
	System.out.println("her er min løsning til 3.1.1");
	System.out.println("List 8:" + list88);
	System.out.println("her er min løsning til 3.1.2");
	System.out.println("List 9:" + counter);
	System.out.println("her er min løsning til 3.1.3");
	System.out.println("List 10:" + list10);
	System.out.println("her er min løsning til 3.1.4");
	System.out.println("List 11:" + list11);
	
	System.out.println("her er min loesing til 3.1.5");
	FunList fof = new FunList(null);
	fof.append(list4);
	fof.append(list5);
	fof.append(list6);
	//System.out.println("List 11:" + fof.flatten(fof));
	
	
    FunList<Double> list8 = list5.map(i -> 2.5 * i);              // 17.5 22.5 32.5
    System.out.println(list8); 
    double sum = list8.reduce(0.0, (res, item) -> res + item),    // 72.5
       product = list8.reduce(1.0, (res, item) -> res * item);    // 12796.875
    System.out.println(sum);
    System.out.println(product);
  }

  public static <T> FunList<T> cons(T item, FunList<T> list) { 
    return list.insert(0, item);
  }
  
  public static boolean isPrime(int i ) {return true;}
}

class FunList<T> {
  //reference til første Node i stream
  final Node<T> first;
  //final Tester<T> pred;
  
  //inner class Node	
  protected static class Node<U> {
    public final U item;
    public final Node<U> next;

    public Node(U item, Node<U> next) {
      this.item = item; 
      this.next = next; 
    }
  }

  
  //constructors for FunList 
  public FunList (Node<T> xs) {    
    this.first = xs;
  }

  public FunList() { 
    this(null);
  }
  
  //Exercise 3.1.1 : KORREKT
  
  public FunList<T> remove(T x) {
	  return new FunList<T>(removeAll(x, this.first));
  }
  
  protected <T> Node<T> removeAll(T x, Node<T> xs) {
    //check if node must be removed- if yes: move on to next node, if no: copy node and move on to next node... 
	return xs == null ?  null : x == xs.item ? removeAll(x, xs.next) : new Node<T>(xs.item, removeAll(x, xs.next));
  }
  
  //Exercise 3.1.2 KORREKT
  public int count(Predicate<T> p) { 
	  Node<T> xs = this.first;
	  int count = 0;
	  //Make recursive call to increment counter every time the predicate is met by current node...
	  return testAndIncrement(p, xs, count);
  } 
  
  public int testAndIncrement(Predicate<T> p, Node<T> xs, int counter) {
	  return xs == null ? counter : p.test(xs.item) ? testAndIncrement(p, xs.next, counter+=1) : testAndIncrement(p, xs.next, counter); 
  }

  
  //Exercise 3.1.3 : KORREKT
  public FunList<T> filter(Predicate<T> p) {
	  return new FunList<T>(filterAllOnPred(p, this.first));
  }
  
  protected <T> Node<T> filterAllOnPred(Predicate<T> p, Node<T> xs) {
	  //first check if any node is passed. Then test if node has given predicate. If yes copy current node and send call to next node, if not ignore current node...
	  return xs == null ? null : p.test(xs.item) ? new Node<T>(xs.item, filterAllOnPred(p, xs.next)) : filterAllOnPred(p, xs.next);
  }
  
  
  //Exercise 3.1.4 : KORREKT 
  public FunList<T> removeFun(T x) {
	  return this.filter(n -> n != x);
  }
  
  //Exercise 3.1.5 : WORKINPROGRESS
  //public static <T> FunList<T> flatten(FunList<FunList<T>> xss) {
	//  return xss.flatten(xss);
  //}
  
  //Exercise 3.1.6  
  //public FunList<T> flattenFun(FunList<FunList<T>> xss) {
	  //- using reduce, lambda expression and append...
	//  FunList<T> ft = new FunList<T>();
	  //return this.reduce(ft, ft.append(xss.get(0)));//append
  //}
  
  //Exercise 3.1.7  
  public <U> FunList<U> flatMap(Function<T,FunList<U>> f) {
	  return new FunList<U>();
  } 
  
  //Exercise 3.1.8 
  public FunList<T> scan(BinaryOperator<T> f) {
	  return new FunList<T>();
  } 

  
  
  
  public int getCount() {
    Node<T> xs = first;
    int count = 0;
    while (xs != null) {
      xs = xs.next;
      count++;
    }
    return count;
  }

  public T get(int i) {
    return getNodeLoop(i, first).item;
  }

  // Loop-based version of getNode
  protected static <T> Node<T> getNodeLoop(int i, Node<T> xs) {
    while (i != 0) {
      xs = xs.next;
      i--;
    }
    return xs;    
  }

  // Recursive version of getNode
  protected static <T> Node<T> getNodeRecursive(int i, Node<T> xs) {    // Could use loop instead
    return i == 0 ? xs : getNodeRecursive(i-1, xs.next);
  }

  public static <T> FunList<T> cons(T item, FunList<T> list) { 
    return list.insert(0, item);
  }

  public FunList<T> insert(int i, T item) { 
    return new FunList<T>(insert(i, item, this.first));
  }

  protected static <T> Node<T> insert(int i, T item, Node<T> xs) { 
	//hvis sand: Indsæt ny node og sæt gamle node til næste node. Hvis falsk: Opret node ud fra aktuelle item og kald resursivt for næste
    return i == 0 ? new Node<T>(item, xs) : new Node<T>(xs.item, insert(i-1, item, xs.next));
  }

  public FunList<T> removeAt(int i) {
	//  
    return new FunList<T>(removeAt(i, this.first));
  }

  protected static <T> Node<T> removeAt(int i, Node<T> xs) {
    //tjek om dette er node, som ønskes fjernet - hvis ja: gå videre til næste node, hvis nej: nyt recursivt kalde og decerement tæller, så i nærmer sig 0! 
	return i == 0 ? xs.next : new Node<T>(xs.item, removeAt(i-1, xs.next));
  }

  public FunList<T> reverse() {
    Node<T> xs = first, reversed = null;
    while (xs != null) {
      reversed = new Node<T>(xs.item, reversed);
      xs = xs.next;      
    }
    return new FunList<T>(reversed);
  }

  public FunList<T> append(FunList<T> ys) {
    return new FunList<T>(append(this.first, ys.first));
  }

  protected static <T> Node<T> append(Node<T> xs, Node<T> ys) {
    return xs == null ? ys : new Node<T>(xs.item, append(xs.next, ys));
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object that) {
    return equals((FunList<T>)that);             // Unchecked cast
  }

  public boolean equals(FunList<T> that) {
    return that != null && equals(this.first, that.first);
  }

  // Could be replaced by a loop
  protected static <T> boolean equals(Node<T> xs1, Node<T> xs2) {
    return xs1 == xs2 
        || xs1 != null && xs2 != null && xs1.item == xs2.item && equals(xs1.next, xs2.next);
  }

  public <U> FunList<U> map(Function<T,U> f) {
    return new FunList<U>(map(f, first));
  }

  protected static <T,U> Node<U> map(Function<T,U> f, Node<T> xs) {
    return xs == null ? null : new Node<U>(f.apply(xs.item), map(f, xs.next));
  }

  public <U> U reduce(U x0, BiFunction<U,T,U> op) {
    return reduce(x0, op, first);
  }

  // Could be replaced by a loop
  protected static <T,U> U reduce(U x0, BiFunction<U,T,U> op, Node<T> xs) {
    return xs == null ? x0 : reduce(op.apply(x0, xs.item), op, xs.next);
  }

  // This loop is an optimized version of a tail-recursive function 
  public void forEach(Consumer<T> cons) {
    Node<T> xs = first;
    while (xs != null) {
      cons.accept(xs.item);
      xs = xs.next;
    }
  }

  @Override 
  public String toString() {
    StringBuilder sb = new StringBuilder();
    forEach(item -> sb.append(item).append(" "));
    return sb.toString();
  }
}

