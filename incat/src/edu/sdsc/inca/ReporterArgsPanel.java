package edu.sdsc.inca;

import edu.sdsc.inca.repository.Repository;
import edu.sdsc.inca.util.ExpandablePattern;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A GUI component that allows the user to specify reporter argument values.
 */
public class ReporterArgsPanel extends JPanel implements ActionListener {

  protected Box box;
  protected Hashtable widgets;

  /** Constructs a new ArgsPanel. */
  public ReporterArgsPanel() {
    super();
    this.widgets = new Hashtable();
    this.box = Box.createVerticalBox();
    this.add(this.box);
  }

  /**
   * Returns the reporter argument value settings.
   *
   * @return a Properties, keyed by argument names, that specifies the value of
   *         each reporter argument
   */
  public Properties getValues() {
    Properties result = new Properties();
    for(Enumeration e = this.widgets.keys(); e.hasMoreElements(); ) {
      String name = (String)e.nextElement();
      result.setProperty(name, ((ArgBox)widgets.get(name)).getValue());
    }
    return result;
  }

  /**
   * Replaces the contents of the panel with a set of widgets that allow the
   * user to specify argument values for a specified reporter.
   *
   * @param reporter the reporter from which to the argument list
   */
  public void setReporter(WrapReporter reporter) {
    this.widgets = new Hashtable();
    this.box.removeAll();
    String prop = reporter == null ? null : reporter.getProperty("arguments");
    if(prop == null || prop.trim().equals("")) {
      return;
    }
    final String hiddenArgs = "help verbose version";
    String[] values = Repository.getPropertyValues(prop);
    for(int i = 0; i < values.length; i++) {
      String[] pieces = values[i].split(" ", 3);
      String argName = pieces[0];
      String argPat = pieces.length <= 1 ? "" : pieces[1];
      String dephault = pieces.length <= 2 ? "" : pieces[2];
      // See if we can list all the choices the argument pattern accepts.
      String[] choices = null;
      try {
        choices = (String [])
          (new ExpandablePattern(argPat).expand().toArray(new String[0]));
        Arrays.sort(choices);
      } catch(Exception e) {
        // empty
      }
      Box widget = new ArgBox(argName, choices, dephault);
      this.widgets.put(argName, widget);
      if(hiddenArgs.indexOf(argName) < 0) {
        widget.add(Box.createHorizontalGlue());
        this.box.add(widget);
      }
    }
    this.box.add(IncatComponents.BoxFactory(new JComponent[] {
      IncatComponents.JButtonFactory("Reset to Defaults", "argreset", this)
    }, true));
    this.box.add(IncatComponents.BoxFactory(new JComponent[] {
      new JLabel("Arguments", JLabel.CENTER)
    }, true));
  }

  /**
   * Changes the reporter argument values settings.
   *
   * @param values a Properties, keyed by argument names, that specifies the
   *               value of each reporter argument
   */
  public void setValues(Properties values) {
    for(Enumeration e = values.keys(); e.hasMoreElements(); ) {
      String name = (String)e.nextElement();
      ArgBox widget = (ArgBox)this.widgets.get(name);
      if(widget != null) {
        widget.setValue(values.getProperty(name));
      }
    }
  }

  /**
   * Responds to the reset to defaults button.
   */
  public void actionPerformed(ActionEvent ae) {
    String action = ae.getActionCommand();
    if(action.equals("argreset")) {
      for(Enumeration e = this.widgets.keys(); e.hasMoreElements(); ) {
        ((ArgBox)widgets.get((String)e.nextElement())).resetValue();
      }
    }
  }

  /**
   * A Box for displaying and allowing the user to change the value of a
   * single reporter argument.
   */
  public class ArgBox extends Box {

    private String dephault;
    private JComponent widget;

    /**
     * Constructs a new ArgBox.
     *
     * @param name the name of the argument
     * @param choices the list of valid argument values; null for text args
     * @param dephault the default value for the argument
     */
    public ArgBox(String name, String[] choices, String dephault) {
      super(BoxLayout.X_AXIS);
      if(choices == null) {
        this.widget = IncatComponents.JTextFieldFactory(20, null, null);
      } else {
        this.widget = new JComboBox(choices);
        this.widget.setMaximumSize(this.widget.getPreferredSize());
      }
      this.add(new JLabel(name + " "));
      this.add(this.widget);
      this.add(Box.createHorizontalGlue());
      this.dephault = dephault;
      this.resetValue();
    }

    /**
     * Returns the selected value of the argument.
     *
     * @return the argument value
     */
    public String getValue() {
      return this.widget instanceof JComboBox ?
        (String)((JComboBox)widget).getSelectedItem() :
        ((JTextField)widget).getText();
    }

    /**
     * Sets the argument value to its default.
     */
    public void resetValue() {
      this.setValue(this.dephault);
    }

    /**
     * Sets the argument value to a specific value.
     *
     * @param value the new argument value
     */
    public void setValue(String value) {
      if(this.widget instanceof JComboBox) {
        IncatComponents.setJComboBoxSelectedString
          ((JComboBox)this.widget, value);
      } else {
        ((JTextField)this.widget).setText(value);
      }
    }

  }

}
