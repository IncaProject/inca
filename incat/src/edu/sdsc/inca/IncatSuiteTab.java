package edu.sdsc.inca;

import edu.sdsc.inca.util.StringMethods;
import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;

/**
 * This class has a unique instance that implements the third (suite) tab
 * in the incat display.
 */
public class IncatSuiteTab extends JSplitPane implements ActionListener {

  protected WrapSeries editSeries;
  protected WrapSuite editSuite;
  protected incat owner;
  protected IncatSeriesDialog seriesDialog;
  protected IncatList seriesList;
  protected IncatSuiteDialog suiteDialog;
  protected IncatList suiteList;

  /**
   * Constructs an IncatSuiteTab.
   *
   * @param owner the incat instance that incorporates this tab.
   */
  public IncatSuiteTab(incat owner) {
    super();
    this.seriesDialog =
      new IncatSeriesDialog(this, "seriesEditOk", "seriesEditCancel");
    this.seriesList =
      new IncatList("Series", "series", "Add ...,Edit ...,Delete,Clone", this);
    // Append a second row of buttons to seriesList
    this.seriesList.add(IncatComponents.BoxFactory(new JComponent[] {
      IncatComponents.JButtonFactory("Move To ...", "seriesMove", this),
      IncatComponents.JButtonFactory("Run Now ...", "seriesRun", this)
    }, true));
    this.suiteDialog =
      new IncatSuiteDialog(this, "suiteEditOk", "suiteEditCancel");
    this.suiteList =
      new IncatList("Suites", "suite", "Add ...,Edit ...,Delete,Clone", this);
    IncatComponents.alignBoxHeights
      (new Box[] {this.seriesList, this.suiteList});
    // Use JSplitPane so that the user can reallocate window space.  Suppress
    // borders to avoid multiple bevel layers.
    this.setLeftComponent(this.suiteList);
    this.setRightComponent(this.seriesList);
    this.setResizeWeight(0.5);
    this.setBorder(null);
    this.seriesList.setCellRenderer(new WrapSeriesRenderer());
    this.owner = owner;
  }

  /**
   * Responds to user GUI actions in this component.
   */
  public void actionPerformed(ActionEvent ae) {
    String action = ae.getActionCommand();
    if(action.equals("seriesAdd ...")) {
      addOrEditSeries(true);
    } else if(action.equals("seriesClone")) {
      cloneSeries();
    } else if(action.equals("seriesDelete")) {
      deleteSeries();
    } else if(action.equals("seriesEdit ...") ||
              action.equals("seriesDoubleClick")) {
      addOrEditSeries(false);
    } else if(action.equals("seriesEditCancel")) {
      this.seriesDialog.setVisible(false);
    } else if(action.equals("seriesEditOk")) {
      updateSeries();
    } else if(action.equals("seriesMove")) {
      moveSeries();
    } else if(action.equals("seriesRun")) {
      runSeries();
    } else if(action.equals("suiteAdd ...")) {
      addOrEditSuite(true);
    } else if(action.equals("suiteClone")) {
      cloneSuite();
    } else if(action.equals("suiteDelete")) {
      deleteSuite();
    } else if(action.equals("suiteEdit ...") ||
              action.equals("suiteDoubleClick")) {
      addOrEditSuite(false);
    } else if(action.equals("suiteEditCancel")) {
      this.suiteDialog.setVisible(false);
    } else if(action.equals("suiteEditOk")) {
      updateSuite();
    } else if(action.equals("suiteSelect") ||
              action.equals("suiteSingleClick")) {
      refreshSeriesPanel();
    } else if(action.indexOf("Focus") >= 0) {
      boolean gained = action.indexOf("Gained") >= 0;
      boolean editable = gained;
      this.owner.getEditMenu().setEditEnabled
        (editable, editable, editable, editable, gained);
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
      if(focused == this.seriesList) {
        addOrEditSeries(add);
      } else if(focused == this.suiteList) {
        addOrEditSuite(add);
      }
      focused = focused.getParent();
    }
  }

  /**
   * An event method: copies either an empty series or the currently-selected
   * one into the series dialog.
   *
   * @param add copy a new series
   */
  protected void addOrEditSeries(boolean add) {
    WrapSuite suite = (WrapSuite)this.suiteList.getSelectedElement();
    if(suite == null) {
      return;
    }
    WrapReporter[] reporters = this.owner.getReporters();
    WrapResource[] resources = this.owner.getResources(false);
    // Disallow add w/no reporters/resources, since these fields are required.
    // Edit is okay, since the series already has values for these.
    if(add && (resources.length == 0 || reporters.length == 0)) {
      this.owner.showErrorMessage
        ("No reporters/resources available for series definition");
    } else {
      this.seriesDialog.setReporterChoices(reporters);
      this.seriesDialog.setResourceChoices(resources);
      this.editSeries =
        add ? null : (WrapSeries)this.seriesList.getSelectedElement();
      this.seriesDialog.setSeries
        (this.editSeries == null ? new WrapSeries() : this.editSeries);
      if(this.editSeries != null) {
        String[] unknown = seriesUnknownMacros(this.editSeries);
        if(unknown.length > 0) {
          this.owner.showErrorMessage(
            "This series contains references to the undefined macro(s)\n" +
            StringMethods.join("\n", unknown)
          );
        }
        if(seriesUnknownReporter(this.editSeries)) {
          this.owner.showErrorMessage(
            "This series uses the unknown reporter " +
            this.editSeries.getReporter()
          );
        }
        if(seriesUnknownResource(this.editSeries)) {
          this.owner.showErrorMessage(
            "This series uses the unknown resource " +
            this.editSeries.getResource()
          );
        }
      }
      this.seriesDialog.setVisible(true);
    }
  }

  /**
   * An event method: copies either an empty suite or the currently-selected
   * one into the suite dialog.
   *
   * @param add copy a new suite
   */
  protected void addOrEditSuite(boolean add) {
    this.editSuite =
      add ? null : (WrapSuite)this.suiteList.getSelectedElement();
    this.suiteDialog.setSuite
      (this.editSuite == null ? new WrapSuite("", "") : this.editSuite);
    this.suiteDialog.setVisible(true);
  }

  /**
   * An event method: clones the currently-selected list element.
   */
  public void cloneListElement() {
    java.awt.Component focused =
      KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    while(focused != null) {
      if(focused == this.seriesList) {
        cloneSeries();
      } else if(focused == this.suiteList) {
        cloneSuite();
      }
      focused = focused.getParent();
    }
  }

  /**
   * An event method: clones the currently-selected series.
   */
  protected void cloneSeries() {
    WrapSeries series = (WrapSeries)this.seriesList.getSelectedElement();
    WrapSuite suite = (WrapSuite)this.suiteList.getSelectedElement();
    if(series == null || suite == null) {
      return;
    }
    String copiedName = series.toString();
    String uniqueName = copiedName;
    for(int i = 1; this.seriesList.findMatchingElement(uniqueName) >= 0; i++) {
      uniqueName = copiedName + i;
    }
    this.editSeries = suite.addNewSeries();
    this.editSeries.copy(series);
    this.editSeries.setNickname(uniqueName);
    this.seriesList.addElement(this.editSeries);
    this.seriesDialog.setSeries(this.editSeries);
    updateSeries();
  }

  /**
   * An event method: clones the currently-selected suite.
   */
  protected void cloneSuite() {
    WrapSuite suite = (WrapSuite)this.suiteList.getSelectedElement();
    if(suite == null) {
      return;
    }
    String copiedName = suite.toString();
    String uniqueName = copiedName;
    for(int i = 1; this.suiteList.findMatchingElement(uniqueName) >= 0; i++) {
      uniqueName = copiedName + i;
    }
    this.editSuite = new WrapSuite("", "");
    this.editSuite.copy(suite);
    this.editSuite.setName(uniqueName);
    this.suiteList.addElement(this.editSuite);
    this.suiteDialog.setSuite(this.editSuite);
    updateSuite();
  }

  /**
   * An event method: deletes the currently-selected list element.
   */
  public void deleteListElement() {
    java.awt.Component focused =
      KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    while(focused != null) {
      if(focused == this.seriesList) {
        deleteSeries();
      } else if(focused == this.suiteList) {
        deleteSuite();
      }
      focused = focused.getParent();
    }
  }

  /**
   * An event method: deletes the currently-selected list series.
   */
  protected void deleteSeries() {
    int index = this.seriesList.getSelectedIndex();
    WrapSuite suite = (WrapSuite)this.suiteList.getSelectedElement();
    if(index < 0 || suite == null) {
      return;
    }
    WrapSeries series = (WrapSeries)this.seriesList.getElementAt(index);
    this.seriesList.removeElementAt(index);
    suite.removeSeries(series);
  }

  /**
   * An event method: deletes the currently-selected list suite.
   */
  protected void deleteSuite() {
    int index = this.suiteList.getSelectedIndex();
    if(index < 0) {
      return;
    }
    this.suiteList.removeElementAt(index);
  }

  /**
   * Returns an array of the suites shown in the suites tab.
   *
   * @return an array of all suites
   */
  public WrapSuite[] getSuites() {
    return (WrapSuite[])
      this.suiteList.toArray(new WrapSuite[this.suiteList.getLength()]);
  }

  /**
   * An event method: moves the currently-selected series to a different suite.
   */
  protected void moveSeries() {
    WrapSeries series = (WrapSeries)seriesList.getSelectedElement();
    if(series == null) {
      return;
    }
    Object[] choices = new Object[this.suiteList.getLength()];
    for(int i = 0; i < choices.length; i++) {
      choices[i] = this.suiteList.getElementAt(i).toString();
    }
    String target = (String)JOptionPane.showInputDialog(null,
              "Move to Suite", "Move Target",
              JOptionPane.INFORMATION_MESSAGE, null,
              choices, this.suiteList.getSelectedElement().toString());
    if(target != null) {
      WrapSuite targetSuite = (WrapSuite)
        this.suiteList.getElementAt(this.suiteList.findMatchingElement(target));
      this.editSeries = targetSuite.addNewSeries();
      this.editSeries.copy(series);
      this.seriesDialog.setSeries(this.editSeries);
      updateSeries();
      deleteSeries();
    }
  }

  /**
   * Rewrites the contents of the Series list to reflect the current selection
   * in the suite list.
   */
  protected void refreshSeriesPanel() {
    this.seriesList.removeAllElements();
    WrapSuite suite = (WrapSuite)this.suiteList.getSelectedElement();
    if(suite == null) {
      return;
    }
    WrapSeries[] series = suite.getAllSeries();
    WrapSeriesRenderer wsr = new WrapSeriesRenderer();
    wsr.startCaching();
    this.seriesList.setCellRenderer(wsr);
    for(int i = 0; i < series.length; i++) {
      WrapSeries s = series[i];
      this.seriesList.addElement(s);
    }
    this.seriesList.sort();
    this.seriesList.setSelectedIndex(0);
    wsr.stopCaching();
  }

  /**
   * An event method: asks the agent to run the currently-selected series
   * immediately.
   */
  public void runSeries() {
    WrapSeries series = (WrapSeries)this.seriesList.getSelectedElement();
    if(series == null) {
      return;
    }
    ArrayList resources = this.owner.getDescendants
      (this.owner.getResource(series.getResource()), true, false);
    Object[] choices = new Object[resources.size()];
    for(int i = 0; i < choices.length; i++) {
      choices[i] = resources.get(i).toString();
    }
    Arrays.sort(choices);
    String target = (String)JOptionPane.showInputDialog(
      null, "Run on Resource", "Run Target", JOptionPane.INFORMATION_MESSAGE,
      null, choices, series.getResource()
    );
    if(target == null) {
      return;
    }
    this.owner.runSeries(series, target);
  }

  /**
   * Indicates whether a specified series contains errors.
   *
   * @param s the series to check
   * @return true if the series contains errors, else false
   */
  public boolean seriesHasErrors(WrapSeries s) {
    return seriesUnknownMacros(s).length > 0 ||
           seriesUnknownReporter(s) ||
           seriesUnknownResource(s);
  }

  /**
   * Returns a collection of undefined macro names referenced by a series.
   *
   * @param s the series to check
   * @return a collection of undefined macro names reference by the series
   */
  protected String[] seriesUnknownMacros(WrapSeries s) {
    WrapResource resource = this.owner.getResource(s.getResource());
    if(resource == null) {
      return new String[0];
    }
    ArrayList<WrapResource> leaves = this.owner.getDescendants(resource, true, true);
    String xml = s.toXml();
    Hashtable<String,String> unknown = new Hashtable<String,String>();
    for(WrapResource r : leaves ) {
      String[] resourceUnknown = this.owner.resourceTab.resourceUnknownMacros
        ( r, xml );
      for(String pairs : resourceUnknown) {
        String name = pairs.split(",")[0];
        String refResource = pairs.split(",")[1];
        if(unknown.get(name) == null) {
          unknown.put(name, name + ": " + refResource);
        } else {
          if ( ! unknown.get(name).matches( name + ": ([^,], )*" + refResource + "(, [^,])*$") ) {
            unknown.put(name, unknown.get(name) + ", " + refResource);
          }
        }
      }
    }
    return unknown.values().toArray( new String[unknown.size()]);
  }

  /**
   * Returns true iff a series references a unknown reporter.
   *
   * @param s the series to check
   * @return true if the series reference a unknown reporter, else false
   */
  protected boolean seriesUnknownReporter(WrapSeries s) {
    String name = s.getReporter();
    String version = s.getReporterVersion();
    WrapReporter[] allReporters = owner.getReporters();
    for(int i = 0; i < allReporters.length; i++) {
      WrapReporter r = allReporters[i];
      if(name.equals(r.getProperty("name")) &&
         (version == null || version.equals(r.getProperty("version")))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true iff a series references a unknown resource.
   *
   * @param s the series to check
   * @return true if the series reference a unknown resource, else false
   */
  protected boolean seriesUnknownResource(WrapSeries s) {
    return this.owner.getResource(s.getResource()) == null;
  }

  /**
   * Replaces the suites shown in the suites tab.
   *
   * @param suites the suites to show
   */
  public void setSuites(WrapSuite[] suites) {
    this.suiteList.removeAllElements();
    for(int i = 0; i < suites.length; i++) {
      this.suiteList.addElement(suites[i]);
    }
    this.suiteList.sort();
    this.suiteList.setSelectedIndex(0);
  }

  /**
   * An event method: copies the contents of the series edit dialog into a new
   * or existing series.
   */
  protected void updateSeries() {
    this.seriesDialog.setVisible(false);
    WrapSuite suite = (WrapSuite)this.suiteList.getSelectedElement();
    if(suite == null) {
      return;
    }
    WrapSeries updated = new WrapSeries();
    this.seriesDialog.getSeries(updated);
    String name = updated.toString();
    if((this.editSeries == null || !name.equals(this.editSeries.toString())) &&
       this.seriesList.findMatchingElement(name) >= 0) {
      this.owner.showErrorMessage("Duplicate series name " + name);
      this.seriesDialog.setVisible(true);
    } else {
      if(this.editSeries == null) {
        this.editSeries = suite.addNewSeries();
        this.seriesList.addElement(this.editSeries);
      }
      this.seriesDialog.getSeries(editSeries);
      this.seriesList.setSelectedElement(this.editSeries);
      this.seriesList.sort();
    }
  }

  /**
   * An event method: copies the contents of the suite edit dialog into a new
   * or existing suite.
   */
  protected void updateSuite() {
    this.suiteDialog.setVisible(false);
    String name = this.suiteDialog.getName();
    if((this.editSuite == null || !name.equals(this.editSuite.toString())) &&
       this.suiteList.findMatchingElement(name) >= 0) {
      this.owner.showErrorMessage("Duplicate suite name " + name);
      this.suiteDialog.setVisible(true);
    } else {
      if(this.editSuite == null) {
        this.editSuite = new WrapSuite("", "");
        this.suiteList.addElement(this.editSuite);
      }
      this.suiteDialog.getSuite(this.editSuite);
      this.suiteList.sort();
      this.suiteList.setSelectedElement(this.editSuite);
    }
  }

  /**
   * A ListCellRenderer that marks faulty series.
   */
  protected class WrapSeriesRenderer implements ListCellRenderer {

    Hashtable componentCache = null;

    public void startCaching() {
      this.componentCache = new Hashtable();
    }

    public void stopCaching() {
      this.componentCache.clear();
      this.componentCache = null;
    }

    /**
     * See ListCellRenderer
     */
    public java.awt.Component getListCellRendererComponent
      (JList list, Object value, int index, boolean isSelected,
       boolean cellHasFocus) {
      if(this.componentCache != null) {
        JLabel label = (JLabel)this.componentCache.get(value);
        if(label != null) {
          return label;
        }
      }
      JLabel result = IncatList.listElementLabel(list, value, isSelected);
      WrapSeries series = (WrapSeries)value;
      if(seriesUnknownMacros(series).length > 0 ||
         seriesUnknownReporter(series) ||
         seriesUnknownResource(series)) {
        result.setForeground(Color.RED);
      }
      if(this.componentCache != null) {
        this.componentCache.put(value, result);
      }
      return result;
    }

  }

}
