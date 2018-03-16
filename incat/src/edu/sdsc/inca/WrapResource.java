package edu.sdsc.inca;

import edu.sdsc.inca.dataModel.util.Macro;
import edu.sdsc.inca.dataModel.util.Macros;
import edu.sdsc.inca.dataModel.util.Resource;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.ExpandablePattern;
import edu.sdsc.inca.util.XmlWrapper;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.xmlbeans.XmlOptions;

/**
 * A class that wraps a resource or resource group, represented by an Inca
 * XmlBean, with some convenience methods.
 */
public class WrapResource {

  /** An enumeration of server types for get/setServerSpec. */
  public static final int COMPUTE_SERVER = 0;
  public static final int FILE_SERVER = 1;
  public static final int PROXY_SERVER = 2;

  private static final String[] DN_MACROS = {
    Protocol.GRAM_DN_MACRO, Protocol.GRIDFTP_DN_MACRO, Protocol.MYPROXY_DN_MACRO
  };
  private static final String[] HOST_MACROS = {
    Protocol.COMPUTE_SERVER_MACRO, Protocol.FILE_SERVER_MACRO,
    Protocol.MYPROXY_HOST_MACRO
  };
  private static final String[] PORT_MACROS = {
    Protocol.COMPUTE_PORT_MACRO, Protocol.FILE_PORT_MACRO,
    Protocol.MYPROXY_PORT_MACRO
  };

  protected Resource resource;
  // Non-bean set of "inherited" macros, and a convenience cache of local ones.
  protected Hashtable inheritedMacros = new Hashtable();
  protected Hashtable localMacros = new Hashtable();

  /**
   * Constructs a new WrapResource.
   */
  public WrapResource() {
    this.resource = Resource.Factory.newInstance();
    this.resource.setName("");
  }

  /**
   * Constructs a new WrapResource to wrap an existing Resource.
   *
   * @param resource the resource to wrap
   */
  public WrapResource(Resource resource) {
    this.resource = resource;
    Macros macros = resource.getMacros();
    if(macros != null) {
      Macro[] allMacros = macros.getMacroArray();
      for(int i = 0; i < allMacros.length; i++) {
        Macro macro = allMacros[i];
        localMacros.put(macro.getName(), macro.getValueArray());
      }
    }
  }

  /**
   * Copies all information from another resouce into this one.
   *
   * @param original the WrapResource to duplicate
   */
  public void copy(WrapResource original) {
    String[] macros = original.getLocalMacroNames();
    this.setName(original.getName());
    this.setServer(COMPUTE_SERVER, original.getServer(COMPUTE_SERVER));
    this.setServer(FILE_SERVER, original.getServer(FILE_SERVER));
    this.setServer(PROXY_SERVER, original.getServer(PROXY_SERVER));
    this.setXpath(original.getXpath());
    for(int i = 0; i < macros.length; i++) {
      this.setMacroValues(macros[i], original.getMacroValues(macros[i]));
    }
  }

  /**
   * Override of the default equals method.
   *
   * @param o the object to compare to this one
   * @return true iff o specifies the same resource
   */
  public boolean equals(Object o) {
    if(!(o instanceof WrapResource)) {
      return false;
    }
    // NOTE: Although XML comparison seems like it might be fragile due to
    // formatting issues, it appears to work well enough for our purposes.
    String oXml =
      ((WrapResource)o).toXml().replaceAll(" xmlns[^\"]*\"[^\"]*\"", "");
    String thisXml = this.toXml().replaceAll(" xmlns[^\"]*\"[^\"]*\"", "");
    return oXml.equals(thisXml);
  }

  /**
   * Returns the set of strings matched by the resource's host pattern.
   *
   * @return the set of string matched by the resource's host pattern
   */
  public String[] expandHostPattern() {
    String pat = this.getMacroValue(Protocol.PATTERN_MACRO);
    String[] result = new String[0];
    try {
      pat = pat.replaceAll("[ ,]", "|");
      result =
        (String[])(new ExpandablePattern(pat, true).expand().toArray(result));
    } catch(Exception e) {
      // empty
    }
    return result;
  }

  /**
   * Returns the names of all inherited macros defined for the resource.
   *
   * @return the set of macro names
   */
  public String[] getInheritedMacroNames() {
    String[] result = new String[this.inheritedMacros.size()];
    Enumeration e = this.inheritedMacros.keys();
    for(int i = 0; i < result.length; i++) {
      result[i] = (String)e.nextElement();
    }
    return result;
  }

  /**
   * Returns the names of all local macros defined for the resource.
   *
   * @return the set of macro names
   */
  public String[] getLocalMacroNames() {
    String[] result = new String[this.localMacros.size()];
    Enumeration e = this.localMacros.keys();
    for(int i = 0; i < result.length; i++) {
      result[i] = (String)e.nextElement();
    }
    return result;
  }

  /**
   * Returns the first value of an existing resource macro with a given name,
   * or null if none.
   *
   * @param name the specified macro name
   * @return the first value of the macro with the given name, null if none
   */
  public String getMacroValue(String name) {
    String[] values = this.getMacroValues(name);
    return values == null ? null : values[0];
  }

  /**
   * Returns all values of an existing resource macro with a given name, or
   * null if none.
   *
   * @param name the specified macro name
   * @return the values of the macro with the given name, null if none
   */
  public String[] getMacroValues(String name) {
    String[] result = (String [])this.localMacros.get(name);
    return result == null ? (String [])this.inheritedMacros.get(name) : result;
  }

  /**
   * Returns the name of the resource.
   *
   * @return the resource name
   */
  public String getName() {
    return this.resource.getName();
  }

  /**
   * Returns the underlying Resource of this resource.
   *
   * @return the Resource
   */
  public Resource getResource() {
    return this.resource;
  }

  /**
   * Retrieves the macros related to one of the resource servers and composes
   * their values into a String of the form host[:port][/dn].
   *
   * @param which one of the server types indicated by the constants above
   * @return a string that contains the host, optional port and option dn
   */
  public String getServer(int which) {
    String dn = this.getMacroValue(DN_MACROS[which]);
    String port = this.getMacroValue(PORT_MACROS[which]);
    String result = this.getMacroValue(HOST_MACROS[which]);
    String service = this.getMacroValue(Protocol.GRAM_SERVICE_MACRO);
    if(result == null) {
      result = "";
    }
    if(port != null) {
      result += ":" + port;
    }
    if(which == COMPUTE_SERVER && service != null) {
      result += "/" + service;
    }
    if(dn != null) {
      result += ":" + dn;
    }
    return result;
  }

  /**
   * Returns the xpath of the resource.
   *
   * @return the resource xpath
   */
  public String getXpath() {
    return this.resource.getXpath();
  }

  /**
   * Indicates whether a macro is defined locally to this resource.
   *
   * @param name the macro name
   * @return whether the macro is defined locally
   */
  public boolean isLocalMacro(String name) {
    return this.localMacros.get(name) != null;
  }

  /**
   * Deletes any inherited macros defined in the resource.
   */
  public void removeAllInheritedMacros() {
    this.inheritedMacros = new Hashtable();
  }

  /**
   * Removes any macro with a specified name from the resource.
   *
   * @param name the specified macro name
   */
  public void removeMacro(String name) {
    if(this.localMacros.remove(name) != null) {
      Macro[] allMacros = this.resource.getMacros().getMacroArray();
      for(int i = 0; i < allMacros.length; i++) {
        if(name.equals(allMacros[i].getName())) {
          this.resource.getMacros().removeMacro(i);
          return;
        }
      }
    }
  }

  /**
   * Adds an inherited macro with a single value to the resource, replacing any
   * other with the same name.
   *
   * @param name the name of the macro
   * @param value the value of the macro
   */
  public void setInheritedMacroValue(String name,
                                     String value) {
    this.setInheritedMacroValues(name, new String[] {value});
  }

  /**
   * Adds an inherited macro with multiple values to the resource, replacing
   * any other with the same name.
   *
   * @param name the name of the macro
   * @param values the values of the macro
   */
  public void setInheritedMacroValues(String name,
                                      String[] values) {
    this.inheritedMacros.put(name, values);
  }

  /**
   * Adds a macro with a single value to the resource, replacing any other with
   * the same name.
   *
   * @param name the name of the macro
   * @param value the value of the macro
   */
  public void setMacroValue(String name,
                            String value) {
    this.setMacroValues(name, new String[] {value});
  }

  /**
   * A convenience that removes a macro from the resource, then sets it to a
   * new single value unless that value is equal to a default value.  Also
   * ignores values that are equivalent to an inherited macro of the same name.
   *
   * @param name the name of the macro
   * @param value the value of the macro
   * @param defaultValue the default value for the macro if it isn't set
   */
  public void setMacroValue(String name, String value, String defaultValue) {
    this.removeMacro(name);
    String[] inherited = (String [])this.inheritedMacros.get(name);
    if(!value.equals(defaultValue) &&
       (inherited==null || inherited.length>1 || !value.equals(inherited[0]))) {
      this.setMacroValue(name, value);
    }
  }
 

  /**
   * Adds a macro with multiple values to the resource, replacing any other
   * with the same name.
   *
   * @param name the name of the macro
   * @param values the values of the macro
   */
  public void setMacroValues(String name,
                             String[] values) {
    this.removeMacro(name);
    Macros macros = this.resource.getMacros();
    if(macros == null) {
      macros = this.resource.addNewMacros();
    }
    Macro macro = macros.addNewMacro();
    macro.setName(name);
    for(int i = 0; i < values.length; i++) {
      macro.addValue(values[i]);
    }
    this.localMacros.put(name, values);
  }

  /**
   * Sets the name of the resource to a specified value.
   *
   * @param name the resource name
   */
  public void setName(String name) {
    this.resource.setName(name);
  }

  /**
   * Splits a server string of the form host[:port][/dn] and stores its
   * component parts into appropriate macros.
   *
   * @param which one of the server types indicated by the constants above
   * @param server the server specification string
   */
  public void setServer(int which, String server) {
    // Note: format of compute server is host[:port][/service][:dn]; format
    // of others is host[:port][:dn].
    if(server == null) {
      server = "";
    }
    String[] pieces = server.split(":", 3);
    String dn = "";
    String port = "";
    String host = pieces[0];
    if(pieces.length == 1) {
      // empty
    } else if(pieces.length == 3) {
      port = pieces[1];
      dn = pieces[2];
    } else if(pieces[1].matches("^\\d+.*$")) {
      port = pieces[1];
    } else {
      dn = pieces[1];
    }
    if(which == COMPUTE_SERVER) {
      if(port.indexOf("/") >= 0) {
        pieces = port.split("/");
        port = pieces[0];
        this.setMacroValue(Protocol.GRAM_SERVICE_MACRO, pieces[1], "");
      } else if(port.equals("") && host.indexOf("/") >= 0) {
        pieces = host.split("/");
        host = pieces[0];
        this.setMacroValue(Protocol.GRAM_SERVICE_MACRO, pieces[1], "");
      } else {
        this.removeMacro(Protocol.GRAM_SERVICE_MACRO);
      }
    }
    this.setMacroValue(HOST_MACROS[which], host, "");
    this.setMacroValue(PORT_MACROS[which], port, "");
    this.setMacroValue(DN_MACROS[which], dn, "");
  }

  /**
   * Sets the xpath of the resource to a specified value.
   *
   * @param xpath the resource xpath
   */
  public void setXpath(String xpath) {
    this.resource.setXpath(xpath);
  }

  /**
   * An override of the default toString function.
   */
  public String toString() {
    return this.resource.getName();
  }

  /**
   * Returns XML for the resource.
   *
   * @return the configuration, as an XML string
   */
  public String toXml() {
    return XmlWrapper.prettyPrint(this.resource.xmlText(new XmlOptions()),"  ");
  }

}
