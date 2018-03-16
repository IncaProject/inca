package edu.sdsc.inca;

import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.ExprEvaluator;
import edu.sdsc.inca.util.StringMethods;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 * A Dialog window that allows the user to edit a series.
 */
public class IncatSeriesDialog extends JFrame implements ActionListener {

  protected JTextField acceptedExpr;
  protected JComboBox acceptedNotifier;
  protected JTextField acceptedTarget;
  protected ReporterArgsPanel args;
  protected ContextDocument context;
  protected JComboBox cronFrom;
  protected JTextField cronSpec;
  protected JComboBox cronStep;
  protected JComboBox cronUnit;
  protected JLabel cronMessage;
  protected JLabel description;
  protected JTextField limitCpu;
  protected JTextField limitMemory;
  protected JTextField limitWall;
  protected ActionListener listener;
  protected JTextField nickname;
  protected Box notificationBox;
  protected IncatList reporter;
  protected IncatList resource;
  protected JRadioButton versionLatest;
  protected JRadioButton versionSpecific;

  /** Reporter descriptions longer than this will be elided. */
  public static final int MAX_REPORTER_DOC_LENGTH = 80;

  /** Maximum and minimum values for the 5 cron units. */
  public static String[] CRON_UNIT_MAX =
    new String[] {"59", "23", "31", "12", "6"};
  public static String[] CRON_UNIT_MIN = new String[] {"0", "0", "1", "1", "0"};

  /**
   * Constructs a new IncatSeriesDialog.
   *
   * @param listener the listener to invoke when OK or cancel is pressed
   * @param okCommand the command to send when OK is pressed
   * @param cancelCommand the command to send when Cancel is pressed
   */
  public IncatSeriesDialog(ActionListener listener,
                           String okCommand,
                           String cancelCommand) {
    this.acceptedExpr =
      IncatComponents.JTextFieldFactory(20, "acceptedExprChange", this);
    String notifiers = System.getProperty("inca.incat.notifiers");
    if(notifiers == null || notifiers.equals("")) {
      notifiers = "EmailNotifier,LogNotifier";
    }
    notifiers = "," + notifiers;
    this.acceptedNotifier = new JComboBox(notifiers.split("[\\s;,]+"));
    this.acceptedTarget =
      IncatComponents.JTextFieldFactory(20, okCommand, this);
    this.args = new ReporterArgsPanel();
    String[] cronAmounts = new String[60];
    for(int i = 0; i < cronAmounts.length; i++) {
      cronAmounts[i] = i + "";
    }
    this.cronStep = new JComboBox(cronAmounts);
    this.cronStep.removeItemAt(0); // Get rid of the 0
    this.cronStep.addActionListener(this);
    this.cronStep.setActionCommand("cronWidgetChange");
    this.cronStep.setMaximumSize(this.cronStep.getPreferredSize());
    this.cronFrom = new JComboBox(cronAmounts);
    this.cronFrom.insertItemAt("random", 0);
    this.cronFrom.addActionListener(this);
    this.cronFrom.setActionCommand("cronWidgetChange");
    this.cronFrom.setMaximumSize(this.cronFrom.getPreferredSize());
    this.cronUnit = new JComboBox(new String[] {
      "minutes", "hours", "days", "months", "weekdays"
    });
    this.cronUnit.addActionListener(this);
    this.cronUnit.setActionCommand("cronUnitChange");
    this.cronUnit.setMaximumSize(this.cronUnit.getPreferredSize());
    this.cronSpec =
      IncatComponents.JTextFieldFactory(10, "cronSpecChange", this);
    this.cronMessage = new JLabel(" ");
    Font f = this.cronMessage.getFont();
    this.cronMessage.setFont
      (new Font(f.getName(), Font.ITALIC, f.getSize() - 2));
    this.context = new ContextDocument();
    JTextPane contextPane = new JTextPane();
    JTextField model = IncatComponents.JTextFieldFactory(40, null, null);
    contextPane.setPreferredSize(model.getPreferredSize());
    contextPane.setMaximumSize(model.getMaximumSize());
    contextPane.setBorder(model.getBorder());
    contextPane.setStyledDocument(this.context);
    this.description = new JLabel("");
    this.limitCpu = IncatComponents.JTextFieldFactory(5, okCommand, this);
    this.limitMemory = IncatComponents.JTextFieldFactory(5, okCommand, this);
    this.limitWall = IncatComponents.JTextFieldFactory(5, okCommand, this);
    this.nickname = IncatComponents.JTextFieldFactory(20, okCommand, this);
    this.notificationBox = Box.createVerticalBox();
    this.reporter = new IncatList("Reporter *", "reporter", null, this);
    this.resource = new IncatList("Resource *", null, null, null);
    ButtonGroup versionButtonGroup = new ButtonGroup();
    this.versionLatest = new JRadioButton("Use Latest Version");
    this.versionSpecific = new JRadioButton("Use Specific Version");
    versionButtonGroup.add(this.versionSpecific);
    versionButtonGroup.add(this.versionLatest);
    this.reporter.add(IncatComponents.BoxFactory(new JComponent[] {
      this.versionSpecific, this.versionLatest
    }, true));
    IncatComponents.alignBoxHeights(new Box[] {this.reporter, this.resource});
    Box contentPane = IncatComponents.BoxFactory(new JComponent[] {
      this.reporter, this.resource, this.args, null,
      this.description, null,
      new JLabel(" "), null, // vertical spacing
      IncatComponents.BoxFactory(new JComponent[] {
        new JLabel("Run Every"), this.cronStep, this.cronUnit,
        new JLabel("From"), this.cronFrom, this.cronSpec, null,
        this.cronMessage
      }, false),
      new JSeparator(), null,
      new JLabel("Nickname "), this.nickname, null,
      new JLabel("Context "), contextPane, null,
      new JLabel("Limits "),
      IncatComponents.BoxFactory(new JComponent[] {
        new JLabel("CPU (Secs) "), this.limitCpu,
        new JLabel(" Wall (Secs) "), this.limitWall,
        new JLabel(" Memory (MB) "), this.limitMemory, null
      }, true), null,
      new JLabel("Comparison "), this.acceptedExpr, null,
      this.notificationBox, null,
      IncatComponents.BoxFactory(new JComponent[] {
        IncatComponents.JButtonFactory("Cancel", cancelCommand, listener),
        IncatComponents.JButtonFactory("Ok", okCommand, this), null
      }, true)
    }, false);
    this.setContentPane(contentPane);
    this.setTitle("Incat Series Dialog");
    this.pack();
    this.listener = listener;
    JMenuBar menuBar = new JMenuBar();
    IncatEditMenu editMenu = new IncatEditMenu(this);
    editMenu.setEditEnabled(false, false, false, false, true);
    menuBar.add(editMenu);
    this.setJMenuBar(menuBar);
  }

  /**
   * An action listener that checks for required fields on dialog exit and
   * modifies the dialog when reporter or cron changes.
   */
  public void actionPerformed(ActionEvent ae) {
    String action = ae.getActionCommand();
    if(action.equals("acceptedExprChange")) {
      updateNotificationInfo();
    } else if(action.equals("cronSpecChange")) {
      updateCronWidgets();
    } else if(action.equals("cronUnitChange")) {
      updateCronSpec(true);
    } else if(action.equals("cronWidgetChange")) {
      updateCronSpec(false);
    } else if(action.equals("reporterSingleClick")) {
      updateReporterInfo();
    } else if(action.equals("reporterDoubleClick")) {
      showReporter();
    } else if(action.equals("elementFind ...")) {
      IncatList.findListElement(true);
    } else if(action.equals("elementFindNext")) {
      IncatList.findListElement(false);
    } else {
      // Only required fields are combo boxes, so can't be blank
      // Check to make sure any AcceptedOutput expression either is parsable or
      // consists of a single macro reference.
      String prop = this.acceptedExpr.getText();
      if(!prop.equals("") &&
         !prop.matches("^\\s*@" + Protocol.MACRO_NAME_PATTERN + "@\\s*$")) {
        // Turn macro references into simple symbol references
        prop = prop.replaceAll("@", " ");
        String err = ExprEvaluator.eval(prop, new Properties(), "");
        if(err != null && !err.startsWith(ExprEvaluator.FAIL_EXPR_FAILED)) {
          JOptionPane.showMessageDialog
            (this, "Bad expression for output test: " + err,
             "Incat Message", JOptionPane.ERROR_MESSAGE);
          return;
        }
      }
      // Check that any limits are valid float values
      String[] limits = new String[] {
        this.limitCpu.getText(), this.limitMemory.getText(),
        this.limitWall.getText()
      };
      for(int i = 0; i < limits.length; i++) {
        if(!limits[i].equals("") && limits[i].indexOf("@") < 0) {
          try {
            Float.parseFloat(limits[i]);
          } catch(Exception e) {
            JOptionPane.showMessageDialog
              (this, "Limit '" + limits[i] + "' is not a valid float value",
               "Incat Message", JOptionPane.ERROR_MESSAGE);
            return;
          }
        }
      }
      this.listener.actionPerformed(ae);
    }
  }

  /**
   * Copies information from the series dialog into a specified series.
   *
   * @param series the series to update
   */
  public void getSeries(WrapSeries series) {
    String prop;
    WrapReporter reporter = (WrapReporter)this.reporter.getSelectedElement();
    prop = this.acceptedExpr.getText();
    if(prop.equals("")) {
      series.setAcceptedComparitor(null);
      series.setAcceptedComparison(null);
      series.setAcceptedTarget(null, null);
    } else {
      series.setAcceptedComparitor("ExprComparitor");
      series.setAcceptedComparison(prop);
      String notifier = (String)this.acceptedNotifier.getSelectedItem();
      prop = this.acceptedTarget.getText();
      series.setAcceptedTarget
        (notifier.equals("") ? null : notifier, prop.equals("") ? null : prop);
    }
    series.setCron(this.cronSpec.getText());
    series.setNickname(this.nickname.getText());
    prop = this.limitCpu.getText();
    series.setCpuLimit(prop.equals("") ? null : prop);
    prop = this.limitMemory.getText();
    series.setMemoryLimit(prop.equals("") ? null : prop);
    prop = this.limitWall.getText();
    series.setWallClockLimit(prop.equals("") ? null : prop);
    series.setReporter(reporter.getProperty("name"));
    series.setReporterVersion
      (this.versionLatest.isSelected()?null:reporter.getProperty("version"));
    series.setResource
      (((WrapResource)this.resource.getSelectedElement()).getName());
    series.setContext(this.context.getText());
    series.setArgs(args.getValues());
  }

  /**
   * Sets the set of reporters to display in the series dialog.
   *
   * @param reporters the reporters to display
   */
  public void setReporterChoices(WrapReporter[] reporters) {
    this.reporter.setElements(reporters);
    this.reporter.sort();
    this.reporter.setSelectedIndex(0);
  }

  /**
   * Sets the set of resources to display in the series dialog.
   *
   * @param resources the resources to display
   */
  public void setResourceChoices(WrapResource[] resources) {
    this.resource.setElements(resources);
    this.resource.sort();
    this.resource.setSelectedIndex(0);
  }

  /**
   * Copies information from the specified series into the series dialog.
   *
   * @param series the series to show
   */
  public void setSeries(WrapSeries series) {

    Properties args = series.getArgs();
    String seriesReporter = series.getReporter();
    String seriesResource = series.getResource();
    String seriesVersion = series.getReporterVersion();

    this.acceptedExpr.setText(series.getAcceptedComparison());
    String notifier = series.getAcceptedNotifier();
    if(notifier != null && notifier.startsWith("edu.sdsc.inca.")) {
      // Backward compatibility--notifiers used to be classes, not scripts
      notifier = notifier.replaceFirst(".*\\.", "");
    }
    this.acceptedNotifier.setSelectedItem
      (notifier == null || notifier.equals("") ? "" : notifier);
    this.acceptedTarget.setText(series.getAcceptedTarget());
    this.cronSpec.setText(series.getCron());
    updateCronWidgets();
    this.nickname.setText(series.getNickname());
    this.limitCpu.setText(series.getCpuLimit());
    this.limitMemory.setText(series.getMemoryLimit());
    this.limitWall.setText(series.getWallClockLimit());
    this.context.setText(series.getContext(), "");

    // Quietly handle a reporter and/or a resource that refers to unknown
    // values--it's up to the caller to warn the user about these.
    if(!seriesReporter.equals("") &&
       !this.reporter.selectMatchingElement(seriesReporter)) {
      String argsValue = "";
      for(Enumeration e = args.keys(); e.hasMoreElements(); ) {
        argsValue += "\n " + (String)e.nextElement() + " .*";
      }
      WrapReporter dummyReporter = new WrapReporter(new Properties());
      dummyReporter.setProperty("name", seriesReporter);
      dummyReporter.setProperty("arguments", argsValue);
      dummyReporter.setProperty
        ("version", seriesVersion == null ? "0" : seriesVersion);
      this.reporter.addElement(dummyReporter);
      this.reporter.selectMatchingElement(seriesReporter);
    }
    this.versionSpecific.setSelected(seriesVersion != null);
    this.versionLatest.setSelected(seriesVersion == null);

    if(!seriesResource.equals("") &&
       !this.resource.selectMatchingElement(seriesResource)) {
      WrapResource dummyResource = new WrapResource();
      dummyResource.setName(seriesResource);
      this.resource.addElement(dummyResource);
      this.resource.selectMatchingElement(seriesResource);
    }

    updateNotificationInfo();
    updateReporterInfo();
    this.args.setValues(args);

  }

  /**
   * Displays the source of the currently-selected reporter.
   */
  protected void showReporter() {
    try {
      IncatRepositoryDialog dialog = new IncatRepositoryDialog();
      dialog.setReporter(this.reporter.getSelectedElement().toString());
      dialog.setVisible(true);
    } catch(Exception e) {
      // empty
    }
  }

  /**
   * Updates the cron text spec based on the values of the cron widgets.
   *
   * @param unitChange indicates whether or not the method is responding to a
   *                   change in the cron units widget
   */
  protected void updateCronSpec(boolean unitChange) {
    String from = (String)this.cronFrom.getSelectedItem();
    String step = (String)this.cronStep.getSelectedItem();
    int unit = this.cronUnit.getSelectedIndex();
    String[] spec = new String[] {"*", "*", "*", "*", "*"};
    for(int i = 0; i < unit; i++) {
      spec[i] = "?";
    }
    // Since different units have different lower bounds, we treat 0 and 1 as
    // equivalent and make adjustments when the unit changes
    if(unitChange && (from.equals("0") || from.equals("1"))) {
      from = CRON_UNIT_MIN[unit];
    }
    if(!from.equals(CRON_UNIT_MIN[unit])) {
      spec[unit] =
        (from.equals("random") ? "?" : from) + "-" + CRON_UNIT_MAX[unit];
    }
    if(!step.equals("1")) {
      spec[unit] += "/" + step;
    }
    this.cronSpec.setText(StringMethods.join(" ", spec));
  }

  /**
   * Updates the cron widgets based on the values of the cron text spec.
   */
  protected void updateCronWidgets() {
    String spec = this.cronSpec.getText();
    String[] pieces = spec.split(" ");
    // NOTE: invoking setSelectedIndex on a JComboBox apparently fires its
    // action listeners, so we have to temporarily remove the action from the
    // cron widgets to avoid invoking updateCronSpec.
    this.cronFrom.setActionCommand("");
    this.cronStep.setActionCommand("");
    this.cronUnit.setActionCommand("");
    this.cronFrom.enable();
    this.cronStep.enable();
    this.cronUnit.enable();
    String unrepresentable = "";
    int unit;
    for(unit = 0;
        unit < pieces.length - 1 && pieces[unit].matches("^(\\?|[0-9]+)$");
        unit++) {
      // empty
    }
    this.cronUnit.setSelectedIndex(unit);
    String piece = pieces[unit];
    int pos;
    if(spec.indexOf("@") >= 0) {
      unrepresentable += ", macro reference";
    }
    if((pos = piece.indexOf("/")) >= 0) {
      IncatComponents.setJComboBoxSelectedString
        (this.cronStep, piece.substring(pos + 1));
      piece = piece.substring(0, pos);
    } else {
      IncatComponents.setJComboBoxSelectedString(this.cronStep, "1");
    }
    if((pos = piece.indexOf(",")) >= 0) {
      piece = piece.substring(0, pos);
      unrepresentable += ", list";
    }
    if(piece.equals("*")) {
      piece = CRON_UNIT_MIN[unit];
    } else if((pos = piece.indexOf("-")) >= 0) {
      String max = piece.substring(pos + 1);
      piece = piece.substring(0, pos);
      if(!max.equals(CRON_UNIT_MAX[unit])) {
        unrepresentable += ", upper range bound";
      }
    }
    IncatComponents.setJComboBoxSelectedString
      (this.cronFrom, piece.equals("?") ? "random" : piece);
    eraseJLabel(this.cronMessage);
    if(!unrepresentable.equals("")) {
      unrepresentable = unrepresentable.substring(2); // Trim leading ", "
      this.cronFrom.disable();
      this.cronStep.disable();
      this.cronUnit.disable();
      pos = unrepresentable.lastIndexOf(",");
      if(pos >= 0) {
        unrepresentable = unrepresentable.substring(0, pos) + " and" +
                          unrepresentable.substring(pos + 1);
      }
      this.cronMessage.setText(
        "*** NOTE: The " + unrepresentable + " used in the cron textbox " +
        "cannot be represented in the pull-down components ***"
      );
    }
    this.cronFrom.repaint(); // Seems required to show dis/enabled status
    this.cronStep.repaint();
    this.cronUnit.repaint();
    this.cronFrom.setActionCommand("cronWidgetChange");
    this.cronStep.setActionCommand("cronWidgetChange");
    this.cronUnit.setActionCommand("cronUnitChange");
  }

  /**
   * Determines whether or not the notification script and arguments widgets
   * appear based on whether or not an accepted expr is specified.
   */
  protected void updateNotificationInfo() {
    boolean showing = this.notificationBox.getComponentCount() > 0;
    boolean shouldShow = !this.acceptedExpr.getText().equals("");
    if(showing == shouldShow) {
      return;
    }
    if(shouldShow) {
      this.notificationBox.add(IncatComponents.BoxFactory(new JComponent[] {
        new JLabel("Notification Script"), this.acceptedNotifier, null,
        new JLabel("Script Arguments "), this.acceptedTarget, null
      }, false));
    } else {
      this.notificationBox.removeAll();
    }
    this.pack();
  }

  /**
   * Updates the arg, nickname, and description widgets based on the value of
   * the reporter widget.
   */
  protected void updateReporterInfo() {

    WrapReporter reporter = (WrapReporter)this.reporter.getSelectedElement();
    if(reporter != null) {
      String prop = reporter.getProperty("description");
      if(prop == null) {
        prop = "";
      } else if(prop.length() > MAX_REPORTER_DOC_LENGTH) {
        prop = prop.substring(0, MAX_REPORTER_DOC_LENGTH - 3) + "...";
      }
      eraseJLabel(this.description);
      this.description.setText("(" + prop + ")");
      prop = reporter.getProperty("name");
      this.context.setText(this.context.getText(), prop);
      prop = reporter.getProperty("version");
      if(prop == null) {
        prop = "0";
      }
      this.versionSpecific.setText("Use Version " + prop + " Only");
      String nickname = this.nickname.getText();
      if(nickname.equals("") ||
         this.reporter.findMatchingElement(nickname) >= 0) {
        this.nickname.setText(reporter.toString());
      }
      this.args.setReporter(reporter);
    }
    this.pack();

  }

  /**
   * A StyledDocument that treats specially the context marker within the
   * document text.
   */
  public class ContextDocument extends DefaultStyledDocument {

    protected Style EDITABLE;
    protected Style UNEDITABLE;
    protected int uneditableStart = 0;
    protected int uneditableEnd = 0;

    /**
     * Constructs a new ContextDocument.
     */
    public ContextDocument() {
      Style dephault = StyleContext.getDefaultStyleContext().
                       getStyle(StyleContext.DEFAULT_STYLE);
      EDITABLE = this.addStyle("editable", dephault);
      UNEDITABLE = this.addStyle("uneditable", dephault);
      StyleConstants.setFontFamily(UNEDITABLE, "serif");
      StyleConstants.setBold(UNEDITABLE, true);
      StyleConstants.setItalic(UNEDITABLE, true);
    }

    /**
     * Returns the content of the document.
     *
     * @return the document content
     */
    public String getText() {
      try {
        String text = this.getText(0, this.getLength());
        return text.substring(0, this.uneditableStart) + "@@" +
               text.substring(this.uneditableEnd + 1);
      } catch(Exception e) {
        return "";
      }
    }

    /**
     * See Document.
     */
    public void insertString(int offset, String str, AttributeSet a)
      throws BadLocationException {
      if(offset <= this.uneditableStart) {
        super.insertString(offset, str, EDITABLE);
        this.uneditableStart += str.length();
        this.uneditableEnd += str.length();
      } else if(offset > this.uneditableEnd) {
        super.insertString(offset, str, EDITABLE);
      }
    }

    /**
     * See Document.
     */
    public void remove(int offset, int length) throws BadLocationException {
      int end = offset + length - 1;
      if(end < this.uneditableStart) {
        super.remove(offset, length);
        this.uneditableStart -= length;
        this.uneditableEnd -= length;
      } else if(offset > this.uneditableEnd) {
        super.remove(offset, length);
      }
    }

    /**
     * Sets the content of the document.
     *
     * @param text the new document content
     * @param reporter the reporter name to show in lieu of the context marker
     */
    public void setText(String text, String reporter) {
      if(text == null) {
        text = "";
      }
      try {
        super.remove(0, this.getLength());
        // Find @@ in the text, taking care not to match adjacent macro
        // references (e.g., find the two at the end of "@a@@b@@@")
        int pos;
        for(pos = text.indexOf("@");
            pos >= 0 && !text.substring(pos).startsWith("@@");
            pos = text.indexOf("@", pos + 1)) {
          if(text.substring(pos).matches("@[\\w\\.\\-]+@.*")) {
            // Skip macro reference
            pos = text.indexOf("@", pos + 1);
          }
        }
        if(pos >= 0) {
          super.insertString(this.getLength(), text.substring(0,pos), EDITABLE);
        }
        this.uneditableStart = this.getLength();
        super.insertString(this.getLength(), reporter + " -args", UNEDITABLE);
        this.uneditableEnd = this.getLength() - 1;
        if(pos >= 0) {
          super.insertString(this.getLength(), text.substring(pos+2), EDITABLE);
        }
      } catch(Exception e) {
        // empty
      }
    }

  }

  /**
   * Replaces any text in a JLabel with blanks.
   *
   * @param j the label to erase
   */
  public static void eraseJLabel(JLabel j) {
    // NOTE: Java work-around: calling setText seems to superimpose the new
    // label value over the old, rather than replacing.  setOpaque takes care
    // of this, but requires extra spaces to erase trailing chars.
    int oldWidth = j.getPreferredSize().width;
    j.setOpaque(true);
    j.setText("");
    while(j.getPreferredSize().width < oldWidth) {
      j.setText(j.getText() + " ");
    }
    j.repaint();
  }

}
