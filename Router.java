
/**
 * 
 * filename: Router.java
 * 
 * version: 1.0 03/24/2017
 *
 * revisions: Initial version
 */
/*
 * This class is used to store the routing table
 */
public class Router {
	private String destination;
	private String nextHop;
	private int cost;
	private boolean flag;

	public Router() {
		this.setDestination(new String());
		this.setNextHop(new String());
		this.setCost(0);
		this.flag = false;
	}

	public Router(String destination, String nextHop, int cost) {
		this.setDestination(destination);
		this.setNextHop(nextHop);
		this.setCost(cost);
		this.flag = false;
	}

	public boolean getFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getNextHop() {
		return nextHop;
	}

	public void setNextHop(String nextHop) {
		this.nextHop = nextHop;
	}

	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public String toString() {
		return this.destination + "," + this.nextHop + "," + this.cost;
	}
}
