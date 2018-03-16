package edu.sdsc.inca;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * A Dialog window that allows the user to edit a suite.
 */
public class IncatSuiteDialog extends JFrame implements ActionListener {

  protected JTextField name;
  protected JTextField description;
  protected ActionListener listener;

  /**
   * Constructs an IncatSuiteDialog.
   *
   * @param listener the listener to invoke when OK or cancel is pressed
   * @param okCommand the command to send when OK is pressed
   * @param cancelCommand the command to send when Cancel is pressed
   */
  public IncatSuiteDialog(ActionListener listener,
                          String okCommand,
                          String cancelCommand) {
    this.name = IncatComponents.JTextFieldFactory(15, okCommand, this);
    this.description = IncatComponents.JTextFieldFactory(50, okCommand, this);
    this.setContentPane(IncatComponents.BoxFactory(new JComponent[] {
      this.name, null,
      new JLabel("Suite Name *"), null,
      this.description, null,
      new JLabel("Suite Description"), null,
      IncatComponents.BoxFactory(new JComponent[] {
        IncatComponents.JButtonFactory("Cancel", cancelCommand, listener),
        IncatComponents.JButtonFactory("Ok", okCommand, this), null
      }, false), null
    }, false));
    this.setTitle("Incat Suite Dialog");
    this.pack();
    this.listener = listener;
  }

  /**
   * Returns the suite name.
   *
   * @return the suite name
   */
  public String getName() {
    return this.name.getText();
  }

  /**
   * Copies information from the suite dialog into the specified suite.
   *
   * @param suite the suite to update
   */
  public void getSuite(WrapSuite suite) {
    suite.setName(this.name.getText());
    suite.setDescription(this.description.getText());
  }

  /**
   * Copies information from the specified suite into the suite dialog.
   *
   * @param suite the suite to show
   */
  public void setSuite(WrapSuite suite) {
    this.name.setText(suite.getName());
    this.description.setText(suite.getDescription());
  }

  /**
   * An action listener that checks for required fields on dialog exit.
   */
  public void actionPerformed(ActionEvent ae) {
    if(this.getName().equals("")) {
      JOptionPane.showMessageDialog
        (this, "Please provide a suite name", "Missing Suite Name",
         JOptionPane.ERROR_MESSAGE);
      return;
    }
    this.listener.actionPerformed(ae);
  }

}
