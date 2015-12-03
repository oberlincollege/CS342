package mocknet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JPanel;

/**
 * Network nodes. Internal data includes:
 * <ul>
 * <li> HashSet&lt;Link&gt; neighbors: network connections 
 * <li> Link backPointer:  used in Dijkstra algorithm
 * <li> HashMap&lt;Router, Integer&gt; costFrom:  used in Dijkstra algorithm
 * <li> HashMap&lt;Router, Integer&gt; costTo:  used in Bellman algorithm
 * <li>	HashMap&lt;Router, Link&gt; fTable: forwarding table 
 * </ul>
 * @author rms
 *
 */
public class Router extends JPanel {
	final static int INFINITY = Integer.MAX_VALUE;
	final static int WIDTH = 100, HEIGHT = 100, CX = WIDTH/2+4, CY = HEIGHT/2-15,
			X = WIDTH/2-10, Y = HEIGHT/2-10, SIZE = 20,
			X1 = WIDTH/2-5, Y1 = HEIGHT/2-5, SIZE1 = 10;
	final static Color seenCol = Color.RED, visitCol = Color.ORANGE, pathCol = Color.magenta, 
			neutralCol = Color.GRAY, sourceCol = Color.GREEN, targetCol = Color.CYAN;
	private int row, col;
	private boolean source = false, target = false, selected = false, current = false, seen = false;
	private Control control;
	private Canvas canvas;
	private Link backPointer;
	private HashSet<Link> neighbors = new HashSet<>();
	private HashMap<Router, Integer> costFrom = new HashMap<>();
	private HashMap<Router, Integer> costTo = new HashMap<>();
	private HashMap<Router, Link> fTable = new HashMap<>();
	private int cost = 0;

	public Router(Control control, Canvas canvas, int row, int col) {
		this.control = control;
		this.canvas = canvas;
		this.row = row;
		this.col = col;
		setBackground(Color.white);
		Dimension size = new Dimension(WIDTH, HEIGHT);
		setPreferredSize(size);
		setMaximumSize(size);
		this.setMinimumSize(size);
		addMouseListener(clickListener);
	}
	@Override
	public void paintComponent(Graphics g) {
		Integer cost = (control.isDijkstra()) ? 
				((costFrom.get(control.getSource()) == null) ? 0 : costFrom.get(control.getSource())) :
					(costTo.get(control.getTarget()) == null) ? 0 : costTo.get(control.getTarget());
				g.setColor((source) ? sourceCol : (control.isComputing() && current) ? visitCol : 
					(control.isComputing()) ? 
							((control.getOpen() != null && control.getOpen().contains(this)) ? pathCol : neutralCol) :
								(source) ? sourceCol : (target) ? targetCol : (seen) ? seenCol : neutralCol);
				g.fillOval(X, Y, SIZE, SIZE);
				if (selected) {
					g.setColor(Color.black);
					g.fillOval(X1, Y1, SIZE1, SIZE1);
				}
				if (control.isRunning() || control.isComputing()) {
					g.setColor(Color.black);
					if (cost == INFINITY) {
						g.drawOval(CX, CY-5, 5, 5);
						g.drawOval(CX+5, CY-5, 5, 5);
					} else {
						String cst = String.format("%d", cost);
						g.drawString(cst, CX, CY);
					}
				}
	}

	private MouseListener clickListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			switch (control.getRMode()) {
			case SOURCESELECT:
				source = true;
				control.selectTarget(Router.this);
				getParent().repaint();
				break;
			case TARGETSELECT:
				if (source == true) break;
				target = true;
				control.send(Router.this);
				getParent().repaint();				
				break;
			default:
				break;
			}
		}
	};

	void clear() {
		source = target = selected = seen = current = false;
		for (Link link : neighbors) link.setSelected(false); 
	}
	@Override
	public String toString() {
		return String.format("[%d, %d]", row, col);
	}

	private Link randomNeighbor() {
		int idx =  (int)Math.floor(Math.random()*neighbors.size());
		return (neighbors.toArray(new Link[0]))[idx];
	}

	/**
	 * Initializes forward table to random neighbor for all destinations.
	 * Initializes costs to Infinity.
	 */
	public void initStrategy() {
		fTable.clear();
		Iterator<Router> it = control.getRouters();
		while (it.hasNext()) {
			Router router = it.next();
			if (this == router) continue;
			costFrom.put(router, INFINITY);
			costTo.put(router, INFINITY);
			fTable.put(router, randomNeighbor());
		}
	}

	Link randomLink() {
		if (neighbors.size() == 0) return null;
		return neighbors.toArray(new Link[0])[(int)Math.floor(Math.random()*neighbors.size())];		
	}
	/**
	 * 
	 * @return	an iterator for the link collection 
	 */
	public Iterator<Link> getLinks() {return neighbors.iterator();}
	Point getCenter() {return new Point(getX() + WIDTH/2, getY() + HEIGHT/2);}
	/**
	 * 
	 * @param router	target router
	 * @return true 	if a link to router exists
	 */
	public boolean hasLink(Router router) {
		if (router == this) return true;
		for (Link link : neighbors) 
			if (link.target == router) return true;
		return false;
	}
	boolean isSource() {return source;}
	void setSource(boolean source) {this.source = source;}
	boolean isTarget() {return target;}
	void setTarget(boolean target) {this.target = target;}
	boolean isSelected() {return selected;}
	void setSelected(boolean selected) {
		this.selected = selected;
		if (selected) seen = true;
	}
	Link getBackPointer() {if (backPointer != null) if (control.isComputing()) backPointer.setSelected(true); return backPointer;}
	void setBackPointer(Link backPointer) {this.backPointer = backPointer;}
	void addNeighbor(Link neighbor) {neighbors.add(neighbor);}
	boolean isNeighbor(Link link) {return neighbors.contains(link);}
	boolean isSeen() {return seen;}
	void setSeen(boolean seen) {this.seen = seen;}
	void putCostTo(Router target, int cost) {costTo.put(target, cost);}
	int getCostTo(Router target) {return costTo.get(target);}
	void putCostFrom(Router source, int cost) {costFrom.put(source, cost);}
	int getCostFrom(Router source) {return costFrom.get(source);}
	void putForward(Router target, Link link) {fTable.put(target, link);	link.setSelected(true);}
	Link getForward(Router target) {return fTable.get(target);}
	int getRow() {return row;}
	void setRow(int row) {this.row = row;}
	int getCol() {return col;}
	void setCol(int col) {this.col = col;}
	int getCost() {return cost;}
	void setCost(int cost) {this.cost = cost;}
	boolean isCurrent() {return current;}
	void setCurrent(boolean current) {this.current = current;}
	void remove(Link link) {neighbors.remove(link);}
	Router getNonNeighbor() {
		Router ans = null;
		Collection<Router> routers = canvas.neighborsOf(this);
		for (Router router : routers) {
			if (!hasLink(router)) {
				ans = router;
				break;
			}
		}
		return ans;
	}
}
