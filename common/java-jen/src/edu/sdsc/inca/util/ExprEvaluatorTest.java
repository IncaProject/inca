package edu.sdsc.inca.util;

import java.util.Properties;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

/**
 *
 * @author jhayes
 *
 */
public class ExprEvaluatorTest extends TestCase {

  private static Logger logger = Logger.getLogger(ExprEvaluator.class);
  private static Properties symbols = new Properties();
  static {
    symbols.setProperty("gcc", "3.3");
    symbols.setProperty("globus_common", "3.17.0");
    symbols.setProperty("globus_common", "3.17.0");
    symbols.setProperty("globus_common_setup", "2.2.0");
    symbols.setProperty("globus_core", "2.19.0");
    symbols.setProperty("globus_cyrus", "1.5.27");
    symbols.setProperty("globus_data_conversion", "2.0.0");
    symbols.setProperty("globus_duct_common", "2.0.0");
    symbols.setProperty("globus_duct_control", "2.0.0");
    symbols.setProperty("has:colons:in:it", "9.8.2");
    symbols.setProperty("multiline", "A first\nsecond\nand third line");
  }

  public String tryIt(String expr, boolean expectSuccess) {
    String result = ExprEvaluator.eval(expr, symbols, null);
    if(expectSuccess && result != null) {
      return result;
    } else if(!expectSuccess && result == null) {
      return "Succeeded on false test " + expr;
    } else {
      return null;
    }
  }

  public void testSimpleConstants() {
    String result = null;
    String[] falseExprs = {
      "2 < 1", "2 <= 1", "1 > 2", "1 >= 2", "1 == 2", "2 != 2"
    };
    String[] trueExprs = {
      "1 < 2", "1 <= 2", "2 > 1", "2 >= 1", "2 == 2", "2 >= 2", "2 <= 2",
      "1 != 2"
    };
    for(int i = 0; i < falseExprs.length; i++) {
      result = tryIt(falseExprs[i], false);
      if(result != null) {
        fail(result);
      }
    }
    for(int i = 0; i < trueExprs.length; i++) {
      result = tryIt(trueExprs[i], true);
      if(result != null) {
        fail(result);
      }
    }
  }

  public void testDifferentDigits() {
    String result;
    if((result = tryIt("10 < 2", false)) != null ||
       (result = tryIt("10 > 2", true)) != null) {
      fail(result);
    }
  }

  public void testPoints() {
    String result;
    if((result = tryIt("10 < 8.3", false)) != null ||
       (result = tryIt("10 < 10.1", true)) != null ||
       (result = tryIt("10.1 < 10.8", true)) != null ||
       (result = tryIt("10.1.2 < 10.8", true)) != null ||
       (result = tryIt("10.1 < 10.1.5", true)) != null ||
       (result = tryIt("10.1.3 < 10.0.3", false)) != null) {
      fail(result);
    }
  }

  public void testAlpha() {
    String result;
    if((result = tryIt("'b' < 'a'", false)) != null ||
       (result = tryIt("10b > 10a", true)) != null ||
       (result = tryIt("10.a7 < 10.b1", true)) != null) {
      fail(result);
    }
  }

  public void testPackageName() {
    String result;
    if((result = tryIt("gcc < 3.3", false)) != null ||
       (result = tryIt("gcc == 3.3", true)) != null ||
       (result = tryIt("gcc != 3.3.1", true)) != null) {
      fail(result);
    }
  }

  public void testSubpackageNames() {
    String result;
    if((result = tryIt("globus_core < 2", false)) != null ||
       (result = tryIt("globus_cyrus >= 1.5", true)) != null ||
       (result = tryIt("globus_core != 3.3.7", true)) != null ||
       (result = tryIt("globus_cyrus == 1.5.27", true)) != null) {
      fail(result);
    }
  }

  public void testPatterns() {
    String result;
    if((result = tryIt("globus_core =~ /2/", true)) != null ||
       (result = tryIt("globus_core =~ /./", true)) != null ||
       (result = tryIt("globus_core !~ /3/", true)) != null ||
       (result = tryIt("globus_core !~ /19/", false)) != null ||
       (result = tryIt("globus_core =~ /2.*0/", true)) != null) {
      fail(result);
    }
  }

  public void testMultilinePatterns() {
    String result;
    if((result = tryIt("multiline =~ /first/", true)) != null ||
       (result = tryIt("multiline =~ /second.*third/", true)) != null ||
       (result = tryIt("multiline =~ /^A f/", true)) != null ||
       (result = tryIt("multiline =~ /^second/", false)) != null ||
       (result = tryIt("multiline =~ /^A f.*line$/", true)) != null) {
      fail(result);
    }
  }

  public void testColons() {
    String result;
    if((result = tryIt("has:colons:in:it > 1", true)) != null) {
      fail(result);
    }
  }

  public void testConjunctions() {
    String result;
    if((result = tryIt("globus_core < 2 || globus_cyrus < 2", true)) != null ||
       (result = tryIt("globus_cyrus >= 1.5 || globus_core > 11", true)) != null ||
       (result = tryIt("globus_core != 3.3.7 && globus_core != 2.19.0", false)) != null ||
       (result = tryIt("globus_cyrus == 1.5.27 && globus_core == 2.19.0", true)) != null) {
      fail(result);
    }
  }

  public void testParentheses() {
    String result;
    if((result = tryIt("(globus_core =~ /2/ && globus_core != 2) || (globus_core =~ /3/ && globus_core != 3)", true)) != null ||
       (result = tryIt("(globus_core =~ /2/ && globus_core == 2) || (globus_core =~ /3/ && globus_core == 3)", false)) != null ||
       (result = tryIt("globus_core == 3.0 || (globus_core =~ /2/ && globus_core != 2)", true)) != null) {
      fail(result);
    }
  }

  public void testFailureMessage() {
    String result = tryIt("gcc == 2", true);
    if(result == null) {
      fail("Failed to fail");
    } else if(!result.startsWith(ExprEvaluator.FAIL_EXPR_FAILED + ":")) {
      fail("Unexpected error '" + result + "'");
    }
    String trimmed =
      result.substring(ExprEvaluator.FAIL_EXPR_FAILED.length() + 1).trim();
    String[] pieces = trimmed.split(" +");
    boolean gccSeen = false;
    for(int i = 0; i < pieces.length; i++) {
      String piece = pieces[i];
      if(piece.equals("gcc")) {
        gccSeen = true;
      } else {
        fail("Unexpected text '" + piece + "' in message '" + result + "'");
      }
    }
    if(!gccSeen) {
      fail("gcc missing from message '" + result + "'");
    }
    result = tryIt("(globus_core == 2 || globus_cyrus == 3) || globus_common == globus_common_setup", true);
    if(result == null) {
      fail("Failed to fail");
    } else if(!result.startsWith(ExprEvaluator.FAIL_EXPR_FAILED + ":")) {
      fail("Unexpected error '" + result + "'");
    }
    trimmed =
      result.substring(ExprEvaluator.FAIL_EXPR_FAILED.length() + 1).trim();
    pieces = trimmed.split(" +");
    boolean globusCoreSeen = false;
    boolean globusCyrusSeen = false;
    boolean globusCommonSeen = false;
    boolean globusCommonSetupSeen = false;
    for(int i = 0; i < pieces.length; i++) {
      String piece = pieces[i];
      if(piece.equals("globus_core")) {
        globusCoreSeen = true;
      } else if(piece.equals("globus_common")) {
        globusCommonSeen = true;
      } else if(piece.equals("globus_cyrus")) {
        globusCyrusSeen = true;
      } else if(piece.equals("globus_common_setup")) {
        globusCommonSetupSeen = true;
      } else {
        fail("Unexpected text '" + piece + "' in message '" + result + "'");
      }
    }
    if(!globusCoreSeen || !globusCyrusSeen || !globusCommonSeen ||
       !globusCommonSetupSeen) {
      fail("one or more of globus_core, globus_cyrus, globus_common, globus_common_setup missing from message '" + result + "'");
    }
  }

  public void testUndefId() {
    String result = tryIt("a < 16", true);
    assertEquals(ExprEvaluator.FAIL_UNDEFINED_SYMBOL + ":a", result);
  }

  public void testSyntaxError() {
    String result = tryIt("gcc 16", true);
    assertTrue("Invalid operator detected",
               result.startsWith(ExprEvaluator.FAIL_INVALID_OPERATOR));
    result = tryIt("< 16", true);
    assertEquals(ExprEvaluator.FAIL_MISSING_OPERAND, result);
    result = tryIt("gcc <", true);
    assertEquals(ExprEvaluator.FAIL_MISSING_OPERAND, result);
    result = tryIt("gcc < 16 < 2", true);
    assertEquals(ExprEvaluator.FAIL_MISSING_OPERATOR, result);
  }

}
