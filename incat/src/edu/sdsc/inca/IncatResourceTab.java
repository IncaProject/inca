package edu.sdsc.inca;

import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.StringMethods;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;

/**
 * This class has a unique instance that implements the second (resource) tab
 * in the incat display.
 */
public class IncatResourceTab extends JSplitPane implements ActionListener {

  protected String editMacroName;
  protected WrapResource editResource;
  protected IncatMacroDialog macroDialog;
  protected IncatList macroList;
  protected IncatList memberList;
  protected incat owner;
  protected IncatResourceDialog resourceDialog;
  protected IncatList resourceList;

  /**
   * Constructs an IncatResourceTab.
   *
   * @param owner the incat instance that incorporates this tab.
   */
  public IncatResourceTab(incat owner) {
    super();
    this.macroDialog =
      new IncatMacroDialog(this, "macroEditOk", "macroEditCancel");
    this.macroList =
      new IncatList("Macros", "macro", "Add ...,Edit ...,Delete,Clone", this);
    this.memberList = new IncatList("Members", "member", null, this);
    this.resourceDialog =
      new IncatResourceDialog(this, "resourceEditOk", "resourceEditCancel");
    this.resourceList = new IncatList
      ("Resources", "resource", "Add ...,Edit ...,Delete,Clone", this);
    IncatComponents.alignBoxHeights
      (new Box[] {this.macroList, this.memberList, this.resourceList});
    // Use JSplitPane so that the user can reallocate window space.  JSplitPane
    // allows only two components, so we nest a JSplitPane for the rightmost
    // two, using a default allocation of 33%/67% for the outer and 50%/50% for
    // the inner.  Suppress borders to avoid multiple bevel layers.
    JSplitPane right = new JSplitPane();
    this.setLeftComponent(this.resourceList);
    this.setRightComponent(right);
    this.setResizeWeight(0.33);
    this.setBorder(null);
    right.setLeftComponent(this.macroList);
    right.setRightComponent(this.memberList);
    right.setResizeWeight(0.5);
    right.setBorder(null);
    this.macroList.setCellRenderer(new MacroRenderer());
    this.memberList.setCellRenderer(new HostRenderer());
    this.owner = owner;
  }

  /**
   * Responds to user GUI actions in this component.
   */
  public void actionPerformed(ActionEvent ae) {
    String action = ae.getActionCommand();
    if(action.equals("macroAdd ...")) {
      addOrEditMacro(true);
    } else if(action.equals("macroClone")) {
      cloneMacro();
    } else if(action.equals("macroDelete")) {
      deleteMacro();
    } else if(action.equals("macroEdit ...") ||
              action.equals("macroDoubleClick")) {
      addOrEditMacro(false);
    } else if(action.equals("macroEditCancel")) {
      this.macroDialog.setVisible(false);
    } else if(action.equals("macroEditOk")) {
      updateMacro();
    } else if(action.equals("resourceAdd ...")) {
      addOrEditResource(true);
    } else if(action.equals("resourceClone")) {
      cloneResource();
    } else if(action.equals("resourceDelete")) {
      deleteResource();
    } else if(action.equals("resourceEdit ...") ||
              action.equals("resourceDoubleClick")) {
      addOrEditResource(false);
    } else if(action.equals("resourceEditCancel")) {
      this.resourceDialog.setVisible(false);
    } else if(action.equals("resourceEditOk")) {
      updateResource();
    } else if(action.equals("resourceSelect") ||
              action.equals("resourceSingleClick")) {
      refreshMacroPanel();
      refreshMemberPanel();
    } else if(action.indexOf("Focus") >= 0) {
      boolean gained = action.indexOf("Gained") >= 0;
      boolean editable = gained && !action.startsWith("member");
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
      if(focused == this.macroList) {
        addOrEditMacro(add);
      } else if(focused == this.resourceList) {
        addOrEditResource(add);
      }
      focused = focused.getParent();
    }
  }

  /**
   * An event method: copies either an empty macro or or the currently-selected
   * one into the macro dialog.
   *
   * @param add copy a new macro
   */
  protected void addOrEditMacro(boolean add) {
    WrapResource r = (WrapResource)this.resourceList.getSelectedElement();
    if(r == null) {
      return;
    }
    if(add || this.macroList.getSelectedIndex() < 0) {
      this.editMacroName = null;
      this.macroDialog.setValues(null);
      this.macroDialog.setName("");
    } else {
      this.editMacroName =
        ((String)this.macroList.getSelectedElement()).replaceFirst("=.*", "");
      this.macroDialog.setName(this.editMacroName);
      this.macroDialog.setValues(r.getMacroValues(this.editMacroName));
    }
    this.macroDialog.setVisible(true);
  }

  /**
   * An event method: copies either an empty resource or the currently-selected
   * one into the resource dialog.
   *
   * @param add copy a new resource
   */
  protected void addOrEditResource(boolean add) {
    this.editResource =
      add ? null : (WrapResource)this.resourceList.getSelectedElement();
    this.resourceDialog.setResource
      (this.editResource == null ? new WrapResource() : this.editResource);
    this.resourceDialog.setVisible(true);
  }

  /**
   * An event method: clones the currently-selected list element.
   */
  public void cloneListElement() {
    java.awt.Component focused =
      KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    while(focused != null) {
      if(focused == this.macroList) {
        cloneMacro();
      } else if(focused == this.resourceList) {
        cloneResource();
      }
      focused = focused.getParent();
    }
  }

  /**
   * An event method: clones the currently-selected macro.
   */
  protected void cloneMacro() {
    WrapResource r = (WrapResource)this.resourceList.getSelectedElement();
    String m = (String)this.macroList.getSelectedElement();
    if(r == null || m == null) {
      return;
    }
    String copiedName = m.substring(0, m.indexOf('='));
    String uniqueName = copiedName;
    for(int i = 1; r.getMacroValues(uniqueName) != null; i++) {
      uniqueName = copiedName + i;
    }
    this.macroDialog.setValues(r.getMacroValues(copiedName));
    this.macroDialog.setName(uniqueName);
    this.editMacroName = null;
    updateMacro();
  }

  /**
   * An event method: clones the currently-selected resource.
   */
  protected void cloneResource() {
    WrapResource r = (WrapResource)this.resourceList.getSelectedElement();
    if(r == null) {
      return;
    }
    String copiedName = r.toString();
    String uniqueName = copiedName;
    for(int i=1; this.resourceList.findMatchingElement(uniqueName) >= 0; i++) {
      uniqueName = copiedName + i;
    }
    this.editResource = new WrapResource();
    this.editResource.copy(r);
    this.editResource.setName(uniqueName);
    this.resourceList.addElement(this.editResource);
    this.resourceDialog.setResource(this.editResource);
    updateResource();
  }

  /**
   * An event method: deletes the currently-selected list element.
   */
  protected void deleteListElement() {
    java.awt.Component focused =
      KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    while(focused != null) {
      if(focused == this.macroList) {
        deleteMacro();
      } else if(focused == this.resourceList) {
        deleteResource();
      }
      focused = focused.getParent();
    }
  }

  /**
   * An event method: deletes the currently-selected macro.
   */
  public void deleteMacro() {
    int index = this.macroList.getSelectedIndex();
    WrapResource r = (WrapResource)this.resourceList.getSelectedElement();
    if(index < 0 || r == null) {
      return;
    }
    String macro = (String)this.macroList.getElementAt(index);
    r.removeMacro(macro.substring(0, macro.indexOf('=')));
    refreshMacroPanel();
    ArrayList descendants = getDescendants(r, false, false);
    for(int i = 0; i < descendants.size(); i++) {
      recomputeInheritedMacros((WrapResource)descendants.get(i));
    }
  }

  /**
   * An event method: deletes the currently-selected resource.
   */
  public void deleteResource() {
    int index = this.resourceList.getSelectedIndex();
    if(index < 0) {
      return;
    }
    WrapResource r = (WrapResource)this.resourceList.getElementAt(index);
    ArrayList oldDescendants = getDescendants(r, false, false);
    this.resourceList.removeElementAt(index);
    refreshMacroPanel();
    refreshMemberPanel();
    for(int i = 0; i < oldDescendants.size(); i++) {
      recomputeInheritedMacros((WrapResource)oldDescendants.get(i));
    }
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
    ArrayList<WrapResource> toProcess = new ArrayList<WrapResource>();
    Hashtable descendants = new Hashtable();
    toProcess.add(resource);
    while(toProcess.size() > 0) {
      WrapResource r = toProcess.remove(0);
      boolean rIsLeaf = false;
      String[] expanded = r.expandHostPattern();
      for(int i = 0; i < expanded.length; i++) {
        int index = this.resourceList.findMatchingElement(expanded[i]);
        if(index < 0) {
          rIsLeaf = true; // host
        } else {
          toProcess.add((WrapResource)this.resourceList.getElementAt(index));
        }
      }
      if((includeSelf || r != resource) && (rIsLeaf || !leavesOnly)) {
        descendants.put(r, "");
      }
    }
    ArrayList result = new ArrayList();
    for(Enumeration e = descendants.keys(); e.hasMoreElements(); ) {
      result.add(e.nextElement());
    }
    return result;
  }

  /**
   * Expands a resource into a set of host names.  In addition to the usual
   * pattern characters, the resource member pattern may contain names of
   * resource groups from the resource list.  These will be expanded into the
   * hosts that match the patterns for those groups.
   */
  protected String[] getHosts(WrapResource resource) {
    ArrayList descendants = getDescendants(resource, true, true);
    Hashtable hosts = new Hashtable();
    for(int i = 0; i < descendants.size(); i++) {
      WrapResource r = (WrapResource)descendants.get(i);
      String[] expanded = r.expandHostPattern();
      for(int j = 0; j < expanded.length; j++) {
        String single = expanded[j];
        if(this.resourceList.findMatchingElement(single) < 0) {
          hosts.put(single, "");
        }
      }
    }
    return (String [])hosts.keySet().toArray(new String[hosts.size()]);
  }

  /**
   * Returns a macro name and value list, formatted for inclusion in a List.
   *
   * @param name the macro name
   * @param values the macro values
   * @return the name and values, properly formatted
   */
  protected String getMacroListElement(String name, String[] values) {
    StringBuffer result = new StringBuffer(name + "=");
    for(int i = 0; i < values.length; i++) {
      String value = values[i];
      if(i > 0) {
        result.append(" ");
      }
      String quote = !value.equals("") && value.indexOf(" ") < 0 ? "" :
                     value.indexOf('"') < 0 ? "\"" : "'";
      result.append(quote).append(value).append(quote);
    }
    return result.toString();
  }

  /**
   * Returns an ArrayList of all resources with a member pattern that includes
   * the resource.
   *
   * @param resource the resource to search for
   * @return all parents of the specified resource
   */
  protected ArrayList getParents(WrapResource resource) {
    String name = resource.getName();
    ArrayList result = new ArrayList();
    for(int i = 0; i < this.resourceList.getLength(); i++) {
      WrapResource r = (WrapResource)this.resourceList.getElementAt(i);
      String pat = r.getMacroValue(Protocol.PATTERN_MACRO);
      if(pat != null && name.matches(pat.replaceAll("[ ,]", "|"))) {
        result.add(r);
      }
    }
    return result;
  }

  /**
   * Returns the resource with a specified name, null if none.
   *
   * @param name the resource name
   * @return the resource with the specified name, null if none
   */
  public WrapResource getResource(String name) {
    int index = this.resourceList.findMatchingElement(name);
    return index<0 ? null : (WrapResource)this.resourceList.getElementAt(index);
  }

  /**
   * Returns an array of the resources shown in the resource tab.
   *
   * @param addHosts whether to include in the returned value resources for the
   *                 member hosts of the resources shown
   * @return an array of all resources
   */
  public WrapResource[] getResources(boolean addHosts) {
    if(!addHosts) {
      return (WrapResource[])this.resourceList.toArray
        (new WrapResource[this.resourceList.getLength()]);
    }
    Hashtable hostHash = new Hashtable();
    for(int i = 0; i < this.resourceList.getLength(); i++) {
      WrapResource resource = (WrapResource)this.resourceList.getElementAt(i);
      String[] members = getHosts(resource);
      for(int j = 0; j < members.length; j++) {
        WrapResource memberResource = new WrapResource();
        memberResource.setName(members[j]);
        hostHash.put(members[j], memberResource);
      }
    }
    String[] hostNames = new String[hostHash.size()];
    Enumeration e = hostHash.keys();
    for(int i = 0; i < hostNames.length; i++) {
      hostNames[i] = (String)e.nextElement();
    }
    Arrays.sort(hostNames);
    WrapResource[] result =
      new WrapResource[this.resourceList.getLength() + hostNames.length];
    int j = 0;
    for(int i = 0; i < this.resourceList.getLength(); i++) {
      result[j++] = (WrapResource)this.resourceList.getElementAt(i);
    }
    for(int i = 0; i < hostNames.length; i++) {
      result[j++] = (WrapResource)hostHash.get(hostNames[i]);
    }
    return result;
  }

  /**
   * Recompute the set of macros that a resource inherits from its ancestors.
   *
   * @param resource the resource to recompute
   */
  protected void recomputeInheritedMacros(WrapResource resource) {
    resource.removeAllInheritedMacros();
    // Do a breadth-first search for macro definitions so that macros defined
    // by closer ancestors take precedence over those of more remote ones.
    ArrayList generation = getParents(resource);
    while(generation.size() > 0) {
      ArrayList nextGeneration = new ArrayList();
      for(int i = 0; i < generation.size(); i++) {
        WrapResource r = (WrapResource)generation.get(i);
        nextGeneration.addAll(getParents(r));
        String[] names = r.getLocalMacroNames();
        for(int j = 0; j < names.length; j++) {
          String name = names[j];
          if(resource.getMacroValues(name) == null) {
            String[] values = r.getMacroValues(name);
            resource.setInheritedMacroValues(name, values);
          }
        }
      }
      generation = nextGeneration;
    }
  }

  /**
   * Rewrites the contents of the macro list to reflect the current selection
   * in the resource list.
   */
  protected void refreshMacroPanel() {
    this.macroList.removeAllElements();
    WrapResource resource=(WrapResource)this.resourceList.getSelectedElement();
    if(resource == null) {
      return;
    }
    // Collect the resource macros, overriding inherited values w/local ones
    Hashtable macros = new Hashtable();
    String[] names = resource.getInheritedMacroNames();
    for(int i = 0; i < names.length; i++) {
      String name = names[i];
      macros.put(name, resource.getMacroValues(name));
    }
    names = resource.getLocalMacroNames();
    for(int i = 0; i < names.length; i++) {
      String name = names[i];
      macros.put(name, resource.getMacroValues(name));
    }
    // Display the combined set
    for(Enumeration e = macros.keys(); e.hasMoreElements(); ) {
      String name = (String)e.nextElement();
      if(name.matches("^" + Protocol.PREDEFINED_MACRO_NAME_PATTERN + "$")) {
        continue;
      }
      this.macroList.addElement
        (getMacroListElement(name, (String[])macros.get(name)));
    }
    this.macroList.sort();
    this.macroList.setSelectedIndex(0);
  }

  /**
   * Rewrites the contents of the member list to reflect the current selection
   * in the resource list.
   */
  protected void refreshMemberPanel() {
    this.memberList.removeAllElements();
    WrapResource resource=(WrapResource)this.resourceList.getSelectedElement();
    if(resource == null) {
      return;
    }
    String[] hosts = getHosts(resource);
    this.memberList.setElements(hosts);
    this.memberList.sort();
    this.memberList.setSelectedIndex(0);
  }

  /**
   * Returns a collection of undefined macro names referenced by a bit of XML.
   *
   *
   * @param resource the resource that defines the macros
   *
   * @param xml the XML that may contain macro references
   *
   * @return a collection of undefined macro names referenced in the XML
   */
  public String[] resourceUnknownMacros(WrapResource resource, String xml) {
    Hashtable<String,String> unknown = new Hashtable<String,String>();
    Pattern p = Pattern.compile('@' + Protocol.MACRO_NAME_PATTERN + '@');
    int start = 0;
    for(Matcher m = p.matcher(xml); m.find(start); start = m.end()) {
      String name = xml.substring(m.start() + 1, m.end() - 1);
      WrapResource macroResource = resource;
      String macroResourceName = macroResource.getName();
      String [] values = null;
      if(name.indexOf("->") > 0) {
        macroResourceName = name.substring(0, name.indexOf("->")) ;
        name = name.substring(name.indexOf("->") + 2);
        macroResource = this.getResource(macroResourceName);
        if ( macroResource != null ) {
          ArrayList<WrapResource> resources =
            this.getDescendants(macroResource, true, true );
          if ( name.equals(Protocol.HOSTS_MACRO) ) {
            values = new String[resources.size()];
            for( int i = 0; i < resources.size(); i++ ) {
              values[i] = resources.get(i).getName();
            }
          } else {
            for( WrapResource innerResource : resources ) {
              String[] innerUnknowns =
                resourceUnknownMacros(innerResource, "@" + name + "@");
              for(String innerUnknown : innerUnknowns ) {
                unknown.put(innerUnknown + "," + macroResourceName, "");
              }
              values = new String[0];
            }
          }
        }
      } else {
        if ( name.equals(Protocol.HOSTS_MACRO) ) {
          ArrayList<WrapResource> resources =
            this.getDescendants(macroResource, true, true );
          values = new String[resources.size()];
          for( int i = 0; i < resources.size(); i++ ) {
            values[i] = resources.get(i).getName();
          }
        } else {
          values = macroResource.getMacroValues(name);
        }
      }
      if(values == null) {
        unknown.put(name + "," + macroResourceName, "");
      } else {
        String innerXml = StringMethods.join(" ", values);
        String[] innerUnknowns = resourceUnknownMacros
          (resource, innerXml);
        for(int i = 0; i < innerUnknowns.length; i++) {
          unknown.put(innerUnknowns[i] + "," + macroResourceName, "");
        }
      }
    }
    return unknown.keySet().toArray( new String[unknown.size()] );
  }

  /**
   * Replaces the resources shown in the resource tab.
   *
   * @param resources the resources to show
   */
  public void setResources(WrapResource[] resources) {
    this.resourceList.removeAllElements();
    for(int i = 0; i < resources.length; i++) {
      WrapResource resource = resources[i];
      if(resource.getXpath() != null) { // Filter host resources
        this.resourceList.addElement(resource);
      }
    }
    this.resourceList.sort();
    this.resourceList.setSelectedIndex(0);
    for(int i = 0; i < this.resourceList.getLength(); i++) {
      recomputeInheritedMacros((WrapResource)this.resourceList.getElementAt(i));
    }
    refreshMacroPanel();
    refreshMemberPanel();
  }

  /**
   * An event method: copies the contents of the macro edit dialog into a new
   * or existing macro.
   */
  protected void updateMacro() {
    this.macroDialog.setVisible(false);
    WrapResource resource=(WrapResource)this.resourceList.getSelectedElement();
    if(resource == null) {
      return;
    }
    String name = this.macroDialog.getName();
    if((this.editMacroName == null || !this.editMacroName.equals(name)) &&
       resource.getMacroValues(name) != null) {
      this.owner.showErrorMessage("Duplicate macro name " + name);
      this.macroDialog.setVisible(true);
      return;
    }
    if(this.editMacroName != null) {
      resource.removeMacro(this.editMacroName);
    }
    String[] values = this.macroDialog.getValues();
    if(values.length == 0) {
      values = new String[] {""};
    }
    resource.setMacroValues(name, values);
    if(this.editMacroName == null || !this.editMacroName.equals(name)) {
      refreshMacroPanel();
    } else {
      int index = this.macroList.getSelectedIndex();
      this.macroList.removeElementAt(index);
      this.macroList.insertElementAt(getMacroListElement(name, values), index);
    }
    ArrayList descendants = getDescendants(resource, false, false);
    for(int i = 0; i < descendants.size(); i++) {
      recomputeInheritedMacros((WrapResource)descendants.get(i));
    }
  }

  /**
   * An event method: copies the contents of the resource edit dialog into a
   * new or existing resource.
   */
  protected void updateResource() {
    this.resourceDialog.setVisible(false);
    String name = this.resourceDialog.getName();
    if((this.editResource==null || !name.equals(this.editResource.getName())) &&
       this.resourceList.findMatchingElement(name) >= 0) {
      this.owner.showErrorMessage("Duplicate resource name " + name);
      this.resourceDialog.setVisible(true);
      return;
    }
    if(this.editResource == null) {
      this.editResource = new WrapResource();
      this.resourceList.addElement(this.editResource);
    }
    ArrayList oldResources = getDescendants(this.editResource, false, false);
    this.resourceDialog.getResource(this.editResource);
    ArrayList newResources = getDescendants(this.editResource, false, false);
    this.resourceList.setSelectedElement(this.editResource);
    this.resourceList.sort();
    // Recompute inherited macros for any added/removed descendants
    for(int i = 0; i < oldResources.size(); i++) {
      if(!newResources.contains(oldResources.get(i))) {
        recomputeInheritedMacros((WrapResource)oldResources.get(i));
      }
    }
    for(int i = 0; i < newResources.size(); i++) {
      if(!oldResources.contains(newResources.get(i))) {
        recomputeInheritedMacros((WrapResource)newResources.get(i));
      }
    }
    refreshMacroPanel();
    refreshMemberPanel();
  }

  /**
   * A ListCellRenderer that marks unreachable hosts.
   */
  protected class HostRenderer implements ListCellRenderer {

    /**
     * See ListCellRenderer
     */
    public Component getListCellRendererComponent
      (JList list, Object value, int index, boolean isSelected,
       boolean cellHasFocus) {
      String host = value.toString();
      JLabel result = IncatList.listElementLabel(list, value, isSelected);
      try {
        InetAddress.getByName(host);
      } catch(Exception e) {
        result.setForeground(Color.RED);
      }
      return result;
    }

  }

  /**
   * A ListCellRenderer that marks inherited macros.
   */
  protected class MacroRenderer implements ListCellRenderer {

    /**
     * See ListCellRenderer
     */
    public Component getListCellRendererComponent
      (JList list, Object value, int index, boolean isSelected,
       boolean cellHasFocus) {
      String macro = value.toString();
      JLabel result = IncatList.listElementLabel(list, value, isSelected);
      WrapResource r = (WrapResource)resourceList.getSelectedElement();
      if(!r.isLocalMacro(macro.substring(0, macro.indexOf('=')))) {
        result.setForeground(Color.GRAY);
      }
      return result;
    }

  }

}
