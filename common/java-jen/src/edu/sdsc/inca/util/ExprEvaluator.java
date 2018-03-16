package edu.sdsc.inca.util;

import edu.sdsc.inca.protocol.Protocol;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple top-down boolean expression evaluator.  Supports the boolean binary
 * operators &lt;, &lt;=, &gt;, &gt;=, ==, !=, &lt;&gt;, &amp;&amp;, and ||,
 * as well as perl's pattern match (=~) and mismatch (!~) operators.  Supports
 * parenthesized sub-expressions.  Operands may be any of: a quoted string
 * literal; a numeric literal; a pattern literal enclosed in slashes; an
 * identifier defined in the report body by an ID tag; a version literal
 * composed of digits, letters, underscores and dots.
 *
 * @author jhayes
 */
public class ExprEvaluator {

  /** Failure result values from eval. */
  public static String FAIL_EXPR_FAILED = "Expression failed";
  public static String FAIL_INVALID_OPERATOR = "Invalid operator";
  public static String FAIL_MISSING_OPERAND = "Missing operand";
  public static String FAIL_MISSING_OPERATOR = "Missing operator";
  public static String FAIL_MISSING_RPAREN = "Missing ')'";
  public static String FAIL_UNDEFINED_SYMBOL = "Undefined symbol";

  /** Operands: a quoted or slash-surrounded literal or a symbol. */
  protected static Pattern OPERAND_PATTERN =
    Pattern.compile ("(([\"'/]).*?\\2|[\\w\\.\\-:]+).*");

  /**
   * Evaluate a boolean expression given a set of symbols.
   *
   * @param expr the expression to evaluate
   * @param symbols a table that associates symbol names with values
   * @param valueIfUndef the value to use for identifiers encountered in the
   *   expression that have no value in the symbol table.  If null, the
   *   method throws an exception when it encounters an undefined symbol.
   * @return null if the expression is true; otherwise, a failure message that
   *   consists of one of the result values above optionally followed by a
   *   colon and a space-delimited set of symbols participating in the failure
   */
  public static String eval
    (String expr, Properties symbols, String valueIfUndef) {
    StringBuffer sb = new StringBuffer(expr);
    String result = conjunction(sb, symbols, valueIfUndef);
    return result == null && sb.length() > 0 ? FAIL_MISSING_OPERATOR : result;
  }

  /**
   * Part of a simple top-down expression evaluator.  This precedence level
   * strips and evaluates the left-associative &amp;&amp; and || operators.
   *
   * @param expr the expression to evaluate
   * @param symbols a table that associates symbol names with values
   * @param valueIfUndef the value to use for identifiers encountered in the
   *   expression that have no value in the symbol table.  If null, the
   *   method throws an exception when it encounters an undefined symbol.
   * @return null if the expression is true; otherwise, a failure message that
   *   consists of one of the result values above optionally followed by a
   *   colon and a space-delimited set of symbols participating in the failure
   */
  protected static String conjunction
    (StringBuffer expr, Properties symbols, String valueIfUndef) {
    String left = parenthesized(expr, symbols, valueIfUndef);
    skipSpaces(expr);
    if(expr.length() < 2) {
      return left; // Insufficient characters remaining for && or ||
    } else if(expr.charAt(0) == '|' && expr.charAt(1) == '|') {
      expr.replace(0, 2, "");
      String right = conjunction(expr, symbols, valueIfUndef);
      // Give syntax errors precedence over evaluation failure
      if(left == null || right == null) {
        return null;
      } else if(!left.startsWith(FAIL_EXPR_FAILED)) {
        return left;
      } else if(!right.startsWith(FAIL_EXPR_FAILED)) {
        return right;
      }
      // Merge symbol lists
      return left + " " + right.substring(FAIL_EXPR_FAILED.length() + 1);
    } else if(expr.charAt(0) == '&' && expr.charAt(1) == '&') {
      expr.replace(0, 2, "");
      String right = conjunction(expr, symbols, valueIfUndef);
      // Give syntax errors precedence over evaluation failure
      if(left != null && !left.startsWith(FAIL_EXPR_FAILED)) {
        return left;
      } else if(right != null && !right.startsWith(FAIL_EXPR_FAILED)) {
        return right;
      }
      return left != null ? left : right != null ? right : null;
    } else {
      return left;
    }
  }

  /**
   * Part of a simple top-down expression evaluator.  This precedence level
   * strips and evaluates parenthesized sub-expressions.
   *
   * @param expr the expression to evaluate
   * @param symbols a table that associates symbol names with values
   * @param valueIfUndef the value to use for identifiers encountered in the
   *   expression that have no value in the symbol table.  If null, the
   *   method throws an exception when it encounters an undefined symbol.
   * @return null if the expression is true; otherwise, a failure message that
   *   consists of one of the result values above optionally followed by a
   *   colon and a space-delimited set of symbols participating in the failure
   */
  protected static String parenthesized
    (StringBuffer expr, Properties symbols, String valueIfUndef) {
    skipSpaces(expr);
    if(expr.length() == 0 || expr.charAt(0) != '(') {
      return binary(expr, symbols, valueIfUndef);
    }
    expr.replace(0, 1, "");
    String result = conjunction(expr, symbols, valueIfUndef);
    skipSpaces(expr);
    if(expr.length() > 0 && expr.charAt(0) == ')') {
      expr.replace(0, 1, "");
    } else {
      result = FAIL_MISSING_RPAREN;
    }
    return result;
  }

  /**
   * Part of a simple top-down expression evaluator.  This precedence level
   * strips and evaluates the binary operations &lt;, &lt;=, &gt;, &gt;=, ==,
   * !=, &lt;&gt;, =~, and !~.
   *
   * @param expr the expression to evaluate
   * @param symbols a table that associates symbol names with values
   * @param valueIfUndef the value to use for identifiers encountered in the
   *   expression that have no value in the symbol table.  If null, the
   *   method throws an exception when it encounters an undefined symbol.
   * @return null if the expression is true; otherwise, a failure message that
   *   consists of one of the result values above optionally followed by a
   *   colon and a space-delimited set of symbols participating in the failure
   */
  protected static String binary
    (StringBuffer expr, Properties symbols, String valueIfUndef) {
    // Pull operands and operator from expr
    String leftOperand = getOperand(expr);
    if(leftOperand == null) {
      return FAIL_MISSING_OPERAND;
    }
    skipSpaces(expr);
    String operator = null;
    if(expr.length() < 1) {
      return FAIL_MISSING_OPERATOR;
    } else if(expr.length() == 1) {
      operator = expr.toString();
    } else if(expr.charAt(0) == '>') {
      operator = expr.charAt(1) == '=' ? ">=" : ">";
    } else if(expr.charAt(0) == '<') {
      operator = expr.charAt(1) == '=' ? "<=" :
                 expr.charAt(1) == '>' ? "<>" : "<";
    } else if(expr.charAt(0) == '=') {
      operator = expr.charAt(1) == '=' ? "==" :
                 expr.charAt(1) == '~' ? "=~" : "=";
    } else if(expr.charAt(0) == '!') {
      operator = expr.charAt(1) == '=' ? "<>" :
                 expr.charAt(1) == '~' ? "!~" : "!";
    } else {
      return FAIL_INVALID_OPERATOR + " '" + expr.charAt(0) + "'";
    }
    expr.replace(0, operator.length(), "");
    String rightOperand = getOperand(expr);
    if(rightOperand == null) {
      return FAIL_MISSING_OPERAND;
    }

    // Check to see if operands are identifiers or literals
    String leftValue = leftOperand;
    String rightValue = rightOperand;
    String identifiers = "";
    if(leftOperand.matches("^" + Protocol.MACRO_NAME_PATTERN + "$")) {
      identifiers = leftOperand;
      if((leftValue = symbols.getProperty(leftOperand)) == null) {
        if(valueIfUndef == null) {
          return FAIL_UNDEFINED_SYMBOL + ":" + leftOperand;
        } else {
          leftValue = valueIfUndef;
        }
      }
    }
    if(rightOperand.matches("^" + Protocol.MACRO_NAME_PATTERN + "$")) {
      identifiers = identifiers.equals("") ?
                    rightOperand : (leftOperand + " " + rightOperand);
      if((rightValue = symbols.getProperty(rightOperand)) == null) {
        if(valueIfUndef == null) {
          return FAIL_UNDEFINED_SYMBOL + ":" + rightOperand;
        } else {
          rightValue = valueIfUndef;
        }
      }
    }

    // Perform the operation
    boolean success;
    if(operator.equals("=~") || operator.equals("!~")) {
      // Strip enclosing slashes/quotes from rightValue (the pattern)
      if(rightValue.length() > 0 &&
         (rightValue.charAt(0) == '/' || rightValue.charAt(0) == '"' ||
          rightValue.charAt(0) == '\'')) {
        rightValue = rightValue.substring(1, rightValue.length() - 1);
      }
      success = leftValue.matches("(?s).*?(" + rightValue + ").*");
      if(operator.equals("!~")) {
        success = !success;
      }
    } else {
      int relation = StringMethods.compareTo(leftValue, rightValue);
      success = relation < 0 ? operator.indexOf('<') >= 0 :
                relation > 0 ? operator.indexOf('>') >= 0 :
                operator.indexOf('=') >= 0;
    }

    return success ? null : (FAIL_EXPR_FAILED + ":" + identifiers);

  }

  /**
   * Part of a simple top-down expression evaluator.  This precedence level
   * strips and returns operands.
   *
   * @param expr the expression to evaluate
   * @return the operand at the beginning of the expression; null if none
   */
  protected static String getOperand(StringBuffer expr) {
    skipSpaces(expr);
    Matcher m = OPERAND_PATTERN.matcher(expr.toString());
    if(m.matches()) {
      String result = expr.substring(0, m.end(1));
      expr.replace(0, m.end(1), "");
      return result;
    } else {
      return null;
    }
  }

  /**
   * Removes leading spaces from a StringBuffer.
   *
   * @param sb the string buffer to trim
   */
  protected static void skipSpaces(StringBuffer sb) {
    int i;
    for(i = 0; i < sb.length() && sb.charAt(i) == ' '; i++)
      ; // empty
    sb.replace(0, i, "");
  }

}
