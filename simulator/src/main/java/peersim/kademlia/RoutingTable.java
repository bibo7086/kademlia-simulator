package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Provides an implementation for the routing table component of a Kademlia node.
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class RoutingTable implements Cloneable {

  /** Node ID of the node. */
  protected BigInteger nodeId = null;

  /** K-buckets. */
  protected TreeMap<Integer, KBucket> k_buckets = null;

  /** Number of k-buckets. */
  protected int nBuckets;

  /** Bucket size. */
  protected int k;
  /** Number of maximum bucket replacements. */
  protected int maxReplacements;

  /** Distance for the lowest bucket. */
  protected int bucketMinDistance;

  protected int findMode;
  /**
   * Instantiates a new routing table with the specified parameters.
   *
   * @param nBuckets the number of k-buckets
   * @param k the bucket size
   * @param maxReplacements the maximum number of bucket replacements
   */
  public RoutingTable(int nBuckets, int k, int maxReplacements) {
    k_buckets = new TreeMap<Integer, KBucket>();
    // Initializes k-buckets.

    this.nBuckets = nBuckets;

    this.k = k;

    this.maxReplacements = maxReplacements;

    bucketMinDistance = KademliaCommonConfig.BITS - nBuckets;

    this.findMode = KademliaCommonConfig.FINDMODE;

    /** Fills the k-buckets map with empty buckets. */
    for (int i = 0; i <= nBuckets; i++) {
      k_buckets.put(i, new KBucket());
    }
  }

  // Add a neighbour to the correct k-bucket
  public boolean addNeighbour(BigInteger node) {
    // add the node to the k-bucket
    if (findMode == 0 || findMode == 1) {
      // Get the lenght of the longest common prefix (corresponding to the correct k-bucket)
      int prefix_len = Util.prefixLen(nodeId, node);

      // Add the node to the k-bucket
      return k_buckets.get(prefix_len).addNeighbour(node);
    } else {
      return bucketAtDistance(Util.logDistance(nodeId, node)).addNeighbour(node);
    }
  }

  // Remove a neighbour from the correct k-bucket
  public void removeNeighbour(BigInteger node) {

    if (findMode == 0 || findMode == 1) {
      // get the lenght of the longest common prefix (correspond to the correct k-bucket)
      int prefix_len = Util.prefixLen(nodeId, node);
      // add the node to the k-bucket
      k_buckets.get(prefix_len).removeNeighbour(node);
    } else {
      // Remove the node from the k-bucket
      bucketAtDistance(Util.logDistance(nodeId, node)).removeNeighbour(node);
    }
  }

  public BigInteger[] getNeighboursDistXOR(final int distance) {
    BigInteger[] result = new BigInteger[0];
    ArrayList<BigInteger> resultList = new ArrayList<BigInteger>();
    // Add neighbors at the given distance
    resultList.addAll(bucketAtDistanceXOR(distance).neighbours.keySet());

    if (resultList.size() < k && (distance + 1) <= 256) {
      // Add neighbors at the next distance
      resultList.addAll(bucketAtDistanceXOR(distance + 1).neighbours.keySet());
      // Remove excess neighbors until the size is <= k
      while (resultList.size() > k) resultList.remove(resultList.size() - 1);
    }

    // Add neighbors at the previous distance
    if (resultList.size() < k & (distance - 1) >= 0) {
      resultList.addAll(bucketAtDistanceXOR(distance - 1).neighbours.keySet());
      // Remove excess neighbors until the size is <= k
      while (resultList.size() > k) resultList.remove(resultList.size() - 1);
    }

    // Convert the resultList to an array and return it
    return resultList.toArray(result);
  }

  public BigInteger[] getNeighboursDistLog(final int dist) {
    ArrayList<BigInteger> resultList = new ArrayList<>();

    // Add neighbors at the given distance
    resultList.addAll(bucketAtDistance(dist).neighbours.keySet());

    if (resultList.size() < k && dist + 1 <= 256) {
      // Add neighbors at the next distance
      resultList.addAll(bucketAtDistance(dist + 1).neighbours.keySet());
    }

    if (resultList.size() < k && dist - 1 >= 0) {
      // Add neighbors at the previous distance
      resultList.addAll(bucketAtDistance(dist - 1).neighbours.keySet());
    }

    // Trim excess neighbors if the list exceeds k
    while (resultList.size() > k) {
      resultList.remove(resultList.size() - 1);
    }

    // Convert the resultList to an array and return it
    return resultList.toArray(new BigInteger[0]);
  }

  // return the closest neighbour to a key from the correct k-bucket
  public BigInteger[] getNeighboursXor(final BigInteger key, final BigInteger src) {
    // resulting neighbours
    BigInteger[] result = new BigInteger[KademliaCommonConfig.K];

    // Neighbor candidates
    ArrayList<BigInteger> neighbour_candidates = new ArrayList<BigInteger>();

    // Get the length of the longest common prefix
    int prefix_len = Util.prefixLen(nodeId, key);

    if (prefix_len < 0) {
      return new BigInteger[] {nodeId};
    }

    // Return the k-bucket if is is full
    if (k_buckets.get(prefix_len).neighbours.size() >= KademliaCommonConfig.K) {
      return k_buckets.get(prefix_len).neighbours.keySet().toArray(result);
    }

    // Otherwise get k closest nodes from all k-buckets
    prefix_len = 0;
    while (prefix_len < KademliaCommonConfig.BITS) {
      neighbour_candidates.addAll(k_buckets.get(prefix_len).neighbours.keySet());
      // Remove source id
      neighbour_candidates.remove(src);
      prefix_len++;
    }

    // Create a map (distance, node)
    TreeMap<BigInteger, List<BigInteger>> distance_map =
        new TreeMap<BigInteger, List<BigInteger>>();

    // Handle the case where neigbour_candidate is empty
    if (neighbour_candidates.isEmpty()) {
      return new BigInteger[0];
    }

    for (BigInteger node : neighbour_candidates) {
      if (distance_map.get(Util.xorDistance(node, key)) == null) {
        List<BigInteger> l = new ArrayList<BigInteger>();
        l.add(node);
        distance_map.put(Util.xorDistance(node, key), l);
      } else {

        distance_map.get(Util.xorDistance(node, key)).add(node);
      }
    }

    // Best neighbors
    List<BigInteger> bestNeighbours = new ArrayList<BigInteger>();
    for (List<BigInteger> list : distance_map.values()) {
      for (BigInteger i : list) {
        if (bestNeighbours.size() < KademliaCommonConfig.K) {
          bestNeighbours.add(i);
        } else break;
      }
    }

    if (bestNeighbours.size() < KademliaCommonConfig.K)
      result = new BigInteger[bestNeighbours.size()];

    return bestNeighbours.toArray(result);
  }

  // Return the closest neighbour to a key from the correct k-bucket using the log distance
  public BigInteger[] getNeighboursLog(final BigInteger key, final BigInteger src) {
    // Resulting neighbours
    BigInteger[] result = new BigInteger[KademliaCommonConfig.K];

    // Neighbour candidates
    ArrayList<BigInteger> neighbour_candidates = new ArrayList<BigInteger>();

    // Get the lenght of the longest common prefix
    int prefix_len = Util.logDistance(nodeId, key);

    if (prefix_len < 0) {
      return new BigInteger[] {nodeId};
    }

    // Return the k-bucket if it is full
    if (bucketAtDistance(prefix_len).neighbours.size() >= KademliaCommonConfig.K) {
      return bucketAtDistance(prefix_len).neighbours.keySet().toArray(result);
    }

    // Else get k closest node from all k-buckets
    prefix_len = 0;
    while (prefix_len < KademliaCommonConfig.BITS) {
      neighbour_candidates.addAll(bucketAtDistance(prefix_len).neighbours.keySet());
      // Remove source id
      neighbour_candidates.remove(src);
      prefix_len++;
    }

    TreeMap<Integer, List<BigInteger>> distance_map = new TreeMap<Integer, List<BigInteger>>();

    for (BigInteger node : neighbour_candidates) {
      if (distance_map.get(Util.logDistance(node, key)) == null) {
        List<BigInteger> l = new ArrayList<BigInteger>();
        l.add(node);
        distance_map.put(Util.logDistance(node, key), l);

      } else {
        distance_map.get(Util.logDistance(node, key)).add(node);
      }
    }

    List<BigInteger> bestNeighbours = new ArrayList<BigInteger>();
    for (List<BigInteger> list : distance_map.values()) {
      for (BigInteger i : list) {
        if (bestNeighbours.size() < KademliaCommonConfig.K) bestNeighbours.add(i);
        else break;
      }
    }
    if (bestNeighbours.size() < KademliaCommonConfig.K)
      result = new BigInteger[bestNeighbours.size()];

    // Print the content of the distance_map
    for (Entry<Integer, List<BigInteger>> entry : distance_map.entrySet()) {
      Integer distance = entry.getKey();
      List<BigInteger> nodes = entry.getValue();

      System.out.println("Distance: " + distance);
      System.out.println("Nodes: " + nodes);
    }
    return bestNeighbours.toArray(result);
  }

  // ______________________________________________________________________________________________
  public Object clone() {
    RoutingTable dolly = new RoutingTable(nBuckets, k, maxReplacements);
    for (int i = 0; i < k_buckets.size(); i++) {
      k_buckets.put(i, new KBucket()); // (KBucket) k_buckets.get(i).clone());
    }
    return dolly;
  }

  // ______________________________________________________________________________________________
  /**
   * Print a string representation of the table
   *
   * @return String
   */
  public String toString() {
    return "";
  }

  public KBucket getBucket(BigInteger node) {
    return bucketAtDistance(Util.logDistance(nodeId, node));
  }

  public int getBucketNum(BigInteger node) {
    int dist = Util.logDistance(nodeId, node);
    if (dist <= bucketMinDistance) {
      return 0;
    }
    return dist - bucketMinDistance - 1;
  }

  protected KBucket bucketAtDistance(int distance) {

    if (distance <= bucketMinDistance) {
      return k_buckets.get(0);
    }

    return k_buckets.get(distance - bucketMinDistance - 1);
  }

  protected KBucket bucketAtDistanceXOR(int distance) {
    if (distance <= bucketMinDistance) {
      return k_buckets.get(0);
    }

    return k_buckets.get(distance - bucketMinDistance - 1);
  }

  public int getbucketMinDistance() {
    return bucketMinDistance;
  }

  public void setNodeId(BigInteger id) {
    this.nodeId = id;
  }

  public BigInteger getNodeId() {
    return this.nodeId;
  }

  // ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
