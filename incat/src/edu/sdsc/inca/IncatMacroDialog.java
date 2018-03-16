package edu.sdsc.inca;

import edu.sdsc.inca.protocol.Protocol;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

/**
 * A Dialog window that allows the user to edit a macro name and value.
 */
public class IncatMacroDialog extends JFrame implements ActionListener {

  protected JTextField name;
  protected ActionListener listener;
  protected JTextField valueEdit;
  protected IncatList valueList;

  /**
   * Constructs an IncatMacroDialog.
   *
   * @param listener the listener to invoke when OK or cancel is pressed
   * @param okCommand the command to send when OK is pressed
   * @param cancelCommand the command to send when Cancel is pressed
   */
  public IncatMacroDialog(ActionListener listener,
                          String okCommand,
                          String cancelCommand) {
    this.name = IncatComponents.JTextFieldFactory(15, okCommand, this);
    this.valueEdit = IncatComponents.JTextFieldFactory(30, "valueCommit", this);
    this.valueList = new IncatList("Value(s)", "value", "Delete", this);
    this.valueList.setCellRenderer(new MacroValueRenderer());
    Box nameBox = IncatComponents.BoxFactory(new JComponent[] {
      this.name, null,
      new JLabel("Macro Name *"), null,
    }, false);
    Box valueBox = IncatComponents.BoxFactory(new JComponent[] {
      this.valueEdit, null,
      this.valueList, null,
    }, false);
    IncatComponents.alignBoxHeights(new Box[] {nameBox, valueBox});
    this.setContentPane(IncatComponents.BoxFactory(new JComponent[] {
      nameBox, valueBox, null,
      IncatComponents.BoxFactory(new JComponent[] {
        IncatComponents.JButtonFactory("Cancel", cancelCommand, listener),
        IncatComponents.JButtonFactory("Ok", okCommand, this)
      }, false)
    }, false));
    this.setTitle("Incat Macro Dialog");
    this.pack();
    this.listener = listener;
  }

  /**
   * Returns the macro name.
   *
   * @return the macro name
   */
  public String getName() {
    return this.name.getText();
  }

  /**
   * Returns the macro values.
   *
   * @return the macro values
   */
  public String[] getValues() {
    int length = this.valueList.getLength() - 1; // Trim trailing empty item
    String[] result = new String[length];
    for(int i = 0; i < length; i++) {
      result[i] = (String)this.valueList.getElementAt(i);
    }
    return result;
  }

  /**
   * Sets the macro name.
   *
   * @param name the macro name
   */
  public void setName(String name) {
    this.name.setText(name);
    this.name.requestFocus();
  }

  /**
   * Sets the macro values.
   *
   * @param values the macro values
   */
  public void setValues(String[] values) {
    if(values == null) {
      values = new String[0];
    }
    this.valueList.removeAllElements();
    for(int i = 0; i < values.length; i++) {
      this.valueList.addElement(values[i]);
    }
    this.valueList.addElement(""); // Add trailing empty item
    this.valueList.setSelectedIndex(this.valueList.getLength() - 1);
    this.valueEdit.setText("");
    this.valueEdit.requestFocus();
  }

  /**
   * An action listener that checks for required fields on dialog exit and
   * coordinates the value list and value edit box.
   */
  public void actionPerformed(ActionEvent ae) {
    String command = ae.getActionCommand();
    int index = this.valueList.getSelectedIndex();
    if(command.equals("valueCommit")) {
      this.valueList.setElementAt(this.valueEdit.getText(), index);
      index++;
      if(index == this.valueList.getLength()) {
        this.valueList.addElement("");
      }
      this.valueList.setSelectedIndex(index);
    } else if(command.equals("valueDelete")) {
      if(index < this.valueList.getLength() - 1) {
        this.valueList.removeElementAt(index);
        this.valueList.setSelectedIndex(index);
        this.valueEdit.setText((String)this.valueList.getSelectedElement());
      }
    } else if(command.equals("valueDoubleClick")) {
      // empty
    } else if(command.startsWith("valueFocus")) {
      // empty
    } else if(command.equals("valueSingleClick")) {
      this.valueEdit.setText((String)this.valueList.getSelectedElement());
      this.valueEdit.requestFocus();
    } else {
      if(this.getName().equals("")) {
        JOptionPane.showMessageDialog
          (this, "Please provide a macro name",
           "Incat Message", JOptionPane.ERROR_MESSAGE);
        return;
      } else if(!this.getName().matches("^"+Protocol.MACRO_NAME_PATTERN+"$")) {
        JOptionPane.showMessageDialog
          (this, "Invalid format for macro name",
           "Incat Message", JOptionPane.ERROR_MESSAGE);
        return;
      }
      if(!this.valueEdit.getText().equals("")) {
        // Implicit commit of edit value when OK button hit
        this.valueList.setElementAt(this.valueEdit.getText(), index);
        if(index == this.valueList.getLength() - 1) {
          this.valueList.addElement("");
        }
      }
      this.listener.actionPerformed(ae);
    }
  }

  /**
   * A ListCellRenderer that makes empty values more visible.
   */
  protected class MacroValueRenderer implements ListCellRenderer {
    protected static final String EMPTY_VALUE = "<empty>";
    /**
     * See ListCellRenderer
     */
    public java.awt.Component getListCellRendererComponent
      (JList list, Object value, int index, boolean isSelected,
       boolean cellHasFocus) {
      JLabel result = IncatList.listElementLabel(list, value, isSelected);
      if(value.equals("")) {
        Font f = result.getFont();
        result.setFont(new Font(f.getName(), Font.ITALIC, f.getSize()));
        result.setForeground(Color.GRAY);
        result.setText(index < list.getModel().getSize()-1 ? EMPTY_VALUE : " ");
      }
      return result;
    }
  }

}
