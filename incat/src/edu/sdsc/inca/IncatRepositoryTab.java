package edu.sdsc.inca;

import edu.sdsc.inca.repository.Repository;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

/**
 * This class has a unique instance that implements the first (repository) tab
 * in the incat display.
 */
public class IncatRepositoryTab extends JSplitPane implements ActionListener {

  protected incat owner;
  protected IncatList reporterList;
  protected IncatList reporterPropertyList;
  protected IncatList repositoryList;
  protected JCheckBox showAllRepositories;

  /**
   * Constructs an IncatRepositoryTab.
   *
   * @param owner the incat instance that incorporates this tab.
   */
  public IncatRepositoryTab(incat owner) {
    super();
    this.reporterList = new IncatList("Reporters", "reporter", null, this);
    // Append a checkbox and Show button to reporterList
    this.showAllRepositories = new JCheckBox("All Repositories");
    this.showAllRepositories.addActionListener(this);
    this.showAllRepositories.setActionCommand("repositorySelect");
    this.showAllRepositories.setSelected(true);
    this.reporterList.add(IncatComponents.BoxFactory(new JComponent[] {
      this.showAllRepositories,
      IncatComponents.JButtonFactory("Show", "reporterShow", this)
    }, true));
    this.reporterPropertyList =
      new IncatList("Reporter Properties", "property", null, this);
    this.repositoryList = new IncatList
      ("Repositories", "repository", "Add ...,Delete,Refresh", this);
    IncatComponents.alignBoxHeights(new Box[] {
      this.reporterList, this.reporterPropertyList, this.repositoryList
    });
    // Use JSplitPane so that the user can reallocate window space.  JSplitPane
    // allows only two components, so we nest a JSplitPane for the rightmost
    // two, using a default allocation of 33%/67% for the outer and 50%/50% for
    // the inner.  Suppress borders to avoid multiple bevel layers.
    JSplitPane right = new JSplitPane();
    this.setLeftComponent(this.repositoryList);
    this.setRightComponent(right);
    this.setResizeWeight(0.33);
    this.setBorder(null);
    right.setLeftComponent(this.reporterList);
    right.setRightComponent(this.reporterPropertyList);
    right.setResizeWeight(0.5);
    right.setBorder(null);
    this.owner = owner;
  }

  /**
   * Responds to user GUI actions in this component.
   */
  public void actionPerformed(ActionEvent ae) {
    String action = ae.getActionCommand();
    if(action.equals("reporterSelect") ||
       action.equals("reporterSingleClick")) {
      refreshReporterPropertyPanel();
    } else if(action.equals("reporterShow") ||
              action.equals("reporterDoubleClick")) {
      showReporter();
    } else if(action.equals("repositoryAdd ...")) {
      addOrEditRepository(true);
    } else if(action.equals("repositoryDelete")) {
      deleteRepository();
    } else if(action.equals("repositoryRefresh")) {
      refreshRepositoryCatalogs();
    } else if(action.equals("repositorySelect") ||
              action.equals("repositorySingleClick")) {
      refreshReporterPanel();
    } else if(action.indexOf("Focus") >= 0) {
      boolean gained = action.indexOf("Gained") >= 0;
      boolean editable = gained && action.startsWith("repository");
      this.owner.getEditMenu().setEditEnabled
        (editable, false, false, editable, gained);
    }
  }

  /**
   * An event method: either adds a new element to the selected list or edits
   * the currently-selected element.
   *
   * @param add whether to add a new element
   */
  public void addOrEditListElement(boolean add) {
    java.awt.Component focused =
      KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    while(focused != null) {
      if(focused == this.repositoryList) {
        addOrEditRepository(add);
      }
      focused = focused.getParent();
    }
  }

  /**
   * An event method: either adds a new element to the repository list or edits
   * the currently-selected element.
   *
   * @param add whether to add a new element
   */
  protected void addOrEditRepository(boolean add) {
    if(!add) {
      return; // editing not supported
    }
    Repository repo = null;
    String urlString = null;
    while(repo == null) {
      urlString =
        JOptionPane.showInputDialog(this, "Repository URL", urlString);
      if(urlString == null || urlString.equals("")) {
        return; // User cancel
      }
      Properties[] catalog = this.owner.getCatalog(urlString);
      try {
        URL url = new URL(urlString);
        repo =
          catalog == null ? new Repository(url) : new Repository(url, catalog);
      } catch(Exception e) {
        this.owner.showErrorMessage("Unable to access repository: " + e);
      }
    }
    this.repositoryList.addElement(repo);
    this.repositoryList.setSelectedElement(repo);
    this.repositoryList.sort();
    refreshReporterPanel();
    IncatRepositoryDialog.setRepositories
      ((Repository[])this.repositoryList.toArray(new Repository[this.repositoryList.getLength()]));
  }

  /**
   * An event method: clones the currently-selected list element.
   */
  public void cloneListElement() {
    // empty -- not supported
  }

  /**
   * An event method: deletes the currently-selected list element.
   */
  public void deleteListElement() {
    java.awt.Component focused =
      KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    while(focused != null) {
      if(focused == this.repositoryList) {
        deleteRepository();
      }
      focused = focused.getParent();
    }
  }

  /**
   * An event method: deletes the currently-selected repository.
   */
  protected void deleteRepository() {
    int index = this.repositoryList.getSelectedIndex();
    if(index < 0) {
      return;
    }
    this.repositoryList.removeElementAt(index);
    refreshReporterPanel();
    IncatRepositoryDialog.setRepositories
      ((Repository[])this.repositoryList.toArray(new Repository[this.repositoryList.getLength()]));
  }

  /**
   * Returns the union of all repository catalogs.
   */
  protected WrapReporter[] getReporters() {
    Hashtable merged = new Hashtable();
    for(int i = 0; i < this.repositoryList.getLength(); i++) {
      Repository repo = (Repository)this.repositoryList.getElementAt(i);
      Properties[] catalog = repo.getCatalog();
      for(int j = 0; j < catalog.length; j++) {
        Properties p = catalog[j];
        String name = p.getProperty("name");
        String version = p.getProperty("version");
        if(name != null && version != null) {
          merged.put(name + " v" + version, p);
        }
      }
    }
    Properties[] result = new Properties[merged.size()];
    int i = 0;
    for(Enumeration e = merged.keys(); e.hasMoreElements(); ) {
      result[i++] = (Properties)merged.get(e.nextElement());
    }
    return wrapCatalog(result);
  }

  /**
   * Returns an array of the repositories shown in the repository tab.
   *
   * @return an array of all repository URLs
   */
  public String[] getRepositories() {
    String[] result = new String[this.repositoryList.getLength()];
    for(int i = 0; i < result.length; i++) {
      result[i] =
       ((Repository)this.repositoryList.getElementAt(i)).getURL().toString();
    }
    return result;
  }

  /**
   * Rewrites the contents of the reporter list to reflect the current
   * selection the repository list.
   */
  protected void refreshReporterPanel() {
    Repository selected = (Repository)this.repositoryList.getSelectedElement();
    WrapReporter[] catalog =
      this.showAllRepositories.isSelected() ? this.getReporters() :
      wrapCatalog(selected == null ? new Properties[0] : selected.getCatalog());
    this.reporterList.setElements(catalog);
    this.reporterList.sort();
    this.reporterList.setSelectedIndex(0);
    refreshReporterPropertyPanel();
  }

  /**
   * Rewrites the contents of the reporter property list to reflect the current
   * selection in the reporter list.
   */
  protected void refreshReporterPropertyPanel() {
    this.reporterPropertyList.removeAllElements();
    WrapReporter reporter=(WrapReporter)this.reporterList.getSelectedElement();
    if(reporter == null) {
      return;
    }
    for(Enumeration e = reporter.reporter.keys(); e.hasMoreElements(); ) {
      String prop = (String)e.nextElement();
      this.reporterPropertyList.addElement
        (prop + ": " + reporter.reporter.getProperty(prop));
    }
    this.reporterPropertyList.sort();
    this.reporterPropertyList.setSelectedIndex(0);
  }

  /**
   * An event method: refreshed the cached catalog for all repositories.
   */
  public void refreshRepositoryCatalogs() {
    this.owner.getCatalog(null); // Force agent refresh if we're connected
    for(int i = 0; i < this.repositoryList.getLength(); i++) {
      Repository r = (Repository)this.repositoryList.getElementAt(i);
      URL url = r.getURL();
      Properties[] catalog = this.owner.getCatalog(url.toString());
      if(catalog == null) {
        try {
          Repository refreshed = new Repository(url);
          catalog = refreshed.getCatalog();
        } catch(Exception e) {
          // empty
        }
      }
      if(catalog != null) {
        r.setCatalog(catalog);
      }
    }
    refreshReporterPanel();
  }

  /**
   * Replaces the repositories shown in the repository tab.
   *
   * @param repositories URLs for the repositories to show
   */
  public void setRepositories(String[] repositories) {
    this.repositoryList.removeAllElements();
    for(int i = 0; i < repositories.length; i++) {
      Repository repo = null;
      String urlString = repositories[i];
      Properties[] catalog = this.owner.getCatalog(urlString);
      try {
        URL url = new URL(urlString);
        repo =
          catalog == null ? new Repository(url) : new Repository(url, catalog);
      } catch(Exception e) {
        this.owner.showErrorMessage("Unable to access repository: " + e);
      }
      if(repo != null) {
        this.repositoryList.addElement(repo);
      }
    }
    this.repositoryList.sort();
    this.repositoryList.setSelectedIndex(0);
    refreshReporterPanel();
    IncatRepositoryDialog.setRepositories
      ((Repository[])this.repositoryList.toArray(new Repository[this.repositoryList.getLength()]));
  }

  /**
   * An event method: displays the currently-selected reporter in a dialog.
   */
  protected void showReporter() {
    String file = null;
    WrapReporter reporter=(WrapReporter)this.reporterList.getSelectedElement();
    if(reporter == null || (file = reporter.getProperty("file")) == null) {
      return;
    }
    try {
      IncatRepositoryDialog dialog = new IncatRepositoryDialog();
      dialog.setReporter(file);
      dialog.setVisible(true);
    } catch(IOException e) {
      this.owner.showErrorMessage(e.toString());
    }
  }

  /**
   * Coverts the elements of a reporter catalog into an array of WrapReporters.
   * Filters elements that represent libraries rather than reporters.
   *
   * @param catalog the reporters to wrap
   * @return the elements of the catalog, each placed in a WrapReporter
   */
  protected WrapReporter[] wrapCatalog(Properties[] catalog) {
    ArrayList result = new ArrayList();
    for(int i = 0; i < catalog.length; i++) {
      String file = catalog[i].getProperty("file");
      if(file != null && !file.matches("^.*\\.(pm|rpm|gz|tgz|tar)$")) {
        result.add(new WrapReporter(catalog[i]));
      }
    }
    return (WrapReporter [])result.toArray(new WrapReporter[result.size()]);
  }

}
