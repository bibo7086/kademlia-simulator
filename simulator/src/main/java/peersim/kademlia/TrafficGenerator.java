package peersim.kademlia;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * This control generates random search traffic from nodes to random destination node.
 *
 * @author: Daniele Furlan, Maurizio Bonani
 * @version: 1.0
 */
public class TrafficGenerator implements Control {

  /** MSPastry Protocol to act. */
  private static final String PAR_PROT = "protocol";

  /** MSPastry Protocol ID to act. */
  private final int pid;

  private boolean first = true;

  /**
   * Constructs a TrafficGenerator object.
   *
   * @param prefix the prefix string
   */
  public TrafficGenerator(String prefix) {
    pid = Configuration.getPid(prefix + "." + PAR_PROT);
  }

  /**
   * Generates a random find node message by selecting a random destination.
   *
   * @return Message
   */
  private Message generateFindNodeMessage() {
    // Get an existing active destination node
    Node n = Network.get(CommonState.r.nextInt(Network.size()));
    while (!n.isUp()) {
      n = Network.get(CommonState.r.nextInt(Network.size()));
    }
    String dst = ((KademliaProtocol) (n.getProtocol(pid))).getKademliaNode().getId();

    Message m = Message.makeInitFindNode(dst);
    m.timestamp = CommonState.getTime();

    return m;
  }

  /**
   * Generates and sends a random find node message (initated by every call of this control).
   *
   * @return boolean
   */
  public boolean execute() {
    Node start;
    do {
      start = Network.get(CommonState.r.nextInt(Network.size()));
    } while ((start == null) || (!start.isUp()));

    // Send message
    EDSimulator.add(0, generateFindNodeMessage(), start, pid);

    return false;
  }
}
