package edu.sdsc.inca;

import edu.sdsc.inca.repository.Repository;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A dialog class that displays information about a set of repositories.
 */
public class IncatRepositoryDialog extends JFrame {

  protected static Repository[] repositories = new Repository[0];

  protected JTextArea source;

  /**
   * Constructs a new IncaRepositoryDialog.
   */
  public IncatRepositoryDialog() {
    this.source = new JTextArea(40, 80);
    this.getContentPane().add(new JScrollPane(this.source));
    this.setTitle("Incat Repository Dialog");
    this.pack();
  }

  /**
   * Sets the list of repositories searched by repository dialogs.
   *
   * @param repositories the list of repositories
   */
  public static void setRepositories(Repository[] repositories) {
    IncatRepositoryDialog.repositories = repositories;
  }

  /**
   * Sets the reporter to be displayed in the dialog source area.
   *
   * @param reporter the name of the reporter to display
   * @throws IOException if the reporter is not in any known repository
   */
  public void setReporter(String reporter) throws IOException {
    // Since we don't have reporter URLs, try to retrieve the reporter from
    // each repository in turn until we succeed
    Repository repo = null;
    for(int i = 0; i < this.repositories.length && repo == null; i++) {
      try {
        repo = this.repositories[i];
        this.source.setText(new String(repo.getReporter(reporter)));
        this.setTitle(reporter);
      } catch(Exception e) {
        repo = null;
      }
    }
    if(repo == null) {
      throw new IOException("Unable to retrieve reporter file " + reporter);
    }
  }

}
