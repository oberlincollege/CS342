package mocknet;

/**
 * Represents a connection between source and target routers at a given cost.
 * Each Link has a dual link with source and target routers switched.
 * @author rms
 *
 */
public class Link {
	static enum Type{HORIZ, VERT, DIAGDOWN, DIAGUP};
	Router source, target;
	Link.Type type;
	int cost;
	private boolean selected = false, newLink = false;
	private Link dual;
	
	public Link(Router source, Router target, int cost) {
		this.source = source;
		this.target = target;
		this.cost = cost;
		int srow = source.getRow();
		int scol = source.getCol();
		int trow = target.getRow();
		int tcol = target.getCol();
		type = (srow == trow) ? Type.HORIZ :
				(scol == tcol) ? Type.VERT:
				((srow < trow && scol < tcol)
					|| (trow < srow && tcol < scol)) ? Type.DIAGDOWN : Type.DIAGUP;	
	}
	@Override
	public boolean equals(Object other) {
		return (other instanceof Link &&
				((Link)other).source == this.source &&					
				((Link)other).target == this.target);
	}
	@Override
	public String toString() {
		return String.format("s:%s t:%s c:%d", source, target, cost); 
	}
	boolean isSelected() {
		return selected;
	}
	void setSelected(boolean selected) {
		this.selected = selected;
		dual.selected = selected;
	}
	Link getDual() {return dual;} 
	void setDual(Link dual) {this.dual = dual;}
	boolean isNewLink() {return newLink;}
	void setNewLink(boolean newLink) {this.newLink = newLink;}
}