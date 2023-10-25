package peersim.kademlia;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.kademlia.operations.Operation;
import peersim.util.IncrementalStats;

/**
 * This class implements a simple observer of search time and hop average in finding a node in the
 * network
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class KademliaObserver implements Control {

  /** Configuration strings to read */
  private static final String PAR_STEP = "step";

  /** The parameter name for FINDMODE. */
  private static final String PAR_TRAFFIC_STEP = "trafficStep";

  /** Keep statistics of the number of hops of every message delivered. */
  public static IncrementalStats hopStore = new IncrementalStats();

  /** keep statistics of the time every message delivered. */
  public static IncrementalStats timeStore = new IncrementalStats();

  /** Keep statistic of number of message delivered */
  public static IncrementalStats msg_deliv = new IncrementalStats();

  /** Keep statistic of number of initiated find operations */
  public static IncrementalStats find_op = new IncrementalStats();

  /** Keep statistics of successful find operations */
  public static IncrementalStats find_ok = new IncrementalStats();

  /** Parameter of the protocol we want to observe */
  private static final String PAR_PROT = "protocol";

  /** Messages exchanged in the Kademlia network */
  private static HashMap<String, Map<String, Object>> messages =
      new HashMap<String, Map<String, Object>>();

  /** Log of the "FIND" operations of the Kademlia network */
  private static HashMap<String, Map<String, Object>> find_log =
      new HashMap<String, Map<String, Object>>();

  /** Name of the folder where experiment logs are written */
  private static String logFolderName;

  /** The time granularity of reporting metrics */
  private static int observerStep;

  /** The time granularity of traffic generation */
  private static int trafficStep;

  /** The number of nodes in the simulation */
  private static int size;

  /** Maximum size in number of message entries */
  private static final int MAX_MESSAGE = 1000;

  private static int accumulatedMessageCount = 0;

  /** Collection to track written message IDs */
  private static Set<String> writtenMessages = new LinkedHashSet<>();

  /** Collection to track written operation IDs */
  private static Set<String> writtenOperations = new LinkedHashSet<>();

  /**
   * Constructor to initialize the observer.
   *
   * @param prefix the configuration prefix
   */
  public KademliaObserver(String prefix) {
    observerStep = Configuration.getInt(prefix + "." + PAR_STEP);
    trafficStep = Configuration.getInt(prefix + "." + PAR_TRAFFIC_STEP);
    size = Network.size();
    logFolderName = "./logs";
  }

  /**
   * Writes a map of messages to a file.
   *
   * @param map the map to write
   * @param filename the name of the file to write to
   */
  private static void writeMap(Map<String, Map<String, Object>> map, String filename) {
    try (FileWriter writer =
        new FileWriter(filename, true)) { // Use append mode to add data to the end of the file
      String[] keys = {"src", "dst", "id", "type", "status"};

      // Write the comma seperated keys as the header of the file
      String header = String.join(",", keys) + "\n";
      // if the file is empty
      File file = new File(filename);
      if (file.length() == 0) { // Check if the file is empty
        writer.write(header);
      }
      // Iterate through each message and writes its content to a file
      for (Map<String, Object> entry : messages.values()) {
        StringBuilder lineBuilder = new StringBuilder();
        for (Object key : keys) {
          lineBuilder.append(entry.get(key)).append(",");
        }
        // Remove the last comma
        String line = lineBuilder.substring(0, lineBuilder.length() - 1) + "\n";
        writer.write(line);
      }
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Writes a map of find operations to a file.
   *
   * @param map the map to write
   * @param filename the name of the file to write to
   */
  private static void writeMapFind(Map<String, Map<String, Object>> map, String filename) {
    try (FileWriter writer = new FileWriter(filename, true);
        FileWriter averageWriter =
            new FileWriter(
                logFolderName
                    + "/average_"
                    + size
                    + "_"
                    + trafficStep
                    + "_"
                    + KademliaCommonConfig.FINDMODE
                    + ".csv",
                true)) {

      // Define the expected order of keys in the header
      String[] keys = {"src", "start", "stop", "messages", "hops", "id", "type"};

      File file = new File(filename);
      File averageFile =
          new File(
              logFolderName
                  + "/average_"
                  + size
                  + "_"
                  + trafficStep
                  + "_"
                  + KademliaCommonConfig.FINDMODE);

      // if the file is empty
      if (file.length() == 0 && averageFile.length() == 0) {

        // Write the comma separated keys as the header of the file
        String header = String.join(",", keys) + "\n";

        // Write the header for file containing the headers for the incremental plots
        String AverageHeader = "find_op,message_count,latency,hopcount\n";

        averageWriter.write(AverageHeader);
        writer.write(header);
      }

      double totalLatency = 0;
      double totalHops = 0;
      int count = 0;

      // Iterate through each find operation and write its data to the file
      for (Map<String, Object> entry : map.values()) {
        StringBuilder lineBuilder = new StringBuilder();
        for (Object key : keys) {
          lineBuilder.append(entry.get(key)).append(",");

          // Calculate the average of "stop" and "hops"
          if (key.equals("stop")) {
            totalLatency += ((Long) entry.get(key)).intValue();
            count++;
          } else if (key.equals("hops")) {
            totalHops += (int) entry.get(key);
          }
        }
        // Remove the last comma
        String line = lineBuilder.substring(0, lineBuilder.length() - 1) + "\n";
        writer.write(line);
      }

      // Calculate averages
      double averageLatency = totalLatency / count;
      double averageHops = totalHops / count;

      // Write average values to teh "average_writer" file
      String averageLine =
          find_op.getN()
              + ","
              + messages.size()
              + ","
              + String.format("%.2f", averageLatency)
              + ","
              + String.format("%.2f", averageHops)
              + "\n";

      averageWriter.write(averageLine);
      // Print or use the average values as needed
      // System.out.println("Count: " + count);
      // System.out.println("Average Stop: " + String.format("%.2f", averageLatency));
      // System.out.println("Average Hops: " + String.format("%.2f", averageHops));

      averageWriter.close();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** Writes log data to files. */
  public void writeOut() {
    // Create log directory if it does not exist
    File directory = new File(logFolderName);
    if (!directory.exists()) {
      directory.mkdir();
    }
    // Write messages log to file if not empty
    if (!messages.isEmpty()) {
      // writeMap(
      //     messages,
      //     logFolderName
      //         + "/messages_"
      //         + trafficStep
      //         + "_"
      //         + KademliaCommonConfig.FINDMODE
      //         + ".csv");

      // writtenMessages.addAll(messages.keySet()); // Track the written message IDs
      resetAccumulatedDataSize();
    }
    // Write find operations log to file if not empty
    if (!find_log.isEmpty()) {
      writeMapFind(
          find_log,
          logFolderName
              + "/operation_"
              + size
              + "_"
              + trafficStep
              + "_"
              + KademliaCommonConfig.FINDMODE
              + ".csv");
      // writtenOperations.addAll(find_log.keySet()); // Track the written operation IDs
    }

    // Write the count data to the "count" file
    try (FileWriter countWriter =
        new FileWriter(
            logFolderName
                + "/count_"
                + size
                + "_"
                + trafficStep
                + "_"
                + KademliaCommonConfig.FINDMODE
                + ".csv")) {

      // Check if the file is empty (newly created)
      File file =
          new File(
              logFolderName
                  + "/count_"
                  + size
                  + "_"
                  + trafficStep
                  + "_"
                  + KademliaCommonConfig.FINDMODE
                  + ".csv");

      if (file.length() == 0) {
        // Write the count data to the "count" file
        String countHeader = "message_count, find_op, find_ok\n";
        countWriter.write(countHeader);
      }

      String countLine =
          // writtenMessages.size()
          // + ","
          msg_deliv.getN() + "," + find_op.getN() + "," + find_ok.getN() + "\n";
      countWriter.write(countLine);
      countWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    messages.clear();
    find_log.clear();
  }

  /**
   * Print the statistical snapshot of the current situation.
   *
   * @return always false
   */
  public boolean execute() {
    // Get the real network size
    int sz = Network.size();
    for (int i = 0; i < Network.size(); i++) {
      if (!Network.get(i).isUp()) {
        sz--;
      }
    }

    System.gc();

    // Check if this is the last execution cycle of the experiment
    if ((shouldWriteOut())) {
      // Write out the logs to disk/permanent storage
      writeOut();
    }

    return false;
  }

  // Reset accumulatedDataSize after writing out the logs
  private void resetAccumulatedDataSize() {
    accumulatedMessageCount = 0;
  }

  private boolean shouldWriteOut() {
    if (CommonState.getEndTime() <= (observerStep + CommonState.getTime())
        || find_op.getN() % 50 == 0) {
      return true;
    }

    return false;
  }

  /**
   * Reports a message, adding it to the message log if it has a source.
   *
   * @param m The message to report
   * @param sent a boolean indicating whether the message was sent or received.
   */
  public static void reportMsg(Message m, boolean sent) {
    // Messages without a source are control messages sent by the traffic control,
    // so we don't want to log them.
    if (m.src == null) return;

    // String messageId = String.valueOf(m.id);

    // Add the message to the message log, but first check if it hasn't already been added
    // Removing the checks for performance reasons when dealing with many lookups
    // if (!writtenMessages.contains(messageId)) {
    messages.put(String.valueOf(m.id), m.toMap(sent));
    msg_deliv.add(1);
    accumulatedMessageCount++;
    // }
  }

  /**
   * Reports an operation, adding it to the find operation log.
   *
   * @param op The operation to report.
   */
  public static void reportOperation(Operation op) {

    // String operationId = String.valueOf(op.getId());
    // Add the operation to the operation log, but first check if it hasn't already been added
    // Commenting out the checks for performance reasons when dealing with large lookups
    // if (!writtenOperations.contains(operationId)) {
    // Calculate the operation stop time and then add the opearation to the find operation log.
    op.setStopTime(CommonState.getTime() - op.getTimestamp());
    find_log.put(String.valueOf(op.getId()), op.toMap());
    // }
  }
}
