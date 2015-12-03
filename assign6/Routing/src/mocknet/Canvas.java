package mocknet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Stroke;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.JPanel;

import mocknet.Control.QuitException;

/**
 * Network canvas
 * @author rms
 *
 */
public class Canvas extends JPanel {
	public final static int COLS = 5, MAXCOST = 100;
	private int size, rows, cols;
	private double prob;
	private Router[][] routers;
	private Control control;
	HashSet<Link> links = new HashSet<>();
	HashMap<Link, Link> dualLinks = new HashMap<>();
	
	
	public Canvas(Control control, int size, double prob) {
		super();
		this.size = size;
		this.prob = prob;
		this.control = control;
		setBackground(Color.white);
		cols = COLS;
		setLayout(new GridLayout(0, cols, 0, 0));
		init();
	}
	
	void init() {
		removeAll();
		links.clear();
		rows = size/COLS + ((size % cols > 0) ? 1 : 0);
		routers = new Router[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				routers[i][j] = new Router(control, this, i, j);
				this.add(routers[i][j]);
			}
		}
		try {
			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < cols; j++) {
					connectUp(i, j);
				}
			}
		} catch (QuitException ex) {
			init();
			return;
		}
		validate();
		repaint();
	}
	
	Collection<Router> neighborsOf(Router router) {
		Vector<Router> ans = new Vector<Router>();		
		int i = router.getRow();
		int j = router.getCol();
		for (int ii = i - 1; ii <= i + 1; ii++) {
			for (int jj = j - 1; jj <= j + 1; jj++) {
				if (ii < 0 || ii >= rows || jj < 0 || jj >= cols) continue;
				if (ii == i && jj == j) continue;
				ans.add(routers[ii][jj]);
			}
		}
		return ans;
	}

	boolean linked(Router x, Router y) {
		for (Link link : links) {
			if ((link.source == x && link.target == y) || (link.target == x && link.source == y)) return true;
		}
		return false;
	}
	
	void connectUp(int i, int j) throws QuitException {
		int ctr = 0;
		for (int ctr0 = 0; ctr0 < 20; ctr0++) {
			for (int ii = i - 1; ii <= i + 1; ii++) {
				for (int jj = j - 1; jj <= j + 1; jj++) {
					if (ii < 0 || ii >= rows || jj < 0 || jj >= cols) continue;
					if (ii == i && jj == j) continue;
					if (linked(routers[i][j], routers[ii][jj])) continue;
					if (Math.random() < prob) {
						ctr++;
						int cost = (int)Math.floor(Math.random()*MAXCOST);
						Link link = new Link(routers[i][j], routers[ii][jj], cost);
						links.add(link);
						routers[i][j].addNeighbor(link);
						Link link1 = new Link(routers[ii][jj], routers[i][j], cost);					
						routers[ii][jj].addNeighbor(link1);
						dualLinks.put(link, link1);
						link.setDual(link1);
						link1.setDual(link);
					}
				}
			}
			if (ctr > 0) return;
		}
		throw new QuitException();
	}
	
	void clearLinks(){
		for (Link link: links) {
			link.setSelected(false);
			dualLinks.get(link).setSelected(false);
		}
		getParent().repaint();
	}
	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g1 = (Graphics2D)g;
		for (Link link : links) {
			Router source = link.source;
			Router target = link.target;
			Point sourceCenter = source.getCenter();
			Point targetCenter = target.getCenter();
			int mx = sourceCenter.x, ox = targetCenter.x, my = sourceCenter.y, oy = targetCenter.y;
			int tx = 0, ty = 0;
			switch (link.type) {
			case HORIZ:
				tx = (mx+ox)/2-5;
				ty = (my-6);				
				break;
			case VERT:
				tx = (mx+ox)/2 + 2;
				ty = (my + oy)/2 + 2;
				break;
			case DIAGDOWN:
				tx = (mx+ox)/2-16;
				ty = (my+oy)/2-17;
				break;
			case DIAGUP:
				tx = (mx+ox)/2+20 ;				
				ty = (my+oy)/2-5;
				break;
			}
			Color chold = g1.getColor();
			Stroke hold = g1.getStroke();
			if (link.isNewLink()) {
				g1.setColor(Color.MAGENTA.darker());
				g1.setStroke(new BasicStroke(2.5f));				
			} else if (control.isComputing() && (link.isSelected() || dualLinks.get(link).isSelected())) {
				g1.setColor(Color.blue);
				g1.setStroke(new BasicStroke(2.5f));
			} else if (link.isSelected() || dualLinks.get(link).isSelected()) {
				g1.setColor(Color.red);
				g1.setStroke(new BasicStroke(2.5f));
			} else {
				g1.setColor(Color.black);
			}
			g1.drawLine(sourceCenter.x, sourceCenter.y, targetCenter.x, targetCenter.y);
			g1.drawString(Integer.toString(link.cost), tx, ty);
			g1.setStroke(hold);
			g1.setColor(chold);
		}
	}

	Router[][] getRouters() {
		return routers;
	}
	
	void removeLink(Link link) {
		Link dual = null;
		if (!links.contains(link)) {
			link = link.getDual();
			dual = dualLinks.get(link);
		} else dual = link.getDual();
		links.remove(link);
		dualLinks.remove(dual);
		link.source.remove(link);
		dual.source.remove(dual);
	}

	void addLink(Link link) {
		Link dual = new Link(link.target, link.source, link.cost);
		link.setDual(dual);
		dual.setDual(link);
		links.add(link);
		dualLinks.put(link, dual);
		link.source.addNeighbor(link);
		link.target.addNeighbor(dual);
	}

	void unNew() {
		for (Link link : links) {
			link.setNewLink(false);
			link.getDual().setNewLink(false);
		}
	}
}
