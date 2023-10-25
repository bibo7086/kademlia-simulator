package peersim.kademlia;

import java.math.BigInteger;

/**
 * Some utility and mathematical function to work with BigInteger numbers and strings.
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class Util {

  /**
   * Given two numbers, returns the length of the common prefix, i.e. how many digits (in base 2)
   * have in common from the leftmost side of the number
   *
   * @param b1 BigInteger
   * @param b2 BigInteger
   * @return int
   */
  public static int prefixLen(BigInteger b1, BigInteger b2) {
    String s1 = put0(b1);
    String s2 = put0(b2);

    int i = 0;
    for (i = 0; i < s1.length(); i++) {
      if (s1.charAt(i) != s2.charAt(i)) return i;
    }

    return i;
  }

  public static final int prefixLen2(String b1, String b2) {
    String s1 = put0_test(b1);
    String s2 = put0_test(b2);

    int i = 0;
    for (i = 0; i < s1.length(); i++) {
      if (s1.charAt(i) != s2.charAt(i)) return i;
    }

    return i;
  }

  /**
   * return the distance between two number which is defined as (a XOR b)
   *
   * @param a BigInteger
   * @param b BigInteger
   * @return BigInteger
   *     <p>public static BigInteger xorDistance(BigInteger a, BigInteger b) { return a.xor(b); }
   */

  /**
   * Convert a BigInteger into a String (base 2) and lead all needed non-significant zeroes in order
   * to reach the canonical length of a nodeid
   *
   * @param b BigInteger
   * @return String
   */
  public static String put0(BigInteger b) {
    if (b == null) return null;

    String s = b.toString(2); // base 2
    int canonicalLength = KademliaCommonConfig.BITS;

    while (s.length() < canonicalLength) {
      s = "0" + s;
    }

    return s;
  }

  public static String put0_test(String input) {
    if (input == null) {
      return null;
    }

    // Convert the input string to a binary representation
    StringBuilder binaryStringBuilder = new StringBuilder();
    for (char c : input.toCharArray()) {
      // Convert each character to binary (8 bits)
      String binaryChar = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
      binaryStringBuilder.append(binaryChar);
    }

    String binaryResult = binaryStringBuilder.toString();

    // Ensure the binary representation has a fixed length of 256 bits with leading zeros
    while (binaryResult.length() < 256) {
      binaryResult = "0" + binaryResult;
    }

    return binaryResult;
  }
  /**
   * Measures the log-distance between two BigInteger values using the length of the differing
   * suffix in bits
   *
   * @param a the first BigInteger value
   * @param b the second BigInteger value
   * @return the log-distance between the two values
   */
  public static int logDistance(String a, String b) {

    BigInteger a_bigInteger = new BigInteger(a.getBytes());
    BigInteger b_bigInteger = new BigInteger(b.getBytes());

    BigInteger x = a_bigInteger.xor(b_bigInteger);

    return x.bitLength();
  }

  /**
   * Returns the XOR distance between two BigInteger values
   *
   * @param a the first BigInteger value
   * @param b the second BigInteger value
   * @return the XOR distance between a and b as a BigInteger
   */
  public static BigInteger xorDistance(String a, String b) {

    BigInteger a_bigInteger = new BigInteger(a.getBytes());
    BigInteger b_bigInteger = new BigInteger(b.getBytes());

    return a_bigInteger.xor(b_bigInteger);
  }
}
