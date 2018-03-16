package edu.sdsc.inca;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * A window that implements a simple browser.
 */
public class BrainDeadBrowser extends JFrame
  implements ActionListener, HyperlinkListener {

  protected static final int SCROLLER_HEIGHT = 600;
  protected static final int SCROLLER_WIDTH = 800;

  protected LinkedList backUrls;
  protected JEditorPane editor;
  protected LinkedList forwardUrls;
  protected JTextField location;

  /**
   * Constructs a new BrainDeadBrowser.
   */
  public BrainDeadBrowser() {

    super();

    this.backUrls = new LinkedList();
    this.editor = new JEditorPane();
    this.editor.addHyperlinkListener(this);
    this.editor.setEditable(false);
    this.forwardUrls = new LinkedList();
    this.location = new JTextField();
    this.location.setActionCommand("goto");
    this.location.addActionListener(this);

    JButton backButton = new JButton("Back");
    backButton.setActionCommand("back");
    backButton.addActionListener(this);
    JButton forwardButton = new JButton("Forward");
    forwardButton.setActionCommand("forward");
    forwardButton.addActionListener(this);
    JScrollPane scroller = new JScrollPane(this.editor);
    scroller.setPreferredSize(new Dimension(SCROLLER_WIDTH, SCROLLER_HEIGHT));

    Box buttonBox = Box.createHorizontalBox();
    buttonBox.add(Box.createHorizontalGlue());
    buttonBox.add(backButton);
    buttonBox.add(forwardButton);
    buttonBox.add(Box.createHorizontalGlue());
    Box contentBox = Box.createVerticalBox();
    contentBox.add(buttonBox);
    contentBox.add(this.location);
    contentBox.add(scroller);
    this.setContentPane(contentBox);

    this.setTitle("Brain Dead Browser");
    this.pack();

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
      JOptionPane.showMessageDialog
        (null, "Cannot open " + page, "Brain Dead Message",
         JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Set the current page to the specified URL.
   *
   * @param page the URL
   */
  public void setPage(String page) {
    this.backUrls.addLast(page);
    this.location.setText(page);
    this.forwardUrls.clear();
    redraw();
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

  /**
   * Invoked when the user presses a navigation button or changes the location.
   */
  public void actionPerformed(ActionEvent ae) {
    String command = ae.getActionCommand();
    if(command.equals("back") && this.backUrls.size() > 1) {
      this.forwardUrls.addFirst((String)this.backUrls.removeLast());
      this.location.setText((String)this.backUrls.getLast());
      redraw();
    } else if(command.equals("forward") && this.forwardUrls.size() > 0) {
      this.backUrls.addLast((String)forwardUrls.removeFirst());
      this.location.setText((String)this.backUrls.getLast());
      redraw();
    } else if(command.equals("goto") && !this.location.getText().equals("")) {
      String page = this.location.getText();
      if(!page.matches("^[a-zA-Z][\\w\\+\\-\\.]*:.*$")) {
        page = "http://" + page;
        this.location.setText(page);
      }
      this.backUrls.addLast(page);
      this.forwardUrls.clear();
      redraw();
    }
  }

  /**
   * Invoked when the user presses a hyperlink.
   */
  public void hyperlinkUpdate(HyperlinkEvent he) {
    if(he.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
      setPage(he.getURL().toString());
    }
  }

  /**
   * A main method that takes a URL to display from the command arguments.
   */
  public static void main(String[] args) {
    BrainDeadBrowser bdb = new BrainDeadBrowser();
    if(args.length > 0) {
      bdb.setPage(args[0]);
    }
    bdb.setVisible(true);
  }

}
