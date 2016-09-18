class TestReaderClass {
  public static void main(String[] args) {
	Thread t1 = new Thread();
	System.out.println(t1.toString());
	Thread s1 = new Thread();
	System.out.println(s1.toString());
	Thread r1 = new Thread();	
	System.out.println(r1.toString());
	
	Thread m = new Thread();
	
	ReaderList t = new ReaderList(t1, null);
	ReaderList s = new ReaderList(s1, t);
	ReaderList r = new ReaderList(r1, s); 
	
	//System.out.println(r.contains(r1));
	
	System.out.println(r.remove(t1));
  
  }
  
  private static class ReaderList { 
	private final Thread thread; 
	private final ReaderList next; //list of current readers, that hold the lock...
		
	public ReaderList(Thread t, ReaderList r) {
		this.thread = t;
		this.next = r;
	}
		
	public boolean contains(Thread t) { 
		if (thread == t) {
			System.out.println("FOUND - DO NOT ASK NEXT!!");
				return true;
		}
		else if (next == null) {
			System.out.println("END OF BUCKET - RETURN!!");
			return false;
		}		
		else {
			System.out.println("NEXT IS NULL : NOT FOUND - ASK NEXT!!");
			return next.contains(t);
			}
		} 
		
		public ReaderList remove(Thread t) { 
		int level = 0; 
		if (next == null){
			System.out.println("END OF BUCKET, BUT NOT FOUND - RETURN!!");
			return null;
		}	
		else if (thread == t && !next.contains(t)) {	//first node is the delete node - and node is not contained elsewhere in list...
			System.out.println("REMOVE: DEN ER FUNDET...");
			return next;
		}
		
		else {
			System.out.println("NOT FIRST - NEED TO GO RECURSIVE!!!");
			//level = level - 1;
			ReaderList r = next.remove(t); //found, but it is down in the queue
			System.out.println("LEVEL ER NU: " + level);		
			if (r == next) {
				System.out.println("1");
				return next;
			}	
			else {
				System.out.println("2");
				return new ReaderList(next.thread, r);
			}	
		  }

		}
	}
}