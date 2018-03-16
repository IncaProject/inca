package edu.sdsc.inca.depot.util;

import edu.sdsc.inca.depot.persistent.AcceptedOutput;
import edu.sdsc.inca.depot.persistent.Arg;
import edu.sdsc.inca.depot.persistent.Report;
import edu.sdsc.inca.depot.persistent.Series;
import junit.framework.TestCase;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 *
 * @author jhayes
 *
 */
public class ExprComparitorTest extends TestCase {

  private static Logger logger = Logger.getLogger(ExprComparitor.class);

  public static final String GCC_BODY =
    "    <package>" +
    "      <ID>gcc</ID>" +
    "      <version>3.3</version>" +
    "    </package>";
  public static final String GCC_UNIT_BODY =
    "  <body>" +
    "    <unitTest>" +
    "      <ID>gcc</ID>" +
    "    </unitTest>" +
    "  </body>";
  public static final String GLOBUS_BODY =
    "    <package>" +
    "      <ID>globus</ID>" +
    "      <subpackage>" +
    "        <ID>globus_common</ID>" +
    "        <version>3.17.0</version>" +
    "      </subpackage>" +
    "      <subpackage>" +
    "        <ID>globus_common_setup</ID>" +
    "        <version>2.2.0</version>" +
    "      </subpackage>" +
    "      <subpackage>" +
    "        <ID>globus_core</ID>" +
    "        <version>2.19.0</version>" +
    "      </subpackage>" +
    "      <subpackage>" +
    "        <ID>globus_cyrus</ID>" +
    "        <version>1.5.27</version>" +
    "      </subpackage>" +
    "      <subpackage>" +
    "        <ID>globus_data_conversion</ID>" +
    "        <version>2.0.0</version>" +
    "      </subpackage>" +
    "      <subpackage>" +
    "        <ID>globus_duct_common</ID>" +
    "        <version>2.0.0</version>" +
    "      </subpackage>" +
    "      <subpackage>" +
    "        <ID>globus_duct_control</ID>" +
    "        <version>2.0.0</version>" +
    "      </subpackage>" +
    "    </package>";

  public String tryIt(String expr, Report r, boolean expectSuccess) {
    AcceptedOutput ao = new AcceptedOutput("ExprComparitor", expr);
    ExprComparitor comp = new ExprComparitor();
    String result = comp.compare(ao, r);
    if(expectSuccess && !result.startsWith(ExprComparitor.SUCCESS_RESULT)) {
      return result;
    } else if(!expectSuccess &&
              !result.startsWith(ExprComparitor.FAILURE_RESULT)) {
      return "Succeeded on false test " + expr;
    } else {
      return null;
    }
  }

  public String tryIt(String expr, String body, boolean expectSuccess) {
    return tryIt
      (expr, new Report(new Boolean(true), null, body, null), expectSuccess);
  }

  public void testSimpleConstants() {
    String result;
    String[] falseExprs = {
      "2 < 1", "2 <= 1", "1 > 2", "1 >= 2", "1 == 2", "2 != 2"
    };
    String[] trueExprs = {
      "1 < 2", "1 <= 2", "2 > 1", "2 >= 1", "2 == 2", "2 >= 2", "2 <= 2",
      "1 != 2"
    };
    for(int i = 0; i < falseExprs.length; i++) {
      result = tryIt(falseExprs[i], "", false);
      if(result != null) {
        fail(result);
      }
    }
    for(int i = 0; i < trueExprs.length; i++) {
      result = tryIt(trueExprs[i], "", true);
      if(result != null) {
        fail(result);
      }
    }
  }

  public void testDifferentDigits() {
    String result;
    if((result = tryIt("10 < 2", "", false)) != null ||
       (result = tryIt("10 > 2", "", true)) != null) {
      fail(result);
    }
  }

  public void testPoints() {
    String result;
    if((result = tryIt("10 < 8.3", "", false)) != null ||
       (result = tryIt("10 < 10.1", "", true)) != null ||
       (result = tryIt("10.1 < 10.8", "", true)) != null ||
       (result = tryIt("10.1.2 < 10.8", "", true)) != null ||
       (result = tryIt("10.1 < 10.1.5", "", true)) != null ||
       (result = tryIt("10.1.3 < 10.0.3", "", false)) != null) {
      fail(result);
    }
  }

  public void testAlpha() {
    String result;
    if((result = tryIt("'b' < 'a'", "", false)) != null ||
       (result = tryIt("10b > 10a", "", true)) != null ||
       (result = tryIt("10.a7 < 10.b1", "", true)) != null) {
      fail(result);
    }
  }

  public void testPackageName() {
    String result;
    if((result = tryIt("gcc < 3.3", GCC_BODY, false)) != null ||
       (result = tryIt("gcc == 3.3", GCC_BODY, true)) != null ||
       (result = tryIt("gcc != 3.3.1", GCC_BODY, true)) != null) {
      fail(result);
    }
  }

  public void testSubpackageNames() {
    String result;
    if((result = tryIt("globus_core < 2", GLOBUS_BODY, false)) != null ||
       (result = tryIt("globus_cyrus >= 1.5", GLOBUS_BODY, true)) != null ||
       (result = tryIt("globus_core != 3.3.7", GLOBUS_BODY, true)) != null ||
       (result = tryIt("globus_cyrus == 1.5.27", GLOBUS_BODY, true)) != null) {
      fail(result);
    }
  }

  public void testPatterns() {
    String result;
    if((result = tryIt("globus_core =~ /2/", GLOBUS_BODY, true)) != null ||
       (result = tryIt("globus_core !~ /3/", GLOBUS_BODY, true)) != null ||
       (result = tryIt("globus_core !~ /19/", GLOBUS_BODY, false)) != null ||
       (result = tryIt("globus_core =~ /2.*0/", GLOBUS_BODY, true)) != null) {
      fail(result);
    }
  }

  public void testValuelessId() {
    String result;
    if((result = tryIt("gcc != \"\"", GCC_UNIT_BODY, false)) != null ||
       (result = tryIt("gcc == \"\"", GCC_UNIT_BODY, true)) != null) {
      fail(result);
    }
  }

  public void testConjunctions() {
    String result;
    if((result = tryIt("globus_core < 2 || globus_cyrus < 2", GLOBUS_BODY, true)) != null ||
       (result = tryIt("globus_cyrus >= 1.5 || globus_core > 11", GLOBUS_BODY, true)) != null ||
       (result = tryIt("globus_core != 3.3.7 && globus_core != 2.19.0", GLOBUS_BODY, false)) != null ||
       (result = tryIt("globus_cyrus == 1.5.27 && globus_core == 2.19.0", GLOBUS_BODY, true)) != null) {
      fail(result);
    }
  }

  public void testParentheses() {
    String result;
    if((result = tryIt("(globus_core =~ /2/ && globus_core != 2) || (globus_core =~ /3/ && globus_core != 3)", GLOBUS_BODY, true)) != null ||
       (result = tryIt("(globus_core =~ /2/ && globus_core == 2) || (globus_core =~ /3/ && globus_core == 3)", GLOBUS_BODY, false)) != null ||
       (result = tryIt("globus_core == 3.0 || (globus_core =~ /2/ && globus_core != 2)", GLOBUS_BODY, true)) != null) {
      fail(result);
    }
  }

  public void testFailureMessage() {
    String result = tryIt("gcc == 2", GCC_BODY, true);
    if(result == null) {
      fail("Failed to fail");
    }
    String[] pieces = result.split("[: ]+");
    boolean gccSeen = false;
    for(int i = 0; i < pieces.length; i++) {
      String piece = pieces[i];
      if(piece.equals(ExprComparitor.FAILURE_RESULT)) {
        continue;
      } else if(piece.equals("gcc")) {
        gccSeen = true;
      } else {
        fail("Unexpected text '" + piece + "' in message '" + result + "'");
      }
    }
    if(!gccSeen) {
      fail("gcc missing from message '" + result + "'");
    }
    result = tryIt("(globus_core == 2 || globus_cyrus == 3) || globus_common == globus_common_setup", GLOBUS_BODY, true);
    if(result == null) {
      fail("Failed to fail");
    }
    pieces = result.split("[: ]+");
    boolean globusCoreSeen = false;
    boolean globusCyrusSeen = false;
    boolean globusCommonSeen = false;
    boolean globusCommonSetupSeen = false;
    for(int i = 0; i < pieces.length; i++) {
      String piece = pieces[i];
      if(piece.equals(ExprComparitor.FAILURE_RESULT)) {
        continue;
      } else if(piece.equals("globus_core")) {
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

  public void testPredefinedIds() {
    Report r = new Report(new Boolean(false), "Host lskjdf.com not found",
                          "", null);
    String result = tryIt("errorMessage =~ /lskjdf/", r, true);
    if(result != null) {
      fail("errorMessage id failed");
    }
    r = new Report(new Boolean(true), null, GCC_BODY, null);
    result = tryIt("body =~ /<package>/", r, true);
    if(result != null) {
      fail("body id failed");
    }
  }

  public void testArgs() {
    Series s = Series.generate("localhost", "no context", 3);
    String body = s.generateReport();
    body = body.substring(body.indexOf("<body>") + 6);
    body = body.substring(0, body.indexOf("</body>"));
    Report r = new Report(new Boolean(true), null, body, s);
    String expr = "1==1";
    Iterator it = s.getArgs().iterator();
    while(it.hasNext()) {
      Arg a = (Arg)it.next();
      expr += "&&" + a.getName() + "==" + a.getValue();
    }
    String result = tryIt(expr, r, true);
    if(result != null) {
      fail("arg id failed");
    }
    // See if a setting in the body overrides args
    int value = 54;
    expr = "1==1";
    it = s.getArgs().iterator();
    while(it.hasNext()) {
      Arg a = (Arg)it.next();
      body += "<override><ID>" + a.getName() + "</ID><value>" + value + "</value></override>";
      expr += "&&" + a.getName() + "==" + value;
      value *= 7;
    }
    r.setBody("<wrapper>" + body + "</body>");
    result = tryIt(expr, r, true);
    if(result != null) {
      fail("arg override failed");
    }
  }

}
