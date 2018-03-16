package edu.sdsc.inca.depot.persistent;

import edu.sdsc.inca.util.StringMethods;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.xmlbeans.XmlObject;

/**
 * An ArgSignature consists of a set of arguments, alphabetized and combined
 * into a string.
 */
public class ArgSignature extends PersistentObject {

  /** id set iff this object is stored in the DB. */
  private Long id;

  /** Persistent fields. */
  private String signature; // Long string
  private Set<Arg> args;

  /**
   * Default constructor.
   */
  public ArgSignature() {
    this(new HashSet<Arg>());
  }

  /**
   * Full constructor.
   */
  public ArgSignature(Set<Arg> args) {
    this.setArgs(args);
  }

  /**
   * Copies information from an Inca schema XmlBean Args object so that this
   * object contains equivalent information.
   *
   * @param o the XmlBean Args object to copy
   * @return this, for convenience
   */
  public PersistentObject fromBean(XmlObject o) {
    return this.fromBean((edu.sdsc.inca.dataModel.util.Args)o);
  }

  /**
   * Copies information from an Inca schema XmlBean Args object so that this
   * object contains equivalent information.
   *
   * @param a the XmlBean Args object to copy
   * @return this, for convenience
   */
  public ArgSignature fromBean(edu.sdsc.inca.dataModel.util.Args a) {
    edu.sdsc.inca.dataModel.util.Args.Arg[] args = a.getArgArray();
    Set<Arg> allArgs = new HashSet<Arg>();
    for(int i = 0; i < args.length; i++) {
      allArgs.add(new Arg().fromBean(args[i]));
    }
    this.setArgs(allArgs);
    return this;
  }

  /**
   * Retrieve the id -- null if not yet connected to database.
   *
   * @return The Long representation of the DB ID
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Set the id.  Hibernate use only.
   *
   * @param id The DB ID.
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Retrieve the arguments involved in creating this signature.
   *
   * @return A set of args that this Signature represents.
   */
  public Set<Arg> getArgs() {
    return this.args;
  }

  /**
   * For internal use.
   *
   * @param args The set of args represented by this signature.
   */
  public void setArgs(Set<Arg> args) {
    this.args = args == null ? new HashSet<Arg>() : args;
    this.regenerateSignature();
  }

  /**
   * Add the argument to the set and invalidate the current signature.
   * @param arg
   */
  public void addArg(Arg arg) {
    this.args.add(arg);
    this.regenerateSignature();
  }

  /**
   * retrieve the signature.
   *
   * @return the String representation of the Signature.
   */
  public String getSignature() {
    return this.signature;
  }

  /**
   * Set the Signature to this previously-computed signature.
   *
   * @param signature The string representation of the signature.
   */
  public void setSignature(String signature) {
    if(signature == null || signature.equals("")) {
      signature = DB_EMPTY_STRING;
    }
    this.signature = truncate(signature, MAX_DB_LONG_STRING_LENGTH, "arg sig");
  }

  /**
   * Compute the signature from the args.
   */
  private void regenerateSignature() {
    String[] strArgs = new String[args.size()];
    Iterator<Arg> it = args.iterator();
    for(int i = 0; i < strArgs.length; i++) {
      strArgs[i] = "-" + it.next().toString();
    }
    Arrays.sort(strArgs);
    this.setSignature(StringMethods.join(" ", strArgs));
  }

  /**
   * Returns a Inca schema XmlBean Args object that contains information
   * equivalent to this object.
   *
   * @return an XmlBean Args object that contains equivalent information
   */
  public XmlObject toBean() {
    edu.sdsc.inca.dataModel.util.Args result =
      edu.sdsc.inca.dataModel.util.Args.Factory.newInstance();
    Iterator<Arg> it = this.getArgs().iterator();
    for(int i = 0; it.hasNext(); i++) {
      result.addNewArg();
      result.setArgArray
        (i, (edu.sdsc.inca.dataModel.util.Args.Arg)((Arg)it.next()).toBean());
    }
    return result;
  }

  /**
   * Override of the default toString method.
   */
  public String toString() {
    return this.getSignature();
  }

  /**
   * Compares another object to this ArgSignature for logical equality.
   *
   * @param o the object to compare
   * @return true iff the comparison object represents the same ArgSignature
   */
  public boolean equals(Object o) {
    return this.toString().equals(o.toString());
  }

  /**
   * Calculate a hash code using the same fields that where used in equals.
   * @return a hash code for this object
   */
  public int hashCode() {
    return 29 + (this.getSignature().hashCode());
  }

}
