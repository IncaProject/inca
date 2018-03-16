package edu.sdsc.inca.util;

import java.util.HashSet;
import junit.framework.TestCase;

/**
 * A JUnit for the ExpandablePattern class.
 */
public class ExpandablePatternTest extends TestCase {

  /**
   * Tests a HashSet to see if it contains the expected members.
   *
   * @param h the HashSet to test
   * @param correct the strings expected to be found in the HashSet
   * @return null on success, an error message otherwise
   */
  public String compare(HashSet h, String[] correct) {
    String[] found = (String [])h.toArray(new String[0]);
    for(int i = 0; i < correct.length; i++) {
      String expected = correct[i];
      int j;
      for(j = 0; j < found.length && !expected.equals(found[j]); j++) {
        // empty
      }
      if(j == found.length) {
        String msg = "Expected '" + expected + "' not found in [";
        for(j = 0; j < found.length; j++) {
          msg += " '" + found[j] + "'";
        }
        msg += " ]";
        return msg;
      }
    }
    if(found.length == correct.length) {
      return null;
    }
    for(int i = 0; i < found.length; i++) {
      String included = found[i];
      int j;
      for(j = 0; j < correct.length && !included.equals(correct[j]); j++) {
        // empty
      }
      if(j < correct.length) {
        return "Unexpected '" + included + "' found";
      }
    }
    return "Duplicates in results";
  }

  /**
   * Tests that translation from glob to regexp is done correctly.
   */
  public void testGlob() {
    ExpandablePattern ep = new ExpandablePattern("a?b*c?d*", true);
    assertEquals("a.b.*c.d.*", ep.toString());
  }

  /**
   * Tests expand for an empty pattern.
   */
  public void testEmptyPattern() throws Exception {
    HashSet s = new ExpandablePattern("").expand();
    String msg = compare(s, new String[] {});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests expand for a simple pattern.
   */
  public void testSimplePattern() throws Exception {
    HashSet s = new ExpandablePattern("a").expand();
    String msg = compare(s, new String[] {"a"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests expand for concatenation.
   */
  public void testConcatenation() throws Exception {
    HashSet s = new ExpandablePattern("abcd").expand();
    String msg = compare(s, new String[] {"abcd"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests expand for alternation.
   */
  public void testAlternation() throws Exception {
    HashSet s = new ExpandablePattern("a|b|c|d").expand();
    String msg = compare(s, new String[] {"a", "b", "c", "d"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests expand for a mix of alternation and catenation.
   */
  public void testAlternationAndCatenation() throws Exception {
    HashSet s = new ExpandablePattern("ab|cd").expand();
    String msg = compare(s, new String[] {"ab", "cd"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests expand for nesting.
   */
  public void testNesting() throws Exception {
    HashSet s = new ExpandablePattern("(ab)").expand();
    String msg = compare(s, new String[] {"ab"});
    if(msg != null) {
      fail(msg);
    }
    s = new ExpandablePattern("((ab))").expand();
    msg = compare(s, new String[] {"ab"});
    if(msg != null) {
      fail(msg);
    }
    s = new ExpandablePattern("(a|b)c").expand();
    msg = compare(s, new String[] {"ac", "bc"});
    if(msg != null) {
      fail(msg);
    }
    s = new ExpandablePattern("(a|b)(c|d)").expand();
    msg = compare(s, new String[] {"ac", "bc", "ad", "bd"});
    if(msg != null) {
      fail(msg);
    }
    s = new ExpandablePattern("(ab)|(cd)").expand();
    msg = compare(s, new String[] {"ab", "cd"});
    if(msg != null) {
      fail(msg);
    }
    s = new ExpandablePattern("((a|b)c)|(de)").expand();
    msg = compare(s, new String[] {"ac", "bc", "de"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests expand for duplicate filtering.
   */
  public void testDuplicates() throws Exception {
    HashSet s = new ExpandablePattern("a|a").expand();
    String msg = compare(s, new String[] {"a"});
    if(msg != null) {
      fail(msg);
    }
    s = new ExpandablePattern("(a|b)|(c|a)|(b|a)").expand();
    msg = compare(s, new String[] {"a", "b", "c"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests expand for the ? qualifier.
   */
  public void testOptional() throws Exception {
    HashSet s = new ExpandablePattern("a?").expand();
    String msg = compare(s, new String[] {"a", ""});
    if(msg != null) {
      fail(msg);
    }
    s = new ExpandablePattern("ab?").expand();
    msg = compare(s, new String[] {"ab", "a"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests expand for the {} qualifier.
   */
  public void testRange() throws Exception {
    HashSet s = new ExpandablePattern("a{2}").expand();
    String msg = compare(s, new String[] {"aa"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests expand for patterns that match an infinite set of strings.
   */
  public void testInfinite() throws Exception {
    HashSet s = null;
    try {
      s = new ExpandablePattern(".").expand();
      fail("No exception for .");
    } catch(Exception e) {
      // empty
    }
    try {
      s = new ExpandablePattern("a*").expand();
      fail("No exception for *");
    } catch(Exception e) {
      // empty
    }
    try {
      s = new ExpandablePattern("a+").expand();
      fail("No exception for *");
    } catch(Exception e) {
      // empty
    }
  }

  /**
   * Tests escaping of special characters.
   */
  public void testEscape() throws Exception {
    HashSet s =
      new ExpandablePattern("\\?\\+\\.\\*\\[\\\\\\{\\|\\(\\^\\$").expand();
    String msg = compare(s, new String[] {"?+.*[\\{|(^$"});
    if(msg != null) {
      fail(msg);
    }
    s = new ExpandablePattern("[\\]][\\^][a\\-z]").expand();
    msg = compare(s, new String[] {"]^a", "]^-", "]^z"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests special escape characters.
   */
  public void testSpecialEscape() throws Exception {
    HashSet s = new ExpandablePattern("\\a\\e\\f\\n\\r\\t").expand();
    String msg = compare(s, new String[] {"\007\033\014\012\015\011"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests numeric escape characters.
   */
  public void testNumericEscape() throws Exception {
    HashSet s =
      new ExpandablePattern("\\07\\011\\0111\\x4a\\x4B\\u004a\\u004B").expand();
    String msg = compare(s, new String[] {"\007\tIJKJK"});
    if(msg != null) {
      fail(msg);
    }
    s = new ExpandablePattern
      ("[\\07\\011\\0111\\x4a\\u004B\\u004a\\u4B]").expand();
    msg = compare(s, new String[] {"\007", "\t", "I", "J", "K"});
    if(msg != null) {
      fail(msg);
    }
  }

  /**
   * Tests expand for character sets.
   */
  public void testCharSets() throws Exception {
    HashSet s = new ExpandablePattern("[ab0-2D-F]").expand();
    String msg =
      compare(s, new String[] {"a", "b", "0", "1", "2", "D", "E", "F"});
    if(msg != null) {
      fail(msg);
    }
  }

}
