package mocknet;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import mocknet.Control.QuitException;
/**
 * Implementation of Dijkstra's algorithm
 * @author rms
 *
 */
public class Dijkstra implements Algorithm {
	final public static int INFINITY = Integer.MAX_VALUE;	
	private Collection<Router> routers;
	private Control control;
	// Vector open holds routers yet to be processed
	private Vector<Router> sources, open = null;
	private Iterator<Router> sit, oit;
	private Router source = null;
	
	public Dijkstra(Control control, Collection<Router> routers) {
		this.control = control;
		this.routers = routers;	
	}
	@Override	
	public Collection<Router> getOpen() {return open;}	

	@Override
	// Called when running non-single-step
	public void go() {
	// Find the shortest path from each source to every other router		
		try {
			for (Router source : routers) {
				shortestPath(source);
			}
		} catch (QuitException ex) {}
	}
	@Override
	// Initializes single step mode
	public void initStep() {
		source = null;
		sources = new Vector<>();
		sources.addAll(routers);
		sit = sources.iterator();
	}
	@Override
	// Called when single stepping
	public void step() {
		if (source == null || !oit.hasNext()) {
			control.stopAlgorithm();
			if (!sit.hasNext()) return;
			source = sit.next();
			open = initLoop(source);
			sit.remove();
			oit = open.iterator();
			control.startAlgorithm(source);
		}
		Router target = open.firstElement();
		open.remove(0);
		try {
			shortestPath(source, target);
			sortOpen(open);
		} catch (QuitException e) {}
	}
	/**
	 * Finds the shortest path to each of the other routers.
	 * Algorithm structure: initialize open list to all routers;
	 * sort list; visit each router as target of path from source;
	 * sort list after each visit
	 * @param source	source of the path 
	 * @throws QuitException	if interrupted 
	 */
	public void shortestPath(Router source) throws QuitException {
		control.startAlgorithm(source);
		open = initLoop(source);
		while (open.size() > 0) {
			Router target = open.firstElement();
			open.remove(0);
			shortestPath(source, target);
			sortOpen(open);
		}
		control.stopAlgorithm();
	}
	/**
	 * When this function is called, shortest path to target is available
	 * by following the backpointers, and its least cost will have been
	 * determined. 
	 * This function visits each neighbor of target and adjusts that neighbor's 
	 * cost if necessary, and setting the back pointer of the neighbor when 
	 * the neighbor's cost improves.
	 * Finally, the algorithm traces the path back from target to source using backpointers,
	 * recording costs and adjusting forwarding tables.
	 * @param source	path source
	 * @param target	path target
	 * @throws QuitException	if interrupted
	 */
	public void shortestPath(Router source, Router target) throws QuitException {
		Iterator<Link> it = target.getLinks();
		while (it.hasNext()) {
			Link l = it.next();
			int tot = target.getCost() + l.cost;
			if (tot < l.target.getCost()) {
				l.target.setCost(tot);
				l.target.setBackPointer(l);
			}
		}
		Router r = target;
		while (r != source) {
			r.putCostFrom(source, r.getCost());
			Link l1 = r.getBackPointer();
			if (l1 == null) break;
			Router s = l1.source;
			s.putForward(target, l1);
			r = s;
		}
		control.showAlgorithm(target);
	}
	/**
	 * Initializes algorithm loop.
	 * Adds all routers to open. 
	 * Sort open in ascending order of cost.
	 * @param source	path source
	 * @return	sorted list of routers other than source 
	 */
	public Vector<Router> initLoop(Router source) {
		Vector<Router> open = new Vector<>();
		initRouters(source, open);
		sortOpen(open);
		return open;
	}
	/**
	 * Initializes the routers to begin the algorithm.
	 * Each router has initial cost of INFINITY 
	 * (except source which is has initial cost of 0).
	 * @param source	current path source
	 * @param open		open list to be initialized
	 */
	public void initRouters(Router source, Vector<Router> open) {
		for (Router target : routers) {
			if (target == source) target.setCost(0);
			else target.setCost(INFINITY);
			open.add(target);
		}
	}
	/**
	 * Sorts open list in order of ascending cost
	 * @param open	list to be sorted
	 */
	public void sortOpen(Vector<Router> open) {
		open.sort(new Comparator<Router>(){
			@Override			
			public int compare(Router x, Router y) {
				return x.getCost() - y.getCost();
			}
		}); 
	}
	@Override
	public void redo(Collection<Router> routers) {
		// Called with affected routers when a link changes.
		try {
			for (Router router : routers) {
				shortestPath(router);
			}
		} catch (QuitException ex) {}				
	}
}
