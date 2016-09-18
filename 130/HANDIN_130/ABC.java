import java.util.Random; 
import java.io.*; 
import akka.actor.*;


// -- MESSAGES --------------------------------------------------
class StartTransferMessage implements Serializable { 
	/* TODO */ 
	 public final ActorRef bank, from, to;
     public StartTransferMessage(ActorRef bank, ActorRef from, ActorRef to) {  
		this.bank = bank;
		this.from = from;
		this.to = to;
	} 
}

class TransferMessage implements Serializable { 
	/* TODO */ 
	public final ActorRef from, to;
	public TransferMessage(ActorRef from, ActorRef to) {
		this.from = from;
		this.to = to;
	}
}

class DepositMessage implements Serializable { 
	/* TODO */ 
	public final int amount;
	public DepositMessage(int amount) {
		this.amount = amount;
	}
}

class PrintBalanceMessage implements Serializable { 
	/* TODO */ 
	public PrintBalanceMessage() {
		//System.out.println("---- PRINTING OUT BALANCE ----");
	}
}

// -- ACTORS --------------------------------------------------

class AccountActor extends UntypedActor { 
	private int curr_amount;
	
	/* TODO */ 
	public void onReceive(Object dep) throws Exception {
		if (dep instanceof DepositMessage) {
			DepositMessage deposit = (DepositMessage) dep;
			int amount = deposit.amount;
			curr_amount += amount; 	
		}
		if (dep instanceof PrintBalanceMessage) {
			System.out.println(this + ": Final amount after all transactions = " + curr_amount);
		}
	}	
}

class BankActor extends UntypedActor { 
	private Random rand = new Random();
	
	/* TODO */ 
	public void onReceive(Object trans) throws Exception {
		if (trans instanceof TransferMessage) {
			TransferMessage transfer = (TransferMessage) trans;
			ActorRef from = transfer.from;
			ActorRef to = transfer.to;
			int amount = rand.nextInt(100);
			if (amount == 0) {
				amount = 1;
			}
			int withdraw = -1 * amount;
			from.tell(new DepositMessage(withdraw), ActorRef.noSender());
			to.tell(new DepositMessage(amount), ActorRef.noSender());
		}
	}
	
}
class ClerkActor extends UntypedActor { 
	private int N = 10; 
	
	/* TODO */ 
	public void onReceive(Object starter) throws Exception {
		if (starter instanceof StartTransferMessage) {
			StartTransferMessage start = (StartTransferMessage) starter;
			ActorRef bank = start.bank;
			ActorRef from = start.from;
			ActorRef to = start.to;
			nTransfers(N, bank, from, to);
		}
	}
	
	public void nTransfers(int no, ActorRef bank, ActorRef from, ActorRef to) {
		if(no == 0) {
			System.out.println("Transfer is done!!!");
		}
		else {
			bank.tell(new TransferMessage(from,to), ActorRef.noSender());
			nTransfers((no - 1), bank, from, to);
		}
	}
	
}

// -- MAIN --------------------------------------------------
public class ABC { // Demo showing how things work:
	public static void main(String[] args) {
		final ActorSystem system = ActorSystem.create("ABCSystem");
 
		/* TODO (CREATE ACTORS AND SEND START MESSAGES) */
		final ActorRef A1 = system.actorOf(Props.create(AccountActor.class), "myaccount1");
		final ActorRef A2 = system.actorOf(Props.create(AccountActor.class), "myaccount2");
		final ActorRef B1 = system.actorOf(Props.create(BankActor.class), "mybank1");
		final ActorRef B2 = system.actorOf(Props.create(BankActor.class), "mybank2");	
		final ActorRef C1 = system.actorOf(Props.create(ClerkActor.class), "myclerk1");
		final ActorRef C2 = system.actorOf(Props.create(ClerkActor.class), "myclerk2");		
		
		C1.tell(new StartTransferMessage(B1,A1,A2), ActorRef.noSender());
		C2.tell(new StartTransferMessage(B2,A2,A1), ActorRef.noSender());			

		try {
			//REMEMBER TO SLEEP FOR 1000ms...
			Thread.sleep(1000);
			
			System.out.println("Press return to inspect...");
			System.in.read();
 
			/* TODO (INSPECT FINAL BALANCES) */
			A1.tell(new PrintBalanceMessage(), ActorRef.noSender());
			A2.tell(new PrintBalanceMessage(), ActorRef.noSender());
			
					
			System.out.println("Press return to terminate...");
			System.in.read();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			system.shutdown();
		}
	}
} 