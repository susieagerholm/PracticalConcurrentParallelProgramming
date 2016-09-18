import java.util.concurrent.atomic.AtomicReference;


class TestSimpleRWTryLock {
    public static void main(String[] args) {
		//SimpleRWTryLock sl = new SimpleRWTryLock();
		//sl.getCurrentLockHolder();
		//sl.readerTryLock();
		//sl.getCurrentLockHolder();
		//sl.writerUnlock();
		//ReaderList t = new ReaderList(new Thread(), null);
		//ReaderList s = new ReaderList(new Thread(), t);
		//ReaderList r = new ReaderList(new Thread(), s); 
		
	}
	
}

class SimpleRWTryLock { 
	private AtomicReference<Holders> holders = new AtomicReference<Holders>();
	
	public boolean readerTryLock() { 
		final Thread current = Thread.currentThread();
		final Holders hold = holders.get(); //we can store reference to current holder since Writer and Reader are immutable...
		if (hold == null) {
			return holders.compareAndSet(hold, new ReaderList(current, null)); 	
		}
		else if (hold.getClass() == ReaderList.class) {
			return holders.compareAndSet(hold, new ReaderList(current, (ReaderList)hold)); //Maybe I should have copied ReaderList for correctness???
		}
		else {
			return false; //current holder is Writer, access denied!!!
		}
	}
	 
	public void readerUnlock() { 
		final Thread current = Thread.currentThread();
		final Holders hold = holders.get();
		if( hold == ReaderList.getClass()) {
			if(hold.contains(current)) {
				holders.compareAndSet(hold, hold.remove(current));
			}
			else {
				throw new RuntimeException("Current is not on list of current lock holders!!");
			}
		}
		else {
			System.out.println("Unlock was successfull - not lock holder!!!");
			throw new RuntimeException("Not the current lock holder!!");
		}
	
	} 
	
	public boolean writerTryLock() { 
		final Thread current = Thread.currentThread();
		Writer w = new Writer(current);
		return holders.compareAndSet(null, w); //is lock currently not held by anyone, else return false
	} 
	public void writerUnlock() {
		final Thread current = Thread.currentThread();
		final Holder hold = holders.get();
		if (current == hold.thread) {
			holders.compareAndSet(hold, null);
		}
		else {
			throw new RuntimeException("Not the current lock holder!!");
			System.out.println("Unlock was successfull");
		}
			
		
	}
	
	public void getCurrentLockHolder() {
		Holders val = holders.get();
		if (val != null) {
			System.out.println(val);
		}
		else {
			System.out.println("no curr lock holder!!s");
		}
		
	}
	
	private static abstract class Holders {
		
		
	}


	private static class ReaderList extends Holders { 
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
			System.out.println("NEXT IS NOT FOUND - ASK NEXT!!");
			return next.contains(t);
			}
		} 
		
		public ReaderList remove(Thread t) { 
		if (thread == t && !next.contains(t)) {	//first node is the delete node - and node is not contained elsewhere in list...
			return next;
		}
		else {
			ReadList r = next.remove(t);
			if (r == next) 
				return node;
			else 
				return new ReadList();
		}
		
		/*if (node == null) //tjek om der er node - ikke end of bucket
        return null; 
		else if (k.equals(node.k)) { //hvis node har efterspurgte key
			old.set(node.v); //Sæt holderen til at holde værdien af gl. key
        return node.next; //Returner næste node
		} else {
        final ItemNode<K,V> newNode = remove(node.next, k, old); //Recursive call: Send delete videre til næste node
        if (newNode == node.next) 
          return node;
        else 
          return new ItemNode<K,V>(node.k, node.v, newNode);
      }*/
	}

	private static class Writer extends Holders { 
		public final Thread thread; //writer : holds the lock currently
		
		public Writer(Thread t) {
			thread = t; //immutable object, since thread is final...
		}
	}

	}	
}
