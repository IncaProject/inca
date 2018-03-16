package edu.sdsc.inca.protocol;

import java.util.Arrays;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit tests for Statment class.
 *
 * @author cmills
 *         *         Date: Feb 8, 2005
 *         *         Time: 2:14:44 PM
 *         *         Description: Unit tests for statement class.
 *         * The only real special case to hit here is when a char[] comes in and has CRLF at the end.
 */
public class StatementTest extends TestCase {

  char[] testcmd = new char[]{'R', 'E', 'P', 'O', 'R', 'T'};
  char[] testdata = new char[]{'D', 'A', 'T', 'A'};
  char[] testStatement = new char[]{'R', 'E', 'P', 'O', 'R', 'T', ' ', 'D', 'A', 'T', 'A'};
  char[] testStatementwcrlf = new char[]{'R', 'E', 'P', 'O', 'R', 'T', ' ', 'D', 'A', 'T', 'A', '\r', '\n'};

  /**
   * test get and set after testing constructor.
   */
  public void testConstructGetSet() {
    Statement statement = new Statement();
    statement.setCmd(testcmd);
    statement.setData(testdata);
    Assert.assertNotSame(testcmd, statement.getCmd());
    Assert.assertNotSame(testdata, statement.getData());
    Assert.assertTrue(Arrays.equals(testcmd, statement.getCmd()));
    Assert.assertTrue(Arrays.equals(testdata, statement.getData()));
    Assert.assertTrue(Arrays.equals(testStatement, statement.toChars()));
  }

  /**
   * Test the constructor.
   */
  public void testConstruct() {
    Statement statement = new Statement(testStatement);
    Assert.assertNotSame(testcmd, statement.getCmd());
    Assert.assertNotSame(testdata, statement.getData());
    Assert.assertTrue(Arrays.equals(testcmd, statement.getCmd()));
    Assert.assertTrue(Arrays.equals(testdata, statement.getData()));
    Assert.assertTrue(Arrays.equals(testStatement, statement.toChars()));
  }

}
