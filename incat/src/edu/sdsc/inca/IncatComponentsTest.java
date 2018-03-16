package edu.sdsc.inca;

import junit.framework.TestCase;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JTextField;

/**
 * A JUnit for the IncatComponents class.
 */
public class IncatComponentsTest extends TestCase implements ActionListener {

  private static int COLUMNS = 50;
  private static String COMMAND = "cmd";
  private static char KEY = ' ';
  private static String LABEL = "blah";

  /**
   * Test the JButtonFactory method.
   */
  public void testJButtonFactory() {
    JButton b = IncatComponents.JButtonFactory(LABEL, COMMAND, this);
    assertNotNull(b);
    assertEquals(LABEL, b.getText());
    assertEquals(COMMAND, b.getActionCommand());
    boolean found = false;
    ActionListener[] listeners = b.getActionListeners();
    for(int i = 0; i < listeners.length; i++) {
      if(listeners[i] == this) {
        found = true;
      }
    }
    assertTrue(found);
  }

  /**
   * Test the JMenuItemFactory methods.
   */
  public void testJMenuItemFactory() {
    JMenuItem mi = IncatComponents.JMenuItemFactory(LABEL, COMMAND, this, KEY);
    assertNotNull(mi);
    assertEquals(LABEL, mi.getText());
    assertEquals(COMMAND, mi.getActionCommand());
    boolean found = false;
    ActionListener[] listeners = mi.getActionListeners();
    for(int i = 0; i < listeners.length; i++) {
      if(listeners[i] == this) {
        found = true;
      }
    }
    assertTrue(found);
  }

  /**
   * Test the JTextFieldFactory method.
   */
  public void testJTextFieldFactory() {
    JTextField tf = IncatComponents.JTextFieldFactory(COLUMNS, COMMAND, this);
    assertNotNull(tf);
    assertEquals(COLUMNS, tf.getColumns());
    // JTextField doesn't defined a getActionCommand method
    // assertEquals(COMMAND, tf.getActionCommand());
    boolean found = false;
    ActionListener[] listeners = tf.getActionListeners();
    for(int i = 0; i < listeners.length; i++) {
      if(listeners[i] == this) {
        found = true;
      }
    }
    assertTrue(found);
  }

  /**
   * Test the setJComboBoxSelectedString method.
   */
  public void testSetJComboBoxSelectedString() {
    String[] choices = new String[50];
    for(int i = 0; i < choices.length; i++) {
      choices[i] = (100 + i) + "";
    }
    JComboBox jcb = new JComboBox(choices);
    IncatComponents.setJComboBoxSelectedString(jcb, choices[choices.length/2]);
    assertEquals(choices.length / 2, jcb.getSelectedIndex());
    IncatComponents.setJComboBoxSelectedString(jcb, choices[choices.length-1]);
    assertEquals(choices.length - 1, jcb.getSelectedIndex());
    IncatComponents.setJComboBoxSelectedString(jcb, choices[0]);
    assertEquals(0, jcb.getSelectedIndex());
  }

  /**
   * Placeholder to allow using this class as an ActionListener.
   */
  public void actionPerformed(ActionEvent ae) {
  }

}
