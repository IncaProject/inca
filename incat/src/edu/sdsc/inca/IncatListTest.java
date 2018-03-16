package edu.sdsc.inca;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import junit.framework.TestCase;

/**
 * A JUnit for the IncatList class.
 */
public class IncatListTest extends TestCase implements ActionListener {

  private static String COMMAND = "CMD";
  private static int ELEMENT_COUNT = 44;
  private static String PREFIX = "Element ";

  /**
   * Tests the IncatList constructor and the addElement method.
   */
  public void testConstructorAndAddElement() {
    IncatList list = this.createList();
    assertEquals(ELEMENT_COUNT, list.getLength());
    assertEquals(ELEMENT_COUNT, list.toArray().length);
    assertEquals(COMMAND, list.getCommandPrefix());
    assertEquals(this, list.getActionListener());
  }

  /**
   * Tests the findMatchingElement method.
   */
  public void testFindMatchingElement() {
    IncatList list = this.createList();
    int index = list.findMatchingElement("");
    assertEquals(-1, index);
    index = list.findMatchingElement(PREFIX + "0");
    assertEquals(0, index);
    index = list.findMatchingElement(PREFIX + (ELEMENT_COUNT - 1));
    assertEquals(ELEMENT_COUNT - 1, index);
    index = list.findMatchingElement(PREFIX + (ELEMENT_COUNT / 2));
    assertEquals(ELEMENT_COUNT / 2, index);
  }

  /**
   * Tests the methods involved in setting and querying the selected element.
   */
  public void testSelection() {
    IncatList list = this.createList();
    assertEquals(-1, list.getSelectedIndex());
    assertEquals(null, list.getSelectedElement());
    assertFalse(list.selectMatchingElement(""));
    assertEquals(-1, list.getSelectedIndex());
    list.setSelectedIndex(ELEMENT_COUNT);
    assertEquals(-1, list.getSelectedIndex());
    list.setSelectedElement("");
    assertEquals(-1, list.getSelectedIndex());
    assertTrue(list.selectMatchingElement(PREFIX + "0"));
    assertEquals(0, list.getSelectedIndex());
    list.setSelectedIndex(ELEMENT_COUNT);
    assertEquals(0, list.getSelectedIndex());
    assertTrue(list.selectMatchingElement(PREFIX + (ELEMENT_COUNT - 1)));
    assertEquals(ELEMENT_COUNT - 1, list.getSelectedIndex());
    assertTrue(list.selectMatchingElement(PREFIX + (ELEMENT_COUNT / 2)));
    assertEquals(ELEMENT_COUNT / 2, list.getSelectedIndex());
    list.setSelectedIndex(ELEMENT_COUNT);
    assertEquals(ELEMENT_COUNT / 2, list.getSelectedIndex());
  }

  /**
   * Tests operations on an empty list.
   */
  public void testEmpty() {
    IncatList list = new IncatList(null, null, null, null);
    assertEquals(0, list.getLength());
    assertEquals(0, list.toArray().length);
    assertEquals(-1, list.getSelectedIndex());
    assertEquals(null, list.getSelectedElement());
    assertFalse(list.selectMatchingElement(""));
    assertEquals(-1, list.getSelectedIndex());
    list.setSelectedIndex(ELEMENT_COUNT);
    assertEquals(-1, list.getSelectedIndex());
    list.setSelectedElement("");
    assertEquals(-1, list.getSelectedIndex());
    assertFalse(list.selectMatchingElement(PREFIX + "0"));
    assertEquals(-1, list.getSelectedIndex());
    list.setSelectedIndex(ELEMENT_COUNT);
    assertEquals(-1, list.getSelectedIndex());
    assertFalse(list.selectMatchingElement(PREFIX + (ELEMENT_COUNT - 1)));
    assertEquals(-1, list.getSelectedIndex());
    assertFalse(list.selectMatchingElement(PREFIX + (ELEMENT_COUNT / 2)));
    assertEquals(-1, list.getSelectedIndex());
    list.setSelectedIndex(ELEMENT_COUNT);
    assertEquals(-1, list.getSelectedIndex());
    list.sort();
    assertEquals(0, list.getLength());
  }

  /**
   * Tests the sort method.
   */
  public void testSort() {
    IncatList list = new IncatList(null, null, null, null);
    // Start with 10 instead of 0 to avoid alpha/numeric sorting issues
    for(int i = 10; i < ELEMENT_COUNT; i++) {
      if(i % 2 != 0) {
        list.addElement(new String(PREFIX + i));
      }
    }
    for(int i = ELEMENT_COUNT - 1; i >= 10; i--) {
      if(i % 2 == 0) {
        list.addElement(new String(PREFIX + i));
      }
    }
    list.setSelectedIndex(0);
    list.sort();
    assertEquals(1, list.getSelectedIndex());
    for(int i = 10; i < ELEMENT_COUNT; i++) {
      assertEquals(PREFIX + i, list.getElementAt(i - 10));
    }
  }

  /**
   * Placeholder to allow using this class as an ActionListener.
   */
  public void actionPerformed(ActionEvent ae) {
  }

  /**
   * Returns an IncatList containing ELEMENT_COUNT elements, each of which is
   * the string PREFIX followed by the element position.
   */
  private IncatList createList() {
    IncatList result = new IncatList(null, COMMAND, null, this);
    for(int i = 0; i < ELEMENT_COUNT; i++) {
      result.addElement(new String(PREFIX + i));
    }
    return result;
  }

}
