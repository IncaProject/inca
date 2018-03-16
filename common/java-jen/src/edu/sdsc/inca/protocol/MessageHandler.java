package edu.sdsc.inca.protocol;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;

import org.apache.log4j.Logger;


/**
 * The MessageHandler class should help guide the creation of all of the
 * message handlers for each Inca server.
 */
public abstract class MessageHandler {

  /**
   * logger that can be used by all MessageHandlers.
   */
  protected static final Logger logger = Logger.getLogger(MessageHandler.class);
  private static Properties permissions = null;
  private static String permissionsPath = null;
  private static final String SEP = "\n";

  /**
   * Sets the path where DN permissions should be stored.
   *
   * @param path path to the permissions file
   */
  public static void setPermissionsPath(String path) {
    permissionsPath = path;
  }

  /**
   * Indicate that a specified DN may no longer perform a specified action.
   *
   * @param dn the entity DN
   * @param action the action now forbidden to the DN
   */
  public static void forbid(String dn, String action) {
    String oldValue;
    if(permissions == null || dn == null ||
       (oldValue = permissions.getProperty(action)) == null) {
      return;
    }
    String newValue = oldValue.replaceFirst(SEP + "?" + dn, "");
    if(newValue.equals(oldValue)) {
      return;
    } else if(newValue.equals("")) {
      permissions.remove(action);
    } else {
      permissions.setProperty(action, newValue);
    }
    storePermissions();
  }

  /**
   * Returns true iff a specified DN may perform a specified action.  All DNs
   * are allowed to perform all actions by default; specific permission is
   * required only for actions where some entity has been granted specific
   * permission.
   *
   * @param dn the entity DN
   * @param action the action to test
   * @return true iff the DN may perform the action
   */
  public static boolean isPermitted(String dn, String action) {
    if(dn == null) { // Non-ssl connection
      logger.debug("Client anonymous allowed to '" + action + "'; non-ssl");
      return true;
    } else if(permissions == null) { // All are permitted to do everything
      logger.debug
        ("Client '" + dn + "' allowed to '" + action + "'; no permissions");
      return true;
    }
    String permit = permissions.getProperty(action);
    boolean permitted = true;
    if(permit == null) {
      logger.debug
        ("Client '" + dn + "' allowed to '" + action + "'; global permit");
    } else if(permit.equals(dn) || permit.startsWith(dn + SEP) ||
              permit.endsWith(SEP + dn)) {
      logger.debug
        ("Client '" + dn + "' allowed to '" + action + "'; specific permit");
    } else {
      permitted = false;
      logger.debug("Client '" + dn + "' forbidden to '" + action + "'");
    }
    return permitted;
  }

  /**
   * Indicate that a specified DN may perform a specified action.
   *
   * @param dn the entity DN
   * @param action the action to test
   */
  public static void permit(String dn, String action) {
    logger.debug("Add permission of '" + dn + "' to '" + action + "'");
    if(dn == null) {
      return;
    }
    if(permissions == null) {
      // First permission to be set
      permissions = new Properties();
      if(permissionsPath != null) {
        try {
          permissions.load(new FileInputStream(permissionsPath));
        } catch(IOException e) {
          // empty
        }
      }
    }
    String permit = permissions.getProperty(action);
    if(permit != null &&
       (permit.equals(dn) ||
        permit.startsWith(dn + SEP) ||
        permit.endsWith(SEP + dn))) {
      return; // duplicate permission
    }
    permissions.setProperty(action, permit != null ? permit + SEP + dn : dn);
    storePermissions();
  }

  /**
   * Returns the set of DNs permitted to perform a specified action, or null if
   * no permissions have been registered for that action.
   *
   * @param action the action to test
   * @return an array of DNs permitted to perform the action, or null if no
   *         DNs have been registered for the action
   */
  public static String[] permittees(String action) {
    if(permissions == null) {
      return null;
    }
    String permit = permissions.getProperty(action);
    return permit == null ? null : permit.split(SEP);
  }

  /**
   * Removes all permissions--useful for testing.
   */
  public static void resetPermissions() {
    permissions = new Properties();
    storePermissions();
  }

  /**
   * Store the current permission set to a file.
   */
  private static void storePermissions() {
    if(permissionsPath == null) {
      return;
    }
    try {
      if(permissions.size() == 0) {
        new File(permissionsPath).delete();
      } else {
        permissions.store(new FileOutputStream(permissionsPath), null);
      }
    } catch(IOException e) {
      logger.warn("Unable to store permissions");
    }
  }

  /**
   * Service an incoming request from a specified client DN, using specified
   * i/o streams.  Close the reader and writer to indicate that the client
   * connection should be closed.
   *
   * @param reader the reader connected to the client
   * @param output the output stream connected to the client
   * @param dn the DN of the client, null if no authentication
   */
  public abstract void execute(ProtocolReader reader,
                               OutputStream output,
                               String dn) throws Exception;

  /**
   * A convenience method that logs and writes an error message.
   *
   * @param output the output stream to use to send the error message
   * @param msg the error message
   */
  public static void errorReply(OutputStream output, String msg) {
    logger.error(msg);
    try {
      (new ProtocolWriter(output)).write(Statement.getErrorStatement(msg));
    } catch(IOException e) {
      logger.error("Unable to send error reply", e);
    }
  }

}
