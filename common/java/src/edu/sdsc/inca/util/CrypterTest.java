package edu.sdsc.inca.util;

import junit.framework.TestCase;

/**
 * @author Jim Hayes
 */
public class CrypterTest extends TestCase {

  static final String PASSWORD = "Inc@P@assw0rd";
  static final String TEST_DATA = "This is a test";

  public void testEncrypt() throws Exception {
    Crypter c = new Crypter(PASSWORD, "AES");
    String encrypted = c.encrypt(TEST_DATA);
    assertFalse(TEST_DATA.equals(encrypted));
  }

  public void testDecrypt() throws Exception {
    Crypter c = new Crypter(PASSWORD, "AES");
    String encrypted = c.encrypt(TEST_DATA);
    String decrypted = c.decrypt(encrypted);
    assertFalse(decrypted.equals(encrypted));
    assertEquals(TEST_DATA, decrypted);
  }

  public void testEncryptSubstrings() throws Exception {
    Crypter c = new Crypter(PASSWORD, "AES");
    String encrypted = c.encryptMatchingSubstrings(TEST_DATA, "is");
    assertFalse(TEST_DATA.equals(encrypted));
    assertTrue(encrypted.matches("Th.* .* a test"));
  }

  public void testDecryptSubstrings() throws Exception {
    Crypter c = new Crypter(PASSWORD, "AES");
    // Encrypt everything between the first and last space
    String encrypted = c.encryptMatchingSubstrings(TEST_DATA, "(?<= ).*(?= )");
    String decrypted = c.decryptMatchingSubstrings(encrypted, "(?<= ).*(?= )");
    assertFalse(decrypted.equals(encrypted));
    assertEquals(TEST_DATA, decrypted);
  }

}
