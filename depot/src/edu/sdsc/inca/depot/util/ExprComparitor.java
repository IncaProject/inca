package edu.sdsc.inca.depot.util;

import edu.sdsc.inca.depot.persistent.AcceptedOutput;
import edu.sdsc.inca.depot.persistent.Arg;
import edu.sdsc.inca.depot.persistent.PersistenceException;
import edu.sdsc.inca.depot.persistent.Report;
import edu.sdsc.inca.depot.persistent.Row;
import edu.sdsc.inca.depot.persistent.Series;
import edu.sdsc.inca.util.ExprEvaluator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Matches the contents of certain tags in a report body against an expected
 * expression.  Supports the boolean binary operators &lt;, &lt;=, &gt;, &gt;=,
 * ==, !=, &lt;&gt;, &amp;&amp;, and ||, as well as perl's pattern match (=~)
 * and mismatch (!~) operators.  Supports parenthesized sub-expressions.
 * Operands may be any of: a quoted string literal; a numeric literal; a
 * pattern literal enclosed in slashes; an identifier defined in the report
 * body by an ID tag; a version literal composed of digits, letters,
 * underscores and dots.
 *
 * @author jhayes
 */
public class ExprComparitor {

  public static final String FAILURE_RESULT = "Failure";
  public static final String SUCCESS_RESULT = "Success";

  private static Logger logger = Logger.getLogger(ExprComparitor.class);
  private static Pattern ID_PATTERN =
    Pattern.compile("<(ID|name)>([^<]*)</\\1>\\s*(<(\\w+)>(.*?)</\\4>)?");

  /**
   * Compares a report against acceptable output.
   *
   * @param ao the acceptable output secification
   * @param report the report to compare
   * @return a string that starts with either FAILURE_RESULT or SUCCESS_RESULT,
   *         depending on whether or not the comparision succeeded
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public String compare(AcceptedOutput ao, Report report) throws IOException, SQLException, PersistenceException {
    Properties symbolTable = new Properties();
    // Generate symbols for report body, arguments, and error message
    String body = report.getBody();
    if(body == null || body.equals(Row.DB_EMPTY_STRING)) {
      body = "";
    }
    symbolTable.setProperty("body", body);
    Series s = report.getSeries();
    if(s != null) {
      Iterator<Arg> it = s.getArgSignature().getArgs().iterator();
      while(it.hasNext()) {
        Arg a = it.next();
        symbolTable.setProperty(a.getName(), a.getValue());
      }
    }
    String errorMessage = report.getExit_message();
    if(errorMessage == null ||
       errorMessage.equals(Row.DB_EMPTY_STRING)) {
      errorMessage = "";
    }
    symbolTable.setProperty("errorMessage", errorMessage);
    String expr = ao.getComparison();
    if(expr == null) {
      logger.warn("Null expression passed to ExprComparitor");
      expr = "1==0"; // Handle error gracefully
    }
    logger.debug("Compare '" + body + "' to '" + expr + "'");
    // For each <ID>name</ID><any>text</any> tag pair in the body, assign text
    // as the value of the name; ditto for <name>name</name><any>text</any>
    Matcher m = ID_PATTERN.matcher(body);
    for(int index = 0; m.find(index); index = m.start() + 1) {
      String name = body.substring(m.start(2), m.end(2));
      String value = m.start(5) < 0 ? "" : body.substring(m.start(5), m.end(5));
      symbolTable.setProperty(name, value);
    }
    String result = ExprEvaluator.eval(expr, symbolTable, null);
    if(result == null) {
      return SUCCESS_RESULT;
    }
    return FAILURE_RESULT + ":" +
           result.replaceAll(ExprEvaluator.FAIL_EXPR_FAILED + ":", "");
  }

}
