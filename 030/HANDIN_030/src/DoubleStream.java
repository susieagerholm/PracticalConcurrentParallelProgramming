import java.util.stream.IntStream;

public class DoubleStream {
	
	//Result should be  21.300481501347942
	public static void main(String[] args) {
		
	//Exercise 3.4.1
	System.out.println("Exercise 3.4.1");
	System.out.printf("Sum = %20.16f%n", IntStream.range(1, 1_000_000_000).mapToDouble(n -> 1.0/n).reduce(0.0, (a, b) -> a + b));
	
	//Exercise 3.4.2
	System.out.println("Exercise 3.4.2");
	System.out.printf("Sum = %20.16f%n", IntStream.range(1, 1_000_000_000).parallel().mapToDouble(n -> 1.0/n).reduce(0.0, (a, b) -> a + b));
	
	//Exercise 3.4.3
	System.out.println("Exercise 3.4.3");
	double counter = 0.0; 
	for(int i = 1; i < 1_000_000_000; i++) {
		Double step = 1.0 / i; 
		//System.out.println(step);
		counter = counter + step;
		
		
	}
	System.out.println("Sum =  " + counter);
	
	
	//Exercise 3.4.4
	System.out.println("Exercise 3.4.4");
	//IntStream is = IntStream.generate(() -> my++);
	//is.limit(999_999_999).mapToDouble(n -> 1.0/n).reduce(0.0, (a, b) -> a + b);
	//is.limit(999_999_999);
	//System.out.printf("Sum = %20.16f%n", IntStream.generate(IntSupplier)
	
	
	}
	

}
