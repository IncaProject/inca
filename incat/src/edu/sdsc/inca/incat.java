package edu.sdsc.inca;

import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.util.XmlWrapper;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class incat extends JTabbedPane
  implements ActionListener, ChangeListener, WindowListener {

  protected JDialog aboutDialog;
  protected AgentClient ac;
  protected IncatEditMenu editMenu;
  protected IncatHelpBrowser helpBrowser;
  protected WrapConfig lastSavedConfig;
  protected WrapConfig lastCommittedConfig;
  protected String path;
  protected IncatRepositoryTab repositoryTab;
  protected IncatResourceTab resourceTab;
  protected IncatSuiteTab suiteTab;
  protected Frame waitDialog; // AWT rather than Swing
  protected Label waitMessage;

  public incat(AgentClient ac, String path) {

    super();

    this.ac = ac;
    this.editMenu = null;
    this.path = path;

    // Construct the main window components ...
    this.repositoryTab = new IncatRepositoryTab(this);
    this.resourceTab = new IncatResourceTab(this);
    this.suiteTab = new IncatSuiteTab(this);
    this.add("Repositories", this.repositoryTab);
    this.add("Resource Configuration", this.resourceTab);
    this.add("Suites", suiteTab);
    this.addChangeListener(this);

    // ... dialog for the "About incat" and "Help" menu items ...
    this.aboutDialog = new JDialog();
    this.aboutDialog.setContentPane(
      IncatComponents.BoxFactory(new JComponent[] {
        new JLabel(" "), null,
        new JLabel("   Inca Administration Tool   "), null,
        new JLabel("   Inca v2.0   "), null,
        new JLabel(" "), null,
        new JLabel("   Copyright \u00A9 2007, SDSC   "), null,
        new JLabel(" "), null
      }, true)
    );
    this.aboutDialog.setTitle("About Incat");
    this.aboutDialog.pack();
    this.helpBrowser = new IncatHelpBrowser();
    this.helpBrowser.setSection(IncatHelpBrowser.INCAT_SECTION);

    // ... and a wait message for long-running actions.  Note that this latter
    // is AWT rather than Swing to avoid refresh problems encountered when
    // opening a JDialog from a Swing event handler.
    this.waitDialog = new Frame("Incat Message");
    this.waitMessage = new Label
      ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", Label.CENTER);
    this.waitDialog.add(new Label(""), BorderLayout.NORTH);
    this.waitDialog.add(waitMessage, BorderLayout.CENTER);
    this.waitDialog.add(new Label(""), BorderLayout.SOUTH);
    this.waitDialog.pack();

    this.lastCommittedConfig = getConfig();
    this.lastSavedConfig = getConfig();

    // Initialize from agent and/or file, as appropriate.  Note that if we have
    // both agent and file that the latter overwrites the former.  We still
    // must query the agent, though, so lastCommittedConfig is set correctly.
    if(this.ac.getPort() >= 0) {
      readFromAgent(false);
      setConfig(lastCommittedConfig);
    }
    if(this.path != null && new File(this.path).exists()) {
      if(readFromFile(false)) {
        setConfig(lastSavedConfig);
      }
    }

  }

  /**
   * Responds to user GUI actions.
   */
  public void actionPerformed(ActionEvent ae) {

    String action = ae.getActionCommand();

    if(action.equals("about")) {
      this.aboutDialog.setVisible(true);
    } else if(action.equals("commit")) {
      writeToAgent();
    } else if(action.equals("connect")) {
      readFromAgent(true);
      WrapConfig current = getConfig();
      if(current.getRepositories().length == 0 &&
         current.getResources().length == 0 &&
         current.getSuites().length == 0) {
        setConfig(lastCommittedConfig);
      }
    } else if(action.equals("elementAdd ...")) {
      addOrEditListElement(true);
    } else if(action.equals("elementClone")) {
      cloneListElement();
    } else if(action.equals("elementDelete")) {
      deleteListElement();
    } else if(action.equals("elementEdit ...")) {
      addOrEditListElement(false);
    } else if(action.equals("elementFind ...")) {
      IncatList.findListElement(true);
    } else if(action.equals("elementFindNext")) {
      IncatList.findListElement(false);
    } else if(action.equals("fileOpen")) {
      if(readFromFile(true)) {
        setConfig(lastSavedConfig);
      }
    } else if(action.equals("fileSave") || action.equals("fileSaveAs")) {
      writeToFile(this.path == null || action.equals("fileSaveAs"));
    } else if(action.equals("help")) {
      this.helpBrowser.setVisible(true);
    } else if(action.equals("quit")) {
      WrapConfig config = getConfig();
      if(!config.equals(this.lastSavedConfig)) {
        int response =
          JOptionPane.showConfirmDialog(this, "Save changes before exiting?");
        if(response == JOptionPane.YES_OPTION) {
          writeToFile(this.path == null);
        } else if(response == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }
      if(ac.getPort() >= 0 && !config.equals(lastCommittedConfig)) {
        int response =
          JOptionPane.showConfirmDialog(this, "Commit changes before exiting?");
        if(response == JOptionPane.YES_OPTION) {
          writeToAgent();
        } else if(response == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }
      System.exit(0);
   }

  }

  /**
   * An event method: either adds a new element to the selected list or edits
   * the currently-selected element.
   *
   * @param add whether to add a new element
   */
  protected void addOrEditListElement(boolean add) {
    java.awt.Component focused =
      KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    while(focused != null) {
      if(focused == this.repositoryTab) {
        this.repositoryTab.addOrEditListElement(add);
      } else if(focused == this.resourceTab) {
        this.resourceTab.addOrEditListElement(add);
      } else if(focused == this.suiteTab) {
        this.suiteTab.addOrEditListElement(add);
      }
      focused = focused.getParent();
    }
  }

  /**
   * An event method: clones the currently-selected list element.
   */
  protected void cloneListElement() {
    java.awt.Component focused =
      KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    while(focused != null) {
      if(focused == this.suiteTab) {
        this.suiteTab.cloneListElement();
      } else if(focused == this.repositoryTab) {
        this.repositoryTab.cloneListElement();
      } else if(focused == this.resourceTab) {
        this.resourceTab.cloneListElement();
      }
      focused = focused.getParent();
    }
  }

  /**
   * An event method: deletes the currently-selected list element.
   */
  protected void deleteListElement() {
    java.awt.Component focused =
      KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    while(focused != null) {
      if(focused == this.suiteTab) {
        this.suiteTab.deleteListElement();
      } else if(focused == this.repositoryTab) {
        this.repositoryTab.deleteListElement();
      } else if(focused == this.resourceTab) {
        this.resourceTab.deleteListElement();
      }
      focused = focused.getParent();
    }
  }

  /**
   * Retrieves a repository catalog from the agent.
   *
   * @param repositoryUrl the URL of the repository
   * @return the repository catalog, null if not connected to an agent
   */
  public Properties[] getCatalog(String repositoryUrl) {
    Properties[] result = null;
    if(this.ac.getPort() >= 0) {
      try {
        this.ac.connect();
        result = ac.getCatalog(repositoryUrl);
        this.ac.close();
      } catch(Exception e) {
        showErrorMessage("Unable to access repository: " + e);
      }
    }
    return result;
  }

  /**
   * Gets the Inca configuration from the display.
   *
   * @return the Inca configuration, as specified on the incat display
   */
  protected WrapConfig getConfig() {
    WrapConfig result = new WrapConfig();
    result.setRepositories(this.repositoryTab.getRepositories());
    result.setResources(this.resourceTab.getResources(true));
    result.setSuites(this.suiteTab.getSuites());
    // NOTE: since we place objects from the config (e.g., suites) directly
    // into the GUI, we use a copy to avoid having changes to the memory and
    // GUI versions modify each other.
    return result.copy();
  }

  /**
   * Returns an ArrayList of resources included, recursively, in the member
   * pattern of a resource.
   *
   * @param resource the resource to search
   * @param includeSelf include the resource itself as a descendant?
   * @param leavesOnly whether to return all descendants, or only those that
   *                   refer to hosts
   * @return an ArrayList of descendants of the specified resource
   */
  public ArrayList<WrapResource> getDescendants(WrapResource resource,
                                  boolean includeSelf,
                                  boolean leavesOnly) {
    return this.resourceTab.getDescendants(resource, includeSelf, leavesOnly);
  }

  /**
   * Returns the edit menu associcated with this incat GUI.
   *
   * @return the GUI edit menu
   */
  public IncatEditMenu getEditMenu() {
    return this.editMenu;
  }

  /**
   * Returns an array of all reporters defined in any repository.
   *
   * @return an array of all reporters
   */
  public WrapReporter[] getReporters() {
    return this.repositoryTab.getReporters();
  }

  /**
   * Returns the resource with a specified name, null if none.
   *
   * @param name the resource name
   * @return the resource with the specified name, null if none
   */
  public WrapResource getResource(String name) {
    return this.resourceTab.getResource(name);
  }

  /**
   * Returns an array of the resources shown in the resource tab.
   *
   * @param addHosts whether to include in the returned value resources for the
   *                 member hosts of the resources shown
   * @return an array of all resources
   */
  public WrapResource[] getResources(boolean addHosts) {
    return this.resourceTab.getResources(addHosts);
  }

  /**
   * Get configuration from the Agent and store it in lastCommittedConfig.
   *
   * @param prompt indicates whether to prompt the user for an agent spec
   */
  protected void readFromAgent(boolean prompt) {
    String oldHost = ac.getHostname();
    int oldPort = ac.getPort();
    if(prompt) {
      String newAgentSpec =
        JOptionPane.showInputDialog(this, "Agent to contact", "");
      if(newAgentSpec == null || newAgentSpec.equals("")) {
        return; // user cancel
      }
      ac.setServer(newAgentSpec, 0);
    }
    if(ac.getPort() < 0) {
      showErrorMessage("Not connected to an Inca Agent");
      return;
    }
    showWaitMessage("Receiving info from Agent ...");
    try {
      ac.connect();
      String xml = ac.getConfig();
      ac.close();
      xml = XmlWrapper.cryptSensitive(xml, ac.getPassword(), true);
      this.lastCommittedConfig = new WrapConfig(xml);
    } catch(Exception e) {
      showErrorMessage("Error reading from agent: " + e);
      ac.setHostname(oldHost);
      ac.setPort(oldPort);
    }
    showWaitMessage(null);
  }

  /**
   * Get configuration from an XML file and store it in lastSavedConfig.
   *
   * @param prompt indicates whether to prompt the user for a file path
   * @return true if file loaded into lastSavedConfig, else false
   */
  protected boolean readFromFile(boolean prompt) {
    String filePath = this.path;
    if(prompt) {
      JFileChooser chooser = new JFileChooser();
      chooser.setCurrentDirectory
        (new File(filePath==null ? System.getProperty("user.dir") : filePath));
      if(chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
        return false;
      }
      filePath = chooser.getSelectedFile().getAbsolutePath();
    }
    if(filePath == null || !new File(filePath).exists()) {
      showErrorMessage("No file available");
      return false;
    }
    try {
      String xml = StringMethods.fileContents(filePath);
      xml = XmlWrapper.cryptSensitive(xml, ac.getPassword(), true);
      this.lastSavedConfig = new WrapConfig(xml);
      this.path = filePath;
    } catch(Exception e) {
      showErrorMessage("Error reading from " + filePath + ": " + e);
    }
    return true;
  }

  /**
   * Ask the agent to run a series immediately.
   *
   * @param s the series to run
   * @param resource the name of the resource to run the series on
   */
  public void runSeries(WrapSeries s, String resource) {
    if(ac.getPort() < 0) {
      showErrorMessage("Not connected to an Inca Agent");
      return;
    }
    WrapSuite suite = new WrapSuite(Protocol.IMMEDIATE_SUITE_NAME, "");
    WrapSeries copy = suite.addNewSeries();
    copy.copy(s);
    copy.setCron(null);
    copy.setAction(Protocol.SERIES_CONFIG_ADD);
    copy.setResource(resource);
    showWaitMessage("Sending info to Agent ...");
    try {
      ac.connect();
      ac.runNow("incat", suite.toXml());
      ac.close();
    } catch(Exception e) {
      showErrorMessage("Run series failed: " + e);
    }
    showWaitMessage(null);
  }

  /**
   * Sets the display to represent a specified Inca configuration.
   *
   * @param config the Inca configuration
   */
  protected void setConfig(WrapConfig config) {
    // NOTE: since we place objects from the config (e.g., suites) directly
    // into the GUI, we use a copy to avoid having changes to the memory and
    // GUI versions modify each other.
    config = config.copy();
    this.repositoryTab.setRepositories(config.getRepositories());
    this.resourceTab.setResources(config.getResources());
    this.suiteTab.setSuites(config.getSuites());
  }

  /**
   * Sets the edit menu associcated with this incat GUI.
   *
   * @param menu the GUI edit menu
   */
  public void setEditMenu(IncatEditMenu menu) {
    this.editMenu = menu;
  }

  /**
   * Sets the help dialog to show a specified section.
   *
   * @param section the help section to show
   */
  public void setHelpSection(String section) {
    this.helpBrowser.setSection(section);
  }

  /**
   * Informs the user of an error.
   *
   * @param message the error message
   */
  public void showErrorMessage(String message) {
    JOptionPane.showMessageDialog
      (null, message, "Incat Message", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Set the message in the incat wait dialog.  Closes the dialog if the
   * message is null.
   *
   * @param message the message to display, null for none
   */
  protected void showWaitMessage(String message) {
    if(message == null) {
      waitDialog.setVisible(false);
    } else {
      waitMessage.setText(message);
      waitDialog.setVisible(true);
    }
  }

  /**
   * Sends the current configuration to the connected Agent.
   */
  protected void writeToAgent() {
    if(ac.getPort() < 0) {
      showErrorMessage("Not connected to an Inca Agent");
      return;
    }
    WrapConfig current = getConfig();
    if(current.equals(this.lastCommittedConfig)) {
      JOptionPane.showMessageDialog
        (this, "No commit needed; the current configuration is the same as the Agent's",
         "Incat Message", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    WrapConfig diffs = this.lastCommittedConfig.differences(current);
    WrapSuite[] suiteDiffs = diffs.getSuites();
    for(int i = 0; i < suiteDiffs.length; i++) {
      WrapSuite thisSuite = suiteDiffs[i];
      for(int j = 0; j < thisSuite.getSeriesCount(); j++) {
        WrapSeries thisSeries = thisSuite.getSeriesAt(j);
        if(!thisSeries.getAction().equals(Protocol.SERIES_CONFIG_DELETE) &&
           this.suiteTab.seriesHasErrors(thisSeries)) {
          showErrorMessage("Cannot commit series with errors");
          return;
        }
      }
    }
    showWaitMessage("Sending info to Agent ...");
    try {
      String xml = diffs.toXml();
      xml = XmlWrapper.cryptSensitive(xml, ac.getPassword(), false);
      ac.connect();
      try {
        String dn = ac.getDn(false);
        if(dn != null) {
          ac.commandPermit
            (dn, Protocol.RUNNOW_ACTION + " " + Protocol.RUNNOW_TYPE_INCAT);
          ac.commandPermit(dn, Protocol.CONFIG_ACTION);
        }
        ac.setConfig(xml);
      } catch(Exception e) {
        showErrorMessage("Error writing to agent: " + e);
      }
      this.lastCommittedConfig = current;
      ac.close();
    } catch(Exception e) {
      showErrorMessage("Error writing to agent: " + e);
    }
    showWaitMessage(null);
  }

  /**
   * Writes the current configuration to a file.
   *
   * @param prompt indicates whether to prompt the user for a file path
   */
  protected void writeToFile(boolean prompt) {
    String filePath = this.path;
    if(prompt) {
      JFileChooser chooser = new JFileChooser();
      chooser.setCurrentDirectory
        (new File(filePath==null ? System.getProperty("user.dir") : filePath));
      if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
        return;
      }
      filePath = chooser.getSelectedFile().getAbsolutePath();
    }
    if(filePath == null) {
      showErrorMessage("No file available");
      return;
    }
    try {
      BufferedWriter bw =
        new BufferedWriter(new FileWriter(new File(filePath)));
      String xml = XmlWrapper.cryptSensitive
        (getConfig().toXml(), ac.getPassword(), false);
      bw.write(XmlWrapper.prettyPrint(xml, "  "));
      bw.close();
      this.path = filePath;
      this.lastSavedConfig = getConfig();
    } catch(Exception e) {
      e.printStackTrace();
      showErrorMessage("Error writing " + filePath + ": " + e);
    }
  }

  /**
   * Invoked when the user clicks a tab.
   */
  public void stateChanged(ChangeEvent e) {
    int tabIndex = ((JTabbedPane)e.getSource()).getSelectedIndex();
    setHelpSection(
      tabIndex == 0 ? IncatHelpBrowser.REPOSITORIES_SECTION :
      tabIndex == 1 ? IncatHelpBrowser.RESOURCES_SECTION :
      IncatHelpBrowser.SUITES_SECTION
    );
  }

  /**
   * Transforms window closing into an action event.
   */
  public void windowClosing(WindowEvent e) {
    actionPerformed
      (new ActionEvent(e.getSource(), ActionEvent.RESERVED_ID_MAX + 1, "quit"));
  }

  // Must define these to implement interface, but don't care about the events.
  public void windowActivated(WindowEvent e)   { /*empty*/ }
  public void windowClosed(WindowEvent e)      { /*empty*/ }
  public void windowDeactivated(WindowEvent e) { /*empty*/ }
  public void windowDeiconified(WindowEvent e) { /*empty*/ }
  public void windowIconified(WindowEvent e)   { /*empty*/ }
  public void windowOpened(WindowEvent e)      { /*empty*/ }

  /**
   * Create the GUI and show it.  For thread safety, this method should be
   * invoked from the event-dispatching thread.
   *
   * @param ac client for Inca agent we should communicate with
   * @param path path to config file we should read/write
   */
  protected static void createAndShowGUI(AgentClient ac, String path) {
    // Get our menus into the Mac system bar, instead of inside the frame
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    // Set up the frame with an initial, empty menu bar
    JMenuBar menuBar = new JMenuBar();
    JFrame.setDefaultLookAndFeelDecorated(true);
    JFrame frame = new JFrame("Inca Administration Tool");
    frame.setJMenuBar(menuBar);
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    // Set up the menus
    final incat contentPane = new incat(ac, path);
    frame.setContentPane(contentPane);
    frame.addWindowListener(contentPane);
    JMenu fileMenu = new JMenu("File");
    fileMenu.add
      (IncatComponents.JMenuItemFactory("Open...","fileOpen",contentPane,'O'));
    fileMenu.add
      (IncatComponents.JMenuItemFactory("Save", "fileSave", contentPane, 'S'));
    fileMenu.add
      (IncatComponents.JMenuItemFactory("Save As...","fileSaveAs",contentPane));
    IncatEditMenu editMenu = new IncatEditMenu(contentPane);
    contentPane.setEditMenu(editMenu);
    JMenu agentMenu = new JMenu("Agent");
    agentMenu.add
      (IncatComponents.JMenuItemFactory("Connect...", "connect", contentPane));
    agentMenu.add
      (IncatComponents.JMenuItemFactory("Commit", "commit", contentPane, 'K'));
    JMenu helpMenu = new JMenu("Help");
    helpMenu.add
      (IncatComponents.JMenuItemFactory("incat Help","help",contentPane,'?'));
    // try to add a Mac ApplicationListener; an exception means we're running
    // on a non-Mac and so need to generate our own about & quit menu items
    try {
      Portability.addMacApplicationListener(contentPane, "about", "quit");
    } catch(NoClassDefFoundError e) {
      helpMenu.add
        (IncatComponents.JMenuItemFactory("About incat", "about", contentPane));
      fileMenu.add
        (IncatComponents.JMenuItemFactory("Quit incat","quit",contentPane,'Q'));
    }
    menuBar.add(fileMenu);
    menuBar.add(editMenu);
    menuBar.add(agentMenu);
    menuBar.add(helpMenu);
    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    final AgentClient ac = new AgentClient();
    final String opts = ConfigProperties.mergeValidOptions
      (AgentClient.AGENT_CLIENT_OPTS, "  f|file  path  XML file path\n", true);
    final ConfigProperties props = new ConfigProperties();
    try {
      props.setPropertiesFromArgs(opts, args);
    } catch(ConfigurationException e) {
      System.err.println("ConfigurationException: " + e);
      System.exit(1);
    }
    try {
      AgentClient.configComponent
        (ac, args, opts, "inca.incat.", "edu.sdsc.inca.incat",
         "inca-common-java-version");
    } catch(Exception e) {
      System.err.println("Configuration failed: " + e);
      System.exit(1);
    }
    // Taken verbatim from the Swing tutorial.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI(ac, props.getProperty("file"));
      }
    });
  }

}
