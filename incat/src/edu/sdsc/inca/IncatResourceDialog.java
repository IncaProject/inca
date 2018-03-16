package edu.sdsc.inca;

import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.ExpandablePattern;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * A Dialog window that allows the user to edit a resource.
 */
public class IncatResourceDialog extends JFrame implements ActionListener {

  protected JComboBox access;
  protected Box accessBox;
  protected Box accessGlobusBox;
  protected Box accessLocalBox;
  protected Box accessManualBox;
  protected Box accessSshBox;
  protected JTextField approvalEmail;
  protected JTextField computeServer;
  protected JTextField fileServer;
  protected JCheckBox isEquivalent;
  protected JTextField keyFile;
  protected JPasswordField keyPassword;
  protected ActionListener listener;
  protected JTextField members;
  protected JTextField name;
  protected JTextField proxyLifetime;
  protected JPasswordField proxyPassword;
  protected JTextField proxyServer;
  protected JTextField proxyUserId;
  protected JComboBox suspendCondition;
  protected JTextField suspendValue;
  protected JTextField userId;
  protected JTextField wd;

  /**
   * Constructs a new IncatResourceDialog.
   *
   * @param listener the listener to invoke when OK or cancel is pressed
   * @param okCommand the command to send when OK is pressed
   * @param cancelCommand the command to send when Cancel is pressed
   */
  public IncatResourceDialog(ActionListener listener,
                             String okCommand,
                             String cancelCommand) {
    this.approvalEmail = IncatComponents.JTextFieldFactory(20, okCommand, this);
    this.computeServer = IncatComponents.JTextFieldFactory(20, okCommand, this);
    this.fileServer = IncatComponents.JTextFieldFactory(20, okCommand, this);
    this.isEquivalent = new JCheckBox("Equivalent");
    this.keyFile = IncatComponents.JTextFieldFactory(30, okCommand, this);
    this.keyPassword =
      IncatComponents.JPasswordFieldFactory(10, okCommand, this);
    this.members = IncatComponents.JTextFieldFactory(15, okCommand, this);
    this.name = IncatComponents.JTextFieldFactory(15, okCommand, this);
    this.proxyLifetime = IncatComponents.JTextFieldFactory(5, okCommand, this);
    this.proxyPassword =
      IncatComponents.JPasswordFieldFactory(10, okCommand, this);
    this.proxyServer = IncatComponents.JTextFieldFactory(20, okCommand, this);
    this.proxyUserId = IncatComponents.JTextFieldFactory(10, okCommand, this);
    this.suspendCondition = new JComboBox(Protocol.SUSPEND_CONDITIONS);
    this.suspendCondition.setMaximumSize(this.suspendCondition.getPreferredSize());
    this.suspendValue = IncatComponents.JTextFieldFactory(20, okCommand, this);
    this.userId = IncatComponents.JTextFieldFactory(5, okCommand, this);
    this.wd = IncatComponents.JTextFieldFactory(30, okCommand, this);
    this.access = new JComboBox(Protocol.COMPUTE_METHODS);
    this.access.setMaximumSize(this.access.getPreferredSize());
    this.access.addActionListener(this);
    this.access.setActionCommand("accesschange");
    this.accessBox = IncatComponents.BoxFactory(new JComponent[] {
      new JLabel("AccessMethod"), this.access, null,
      Box.createVerticalBox() // Placeholder
    }, false);
    this.accessGlobusBox = IncatComponents.BoxFactory(new JComponent[] {
      new JLabel("Gram Server "), this.computeServer, null,
      new JLabel("GridFtp Server "), this.fileServer
    }, false);
    this.accessLocalBox = IncatComponents.BoxFactory(new JComponent[] {
    }, false);
    this.accessManualBox = IncatComponents.BoxFactory(new JComponent[] {
    }, false);
    this.accessSshBox = IncatComponents.BoxFactory(new JComponent[] {
      new JLabel("User Id "), this.userId, null,
      new JLabel("Key File "), this.keyFile, null,
      new JLabel("Password "), this.keyPassword
    }, false);
    Box nameBox = IncatComponents.BoxFactory(new JComponent[] {
      this.name, null,
      new JLabel("Group Name *")
    }, false);
    Box membersBox = IncatComponents.BoxFactory(new JComponent[] {
      this.members, null,
      new JLabel("Members *"), null,
      this.isEquivalent
    }, false);
    // Add glues to keep the top set of widgets aligned regardless of which
    // access method box is shown
    nameBox.add(Box.createVerticalGlue());
    membersBox.add(Box.createVerticalGlue());
    accessBox.add(Box.createVerticalGlue());
    this.setContentPane(IncatComponents.BoxFactory(new JComponent[] {
      nameBox, membersBox, this.accessBox, null,
      IncatComponents.BoxFactory(new JComponent[] {
        new JLabel("Install Directory "), this.wd, null,
        new JLabel("Approval Email "), this.approvalEmail, null,
        new JLabel("Proxy Server "), this.proxyServer, null,
        new JLabel("Proxy User ID "), this.proxyUserId, null,
        new JLabel("Proxy Password "), this.proxyPassword, null,
        new JLabel("Proxy Lifetime "), this.proxyLifetime, null,
        new JLabel("Suspend Execution "), this.suspendCondition, new JLabel(" > "), this.suspendValue, null,
      }, false), null,
      IncatComponents.BoxFactory(new JComponent[] {
        IncatComponents.JButtonFactory("Cancel", cancelCommand, listener),
        IncatComponents.JButtonFactory("Ok", okCommand, this), null
      }, false), null
    }, false));
    this.setTitle("Incat Resource Dialog");
    this.pack();
    this.listener = listener;
  }

  /**
   * Returns the resource name.
   *
   * @return the resource name
   */
  public String getName() {
    return this.name.getText();
  }

  /**
   * Copies information from the resource dialog into the specified resource.
   *
   * @param resource the resource to update
   */
  public void getResource(WrapResource resource) {
    resource.setName(this.name.getText());
    resource.setMacroValue(Protocol.GROUPNAME_MACRO, this.name.getText());
    String pattern = this.members.getText();
    resource.setMacroValue(Protocol.PATTERN_MACRO, pattern);
    pattern =
      new ExpandablePattern(pattern.replaceAll("[ ,]", "|"), true).toString();
    resource.setXpath("//resource[matches(name, '^(" + pattern + ")$')]");
    String method = (String)this.access.getSelectedItem();
    resource.setMacroValue(Protocol.COMPUTE_METHOD_MACRO, method);
    // Clear all method-specific fields, then set the appropriate ones
    resource.setServer(WrapResource.COMPUTE_SERVER, "");
    resource.setServer(WrapResource.FILE_SERVER, "");
    resource.removeMacro(Protocol.EMAIL_MACRO);
    resource.removeMacro(Protocol.LOGIN_ID_MACRO);
    resource.removeMacro(Protocol.SSH_IDENTITY_MACRO);
    resource.removeMacro(Protocol.SSH_PASSWORD_MACRO);
    if(method.startsWith("globus")) {
      resource.setServer
        (WrapResource.COMPUTE_SERVER, this.computeServer.getText());
      resource.setServer(WrapResource.FILE_SERVER, this.fileServer.getText());
    } else if(method.equals("ssh")) {
      resource.setMacroValue
        (Protocol.LOGIN_ID_MACRO, this.userId.getText(), "");
      resource.setMacroValue
        (Protocol.SSH_IDENTITY_MACRO, this.keyFile.getText(), "");
      resource.setMacroValue
        (Protocol.SSH_PASSWORD_MACRO,
         new String(this.keyPassword.getPassword()), "");
    }
    resource.setMacroValue
        (Protocol.EMAIL_MACRO, this.approvalEmail.getText(), "");
    resource.setServer(WrapResource.PROXY_SERVER, this.proxyServer.getText());
    resource.setMacroValue
      (Protocol.MYPROXY_LIFETIME_MACRO, this.proxyLifetime.getText(), "");
    resource.setMacroValue(Protocol.MYPROXY_PASSWORD_MACRO,
                           new String(this.proxyPassword.getPassword()), "");
    resource.setMacroValue
      (Protocol.MYPROXY_USERNAME_MACRO, this.proxyUserId.getText(), "");
    resource.setMacroValue
      (Protocol.EQUIVALENT_MACRO,
       new Boolean(this.isEquivalent.isSelected()).toString());
    resource.setMacroValue(Protocol.WORKING_DIR_MACRO, this.wd.getText(), "");
    String suspendCondition = (String)this.suspendCondition.getSelectedItem();
    String suspendValue = this.suspendValue.getText().replaceAll(" ", "" );
    if ( ! suspendCondition.equals("") && ! suspendValue.equals("")) {
      resource.setMacroValue
        (Protocol.SUSPEND_MACRO, suspendCondition + ">" + suspendValue );
    } else {
      resource.removeMacro(Protocol.SUSPEND_MACRO);
    }

  }

  /**
   * Copies information from the specified resource into the resource dialog.
   *
   * @param resource the resource to show
   */
  public void setResource(WrapResource resource) {
    this.name.setText(resource.getName());
    this.members.setText(resource.getMacroValue(Protocol.PATTERN_MACRO));
    String method = resource.getMacroValue(Protocol.COMPUTE_METHOD_MACRO);
    IncatComponents.setJComboBoxSelectedString
      (this.access, method == null ? Protocol.COMPUTE_METHODS[0] : method);
    this.approvalEmail.setText(resource.getMacroValue(Protocol.EMAIL_MACRO));
    this.keyFile.setText
      (resource.getMacroValue(Protocol.SSH_IDENTITY_MACRO));
    this.keyPassword.setText
      (resource.getMacroValue(Protocol.SSH_PASSWORD_MACRO));
    this.userId.setText(resource.getMacroValue(Protocol.LOGIN_ID_MACRO));
    this.computeServer.setText(resource.getServer(WrapResource.COMPUTE_SERVER));
    this.fileServer.setText(resource.getServer(WrapResource.FILE_SERVER));
    this.proxyServer.setText(resource.getServer(WrapResource.PROXY_SERVER));
    this.proxyLifetime.setText
      (resource.getMacroValue(Protocol.MYPROXY_LIFETIME_MACRO));
    this.proxyPassword.setText
      (resource.getMacroValue(Protocol.MYPROXY_PASSWORD_MACRO));
    this.proxyUserId.setText
      (resource.getMacroValue(Protocol.MYPROXY_USERNAME_MACRO));
    String state = resource.getMacroValue(Protocol.EQUIVALENT_MACRO);
    this.isEquivalent.setSelected(state != null && state.equals("true"));
    this.wd.setText(resource.getMacroValue(Protocol.WORKING_DIR_MACRO));
    String suspend = resource.getMacroValue(Protocol.SUSPEND_MACRO);
    String suspendCondition = (suspend == null) ? Protocol.SUSPEND_CONDITIONS[0] : suspend.split(">")[0];
    String suspendValue = (suspend == null) ? "" : suspend.split(">")[1];
    IncatComponents.setJComboBoxSelectedString
      (this.suspendCondition, suspendCondition);
    this.suspendValue.setText(suspendValue);
    this.pack();
  }

  /**
   * An action listener that checks for required fields on dialog exit and
   * modifies the dialog when the access method changes.
   */
  public void actionPerformed(ActionEvent ae) {
    String command = ae.getActionCommand();
    if(command.equals("accesschange")) {
      String method = (String)this.access.getSelectedItem();
      // Note: Nested box for access-specific is second element in accessBox
      this.accessBox.remove(1);
      if(method.startsWith("globus")) {
        this.accessBox.add(this.accessGlobusBox, 1);
      } else if(method.equals("local")) {
        this.accessBox.add(this.accessLocalBox, 1);
      } else if(method.equals("manual")) {
        this.accessBox.add(this.accessManualBox, 1);
      } else if(method.equals("ssh")) {
        this.accessBox.add(this.accessSshBox, 1);
      }
      this.pack();
    } else {
      if(this.name.getText().equals("") ||
         this.members.getText().equals("")) {
        JOptionPane.showMessageDialog
          (this, "Please provide a resource name and member set",
           "Incomplete", JOptionPane.ERROR_MESSAGE);
          return;
      }
      if( ! this.suspendCondition.getSelectedItem().equals("") &&
          ! suspendValue.getText().matches("^\\s*\\d+\\s*$")  ) {
        JOptionPane.showMessageDialog
          (this, "Invalid value '" + suspendValue.getText() + "' for suspend execution -- must be a number",
           "Error", JOptionPane.ERROR_MESSAGE);
          return;
      }
      this.listener.actionPerformed(ae);
    }
  }

}
