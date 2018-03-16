package edu.sdsc.inca;

import java.net.URL;
import javax.swing.JOptionPane;

/**
 * A window that gives help information about the incat tool.
 */
public class IncatHelpBrowser extends BrainDeadBrowser {

  public static final String INCAT_SECTION = "USERGUIDE-INCAT";
  public static final String REPOSITORIES_SECTION = INCAT_SECTION + "-REPOS";
  public static final String RESOURCES_SECTION = INCAT_SECTION + "-RESOURCES";
  public static final String SUITES_SECTION = INCAT_SECTION + "-SUITES";
  
  protected static final String BACKUP_HELP_FILE =
    "edu/sdsc/inca/incahelp.html";
  protected static final String RELEASE = "latest";
  protected static final String USERGUIDE_DIR_URL =
    "http://inca.sdsc.edu/releases/" + RELEASE + "/guide/";
  protected static final String USERGUIDE_NAME = "userguide-incat.html";
  protected static final String USERGUIDE_URL =
    USERGUIDE_DIR_URL + USERGUIDE_NAME;

  /**
   * Constructs a new IncatHelpBrowser.
   */
  public IncatHelpBrowser() {
    super();
    this.setTitle("Incat Help");
  }

  /**
   * If the browser is visible, fills the editor with the contents of the URL
   * in the location text box.
   */
  protected void redraw() {
    String page = this.location.getText();
    if(!this.isVisible() || page.equals("")) {
       return;
    }
    try {
      this.editor.setPage(page);
    } catch(Exception e) {
      URL helpUrl;
      if(!page.startsWith(USERGUIDE_URL)) {
        JOptionPane.showMessageDialog
          (this, "Unable to access " + page + ":\n" + e,
           "Incat Message", JOptionPane.ERROR_MESSAGE);
      } else if((helpUrl = ClassLoader.getSystemClassLoader().
                             getResource(BACKUP_HELP_FILE)) == null) {
        JOptionPane.showMessageDialog
          (this, "Unable to open help file:\n" + e,
           "Incat Message", JOptionPane.ERROR_MESSAGE);
      } else {
        JOptionPane.showMessageDialog
          (this, "Online help is unavailable:\n" + e,
           "Incat Message", JOptionPane.INFORMATION_MESSAGE);
        page = page.replaceFirst(USERGUIDE_URL, helpUrl.toString());
        this.location.setText(page);
        redraw();
      }
    }
  }

  /**
   * Sets the help browser to show a particular section of the incat user doc.
   *
   * @param section one of the section constants defined above
   */
  public void setSection(String section) {
    setPage(USERGUIDE_URL + "#" + section);
  }

  /**
   * Makes the help dialog visible or invisible.
   *
   * @param visible true to make the dialog visible; false to make it invisible
   */
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    redraw();
  }

}
