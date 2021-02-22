package edu.sdsc.inca.depot.persistent;

import edu.sdsc.inca.dataModel.util.Notifications;
import org.apache.xmlbeans.XmlObject;

/**
 * @author cmills
 */
public class Notification extends XmlBeanObject {

  private String notifier;
  private String target;

  /**
   * Default constructor.
   */
  public Notification() {
    this("", "");
  }

  /**
   * Full constructor.
   */
  public Notification(String notifier, String target) {
    this.setNotifier(notifier);
    this.setTarget(target);
  }

  /**
   * Copies information from an Inca schema XmlBean Notification object so that
   * this object contains equivalent information.
   *
   * @param o the XmlBean Notification object to copy
   * @return this, for convenience
   */
  @Override
  public XmlBeanObject fromBean(XmlObject o) {
    return this.fromBean((edu.sdsc.inca.dataModel.util.Notifications.Notification)o);
  }

  /**
   * Copies information from an Inca schema XmlBean Notification object so that
   * this object contains equivalent information.
   *
   * @param n the XmlBean Notification object to copy
   * @return this, for convenience
   */
  public Notification fromBean(Notifications.Notification n) {
    this.setNotifier(n.getNotifier());
    this.setTarget(n.getTarget());
    return this;
  }

  /**
   * Returns the name of the class that performs this notification.
   *
   * @return this name of the class that performs this notification.
   */
  public String getNotifier() {
    return this.notifier;
  }

  /**
   * Sets the name of the class that performs this notification.
   *
   * @param notifier this name of the class that performs this notification.
   */
  public void setNotifier(String notifier) {
    this.notifier = normalize(notifier, Row.MAX_DB_STRING_LENGTH, "notifier");
  }

  /**
   * Returns the target that is sent this notification.
   *
   * @return the target that is sent this notification.
   */
  public String getTarget() {
    return this.target;
  }

  /**
   * Set the target that is sent this notification.
   *
   * @param target the target that is sent this notification.
   */
  public void setTarget(String target) {
    this.target = normalize(target, Row.MAX_DB_STRING_LENGTH, "target");
  }

  /**
   * Returns a Inca schema XmlBean Notification object that contains
   * information equivalent to this object.
   *
   * @return an XmlBean Notification object that contains equivalent information
   */
  @Override
  public XmlObject toBean() {
    Notifications.Notification result =
      Notifications.Notification.Factory.newInstance();
    result.setNotifier(this.getNotifier());
    result.setTarget(this.getTarget());
    return result;
  }

}
