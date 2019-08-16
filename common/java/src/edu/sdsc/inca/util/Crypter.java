package edu.sdsc.inca.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

// Adapted from http://javaalmanac.com/egs/javax.crypto/PassKey.html
/**
 * The Crypter class provides the ability to de/encrypt strings.
 */
public class Crypter {

  private static final byte[] LF = "\n".getBytes();
  protected Cipher decipher;
  protected Cipher encipher;

  /**
   * Constructs a Crypter that can de/encrypt strings using a specified
   * passphrase and algorithm.  The caller must assure that an installed
   * security provider supports the specified algorithm.
   *
   * @param passphrase the de/encryption passphrase
   * @param algorithm the de/encryption algorithm, e.g., DES
   * @throws CrypterException if initialization of the ciphers fail
   */
  public Crypter(String passphrase, String algorithm) throws CrypterException {

    final int ITERATIONS = 1024;
    final int KEY_LENGTH = 128;
    final byte[] SALT = {
      (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
      (byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03
    };
    try {
      KeySpec keySpec =
        new PBEKeySpec(passphrase.toCharArray(), SALT, ITERATIONS, KEY_LENGTH);
      SecretKey key =
       SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
      // "salt" the key to make it harder to break
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(key.getEncoded());
      md.update(SALT);
      for(int i = 0; i < ITERATIONS; i++) {
        md.update(md.digest());
      }
      SecretKeySpec paramSpec = new SecretKeySpec(md.digest(), algorithm);
      this.decipher = Cipher.getInstance(algorithm);
      this.decipher.init(Cipher.DECRYPT_MODE, paramSpec);
      this.encipher = Cipher.getInstance(algorithm);
      this.encipher.init(Cipher.ENCRYPT_MODE, paramSpec);
    } catch ( Exception e ) {
      throw new CrypterException( "Encryption error: " + e );
    }
  }

  /**
   * Returns the decrypted version of a specified string.
   *
   * @param s the string to decrypt
   * @return the decrypted equivalent of the string
   * @throws CrypterException on error
   */
  public String decrypt(String s) throws CrypterException {

    try {
      ByteArrayInputStream inBytes = new ByteArrayInputStream(s.getBytes());
      Base64InputStream decoder = new Base64InputStream(inBytes);
      byte[] dec;

      try {
        int bytesRead;
        byte[] readBuffer = new byte[8192];
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        while ((bytesRead = decoder.read(readBuffer, 0, readBuffer.length)) > 0)
          result.write(readBuffer, 0, bytesRead);

        dec = result.toByteArray();
      }
      finally {
        decoder.close();
      }

      byte[] utf8 = this.decipher.doFinal(dec);

      return new String(utf8, "UTF8");
    } catch(Exception e) {
      throw new CrypterException(e.toString());
    }
  }

  /**
   * Returns a specified string with all substrings that match a specified
   * pattern decrypted.
   *
   * @param s the string to decrypt
   * @param p the pattern to search for and decrypt in the string
   * @return the string with matching substrings decrypted
   * @throws CrypterException on error
   */
  public String decryptMatchingSubstrings(String s, Pattern p)
    throws CrypterException {
    return this.cryptMatchingSubstrings(s, p, true);
  }

  /**
   * Returns a specified string with all substrings that match a specified
   * pattern decrypted.
   *
   * @param s the string to decrypt
   * @param p the pattern to search for and decrypt in the string
   * @return the string with matching substrings decrypted
   * @throws CrypterException on error
   * @throws PatternSyntaxException if the pattern is faulty
   */
  public String decryptMatchingSubstrings(String s, String p)
    throws CrypterException, PatternSyntaxException {
    return this.decryptMatchingSubstrings(s, Pattern.compile(p));
  }

  /**
   * Returns the encrypted version of a specified string.
   *
   * @param s the string to encrypt
   * @return the encrypted equivalent of the string, or null on error
   * @throws CrypterException on error
   */
  public String encrypt(String s) throws CrypterException {
    try {
      byte[] enc = this.encipher.doFinal(s.getBytes("UTF8"));
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      OutputStream encoder = new Base64OutputStream(result, true, 76, LF);

      try {
        encoder.write(enc);
      }
      finally {
        encoder.close();
      }

      return result.toString();
    } catch(Exception e) {
      throw new CrypterException(e.toString());
    }
  }

  /**
   * Returns a specified string with all substrings that match a specified
   * pattern encrypted.
   *
   * @param s the string to encrypt
   * @param p the pattern to search for and encrypt in the string
   * @return the string with matching substrings encrypted
   */
  public String encryptMatchingSubstrings(String s, Pattern p)
    throws CrypterException {
    return this.cryptMatchingSubstrings(s, p, false);
  }

  /**
   * Returns a specified string with all substrings that match a specified
   * pattern encrypted.
   *
   * @param s the string to encrypt
   * @param p the pattern to search for and encrypt in the string
   * @return the string with matching substrings encrypted
   * @throws CrypterException on error
   * @throws PatternSyntaxException if the pattern is faulty
   */
  public String encryptMatchingSubstrings(String s, String p)
    throws CrypterException, PatternSyntaxException {
    return this.encryptMatchingSubstrings(s, Pattern.compile(p));
  }

  /**
   * Return a cipher that can be used for decryption.
   *
   * @return A cipher object.
   */
  public Cipher getDecipher() {
    return decipher;
  }

  /**
   * Return a cipher that can be used for encryption.
   *
   * @return A cipher object.
   */
  public Cipher getEncipher() {
    return encipher;
  }

  /**
   * Returns a specified string with all substrings that match a specified
   * pattern de/encrypted.
   *
   * @param s the string to encrypt
   * @param p the pattern to search for and de/encrypt in the string
   * @param decrypt whether the method should decrypt or decrypt
   * @return the string with matching substrings de/encrypted
   * @throws CrypterException on error
   */
  protected String cryptMatchingSubstrings
    (String s, Pattern p, boolean decrypt) throws CrypterException {
    Matcher m = p.matcher(s);
    String result = "";
    int end = 0;
    while(m.find(end)) {
      int start = m.start();
      result += s.substring(end, start);
      end = m.end();
      String crypted = s.substring(start, end);
      crypted = decrypt ? this.decrypt(crypted) : this.encrypt(crypted);
      result += crypted;
    }
    result += s.substring(end);
    return result;
  }

}
