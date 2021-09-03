package edu.sdsc.inca;


import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import edu.sdsc.inca.dataModel.inca.IncaDocument;
import edu.sdsc.inca.dataModel.inca.IncaDocument.Inca;
import edu.sdsc.inca.dataModel.suite.Suite;
import edu.sdsc.inca.dataModel.util.Resource;
import edu.sdsc.inca.dataModel.util.Resources;
import edu.sdsc.inca.util.StringMethods;


/**
 * A class that wraps an IncaDocument.
 */
public class WrapConfig {

  protected IncaDocument doc;
  protected Inca inca;

  /**
   * Creates a new WrapConfig.
   */
  public WrapConfig() {
    this.doc = IncaDocument.Factory.newInstance();
    this.inca = this.doc.addNewInca();
  }

  /**
   * Creates a new WrapConfig by parsing XML.
   *
   * @param xml the IncaDocument XML
   * @throws XmlException if the XML is invalid
   */
  public WrapConfig(String xml) throws XmlException {
    ArrayList<Object> errors = new ArrayList<Object>();
    this.doc = IncaDocument.Factory.parse(xml, (new XmlOptions()).setLoadStripWhitespace());
    if(!this.doc.validate(new XmlOptions().setErrorListener(errors))) {
      throw new XmlException("Invalid inca XML:" + errors.get(0).toString());
    }
    this.inca = this.doc.getInca();
  }

  /**
   * Return a copy of this object.
   *
   * @return A copy of this object.
   */
  public WrapConfig copy() {
    try {
      return new WrapConfig(this.toXml());
    } catch(XmlException e) {
      System.err.println("Faulty XML: " + this.toXml());
      return new WrapConfig();
    }
  }

  /**
   * Returns a new WrapConfig that contains the modifications necessary to
   * produce a specified WrapConfig from this one.
   *
   * @param wc the modified WrapConfig
   * @return a new WrapConfig that shows the differences needed to produce wc
   */
  public WrapConfig differences(WrapConfig wc) {
    WrapConfig result = new WrapConfig();
    // Repositories and resources have no action, so the "difference" is simply
    // the target values.  However, if the original and changed contents of one
    // of these elements are identical, by convention we leave it out of the
    // result.
    result.setRepositories(
      Arrays.equals(this.getRepositories(), wc.getRepositories()) ?
      null : wc.getRepositories()
    );
    result.setResources(
      Arrays.equals(this.getResources(), wc.getResources()) ?
      null : wc.getResources()
    );
    // For suites, we have to figure out which suites to change/delete ...
    ArrayList<WrapSuite> changedSuites = new ArrayList<WrapSuite>();
    WrapSuite[] thisSuites = this.getSuites();
    WrapSuite[] wcSuites = wc.getSuites();
    if(thisSuites == null) {
      thisSuites = new WrapSuite[0];
    }
    if(wcSuites == null) {
      wcSuites = new WrapSuite[0];
    }
    for(int i = 0; i < thisSuites.length; i++) {
      WrapSuite thisSuite = thisSuites[i];
      String name = thisSuite.getName();
      int j;
      for(j = 0;
          j < wcSuites.length &&
          (wcSuites[j] == null || !name.equals(wcSuites[j].getName()));
          j++) {
        // empty
      }
      WrapSuite wcSuite;
      if(j == wcSuites.length) {
        wcSuite = new WrapSuite(thisSuite.getName(),thisSuite.getDescription());
      } else {
        wcSuite = wcSuites[j];
        wcSuites[j] = null; // So we don't consider it added below
      }
      WrapSuite changes = thisSuite.differences(wcSuite);
      if(changes.getSeriesCount() > 0) {
        changedSuites.add(changes);
      }
    }
    // ... and which to add
    for(int i = 0; i < wcSuites.length; i++) {
      WrapSuite wcSuite = wcSuites[i];
      if(wcSuite != null) {
        changedSuites.add(new WrapSuite("", "").differences(wcSuite));
      }
    }
    result.setSuites
      (changedSuites.toArray(new WrapSuite[changedSuites.size()]));
    return result;
  }

  /**
   * Override of the default equals method.
   *
   * @param o the object to compare to this one
   * @return true iff o specifies the same config
   */
  @Override
  public boolean equals(Object o) {
    if(!(o instanceof WrapConfig)) {
      return false;
    }
    // NOTE: Although XML comparison seems like it might be fragile due to
    // formatting issues, it appears to work well enough for our purposes.
    String oXml =
      ((WrapConfig)o).toXml().replaceAll(" xmlns[^\"]*\"[^\"]*\"", "");
    String thisXml = this.toXml().replaceAll(" xmlns[^\"]*\"[^\"]*\"", "");
    return oXml.equals(thisXml);
  }

  /**
   * Returns the set of configuration repositories.
   *
   * @return the configuration repositories, null if none
   */
  public String[] getRepositories() {
    return this.inca.getRepositories() == null ? null :
           this.inca.getRepositories().getRepositoryArray();
  }

  /**
   * Returns the set of configuration resources.
   *
   * @return the configuration resources, null if none
   */
  public WrapResource[] getResources() {
    if(this.inca.getResourceConfig() == null) {
      return null;
    }
    Resource[] resArray =
      this.inca.getResourceConfig().getResources().getResourceArray();
    WrapResource[] result = new WrapResource[resArray.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = new WrapResource(resArray[i]);
    }
    return result;
  }

  /**
   * Returns the set of configuration suites.
   *
   * @return the configuration suites, null if none
   */
  public WrapSuite[] getSuites() {
    if(this.inca.getSuites() == null) {
      return null;
    }
    Suite[] suites = this.inca.getSuites().getSuiteArray();
    WrapSuite[] result = new WrapSuite[suites.length];
    for(int i = 0; i < suites.length; i++) {
      result[i] = new WrapSuite(suites[i]);
    }
    return result;
  }

  /**
   * Sets the configuration repositories to a specified set.
   *
   * @param repositories the configuration repositories, null for none
   */
  public void setRepositories(String[] repositories) {
    if(this.inca.getRepositories() != null) {
      this.inca.unsetRepositories();
    }
    if(repositories != null) {
      Inca.Repositories beanRepositories = this.inca.addNewRepositories();
      for(int i = 0; i < repositories.length; i++) {
        beanRepositories.addRepository(repositories[i]);
      }
    }
  }

  /**
   * Sets the configuration resources to a specified set.
   *
   * @param resources the configuration resources, null for none
   */
  public void setResources(WrapResource[] resources) {
    if(this.inca.getResourceConfig() != null) {
      this.inca.unsetResourceConfig();
    }
    if(resources != null) {
      Resources beanResources =
        this.inca.addNewResourceConfig().addNewResources();
      for(int i = 0; i < resources.length; i++) {
        beanResources.addNewResource();
        beanResources.setResourceArray(i, resources[i].getResource());
      }
    }
  }

  /**
   * Sets the configuration suites to a specified set.
   *
   * @param suites the configuration suites, null for none
   */
  public void setSuites(WrapSuite[] suites) {
    if(this.inca.getSuites() != null) {
      this.inca.unsetSuites();
    }
    if(suites != null) {
      Inca.Suites beanSuites = this.inca.addNewSuites();
      for(int i = 0; i < suites.length; i++) {
        beanSuites.addNewSuite();
        beanSuites.setSuiteArray(i, suites[i].getSuite());
      }
    }
  }

  /**
   * Returns XML for the configuration.
   *
   * @return the configuration, as an XML string
   */
  public String toXml() {
    return this.doc.xmlText((new XmlOptions()).setSavePrettyPrint());
  }

  static public void main( String [] args ) {
    String usage =
      "WrapConfig <new incat> <old incat> [<incat changes>]\n";

    if ( args.length!= 2 && args.length != 3 ) {
      System.err.println( usage );
      System.exit(1);
    }
    WrapConfig config1 = null, config2 = null;
    try {
      config1 = new WrapConfig( StringMethods.fileContents( args[0]) );
      config2 = new WrapConfig( StringMethods.fileContents( args[1]) );
    } catch (Exception e) {
      System.err.println( "Error opening files: " + e );
      System.exit(1);
    }
    WrapConfig diffs = config2.differences(config1);
    if ( args.length == 3 ) {
      File outfile = new File(args[2]);
      try {
        FileWriter out = new FileWriter(outfile);
        out.write( diffs.toXml() );
        out.close();
      } catch (Exception e) {
        System.err.println( "Error writing diffs to file: " + e );
        System.exit(1);
      }
    } else {
      System.out.print( diffs.toXml() );
    }
  }
}
