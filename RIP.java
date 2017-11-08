
/**
 * 
 * filename: RIP.java
 * 
 * version: 1.0 03/24/2017
 *
 *         revisions: Initial version
 */

import java.awt.DisplayMode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/*
 * This program implements the Distance Vector Algorithm RIPv2 protocol
 * 
 * @author pan7447 Parvathi Nair 
 */
public class RIP {
	static List<Link> links = new ArrayList<>();
	static List<String> networks = new ArrayList<>();
	static List<Router> routingTable = new ArrayList<>();

	/**
	 * 
	 * @param args
	 *            path of the file/filename.txt - input configuration file
	 */
	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
		Scanner sc = new Scanner(new File(args[0]));
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.contains("LINK")) {
				String split[] = line.split("\\s");
				links.add(new Link(split[1].split(":")[0], split[2].split(":")[0],
						Integer.parseInt(split[1].split(":")[1]), Integer.parseInt(split[2].split(":")[1])));

			} else {
				String split[] = line.split(" ");
				networks.add(split[1]);
			}
		}
		System.out.println("RIP message :");
		for (Link link : links) {
			for (String net : networks) {
				System.out.println("------------------------");
				System.out.println("    2200");
				String split[] = net.split("/");
				System.out.println(split[0]);
				System.out.println(split[1]);
				System.out.println(link.dest_ip + ":" + link.dest_port);
				System.out.println("   " + 0 + "   ");
				System.out.println("-------------------------");
			}

		}

		initializeRoutingTable();
		Thread.sleep(1000);
		initializeListeners();
		RIP rip = new RIP();
		Broadcast broadcast = rip.new Broadcast();
		broadcast.start();

	}

	// This method handles broadcasting of routing tables periodically after
	// every 5 seconds
	private class Broadcast extends Thread {
		public void run() {
			while (true) {
				broadcastToNeighbors();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
	}

	// This method starts he client which eventually broadcasts the routing
	// table
	private static void broadcastToNeighbors() {
		RIP rip = new RIP();

		for (Link link : links) {
			Client client = rip.new Client(link.dest_ip, link.dest_port);
			client.start();
		}

	}

	// This method starts the server
	private static void initializeListeners() {
		RIP rip = new RIP();

		for (Link link : links) {
			Server server = rip.new Server(link.source_port);
			server.start();
		}

	}

	// This method initialize the routing table
	private static void initializeRoutingTable() {
		for (String str : networks) {
			routingTable.add(new Router(str, "0.0.0.0:0", 0));
		}
		display();
	}

	// This is the client class which uses the destination ip address adn
	// destination port number to broadcast the routing table
	private class Client extends Thread {
		private String dest_ip;
		private int dest_port;

		public Client() {
			dest_ip = new String();
			dest_port = 0;
		}

		public Client(String dest_ip, int dest_port) {
			this.dest_ip = dest_ip;
			this.dest_port = dest_port;
		}

		public void run() {
			try {
				sender();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// This method conerts the data to byte and sends the routing table
		// information using UDP protocol
		private void sender() throws IOException {
			StringBuffer routingTableToSend = new StringBuffer();
			DatagramSocket clientSocket = new DatagramSocket();
			byte[] sendData = new byte[10240];
			for (Router router : routingTable) {
				routingTableToSend.append(router + ";");
			}
			sendData = routingTableToSend.toString().substring(0, routingTableToSend.length() - 1).getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(dest_ip),
					dest_port);

			clientSocket.send(sendPacket);
			clientSocket.close();
		}
	}

	// This is the server class which is used to listen and accept the the
	// routing table information
	private class Server extends Thread {
		private int port;

		public Server() {
			port = 0;
		}

		public Server(int port) {
			this.port = port;
		}

		private void listener() {
			{
				DatagramSocket serverSocket = null;
				try {
					serverSocket = new DatagramSocket(port);
				} catch (SocketException e1) {

				}
				byte[] receiveData = new byte[10240];

				while (true) {
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

					try {
						serverSocket.setSoTimeout(10000);
					} catch (SocketException e) {

					}

					try {
						serverSocket.receive(receivePacket);
						String sentence = new String(receivePacket.getData());
						sentence = sentence.trim();
						RIP rip = new RIP();

						UpdateRoutingTable updateRoutingTable = rip.new UpdateRoutingTable(sentence,
								receivePacket.getAddress().getHostAddress(), port);
						updateRoutingTable.start();
					} catch (IOException e) {
						synchronized (routingTable) {
							updateToInfinity(port);

						}

					}
				}
			}
		}

		// This method updates the cost metric to 16 when the node disappears
		private void updateToInfinity(int port) {

			for (Link link : links) {
				if (port == link.source_port) {
					for (Router r : routingTable) {
						if (r.getNextHop().equals(link.dest_ip + ":" + link.dest_port)) {
							r.setCost(16);

						}
					}
				}
			}
			display();
		}

		public void run() {
			listener();
		}

	}

	// This class is used to update the routing table using the Distance Vector
	// algorithm
	private class UpdateRoutingTable extends Thread {
		String receivedInfo;
		String neighborInfo;
		int port;
		Set<Router> receivedRoutingTable = new HashSet<>();

		public UpdateRoutingTable() {
			receivedInfo = new String();
			neighborInfo = new String();
			port = 0;
		}

		public UpdateRoutingTable(String receivedInfo, String neighborInfo, int port) {
			this.receivedInfo = receivedInfo.trim();
			this.neighborInfo = neighborInfo;
			this.port = port;
		}

		public void run() {
			synchronized (links) {
				if (receivedInfo.contains(",")) {
					update();
				}
			}
		}

		// This method ensures that whenever the routing table is manipulated,
		// it is manipulated by only single thread
		private synchronized void update() {
			createReceivedRoutingTable();
			updateRoutingTable();
		}

		// This method implements the distance vector algorithm
		private synchronized void createReceivedRoutingTable() {
			String[] input;
			if (receivedInfo.contains(";")) {
				input = receivedInfo.split(";");
			} else {
				input = new String[1];
				input[0] = receivedInfo;
			}

			for (String str : input) {
				String split[] = str.split(",");
				receivedRoutingTable.add(new Router(split[0], split[1], Integer.parseInt(split[2])));

			}
		}

		private void updateRoutingTable() {
			
			Set<Router> tempRoutingTable = new HashSet<>();
			int neighbor = getNeighborInfo();
			for (Router r1 : routingTable) {
				for (Router r2 : receivedRoutingTable) {
					if (r1.getDestination().equals(r2.getDestination())) {
						if (r1.getCost() > r2.getCost() + 1 && r1.getCost() != 16 && r2.getCost() != 16) {
							r1.setCost(r2.getCost() + 1);
							r1.setNextHop(links.get(neighbor).dest_ip + ":" + links.get(neighbor).dest_port);
						} else if (r1.getCost() == 16 && r2.getCost() != 16) {
							if (r1.getNextHop()
									.equals(links.get(neighbor).dest_ip + ":" + links.get(neighbor).dest_port)) {
								if (r1.getCost() > r2.getCost() + 1) {
									r1.setCost(r2.getCost() + 1);
									r1.setNextHop(links.get(neighbor).dest_ip + ":" + links.get(neighbor).dest_port);
								}

							}
						} else if (r2.getCost() == 16) {
							if (r1.getNextHop()
									.equals(links.get(neighbor).dest_ip + ":" + links.get(neighbor).dest_port)) {
								if (r1.getCost() != 0) {
									r1.setCost(16);
								}
							}
						}

					} else {
						boolean check = false;
						for (Router r3 : routingTable) {
							if (r2.getDestination().equals(r3.getDestination())) {
								check = true;
							}
						}
						for (Router r3 : tempRoutingTable) {
							if (r2.getDestination().equals(r3.getDestination())) {
								check = true;
							}

						}
						if (!check) {
							tempRoutingTable.add(new Router(r2.getDestination(),
									links.get(neighbor).dest_ip + ":" + links.get(neighbor).dest_port,
									1 + r2.getCost()));
						}
					}
				}
			}
			routingTable.addAll(tempRoutingTable);
			tempRoutingTable.clear();

			display();

		}

		// This method returns the neighbor information
		private int getNeighborInfo() {
			for (int i = 0; i < links.size(); i++) {
				if (links.get(i).dest_ip.equals(neighborInfo)) {
					if (links.get(i).source_port == port) {
						return i;
					}
				}
			}
			return Integer.MIN_VALUE;
		}
	}

	// This method dispalys the routing table
	public static void display() {
		System.out.println();
		System.out.println();

		System.out.println("Destination" + "     " + "NextHop" + "     " + " Cost");
		System.out.println(
				"============================================================================================");
		for (int i = 0; i < routingTable.size(); i++) {
			System.out.println(routingTable.get(i).getDestination() + "     " + routingTable.get(i).getNextHop()
					+ "     " + routingTable.get(i).getCost());
		}

	}

}
