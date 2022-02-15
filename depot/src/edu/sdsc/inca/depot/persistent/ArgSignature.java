/*
 * ArgSignature.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.xmlbeans.XmlObject;


/**
 * An ArgSignature consists of a set of arguments, alphabetized and combined
 * into a string.
 */
public class ArgSignature extends GeneratedKeyRow implements Comparable<ArgSignature> {

  // nested classes


  /**
   *
   */
  private class AddArgOp implements RowOperation {

    // data fields


    private final Arg m_element;


    // constructors


    protected AddArgOp(Arg element)
    {
      m_element = element;
    }


    // public methods


    @Override
    public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
    {
      if (m_element.isNew())
        m_element.save(dbConn);

      Column<Long> inputId = new LongColumn("incainput_id", false, m_element.getId());
      Column<Long> argId = new LongColumn("incaargs_id", false, getId());
      List<Column<?>> cols = new ArrayList<Column<?>>();

      cols.add(inputId);
      cols.add(argId);

      (new InsertOp("INCAARGS", cols)).execute(dbConn);
    }
  }

  /**
   *
   */
  private class RemoveArgOp implements RowOperation {

    // data fields


    private final Arg m_element;


    // constructors


    protected RemoveArgOp(Arg element)
    {
      m_element = element;
    }


    // public methods


    @Override
    public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
    {
      Column<Long> inputId = new LongColumn("incainput_id", false, m_element.getId());
      Column<Long> argId = new LongColumn("incaargs_id", false, getId());
      CompositeKey key = new CompositeKey(inputId, argId);

      (new DeleteOp("INCAARGS", key)).execute(dbConn);

      m_element.delete(dbConn);
    }
  }

  /**
   *
   */
  private class ArgSet extends MonitoredSet<Arg> {

    // constructors


    protected ArgSet(Set<Arg> args)
    {
      super(args);
    }


    // protected methods


    @Override
    protected void addSetAddOp(Arg element)
    {
      m_opQueue.add(new AddArgOp(element));

      regenerateSignature();
    }

    @Override
    protected void addSetRemoveOp(Arg element)
    {
      m_opQueue.add(new RemoveArgOp(element));

      regenerateSignature();
    }

    @Override
    protected void addSetClearOp(List<Arg> elements)
    {
      for (Arg element : elements)
        m_opQueue.add(new RemoveArgOp(element));

      regenerateSignature();
    }
  }


  // data fields


  private static final String TABLE_NAME = "INCAARGSIGNATURE";
  private static final String KEY_NAME = "incaid";
  private final Column<String> m_signature = new StringColumn("incasignature", false, MAX_DB_LONG_STRING_LENGTH);
  private final Deque<RowOperation> m_opQueue = new LinkedList<RowOperation>();
  private ArgSet m_args;


  // constructors


  /**
   *
   */
  public ArgSignature()
  {
    super(TABLE_NAME, KEY_NAME);

    construct(m_signature);

    m_signature.setValue(DB_EMPTY_STRING);
  }

  /**
   *
   * @param args
   */
  public ArgSignature(Set<Arg> args)
  {
    this();

    m_args = new ArgSet(new HashSet<Arg>());

    if (args != null && !args.isEmpty()) {
      for (Arg arg : args)
        m_args.add(arg);
    }
    else
      setSignature(null);
  }

  /**
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public ArgSignature(long id) throws IOException, SQLException, PersistenceException
  {
    this();

    m_key.assignValue(id);

    load();
  }

  /**
   * @param dbConn
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  ArgSignature(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    this();

    m_key.assignValue(id);

    load(dbConn);
  }


  // public methods


  /**
   * Retrieve the arguments involved in creating this signature.
   *
   * @return A set of args that this Signature represents.
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Set<Arg> getArgs() throws IOException, SQLException, PersistenceException
  {
    if (m_args == null) {
      Set<Arg> args = new HashSet<Arg>();

      if (!isNew()) {
        try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
          List<Long> argIds = getArgIds(dbConn, m_key.getValue());

          for (Long id : argIds)
            args.add(new Arg(id));
        }
      }

      m_args = new ArgSet(args);
    }

    return m_args;
  }

  /**
   * For internal use.
   *
   * @param args The set of args represented by this signature.
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public void setArgs(Set<Arg> args) throws IOException, SQLException, PersistenceException
  {
    getArgs().clear();

    if (args != null)
      getArgs().addAll(args);
  }

  /**
   * retrieve the signature.
   *
   * @return the String representation of the Signature.
   */
  public String getSignature()
  {
    return m_signature.getValue();
  }

  /**
   * Set the Signature to this previously-computed signature.
   *
   * @param signature The string representation of the signature.
   */
  public void setSignature(String signature)
  {
    signature = normalize(signature, MAX_DB_LONG_STRING_LENGTH, "arg sig");

    m_signature.setValue(signature);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Row fromBean(XmlObject o) throws IOException, SQLException, PersistenceException
  {
    return fromBean((edu.sdsc.inca.dataModel.util.Args)o);
  }

  /**
   * Copies information from an Inca schema XmlBean Args object so that this
   * object contains equivalent information.
   *
   * @param a the XmlBean Args object to copy
   * @return this, for convenience
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public ArgSignature fromBean(edu.sdsc.inca.dataModel.util.Args a) throws IOException, SQLException, PersistenceException
  {
    edu.sdsc.inca.dataModel.util.Args.Arg[] args = a.getArgArray();
    Set<Arg> allArgs = new HashSet<Arg>();

    for(int index = 0 ; index < args.length ; index++)
      allArgs.add(new Arg().fromBean(args[index]));

    setArgs(allArgs);

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlObject toBean() throws IOException, SQLException, PersistenceException
  {
    edu.sdsc.inca.dataModel.util.Args result = edu.sdsc.inca.dataModel.util.Args.Factory.newInstance();
    int index = 0;

    for (Arg arg : getArgs()) {
      result.addNewArg();
      result.setArgArray(index++, (edu.sdsc.inca.dataModel.util.Args.Arg)arg.toBean());
    }

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object other)
  {
    if (other == null)
      return false;

    if (this == other)
      return true;

    if (other instanceof ArgSignature == false)
      return false;

    ArgSignature otherSignature = (ArgSignature) other;

    return getSignature().equals(otherSignature.getSignature());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    return 29 * getSignature().hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(ArgSignature other)
  {
    if (other == null)
      throw new NullPointerException("other");

    if (this == other)
      return 0;

    return hashCode() - other.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return getSignature();
  }

  /**
   *
   * @param signature
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static ArgSignature find(ArgSignature signature) throws IOException, SQLException, PersistenceException
  {
    return find(signature.getSignature());
  }

  /**
   *
   * @param signature
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static ArgSignature find(String signature) throws IOException, SQLException, PersistenceException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      Long id = find(dbConn, signature);

      if (id == null)
        return null;

      return new ArgSignature(dbConn, id);
    }
  }


  // package methods


  /**
   * {@inheritDoc}
   */
  @Override
  void save(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    super.save(dbConn);

    while (!m_opQueue.isEmpty()) {
      RowOperation op = m_opQueue.remove();

      op.execute(dbConn);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  void load(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    super.load(dbConn);

    m_opQueue.clear();

    m_args = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean delete(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    m_opQueue.clear();

    m_args = null;

    deleteDependencies(dbConn, m_key.getValue());

    return super.delete(dbConn);
  }

  /**
   *
   * @param dbConn
   * @param id
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  static boolean delete(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    deleteDependencies(dbConn, id);

    Criterion key = new LongCriterion(KEY_NAME, id);

    return Row.delete(dbConn, TABLE_NAME, key);
  }


  // protected methods


  /**
   * {@inheritDoc}
   */
  @Override
  protected Long findDuplicate(Connection dbConn) throws SQLException
  {
    return find(dbConn, getSignature());
  }


  // private methods


  /*
   * Compute the signature from the args.
   */
  private void regenerateSignature()
  {
    StringBuilder newSignature = new StringBuilder();

    if (!m_args.isEmpty()) {
      Set<String> args = new TreeSet<String>();

      for (Arg arg : m_args) {
        String newArg = "-" + arg.toString();

        args.add(newArg);
      }

      Iterator<String> argSet = args.iterator();

      newSignature.append(argSet.next());

      while (argSet.hasNext()) {
        newSignature.append(" ");
        newSignature.append(argSet.next());
      }
    }

    setSignature(newSignature.toString());
  }

  /**
   *
   */
  private static List<Long> getArgIds(Connection dbConn, long id) throws SQLException
  {
    try (PreparedStatement selectStmt = dbConn.prepareStatement(
      "SELECT INCAARG.incaid " +
      "FROM INCAARG " +
      "INNER JOIN INCAARGS ON INCAARG.incaid = INCAARGS.incainput_id " +
      "WHERE INCAARGS.incaargs_id = ?"
    )) {
      selectStmt.setLong(1, id);

      ResultSet rows = selectStmt.executeQuery();

      List<Long> result = new ArrayList<Long>();

      while (rows.next())
        result.add(rows.getLong(1));

      return result;
    }
  }

  /*
   *
   */
  private static Long find(Connection dbConn, String signature) throws SQLException
  {
    try (PreparedStatement selectStmt = dbConn.prepareStatement(
      "SELECT incaid " +
      "FROM INCAARGSIGNATURE " +
      "WHERE incasignature = ?"
    )) {
      selectStmt.setString(1, signature);

      ResultSet row = selectStmt.executeQuery();

      if (!row.next())
        return null;

      return row.getLong(1);
    }
  }

  /**
   *
   */
  private static void deleteDependencies(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    try (PreparedStatement deleteLinksStmt = dbConn.prepareStatement(
      "DELETE FROM INCAARGS " +
      "WHERE incaargs_id = ?"
    )) {
      deleteLinksStmt.setLong(1, id);
      deleteLinksStmt.executeUpdate();
    }

    List<Long> argIds = getArgIds(dbConn, id);

    for (Long argId : argIds)
      Arg.delete(dbConn, argId);
  }
}
