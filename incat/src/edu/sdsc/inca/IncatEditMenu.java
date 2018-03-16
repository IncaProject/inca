package edu.sdsc.inca;

import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 * A class that constructs and manages a common edit menu for incat windows.
 */
public class IncatEditMenu extends JMenu {

  /**
   * Constructs a new IncatEditMenu.
   *
   * @param listener the object notified when the user selects menu elements
   */
  public IncatEditMenu(ActionListener listener) {
    super("Edit");
    JMenuItem menuItem = IncatComponents.JMenuItemFactory
      ("Add ...", "elementAdd ...", listener, 'I');
    menuItem.setEnabled(false);
    this.add(menuItem);
    menuItem = IncatComponents.JMenuItemFactory
      ("Edit ...", "elementEdit ...", listener, 'E');
    menuItem.setEnabled(false);
    this.add(menuItem);
    menuItem = IncatComponents.JMenuItemFactory
      ("Clone", "elementClone", listener, 'L');
    menuItem.setEnabled(false);
    this.add(menuItem);
    menuItem = IncatComponents.JMenuItemFactory
      ("Delete", "elementDelete", listener, 'D');
    menuItem.setEnabled(false);
    this.add(menuItem);
    this.addSeparator();
    menuItem = IncatComponents.JMenuItemFactory
      ("Find ...", "elementFind ...", listener, 'F');
    menuItem.setEnabled(false);
    this.add(menuItem);
    menuItem = IncatComponents.JMenuItemFactory
      ("Find Next", "elementFindNext", listener, 'G');
    menuItem.setEnabled(false);
    this.add(menuItem);
  }

  /**
   * Sets the enabled status of several items in the incat edit menu.
   *
   * @param add the enable status of the edit menu's add item
   * @param klone the enable status of the edit menu's clone item
   * @param edit the enable status of the edit menu's edit item
   * @param delete the enable status of the edit menu's delete item
   * @param find the enable status of the edit menu's find items
   */
  public void setEditEnabled
    (boolean add, boolean klone, boolean edit, boolean delete, boolean find) {
    for(int i = 0; i < this.getItemCount(); i++) {
      JMenuItem mi = this.getItem(i);
      if(mi == null) {
        continue; // Not a JMenuItem
      }
      String text = mi.getText();
      if(text.startsWith("Add")) {
        mi.setEnabled(add);
      } else if(text.startsWith("Clone")) {
        mi.setEnabled(klone);
      } else if(text.startsWith("Edit")) {
        mi.setEnabled(edit);
      } else if(text.startsWith("Delete")) {
        mi.setEnabled(delete);
      } else if(text.startsWith("Find")) {
        mi.setEnabled(find);
      }
    }
  }

}
