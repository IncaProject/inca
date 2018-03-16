package edu.sdsc.inca.depot.persistent;

import edu.sdsc.inca.dataModel.util.Notifications;
import org.apache.xmlbeans.XmlObject;

/**
 * This class represents the accepted output for a reporter and matches with
 * the xml schemas idea of acceptedOutput.
 *
 * @author cmills
 */
public class AcceptedOutput extends PersistentObject {

  private String comparitor;
  private String comparison;
  private Notification notification;

  /**
   * Default constructor.
   */
  public AcceptedOutput() {
    this("", "");
  }

  /**
   * Full constructor.
   *
   * @param comparitor this name of the class that does the comparison
   * @param comparison the correct value to pass to the comparitor
   */
  public AcceptedOutput(String comparitor, String comparison) {
    this.setComparitor(comparitor);
    this.setComparison(comparison);
    this.setNotification(null);
  }

  /**
   * Copies information from an Inca schema XmlBean AcceptedOutput object so
   * that this object contains equivalent information.
   *
   * @param o the XmlBean AcceptedOutput object to copy
   * @return this, for convenience
   */
  public PersistentObject fromBean(XmlObject o) {
    return this.fromBean((edu.sdsc.inca.dataModel.util.AcceptedOutput)o);
  }

  /**
   * Copies information from an Inca schema XmlBean AcceptedOutput object so
   * that this object contains equivalent information.
   *
   * @param ao the XmlBean AcceptedOutput object to copy
   * @return this, for convenience
   */
  public AcceptedOutput fromBean
    (edu.sdsc.inca.dataModel.util.AcceptedOutput ao) {
    this.setComparitor(ao.getComparitor());
    this.setComparison(ao.getComparison());
    if(ao.getNotifications() != null &&
       ao.getNotifications().sizeOfNotificationArray() > 0) {
      this.setNotification(
        new Notification().fromBean
          (ao.getNotifications().getNotificationArray(0))
      );
    }
    return this;
  }

  /**
   * Get the comparison portion of the AcceptedOutput.
   * @return the AcceptedObject comparison string
   */
  public String getComparison() {
    return this.comparison;
  }

  /**
   * Set the comparison of the AcceptedOutput.
   * @param comparison the AcceptedOutput comparison string
   */
  public void setComparison(String comparison) {
    if(comparison == null || comparison.equals("")) {
      comparison = DB_EMPTY_STRING;
    }
    this.comparison =
      truncate(comparison, MAX_DB_LONG_STRING_LENGTH, "comparison");
  }

  /**
   * Get the comparitor class name of the AcceptedOutput.
   * @return the AcceptedObject comparitor class name
   */
  public String getComparitor() {
    return this.comparitor;
  }

  /**
   * Set the comparitor class name of the AcceptedOutput.
   * @param comparitor the AcceptedObject comparitor class name
   */
  public void setComparitor(String comparitor) {
    if(comparitor == null || comparitor.equals("")) {
      comparitor = DB_EMPTY_STRING;
    }
    this.comparitor = truncate(comparitor, MAX_DB_STRING_LENGTH, "comparitor");
  }

  /**
   * Get the notification information of the AcceptedOutput.
   * @return the AcceptedObject notification information
   */
  public Notification getNotification() {
    return notification;
  }

  /**
   * Set the notification information of the AcceptedOutput.
   * @param notification the AcceptedObject notification information
   */
  public void setNotification(Notification notification) {
    this.notification = notification;
  }

  /**
   * Returns a Inca schema XmlBean AcceptedOutput object that contains
   * information equivalent to this object.
   *
   * @return an XmlBean AcceptedOutput object that contains equivalent
   * information
   */
  public XmlObject toBean() {
    edu.sdsc.inca.dataModel.util.AcceptedOutput result =
      edu.sdsc.inca.dataModel.util.AcceptedOutput.Factory.newInstance();
    result.setComparitor(this.getComparitor());
    result.setComparison(this.getComparison());
    if(this.getNotification() != null) {
      result.addNewNotifications().addNewNotification();
      result.getNotifications().setNotificationArray
        (0, (Notifications.Notification)this.getNotification().toBean());
    }
    return result;
  }

  public String toString() {
    return this.getComparitor() + "," + this.getComparison();
  }

  /**
   * Override of the default equals method.
   */
  public boolean equals(Object o) {
    return this.toString().equals(o.toString());
  }

}
