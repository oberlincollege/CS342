package mocknet;
/**
 * Control panel
 * @author rms
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

public class Control extends JPanel {
	static enum RMode{NEUTRAL, SOURCESELECT, TARGETSELECT};
	static enum CMode{FAST, SLOW, SINGLE_STEP};
	static enum OMode{QUIET, COMPUTING, RUNNING};
	private Canvas canvas;
	private HashSet<Router> routers;
	private RMode rmode = RMode.NEUTRAL;
	private CMode cmode = CMode.FAST;
	private OMode omode = OMode.QUIET;
	private JPanel main = new JPanel();
	private JButton send = new JButton("Send");
	private JButton resend = new JButton("Re-Send");
	private JButton quit = new JButton("Quit");
	private JButton reinit = new JButton("Re-Init");
	private JButton newnet = new JButton("New Network");
	private JButton flip = new JButton("Change a link");
	private JToggleButton compAlg = new JToggleButton("Compute Algorithm");
	private JButton step = new JButton("Step");
	private JRadioButton dijk = new JRadioButton("Dijkstra"), bell = new JRadioButton("Bellman"),
			fast = new JRadioButton("fast"),
			slow = new JRadioButton("slow"),
			sstep = new JRadioButton("single step");
	private JButton[] buts = {send, resend, quit, reinit, newnet, flip};
	private AbstractButton[] buts1 = {dijk, bell, compAlg, fast, slow, sstep, step};	
	private JLabel status = new JLabel("");
	private Router source, target;
	private Dijkstra dijks;
	private Bellman bells;
	private Algorithm[] algorithms;
	private int aidx = 0;
	
	public Control() {
		super();
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(100, 100));
		setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		main.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		add(main, BorderLayout.CENTER);
		add(status, BorderLayout.SOUTH);
		status.setPreferredSize(new Dimension(100, 20));
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
		init();
	}
	
	private void init() {
		JPanel panel = new JPanel();
		for (JButton but : buts) {
			panel.add(but);
			but.addActionListener(butactions);
		};
		main.add(panel);
		panel = new JPanel();
		for (AbstractButton but : buts1) {
			panel.add(but);
			but.addActionListener(butactions);
		};
		main.add(panel);		
		ButtonGroup bg = new ButtonGroup();
		bg.add(dijk);
		bg.add(bell);
		ButtonGroup bg1 = new ButtonGroup();
		bg1.add(fast);
		bg1.add(slow);
		bg1.add(sstep);
		compAlg.setPreferredSize(new Dimension(150, compAlg.getPreferredSize().height));
		dijk.setSelected(true);
		fast.setSelected(true);
		step.setEnabled(false);
	}
	
	private ActionListener butactions = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().equals(send)) selectSource();
			else if (e.getSource().equals(quit)) {setRunning(false); clear();}
			else if (e.getSource().equals(reinit)) {reinit();}
			else if (e.getSource().equals(resend)) {resend();}
			else if (e.getSource().equals(newnet)) {newnet();}
			else if (e.getSource().equals(flip)) {flip();}
			else if (e.getSource().equals(dijk)) {aidx = 0; if (cmode == CMode.SINGLE_STEP) sstep();}
			else if (e.getSource().equals(bell)) {aidx = 1; if (cmode == CMode.SINGLE_STEP) sstep();}
			else if (e.getSource().equals(fast)) {cmode = CMode.FAST; step.setEnabled(false); compAlg.setEnabled(true);}
			else if (e.getSource().equals(slow)) {cmode = CMode.SLOW; step.setEnabled(false); compAlg.setEnabled(true);}
			else if (e.getSource().equals(sstep)) {sstep();}
			else if (e.getSource().equals(step)) {algorithms[aidx].step();}
			else if (e.getSource().equals(compAlg)) {compAlg();}
		}
	};
	
	void setCanvas(Canvas canvas) {
		this.canvas = canvas;
		initState();
	}
	
	private void initState() {
		Router[][] routers = canvas.getRouters();
		HashSet<Router> ans = new HashSet<>();
		for (Router[] routerRow : routers) 
			for (Router router : routerRow) ans.add(router);		
		this.routers = ans;
		dijks = new Dijkstra(this, this.routers);
		bells = new Bellman(this, this.routers);
		algorithms = new Algorithm[]{dijks, bells};
		initStrategy();
	}
	
	RMode getRMode() {return rmode;}

	private void initStrategy() {
		for (Router router : this.routers) router.initStrategy();		
	}

	private void sstep() {
		cmode = CMode.SINGLE_STEP;
		step.setEnabled(true);
		compAlg.setEnabled(false);
		reinit();
		algorithms[aidx].initStep();
	}

	private void compAlg() {
		if (compAlg.isSelected()) {
			if (omode != OMode.QUIET) {
				compAlg.doClick();
				return;
			}
			compAlg.setText("Stop Algorithm");
			reinit();
			new Thread(){
				public void run() {
					algorithms[aidx].go();
					canvas.clearLinks();
					setComputing(false);
					if (compAlg.isSelected()) compAlg.doClick();
				}
			}.start();
		} else {
			compAlg.setText("Compute Algorithm");
			setComputing(false);
		}
	}
	
	private void reinit() {
		setRunning(false);
		clear();
		initStrategy();
		if (cmode == CMode.SINGLE_STEP) algorithms[aidx].initStep();
		canvas.getParent().repaint();
	}
	
	private void resend() {
		if (omode == OMode.RUNNING) return;
		if (source == null || target == null) {
			setStatus("Select source and target first");
			return;
		}
		source.setSource(true);
		target.setTarget(true);
		send(target);		
	}
	
	private void newnet() {
		canvas.init();
		initState();
		reinit();
	}
	
	private void flip() {
		quit.doClick();
		canvas.unNew();
		Link link;
		Router router;
		HashSet<Link> changes = new HashSet<>();
		final HashSet<Router> rchanges = new HashSet<>();
		do {
			router = routers.toArray(new Router[0])[(int)Math.floor(Math.random()*routers.size())];
			link = router.randomLink();
		} while (link == null);
		int cost = (int)Math.floor(Math.random()*100);
		double flip = Math.random();
		if (flip < .25) {
			link.cost = cost;
			link.setNewLink(true);
			link.getDual().cost = cost;
			link.getDual().setNewLink(true);
			changes.add(link);
			canvas.getParent().repaint();
		} else if (flip < .5) {
			canvas.removeLink(link);
			changes.add(link);			
		} else if (flip < .75) {
			Router nn = router.getNonNeighbor();
			if (nn != null) {
				Link nlink = new Link(router, nn, cost);
				nlink.setNewLink(true);
				canvas.addLink(nlink);
				nlink.getDual().setNewLink(true);
				changes.add(nlink);				
			} else {
				link.cost = cost;
				link.setNewLink(true);
				link.getDual().cost = cost;
				link.getDual().setNewLink(true);
				changes.add(link);								
			}
		} else {
			canvas.removeLink(link);
			changes.add(link);							
			Router nn = router.getNonNeighbor();
			if (nn != null) {
				Link nlink = new Link(router, nn, cost);
				nlink.setNewLink(true);
				canvas.addLink(nlink);
				nlink.getDual().setNewLink(true);
				changes.add(nlink);								
			}
		}
		for (Link l : changes) {
			rchanges.add(l.source);
			rchanges.add(l.target);
		}
		new Thread() {
			public void run() {
				getParent().repaint();
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				canvas.unNew();
				getParent().repaint();
				if (cmode == CMode.FAST) slow.doClick();
				algorithms[aidx].redo(rchanges);
				canvas.clearLinks();
				setComputing(false);
				getParent().repaint();				
			}
		}.start();

	}
	
	private void clear() {
		clearStatus();
		for (Router router : routers) router.clear();
		canvas.clearLinks();
		getParent().repaint();
	}
	
	private void selectSource() {
		clear();
		rmode = RMode.SOURCESELECT;
		setStatus("Select Message Source");
		getParent().repaint();
	}

	void selectTarget(Router source) {
		this.source = source;
		rmode = RMode.TARGETSELECT;
		setStatus("Select Message Target");
		getParent().repaint();		
	}

	void send(Router target) {
		this.target = target;
		rmode = RMode.NEUTRAL;
		setStatus("Starting at "+source);
		go();
	}
	
	private void clearStatus() {setStatus("");}
	private void setStatus(String text) {
		status.setText(text);
		validate();
	}
	
	boolean isRunning(){return omode == OMode.RUNNING;}
	
	private void setRunning(boolean running) {
		if (running) {
			if (omode != OMode.QUIET) return;
			compAlg.setEnabled(false);			
			omode = OMode.RUNNING;
		} else {
			compAlg.setEnabled(cmode != CMode.SINGLE_STEP);
			omode = OMode.QUIET;
		}
	}
	boolean isComputing() {return omode == OMode.COMPUTING;}
	void setComputing(boolean computing) {
		if (computing) {
			if (omode != OMode.QUIET) return;
			omode = OMode.COMPUTING;
		} else omode = OMode.QUIET;
	}
	boolean isDijkstra() {return aidx == 0;}

	Iterator<Router> getRouters() {
		return routers.iterator();
	}
	
	private void go() {
		setRunning(true);
		new Thread(new Runnable() {
			public void run() {
				Router current = source;
				while (current != target && omode == OMode.RUNNING) {
					Link link = current.getForward(target);
					if (link != null) link.setSelected(true);
					current.setSelected(true);
					getParent().repaint();					
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}
					current.setSelected(false);
					current = link.target;
					getParent().repaint();
				}
			}
		}).start();
	}

	/**
	 * Called to initiate display mode
	 * @param router	current source or target
	 */
	public void startAlgorithm(Router router) {
		if (cmode == CMode.FAST) return;
		setComputing(true);
		if (isDijkstra()) { 
			this.source = router;
			router.setSource(true);
		} else {
			this.target = router;
			router.setSource(true);			
		}
	}

	/**
	 * Called to end display mode
	 * 
	 */
	public void stopAlgorithm() {
		if (cmode == CMode.FAST) return;
		setComputing(false);
		clear();
		this.source = null;
		this.target = null;
	}

	/**
	 * Displays current state of algorithm
	 * @param current	current source or target
	 * @throws QuitException	if terminated prematurely
	 */
	public void showAlgorithm(Router current) throws QuitException {
		if (cmode == CMode.FAST) return;
		if (omode == OMode.COMPUTING) {
			for (Router router : routers) router.setCurrent(false);
			current.setCurrent(true);
			getParent().repaint();
			if (cmode == CMode.SLOW) {
				try {Thread.sleep(500);} catch (InterruptedException ex) {}
				current.setCurrent(false);
			}
		} else {
			stopAlgorithm();
			throw new QuitException();
		}
	}
	
	Collection<Router> getOpen() {return algorithms[aidx].getOpen();}
	Router getTarget() {return target;}
	Router getSource() {return source;}
	static class QuitException extends Exception{}
}
