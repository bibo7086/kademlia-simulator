package peersim.kademlia.operations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.kademlia.KademliaCommonConfig;
import peersim.kademlia.KademliaProtocol;
import peersim.kademlia.UniformRandomGenerator;
import peersim.kademlia.Util;

/**
 * This class represents a find operation and offer the methods needed to maintain and update the
 * closest set.<br>
 * It also maintains the number of parallel requsts that can has a maximum of ALPHA.
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class FindOperation extends Operation {

  /** number of available find request message to send (it must be always less than ALPHA) */
  protected int available_requests;

  protected int findMode;
  /**
   * This map contains the K closest nodes and corresponding boolean value that indicates if the
   * nodes has been already queried or not
   */
  protected HashMap<String, Boolean> closestSet;

  /**
   * defaul constructor
   *
   * @param srcNode Id of the node to find
   * @param destNode Id of the node to find
   * @param timestamp Id of the node to find
   */
  public FindOperation(String srcNode, String destNode, long timestamp) {
    super(srcNode, destNode, timestamp);

    // initialize closestSet
    closestSet = new HashMap<String, Boolean>();
    findMode = KademliaCommonConfig.FINDMODE;
    returned = new ArrayList<String>();
  }

  public int getAvailableRequests() {
    return this.available_requests;
  }

  public void increaseAvailableRequests() {
    this.available_requests++;
  }

  public void setAvailableRequests(int requests) {
    this.available_requests = requests;
  }

  public HashMap<String, Boolean> getClosest() {
    return this.closestSet;
  }

  /**
   * This method adds the given neighbours to the closest set, which is a set of the k closest nodes
   * to the destination node. If the set already contains k nodes, it replaces the node with the
   * greatest distance to the destination node with the new node, if the new node is closer to the
   * destination node. update closesetSet with the new information received
   *
   * @param neighbours the array of neighbours to be added to the closest set
   */
  public void elaborateResponse(String[] neighbours) {

    // Add each neighbor to the closest set
    for (String n : neighbours) {
      if (n != null) {
        if (!closestSet.containsKey(n)) {
          // If closest set is not full, add directly
          if (closestSet.size() < KademliaCommonConfig.K) {
            closestSet.put(n, false);
          } else { // Otherwise, find nodes with less distance in the closest set
            BigInteger newdist = Util.xorDistance(n, destNode);

            // Find the node with max distance
            BigInteger maxdist = newdist;
            String nodemaxdist = n;
            for (String i : closestSet.keySet()) {
              BigInteger dist = Util.xorDistance(i, destNode);

              if (dist.compareTo(maxdist) > 0) {
                maxdist = dist;
                nodemaxdist = i;
              }
            }

            if (nodemaxdist.compareTo(n) != 0) {
              closestSet.remove(nodemaxdist);
              closestSet.put(n, false);
            }
          }
        }
      }
    }
    // update responseNumber
    available_requests++;
  }

  /**
   * Get the first neighbor in the closest set that has not been already queried.
   *
   * @return the ID of the node or null if there are no available nodes
   */
  public String getNeighbour() {
    // Find closest neighbor (the first not already queried)
    String res = null;

    for (String n : closestSet.keySet()) {
      if (n != null && closestSet.get(n) == false) {
        if (res == null) {
          res = n;
        } else if (Util.xorDistance(n, destNode).compareTo(Util.xorDistance(res, destNode)) < 0) {
          res = n;
        }
      }
    }

    // Check if a valid neighbor has been found
    if (res != null) {
      closestSet.remove(res);
      closestSet.put(res, true);
      available_requests--; // Decrease available requests
    }

    return res;
  }

  /**
   * get the neighbours in closest set which has not been already queried
   *
   * @return the closest nodes set up to K
   */
  public List<String> getNeighboursList() {
    return new ArrayList<String>(closestSet.keySet());
    // return new ArrayList<BigInteger>(closestSet.keySet()).subList(0, KademliaCommonConfig.K-1);
  }

  public void visualize() {
    Graph graph = new SingleGraph("Operation");
    UniformRandomGenerator urg =
        new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
    BigInteger max = urg.getMaxID();
    int kademliaid = Configuration.getPid("init.2statebuilder" + ".protocol");

    for (int i = 0; i < Network.size(); i++) {
      Node node = Network.get(i);
      KademliaProtocol prot = (KademliaProtocol) (node.getProtocol(kademliaid));
      String id = prot.node.getId();
      double ratio = (double) id.hashCode() / Integer.MAX_VALUE * 360;
      double alpha = Math.toRadians(ratio);
      double x = Math.cos(alpha);
      double y = Math.sin(alpha);

      org.graphstream.graph.Node gnode = graph.addNode(id.toString());
      gnode.setAttribute("x", x);
      gnode.setAttribute("y", y);
      if (returned.contains(id)) {
        gnode.setAttribute(
            "ui.style", "fill-color: rgb(255,0,0); size: 50px, 50px;"); // text-size: 30pt");
        gnode.setAttribute("label", String.valueOf(returned.indexOf(id)));
      } else if (id.equals(srcNode)) {
        gnode.setAttribute("ui.style", "fill-color: rgb(0,0,255); size: 50px, 50px;");
      } else {
        gnode.setAttribute("ui.style", "fill-color: rgba(0,100,255, 50); size: 8px, 8px;");
      }
    }
    org.graphstream.graph.Node dst = graph.getNode(destNode.toString());
    if (dst == null) {
      dst = graph.addNode(destNode.toString());
      double ratio = (double) destNode.hashCode() / max.doubleValue() * 360;
      double alpha = Math.toRadians(ratio);
      double x = Math.cos(alpha);
      double y = Math.sin(alpha);
      dst.setAttribute("x", x);
      dst.setAttribute("y", y);
    }
    dst.setAttribute("ui.style", "fill-color: rgb(0,255,0); size: 50px, 50px;");

    System.setProperty("org.graphstream.ui", "swing");
    Viewer viewer = graph.display();
    viewer.disableAutoLayout();
  }

  public Map<String, Object> toMap() {
    Map<String, Object> result = new HashMap<String, Object>();

    result.put("id", this.operationId);
    result.put("src", this.srcNode);
    result.put("type", this.getClass().getSimpleName());
    result.put("messages", this.messages);
    result.put("start", this.timestamp);
    result.put("stop", this.stopTime);
    result.put("hops", this.nrHops);
    return result;
  }
}
