package mocknet;
import java.util.Collection;

/**
 * Prototype for Dijstra and Bellman algorithms
 */
public interface Algorithm {
	/**
	 * Runs the algorithm in continuous mode
	 */
	void go();
	/**
	 * Performs single step of the algorithm 
	 */
	void step();
	/**
	 * Initializes single-step mode
	 */	
	void initStep();
	/**
	 * Called when a link change occurs
	 * @param routers	routers affected by change
	 */
	void redo(Collection<Router> routers);
	Collection<Router> getOpen();
}
