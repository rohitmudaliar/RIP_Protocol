/**
 * 
 * filename: Link.java
 * 
 * version: 1.0 03/24/2017
 *
 * revisions: Initial version
 */
/*
 * This class is used to store the source and destination ip adresses and port
 * numbers
 */
public class Link {
	String source_ip;
	String dest_ip;
	int source_port;
	int dest_port;

	public Link() {
		source_port = 0;
		dest_port = 0;
		source_ip = new String();
		dest_ip = new String();

	}

	public Link(String source_ip, String dest_ip, int source_port, int dest_port) {
		this.source_ip = source_ip;
		this.dest_ip = dest_ip;
		this.source_port = source_port;
		this.dest_port = dest_port;

	}

}
