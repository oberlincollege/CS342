import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class RDT22 implements Runnable {
	private UChannel forward, backward;
	private RSender22 sender;
	private RReceiver22 receiver;
	private StringPitcher sp = null;
	
	public RDT22(double pmunge) throws IOException {this(pmunge, 0.0, null);}

	public RDT22(double pmunge, double plost, String filename) throws IOException {
		forward = new UChannel(pmunge, plost);
		backward = new UChannel(pmunge, plost);
		if (filename != null) sp = new StringPitcher(new File(System.getenv("user.dir"), filename), 2000, 1000);
		sender = new RSender22(forward, backward);
		receiver = new RReceiver22(forward, backward);
	}

	public static class Packet implements PacketType{
		String checksum;
		String data;
		String seqnum;
		
		public Packet(String data){
			this(data, " ");
		}
		public Packet(String data, String seqnum){
			this(data, seqnum, CkSum.genCheck(seqnum+data));
		}
		public Packet(String data, String seqnum, String checksum) {
			this.data = data;
			this.seqnum = seqnum;
			this.checksum = checksum;
		}
		public static Packet deserialize(String data) {
			String hex = data.substring(0, 4);
			String seqnum = data.substring(4,5);
			String dat = data.substring(5);
			return new Packet(dat, seqnum, hex);
		}
		@Override
		public String serialize() {
			return checksum+seqnum+data;
		}
		@Override
		public boolean isCorrupt() {
			return !CkSum.checkString(seqnum+data, checksum);
		}
		@Override
		public String toString() {
			return String.format("%s %s (%s/%s)", data, seqnum, checksum, CkSum.genCheck(seqnum+data));
		}
	}
	
	public class RSender22 extends FSM {
		protected UChannel forward;
		protected UChannel backward;
		Packet packet = null;
		protected BufferedReader br;
		public RSender22(UChannel forward, UChannel backward) throws IOException {
			this.forward = forward;
			this.backward = backward;
			this.br = (sp != null) ? sp.getReader() : new BufferedReader(new InputStreamReader(System.in));
		}
		@Override
		public int loop(int myState) throws IOException {
			// Your code here
			return myState;
		}
	}

	public class RReceiver22 extends FSM {
		protected UChannel forward;
		public RReceiver22(UChannel forward, UChannel backward) throws IOException {
			this.forward = forward;
		}
		@Override
		public int loop(int myState) throws IOException {
			// Your code here
			return myState;
		}
	}
	@Override
	public void run() {
		new Thread(forward).start();
		new Thread(backward).start();
		new Thread(sender).start();
		new Thread(receiver).start();
		new Thread(sp).start();
	}

	public static void main(String[] args) throws IOException {
		Object[] pargs = UChannel.argParser("RDT10", args);
		RDT22 rdt22 = new RDT22((Double)pargs[0], (Double)pargs[1], (String)pargs[3]);
		rdt22.run();
	}
	
}
