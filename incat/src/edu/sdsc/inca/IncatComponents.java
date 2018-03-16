package edu.sdsc.inca;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * This class contains static convenience methods and constants used in the
 * construction and display of Incat GUI components.
 */
public class IncatComponents {

  /**
   * Adds vertical struts to a set of Boxes to make them the same height.
   *
   * @param boxes the boxes to align
   */
  public static void alignBoxHeights(Box[] boxes) {
    int[] heights = new int[boxes.length];
    int maxHeight = 0;
    for(int i = 0; i < boxes.length; i++) {
      int height = boxes[i].getPreferredSize().height;
      maxHeight = Math.max(maxHeight, height);
      heights[i] = height;
    }
    for(int i = 0; i < boxes.length; i++) {
      if(maxHeight > heights[i]) {
        boxes[i].add(Box.createVerticalStrut(maxHeight - heights[i]));
      }
    }
  }

  /**
   * Returns a Box that contains the specified components.
   *
   * @param components components to place in the Box; null elements indicate
   *                   the end of a row of components
   * @param centered indicates whether or not the components should be centered
   * @return a Box, configured as specified
   */
  public static Box BoxFactory(JComponent[] components,
                               boolean centered) {
    Box result = Box.createVerticalBox();
    int rowStart = 0;
    for(int i = 1; i <= components.length; i++) {
      if(i == components.length || components[i] == null) {
        if(i > rowStart) {
          Box row = Box.createHorizontalBox();
          for(int j = rowStart; j <= i - 1; j++) {
            row.add(components[j]);
          }
          if(!centered) {
            row.add(Box.createHorizontalGlue());
          }
          result.add(row);
        }
        rowStart = i + 1;
      }
    }
    return
      result.getComponentCount() == 1 ? (Box)result.getComponent(0) : result;
  }

  /**
   * Returns a JButton that will send the specified event to the specified
   * listener when pressed.
   *
   * @param label the label displayed on the button
   * @param actionCommand command sent when the button is pressed, optional
   * @param listener listener notified when button is pressed, optional
   * @return a button, configured as specified
   */
  public static JButton JButtonFactory(String label,
                                       String actionCommand,
                                       ActionListener listener) {
    JButton result = new JButton(label);
    if(actionCommand != null && listener != null) {
      result.setActionCommand(actionCommand);
      result.addActionListener(listener);
    }
    result.setMaximumSize(result.getPreferredSize());
    return result;
  }

  /**
   * Returns a JMenuItem that will send the specified event to the specified
   * listener when selected or when a specified shortcut key combo is pressed.
   *
   * @param label the label displayed on the menu item
   * @param actionCommand command sent when the menu item is selected
   * @param listener listener notified when menu item is selected
   * @param shortcut if non-blank, a keyboard shortcut character for the item
   * @return a menu item, configured as specified
   */
  public static JMenuItem JMenuItemFactory(String label,
                                           String actionCommand,
                                           ActionListener listener,
                                           char shortcut) {
    JMenuItem result = new JMenuItem(label);
    if(actionCommand != null && listener != null) {
      result.setActionCommand(actionCommand);
      result.addActionListener(listener);
    }
    if(shortcut != ' ') {
      result.setAccelerator
        (KeyStroke.getKeyStroke((int)shortcut,
         Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }
    return result;
  }

  /**
   * Returns a JMenuItem that will send the specified event to the specified
   * listener when selected.
   *
   * @param label the label displayed on the menu item
   * @param actionCommand command sent when the menu item is selected
   * @param listener listener notified when menu item is selected
   * @return a menu item, configured as specified
   */
  public static JMenuItem JMenuItemFactory(String label,
                                           String actionCommand,
                                           ActionListener listener) {
    return JMenuItemFactory(label, actionCommand, listener, ' ');
  }

  /**
   * Returns a JPasswordField of the specified width that will send the
   * specified event to the specified listener when text is entered.
   *
   * @param width the number of characters in the password field
   * @param actionCommand command sent when return is pressed, optional
   * @param listener listener notified when return is pressed, optional
   * @return a password field, configured as specified
   */
  public static JPasswordField JPasswordFieldFactory(int width,
                                                     String actionCommand,
                                                     ActionListener listener) {
    JPasswordField result = new JPasswordField(width);
    if(actionCommand != null && listener != null) {
      result.setActionCommand(actionCommand);
      result.addActionListener(listener);
    }
    Dimension size = result.getPreferredSize();
    result.setMinimumSize(size);
    // Allow width to grow, but not height
    size.width = result.getMaximumSize().width;
    result.setMaximumSize(size);
    result.setEchoChar('*');
    return result;
  }

  /**
   * Returns a JTextField of the specified width that will send the specified
   * event to the specified listener when text is entered.
   *
   * @param width the number of characters in the text field
   * @param actionCommand command sent when return is pressed, optional
   * @param listener listener notified when return is pressed, optional
   * @return a text field, configured as specified
   */
  public static JTextField JTextFieldFactory(int width,
                                             String actionCommand,
                                             ActionListener listener) {
    JTextField result = new JTextField(width);
    if(actionCommand != null && listener != null) {
      result.setActionCommand(actionCommand);
      result.addActionListener(listener);
    }
    Dimension size = result.getPreferredSize();
    result.setMinimumSize(size);
    // Allow width to grow, but not height
    size.width = result.getMaximumSize().width;
    result.setMaximumSize(size);
    return result;
  }

  /**
   * Sets the selected element of the specified JComboBox to the element that
   * matches the specified string.
   *
   * @param box the box to set
   * @param s the value to match
   * @return true if the specified string was found, otherwise false
   */
  public static boolean setJComboBoxSelectedString(JComboBox box, String s) {
    for(int i = 0; i < box.getItemCount(); i++) {
      if(box.getItemAt(i).toString().equals(s)) {
        box.setSelectedIndex(i);
        return true;
      }
    }
    return false;
  }

}
