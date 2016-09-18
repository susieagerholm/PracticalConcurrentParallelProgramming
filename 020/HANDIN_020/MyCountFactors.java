class MyCountFactors {

public static void main(String[] args ) {
  int upper = 5_000_000;
  int result = 0;
  for (int i=0; i<upper; i++) {
    result += countFactors(i);
    //System.out.println("i er " + i + " og resultat er" + result);
  }
  System.out.println("Final result from one thread is in: " + result);

}

public static int countFactors(int p) {
    if (p < 2) 
      return 0;
    int factorCount = 1, k = 2;
    while (p >= k * k) {
      if (p % k == 0) {
	factorCount++;
	p /= k;
      } else 
	k++;
    }
    return factorCount;
  }

}

