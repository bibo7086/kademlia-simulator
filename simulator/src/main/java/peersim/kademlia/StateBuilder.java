package peersim.kademlia;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.transport.Transport;

/**
 * Initialization class that performs the bootsrap filling the k-buckets of all initial nodes.<br>
 * In particular every node is added to the routing table of every other node in the network. In the
 * end however the various nodes don't have the same k-buckets because when a k-bucket is full a
 * random node in it is deleted.
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class StateBuilder implements peersim.core.Control {

  private static final String PAR_PROT = "protocol";
  private static final String PAR_TRANSPORT = "transport";

  private String prefix;
  private int kademliaid;
  private int transportid;

  /**
   * Constructor method for the StateBuilder class. It performs the necessary initialization of the
   * prefix of the parameters, IDs of the KademliaProtocol and the transport protocol Protocol.
   *
   * @param prefix the prefix string of the parameters read from the configuration file
   */
  public StateBuilder(String prefix) {
    this.prefix = prefix;
    kademliaid = Configuration.getPid(this.prefix + "." + PAR_PROT);
    transportid = Configuration.getPid(this.prefix + "." + PAR_TRANSPORT);
  }

  /**
   * Returns the Kademlia protocol of a node at a given index in the network.
   *
   * @param i the index of the node in the network
   * @return the Kademlia protocol of the node
   */
  public final KademliaProtocol get(int i) {
    return ((KademliaProtocol) (Network.get(i)).getProtocol(kademliaid));
  }

  /**
   * Returns the transport protocol of a node at a given index in the network.
   *
   * @param i the index of the node in the network
   * @return the transport protocol of the node
   */
  public final Transport getTr(int i) {
    return ((Transport) (Network.get(i)).getProtocol(transportid));
  }

  /**
   * Prints the given object.
   *
   * @param o the object to print
   */
  public static void o(Object o) {
    System.out.println(o);
  }

  /**
   * Executes the Kademlia network by sorting the nodes in ascending order of nodeID, and randomly
   * adding 100 (not the 50 mentioned in the previous comment) nodes to each node's k-bucket. Then
   * adds 50 nearby nodes to each node's k-bucket.
   *
   * @return always false
   */
  public boolean execute() {

    // Sort the network by nodeId (Ascending)
    Network.sort(
        new Comparator<Node>() {
          /**
           * Compares the node IDs of two nodes.
           *
           * @param o1 the first node
           * @param o2 the second node
           * @return 0 if same, negative if o1 < 02, and positive if o1 > o2
           */
          public int compare(Node o1, Node o2) {
            Node n1 = (Node) o1;
            Node n2 = (Node) o2;
            KademliaProtocol p1 = (KademliaProtocol) (n1.getProtocol(kademliaid));
            KademliaProtocol p2 = (KademliaProtocol) (n2.getProtocol(kademliaid));
            return Util.put0(p1.getKademliaNode().getId())
                .compareTo(Util.put0(p2.getKademliaNode().getId()));
          }
        });

    int sz = Network.size();
    // For every node, add 100 random nodes to its k-bucket - not sure why this was 50 previously...
    for (int i = 0; i < sz; i++) {
      Node iNode = Network.get(i);
      KademliaProtocol iKad = (KademliaProtocol) (iNode.getProtocol(kademliaid));

      for (int k = 0; k < 100; k++) {
        KademliaProtocol jKad =
            (KademliaProtocol) (Network.get(CommonState.r.nextInt(sz)).getProtocol(kademliaid));
        iKad.getRoutingTable().addNeighbour(jKad.getKademliaNode().getId());
      }
    }

    // Add 50 nearby nodes to each node's k-bucket based on proximity to ID
    for (int i = 0; i < sz; i++) {
      Node iNode = Network.get(i);
      KademliaProtocol iKad = (KademliaProtocol) (iNode.getProtocol(kademliaid));
      BigInteger iNodeId = iKad.getKademliaNode().getId();

      int start = i + 1;
      if (start > sz - 50) {
        start = sz - 25;
      }
      for (int k = 0; k < 50; k++) {
        start++;
        if (start < sz) {
          KademliaProtocol jKad = (KademliaProtocol) (Network.get(start).getProtocol(kademliaid));
          BigInteger jNodeId = jKad.getKademliaNode().getId();
          if (!jNodeId.equals(iNodeId)) {
            iKad.getRoutingTable().addNeighbour(jKad.getKademliaNode().getId());
          }
        }
      }
    }

    // Filepath to write the routing table
    String filePath = "./logs/routingtable.csv";

    // Create a Filewriter object to write to the file
    try (FileWriter fileWriter = new FileWriter(filePath)) {
      fileWriter.write("Initial state of the routing table\n");

      List<Node> selectedNodes = new ArrayList<>();
      // Randomly choose 3 nodes
      // for (int i = 0; i < 3; i++) {
      //   int randomIndex = CommonState.r.nextInt(sz);
      //   Node selectedNode = Network.get(randomIndex);
      //   selectedNodes.add(selectedNode);
      // }

      // Choose the first node from the beginning
      Node firstNode = Network.get(0);
      selectedNodes.add(firstNode);

      // Choose the node from the middle
      int middleIndex = sz / 2; // Index of the middle node
      Node middleNode = Network.get(middleIndex);
      selectedNodes.add(middleNode);

      // Choose the last node form the end
      Node lastNode = Network.get(sz - 1);
      selectedNodes.add(lastNode);

      // Iterate over the selected nodes
      for (Node node : selectedNodes) {

        KademliaProtocol randomNodeKademliaProtocol =
            (KademliaProtocol) node.getProtocol(kademliaid);
        // BigInteger randomNodeId = randomNodeKademliaProtocol.getKademliaNode().getId();
        RoutingTable routingTable = randomNodeKademliaProtocol.getRoutingTable();
        String routingTableString = routingTable.generateRoutingTableString();

        fileWriter.write(routingTableString);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  } // end execute()
}
