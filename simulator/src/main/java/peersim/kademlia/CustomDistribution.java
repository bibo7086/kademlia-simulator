package peersim.kademlia;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;

public class CustomDistribution implements peersim.core.Control {

  private static final String PAR_PROT = "protocol";
  private int protocolID;
  private Connection dbConnection;

  public CustomDistribution(String prefix) {
    protocolID = Configuration.getPid(prefix + "." + PAR_PROT);

    // Initialize the database connection
    try {
      Class.forName("org.postgresql.Driver"); // PostgreSQL JDBC driver
      dbConnection =
          DriverManager.getConnection(
              "jdbc:postgresql://localhost:5432/punchr", "punchr", "password");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean execute() {
    List<String> nodeIds = retrieveNodeIdFromDatabase(Network.size());

    for (int i = 0; i < nodeIds.size(); ++i) {
      Node generalNode = Network.get(i);
      String id = nodeIds.get(i);

      System.out.println(id);

      KademliaNode node;

      // Fetch node information (ID and latency) from the database
      // int latency = retrieveLatencyFromDatabase(id); // Use the node ID for lookup

      node = new KademliaNode(id, "0.0.0.0", 0);

      KademliaProtocol kadProt = ((KademliaProtocol) (Network.get(i).getProtocol(protocolID)));

      generalNode.setKademliaProtocol(kadProt);
      kadProt.setNode(node);
      kadProt.setProtocolID(protocolID);
    }

    // Close the database connection
    try {
      dbConnection.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return false;
  }

  // Implement methods to retrieve node ID and latency from the database
  private List<String> retrieveNodeIdFromDatabase(int limit) {

    List<String> nodeIds = new ArrayList<>();

    String query = "SELECT multi_hash FROM peers LIMIT ?";

    try {
      PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
      preparedStatement.setInt(1, limit);
      ResultSet resultSet = preparedStatement.executeQuery();

      while (resultSet.next()) {
        String nodeId = new String(resultSet.getString("multi_hash"));
        nodeIds.add(nodeId);
      }
      resultSet.close();
      preparedStatement.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return nodeIds;
  }

  private int retrieveLatencyFromDatabase(BigInteger nodeID) {
    try {
      Statement statement = dbConnection.createStatement();
      String query =
          "SELECT latency FROM latency_measurements WHERE id = '" + nodeID.toString() + "'";
      ResultSet result = statement.executeQuery(query);
      result.next();
      int latency = result.getInt("latency_measurements");
      statement.close();
      return latency;
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }
}
