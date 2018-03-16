package edu.sdsc.inca.util;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.dataModel.resourceConfig.ResourceConfigDocument;
import edu.sdsc.inca.dataModel.util.Macro;
import edu.sdsc.inca.dataModel.util.Resource;
import edu.sdsc.inca.protocol.Protocol;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

/**
 * A convenience class for dealing with resource configuration files which
 * provides methods for accessing the content.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class ResourcesWrapper extends XmlWrapper {

  private static Logger logger = Logger.getLogger(ResourcesWrapper.class);
  private static Pattern macroPattern =
    Pattern.compile("@" + Protocol.MACRO_NAME_PATTERN + "@");

  // Member variables
  private String filePath = "/tmp/resources.xml";
  private String passphrase = "";
  private ResourceConfigDocument rcDoc;
  private Hashtable<String, ResourceInfo> resources =
    new Hashtable<String, ResourceInfo>();

  public class ResourceInfo {
    public String[] childrenWithEquiv = new String[0];
    public String[] childrenWithoutEquiv = new String[0];
    public Hashtable<String,String[]> macros = new Hashtable<String,String[]>();
    public Resource resource = null;
    public String[] parents = new String[0];
  }

  // PUBLIC FUNCTIONS

  /**
   * Default constructor which creates a blank resource configuration document.
   */
  public ResourcesWrapper() {
    this.rcDoc = ResourceConfigDocument.Factory.newInstance();
    this.rcDoc.addNewResourceConfig().addNewResources();
    try {
      validateAndInitialize();
    } catch ( XmlException e ) {
      logger.error( "Implementation error creating blank resource config", e );
    }
  }

  /**
   * Create a new ResourcesWrapper object from an XML Beans class instance.
   *
   * @param rcDoc  An instance of ResourceConfigDocument, a class generated
   *               from the schema file by XML Beans
   *
   * @throws XmlException if unable to verify document
   */
  public ResourcesWrapper( ResourceConfigDocument rcDoc ) throws XmlException {
    try {
      this.setResourceConfigDocument( rcDoc );
    } catch ( Exception e ) {
      // the others should not be thrown since there is not file path specified
      throw new XmlException( "Unexpected exception: " + e );
    }
  }

  /**
   * Create a new ResourcesWrapper object from a unencrypted resource
   * configuration XML document file.
   *
   * @param filename  Path to a resource configuration XML file
   *
   * @throws CrypterException if unable to decrypt file
   * @throws IOException if unable to read file
   * @throws XmlException if unable to parse file
   */
  public ResourcesWrapper( String filename )
    throws CrypterException, IOException, XmlException {

    this( filename, "" );
  }

  /**
   * Create a new ResourcesWrapper object from an encrypted resource
   * configuration XML document file.
   *
   * @param filename  Path to an encrypted resource configuration XML file
   * @param passphrase  Secret string used to encrypt/decrypt file to disk
   *
   * @throws CrypterException if unable to decrypt file
   * @throws IOException if unable to read file
   * @throws XmlException if unable to parse file
   */
  public ResourcesWrapper( String filename, String passphrase )
    throws CrypterException, IOException, XmlException {

    this();
    this.filePath = filename;
    this.passphrase = passphrase;

    String type =
      ( passphrase==null||passphrase.equals("") ? "unencrypted" : "encrypted" );
    logger.debug( "Reading in " + type + " resource config " + filename );

    File rcFile  = new File( filename );
    if ( rcFile.exists() ) {
      String xml = read( filename, passphrase );
      this.rcDoc = ResourceConfigDocument.Factory.parse( xml );
      validateAndInitialize();
    } else {
      logger.debug( "Resource configuration file '" + filename +
                   "' not available...creating blank file" );
    }
  }

  /**
   * Create a new ResourcesWrapper object from a reader object.
   *
   * @param rcXml  Reader connected to a character stream to a resource config.
   *
   * @throws IOException if unable to read document
   * @throws XmlException if unable to parse document
   */
  public ResourcesWrapper( final Reader rcXml )
    throws IOException, XmlException {

    this.rcDoc = ResourceConfigDocument.Factory.parse( rcXml );
    validateAndInitialize();
  }

  /**
   * Continues Expanding macros, replacing the oringinal string with the expanded values,
   *  and then recursivly expeanding again
   * 
   * @param resource to expand with
   * @param values of the macro
   * @param macro that gave the values
   * @param part of the string to replace 
   * @param the string to replace it in
   * 
   * @return A vector of strings representing the specified string with macros
   * expanded. 
   * @throws if there are no macros in macro_values, then
   *
   */
  public Vector<String> continueExpanding (String resource, String [] macro_values, String macro, String replacement, String aString)
	throws ConfigurationException {
	Vector<String> someValues = new Vector<String>();
	if ( macro_values == null ) {
      throw new ConfigurationException
        ( "Macro '" + macro + "' not defined for resource '" + resource + "'");
    }
    // replace the values and keep expanding
    for ( String macroValue : macro_values ) {
      // clean up the value and make sure all special chars are escaped
      String valEscaped = XmlWrapper.escape(XmlWrapper.unescape(macroValue));
      // replace the macro with real value
      String aStringSubst = aString.replaceAll("@"+replacement+"@", valEscaped);
      // keep expanding if there are still macros in the string
      someValues.addAll( expand(aStringSubst, resource, macro) );
    }
    return someValues;
  }
  
  /**
   * Return true iff another object is a ResourcesWrapper with identical
   * resource information.
   *
   * @param o the object to compare
   * @return true if o contains identical resource information, else false
   */
  public boolean equals( Object o ) {
    // NOTE: to be precise, we should consider two ResourcesWrappers equal if
    // the resources are simply reordered, or if macros are reordered w/in a
    // resource.  This simpler definition is sufficient for our needs for now.
    XmlOptions xmlOptions = XmlWrapper.getPrettyPrintOptions();
    return o instanceof ResourcesWrapper &&
           this.rcDoc.xmlText(xmlOptions).equals
             ( ((ResourcesWrapper)o).rcDoc.xmlText(xmlOptions) );
  }

  /**
   * Expand any macros in the specified string and return the results as a
   * vector of strings. In the case where you have multiple multi-valued
   * macros, the cross product is returned.
   *
   * @param aString A string that may contain macros that need to be
   *                expanded
   * @param resource The name of the resource this reportSeries is being
   *                 expanded for.
   * @param prevMacro  The name of the previously expanded macro in order to
   *                   detect infinite recursion.
   *
   * @return A vector of strings representing the specified string with macros
   * expanded.
   *
   * @throws ConfigurationException if unable to expand string
   */
  public Vector<String> expand(String aString, String resource,String prevMacro)
    throws ConfigurationException {

    // holds expanded values
    Vector<String> allValues = new Vector<String>();

    // PROXY_MACRO is special so doesn't get expanded -- we disguise it here so
    // it passes thru untouched
    aString = aString.replaceAll
      ( "@" + Protocol.PROXY_MACRO + "@", "#" + Protocol.PROXY_MACRO + "#" );

    // does stringValue contain a macro? (i.e., do we keep expanding)
    Matcher macroMatcher = macroPattern.matcher(aString);
    if ( macroMatcher.find() ) {
      // extract macro name -- returns array of 3 elements: "", macro name, ""
      String[] splitOnAt = Pattern.compile("@").split( macroMatcher.group() );
      String macro = splitOnAt[1];
      if ( macro.equals(prevMacro)) {
        throw new ConfigurationException
          ( "Infinite recursion possibly detected on macro '" + prevMacro +
            "': " + macro );
      }
      String [] macro_values = null;
      if(macro.indexOf("->") > 0) {
        String rgroup = macro.substring(0, macro.indexOf("->")) ;
        macro = macro.substring(macro.indexOf("->") + 2);
        String[] resources = null;
        //new section, exception for macro incaHost
        if(macro.equals(Protocol.HOSTS_MACRO)){
          macro_values = getResources(rgroup,true);
          allValues.addAll(continueExpanding(rgroup, macro_values, macro, rgroup + "->" + macro, aString));
        }else{
          resources = getResources(rgroup, true);
          for(String resour : resources){
            macro_values = getValues( resour, macro, macro );
            allValues.addAll(continueExpanding(resour, macro_values, macro, rgroup + "->" + macro, aString));
          }
        }
      }else{
        macro_values = getValues( resource, macro, macro );
        allValues.addAll(continueExpanding(resource, macro_values, macro, macro, aString));
      }
    } else {
      // no more macros to expand
      allValues.add(aString);
    }
    // restore special PROXY_MACRO in all expanded strings
    for ( int i = 0; i < allValues.size(); i++ ) {
      String value = allValues.elementAt(i);
      value = value.replaceAll
        ( "#" + Protocol.PROXY_MACRO + "#", "@" + Protocol.PROXY_MACRO + "@" );
      allValues.setElementAt( value, i);
    }
    return allValues;
  }

  /**
   * Return the path to resources file if applicable.
   *
   * @return A string containing the path to the resources file or null
   * if not set.
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Return the passphrase associated with these resources.
   *
   * @return A string containing the passphrase
   */
  public String getPassphrase() {
    return this.passphrase;
  }

  /**
   * Return the stored resource configuration document.
   *
   * @return The resource configuration document currently stored in this data
   * access object.
   */
  public ResourceConfigDocument getResourceConfigDocument( ) {
    return rcDoc;
  }

  /**
   * Recursively find the resource members (i.e., not resource sets) for the
   * provided resource set.
   *
   * @param resource The name of a resource set
   *
   * @param hostEquivalence  A value of true indicates that resources that have
   * the .equivalent macro defined as true should be treated as one
   * resource.  A value of false indicates that that resources that have the
   * .equivalent macro defined as true should be resolved further.  The
   * former is good for launching reporter managers (because we only need one
   * reporter manager launched on a resource while the latter is good for
   * sending to the depot (because we don't know what resource the reporter
   * manager will be started on).
   *

   * @return  A string array of resource names belonging to the provided
   * resource set.
   *
   * @throws ConfigurationException if unable to find resource
   */
  public String[] getResources( String resource, boolean hostEquivalence )
    throws ConfigurationException {

    ResourceInfo resourceInfo = this.getResourceInfo( resource );
    if ( resourceInfo == null ) {
      logger.warn( "Resource " + resource + " not found" );
      return new String[0];
    }
    if ( hostEquivalence ) {
      return resourceInfo.childrenWithEquiv;
    } else {
      return resourceInfo.childrenWithoutEquiv;
    }
  }

  /**
   * Recursively find the resource members (i.e., not resource sets) for the
   * resource sets specified in the provided xpath expression.
   *
   * @param xpath  A xpath expression that can be used to reference resources
   *               specified in the resource configuration file.
   * @param hostEquivalence  A value of true indicates that resources that have
   * the .equivalent macro defined as true should be treated as one
   * resource.  A value of false indicates that that resources that have the
   * .equivalent macro defined as true should be resolved further.  The
   * former is good for launching reporter managers (because we only need one
   * reporter manager launched on a resource while the latter is good for
   * sending to the depot (because we don't know what resource the reporter
   * manager will be started on).

   * @return  A string array of resource names belonging to the provided
   * resource set.
   *
   * @throws ConfigurationException if unable to retrieve resources
   */
  public String[] getResourcesByXpath( String xpath, boolean hostEquivalence )
    throws ConfigurationException {

    logger.debug( "Get resources at " + xpath );
    XmlObject[] objects = rcDoc.selectPath( xpath );
    if ( objects == null ) return new String[0];
    Vector<String> members = new Vector<String>();
    for ( XmlObject object : objects ) {
      Resource targetResource = (Resource)object;
      members.addAll( getResources(targetResource, hostEquivalence) );
    }
    String[] stringArrayType = new String[members.size()];
    return members.toArray(stringArrayType);
  }

  /**
   * Convenience method for getValues (see getValues for more documentation)
   * when only one value is defined for the specified macro.
   *
   * @param resource  The name of the resource contained in resourceConfig
   * @param macro   The name of a macro contained in resourceConfig
   *
   * @return  The value of the macro or null if not found
   *
   * @throws ConfigurationException if unable to get macro
   */
  public String getValue( String resource, String macro )
    throws ConfigurationException {
    String[] values = getValues( resource, macro, null );

    // return the first and only value or null if nothing returned
    return values == null ? null : values[0];
  }

  /**
   * Scan resourceConfig and return a list of values for the macro that applies
   * to this resource.  Resources can be grouped by specifying a xpath
   * expression.  Since a group of resources can contain macros, in the case
   * that a resource defines a macro and a resource group that contains
   * the resource also defines the same macro, the resource's macro takes
   * precedence. For example, in the following snippet of xml, the
   * value returned for the macro gridFTPPort will be 2812.  If
   * tg-login.sdsc.teragrid.org did not have gridFTPPort defined, then 2811
   * (from sdsc) would be returned.
   *
   * <pre>
   *   &lt;resource&gt;
   *     &lt;name&gt;sdsc&lt;/name&gt;
   *     &lt;resources&gt;
   *       &lt;resource&gt;
   *         &lt;name&gt;tg-login.sdsc.teragrid.org&lt;/name&gt;
   *         &lt;macros&gt;
   *           &lt;macro&gt;
   *             &lt;name&gt;gridFTPPort&lt;/name&gt;
   *             &lt;value&gt;2812&lt;/value&gt;
   *             ...
   *   &lt;resource&gt;
   *     &lt;name&gt;sdsc&lt;/name&gt;
   *     &lt;xpath&gt;//resource[matches(name, '^*.sdsc.*$')]&lt;/xpath&gt;
   *     &lt;macros&gt;
   *       &lt;macro&gt;
   *         &lt;name&gt;.gridFtpPort&lt;/name&gt;
   *         &lt;value&gt;2811&lt;/value&gt;
   *      &lt;/macro&gt;
   *       ...
   * </pre>
   *
   * @param resource  The name of the resource contained in resourceConfig
   * @param macro   The name of a macro contained in resourceConfig
   * @return  A string array of values for the macro or null if not found
   *
   * @throws ConfigurationException if unable to find macro
   */
  public String[] getValues( String resource, String macro )
    throws ConfigurationException {

    return getValues( resource, macro, null );
  }

  /**
   * Returns true if the given resource resource contains a set of hosts
   * that are considered equivalent (i.e., only need to monitor on one of them)
   *
   * @param resourceName  The name of a resource.
   *
   * @return  True if the resource contains a set of hosts that are
   * equivalent and false otherwise.
   */
  public boolean hasHostEquivalence( String resourceName ) {
    Resource resource = this.getResourceInfo( resourceName ).resource;
    String resourceXpath = "macros/macro[name='" + Protocol.EQUIVALENT_MACRO + "' " +
                           "and value='true']";
    XmlObject[] objects = resource.selectPath( resourceXpath );
    return objects.length >= 1;
  }

  /**
   * Save the resource configuration file to disk.
   *
   * @throws CrypterException if unable to encrypt file
   * @throws IOException if unable to write to disk
   */
  public void save() throws CrypterException, IOException {

    if ( this.filePath == null ) return;

    String xml = this.rcDoc.xmlText( XmlWrapper.getPrettyPrintOptions() );
    save( xml, this.filePath, this.passphrase );
    logger.debug( "Saved resources to file '" + filePath + "'" );
  }

  /**
   * Set the path to the resources file.
   *
   * @param filePath A string containing the path to where the resources file
   * can be read/stored.
   */
  public void setFilePath( String filePath ) {
    this.filePath = filePath;
  }

  /**
   * Set the passphrase associated with these resources.
   *
   * @param passphrase A string containing the passphrase
   */
  public void setPassphrase( String passphrase ) {
    this.passphrase = passphrase;
  }

  /**
   * Set the resource configuration document to the provided document.
   *
   * @param rcDoc  An instance of ResourceConfigDocument, a class generated
   *               from the schema file by XML Beans
   *
   * @throws XmlException if unable to validate document
   */
  public void setResourceConfigDocument( ResourceConfigDocument rcDoc )
    throws XmlException {

    this.rcDoc = rcDoc;
    validateAndInitialize();
  }

  // PRIVATE FUNCTIONS

  /**
   * Find the parent resources of all resources
   */
  private void getParentResources() {

    // resources in document
    Resource[] resources =
      this.rcDoc.getResourceConfig().getResources().getResourceArray();

    // get immediate children for each resources
    Hashtable<String, XmlObject[]> immediateChildren =
      new Hashtable<String, XmlObject[]>();
    for( Resource resource : resources ) {
      Resource[] resourceChildren = new Resource[0];
      if ( resource.isSetXpath() ) {
        XmlObject[] results = rcDoc.selectPath(resource.getXpath());
        if ( results.length > 0 ) {
          resourceChildren = (Resource[])results;
        }
      }
      immediateChildren.put( resource.getName(), resourceChildren );
    }

    // find parents for each resource by iterating thru all resources and seeing
    // if a resource is part of any other resources immediate children
    for( Resource resource : resources ) {
      Vector<String> parents = new Vector<String>();
      for( Resource otherResource : resources ) {
        Resource[] candidateChildren =
          (Resource[])immediateChildren.get(otherResource.getName());
        for ( Resource candidateChild : candidateChildren ) {
          if ( candidateChild.getName().equals(resource.getName()) ) {
            logger.debug
              (resource.getName() + " part of group "+ otherResource.getName());
            parents.add( otherResource.getName() );
            break;
          }
        }
      }
      ResourceInfo resourceInfo = this.getResourceInfo(resource.getName());
      resourceInfo.parents = parents.toArray( new String[parents.size()]);
      logger.debug
        ("Parents of " + resource.getName() + ": " +
         StringMethods.join( " ", resourceInfo.parents ) );
      this.resources.put( resource.getName(), resourceInfo );
    }
  }

  private ResourceInfo getResourceInfo( String resource ) {
    return this.resources.get( resource );
  }

  /**
   * Recursively find the resource members (i.e., not resource sets) for the
   * provided resource set.
   *
   * @param resource A resource set
   * @param hostEquivalence  A value of true indicates that resources that have
   * the .equivalent macro defined as true should be treated as one
   * resource.  A value of false indicates that that resources that have the
   * .equivalent macro defined as true should be resolved further.  The
   * former is good for launching reporter managers (because we only need one
   * reporter manager launched on a resource while the latter is good for
   * sending to the depot (because we don't know what resource the reporter
   * manager will be started on).

   * @return  A vector of resource names belonging to the provided
   * resource set.
   */
  private Vector<String> getResources( Resource resource, boolean hostEquivalence ) {
    Vector<String> resource_members = new Vector<String>();
    if ( ! resource.isSetXpath() ||
         (hostEquivalence && hasHostEquivalence(resource.getName())) )  {
      resource_members.add( resource.getName() );
    } else {
      XmlObject[] resources = rcDoc.selectPath( resource.getXpath() );
      for ( XmlObject resourceObj : resources ) {
        resource_members.addAll
          ( getResources( (Resource)resourceObj, hostEquivalence ) );
      }
    }
    return resource_members;
  }

  /**
   * Scan resourceConfig and return a list of values for the macro that applies
   * to this resource.  Resources can be grouped by specifying a xpath
   * expression.  Since a group of resources can contain macros, in the case
   * that a resource defines a macro and a resource group that contains
   * the resource also defines the same macro, the resource's macro takes
   * precedence. For example, in the following snippet of xml, the
   * value returned for the macro gridFTPPort will be 2812.  If
   * tg-login.sdsc.teragrid.org did not have gridFTPPort defined, then 2811
   * (from sdsc) would be returned.
   *
   * <pre>
   *   &lt;resource&gt;
   *     &lt;name&gt;sdsc&lt;/name&gt;
   *     &lt;resources&gt;
   *       &lt;resource&gt;
   *         &lt;name&gt;tg-login.sdsc.teragrid.org&lt;/name&gt;
   *         &lt;macros&gt;
   *           &lt;macro&gt;
   *             &lt;name&gt;gridFTPPort&lt;/name&gt;
   *             &lt;value&gt;2812&lt;/value&gt;
   *             ...
   *   &lt;resource&gt;
   *     &lt;name&gt;sdsc&lt;/name&gt;
   *     &lt;xpath&gt;//resource[matches(name, '^*.sdsc.*$')]&lt;/xpath&gt;
   *     &lt;macros&gt;
   *       &lt;macro&gt;
   *         &lt;name&gt;.gridFtpPort&lt;/name&gt;
   *         &lt;value&gt;2811&lt;/value&gt;
   *      &lt;/macro&gt;
   *       ...
   * </pre>
   *
   * @param resource  The name of the resource contained in resourceConfig
   * @param macro   The name of a macro contained in resourceConfig
   * @param prevMacro The name of the previously expanded macro in order to
   *                  detect infinite recursion.
   * @return  A string array of values for the macro or null if not found
   *
   * @throws ConfigurationException if resource not found
   */
  private String[] getValues( String resource, String macro, String prevMacro )
    throws ConfigurationException {

    if ( ! this.resources.containsKey( resource ) ) {
      throw new ConfigurationException("Resource '" + resource + "' not found");
    }
    if ( this.getResourceInfo( resource ).macros.containsKey( macro ) ) {
      return this.getResourceInfo( resource ).macros.get( macro );
    }
    Resource targetResource = this.getResourceInfo( resource ).resource;
    logger.debug(
      "Search for macro '" + macro + "' in resource " +  resource
    );

    // we recursively find macro definition for this resource from
    // parent resources
    String[] values = getValuesRecurse( targetResource, macro );
    if (values == null) return null;
    Vector<String> expandedValues = new Vector<String>();
    for ( String value : values ) {
      logger.debug( "Found values " + value );
      expandedValues.addAll( expand( value, resource, prevMacro ) );
    }
    String[] evArray = expandedValues.toArray
      ( new String[expandedValues.size()] );

    this.getResourceInfo( resource ).macros.put( macro, evArray );

    return evArray;
  }

  /**
   * Recursively discover the macro definition for a given resource.  Called
   * by getValues.
   *
   * @param resource Resource in which we are searching for the macro definition
   * @param macro The name of a macro defined for the given resource
   * @return  A string array of values for the macro or null if not found
   */
  private String[] getValuesRecurse( Resource resource, String macro ) {

    // query for macro - macro definitions are unique within a resource
    XmlObject[] objects = resource.selectPath("macros/macro[name='"+macro+"']");

    // if it exists, we return it because the innermost value wins and we're
    // executing from innermost resource group to outermost
    if ( objects.length == 1 ) {
      return ((Macro)objects[0]).getValueArray();
    }

    // otherwise we keep searching to see if a parent resource contains the
    // value
    ResourceInfo resourceInfo = getResourceInfo( resource.getName() );
    for( int i = 0; i < resourceInfo.parents.length; i++) {
      ResourceInfo parentInfo = getResourceInfo( resourceInfo.parents[i] );
      logger.debug
        ( "Search for macro '" + macro + "' in parent resource " +
          resourceInfo.parents[i] );
      String[] values = getValuesRecurse( parentInfo.resource, macro );
      if ( values != null ) { // macro found in parent class
        return values;
      }
    }
    return null;
  }

  private void validateAndInitialize() throws XmlException {
    validate(this.rcDoc);

    // read in all resources for easy reference
    Resource[] resources =
      this.rcDoc.getResourceConfig().getResources().getResourceArray();
    for ( Resource resource : resources ) {
      ResourceInfo resourceInfo = new ResourceInfo();
      resourceInfo.resource = resource;
      this.resources.put( resource.getName(), resourceInfo );
    }
    getParentResources();
    for ( Resource resource : resources ) {
      ResourceInfo resourceInfo = this.getResourceInfo(resource.getName());
      Vector<String> children = getResources( resource, true );
      resourceInfo.childrenWithEquiv = children.toArray
        ( new String[children.size()] );
      logger.debug(
        "Member resources of " + resource.getName() + ": " +
        StringMethods.join( " ", resourceInfo.childrenWithEquiv )
      );
      children = getResources(resource, false );
      resourceInfo.childrenWithoutEquiv = children.toArray
        ( new String[children.size()] );
      logger.debug(
        "Member hosts of " + resource.getName() + ": " +
        StringMethods.join( " ", resourceInfo.childrenWithoutEquiv )
      );
      this.resources.put( resource.getName(), resourceInfo );
    }

  }

} // END ResourceWrapper
