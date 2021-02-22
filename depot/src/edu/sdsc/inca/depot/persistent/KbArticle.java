/*
 * KbArticle.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import org.apache.xmlbeans.XmlObject;


/**
 *
 * @author Paul Hoover
 *
 */
public class KbArticle extends GeneratedKeyRow implements Comparable<KbArticle> {

  // data fields


  private static final String TABLE_NAME = "INCAKBARTICLE";
  private static final String KEY_NAME = "incaid";
  private final Column<Date> m_entered = new DateColumn("incaentered", false);
  private final Column<String> m_errorMsg = new StringColumn("incaerrormsg", true, MAX_DB_LONG_STRING_LENGTH);
  private final Column<String> m_series = new StringColumn("incaseries", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_reporter = new StringColumn("incareporter", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_authorName = new StringColumn("incaauthorname", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_authorEmail = new StringColumn("incaauthoremail", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_articleTitle = new StringColumn("incaarticletitle", false, 2000);
  private final Column<String> m_articleText = new TextColumn("incaarticletext", false, this);


  // constructors


  /**
   *
   */
  public KbArticle()
  {
    super(TABLE_NAME, KEY_NAME);

    construct(m_entered, m_errorMsg, m_series, m_reporter, m_authorName, m_authorEmail, m_articleTitle, m_articleText);
  }

  /**
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public KbArticle(long id) throws IOException, SQLException, PersistenceException
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
  KbArticle(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    this();

    m_key.assignValue(id);

    load(dbConn);
  }


  // public methods


  /**
   *
   * @return
   */
  public Date getEntered()
  {
    return m_entered.getValue();
  }

  /**
   *
   * @param entered
   */
  public void setEntered(Date entered)
  {
    m_entered.setValue(entered);
  }

  /**
   *
   * @return
   */
  public String getErrorMsg()
  {
    return m_errorMsg.getValue();
  }

  /**
   *
   * @param msg
   */
  public void setErrorMsg(String msg)
  {
    msg = normalize(msg, MAX_DB_LONG_STRING_LENGTH, "error message");

    m_errorMsg.setValue(msg);
  }

  /**
   *
   * @return
   */
  public String getSeries()
  {
    return m_series.getValue();
  }

  /**
   *
   * @param series
   */
  public void setSeries(String series)
  {
    series = normalize(series, MAX_DB_STRING_LENGTH, "series name");

    m_series.setValue(series);
  }

  /**
   *
   * @return
   */
  public String getReporter()
  {
    return m_reporter.getValue();
  }

  /**
   *
   * @param reporter
   */
  public void setReporter(String reporter)
  {
    reporter = normalize(reporter, MAX_DB_STRING_LENGTH, "reporter name");

    m_reporter.setValue(reporter);
  }

  /**
   *
   * @return
   */
  public String getAuthorName()
  {
    return m_authorName.getValue();
  }

  /**
   *
   * @param name
   */
  public void setAuthorName(String name)
  {
    name = normalize(name, MAX_DB_STRING_LENGTH, "author name");

    m_authorName.setValue(name);
  }

  /**
   *
   * @return
   */
  public String getAuthorEmail()
  {
    return m_authorEmail.getValue();
  }

  /**
   *
   * @param email
   */
  public void setAuthorEmail(String email)
  {
    email = normalize(email, MAX_DB_STRING_LENGTH, "author email");

    m_authorEmail.setValue(email);
  }

  /**
   *
   * @return
   */
  public String getArticleTitle()
  {
    return m_articleTitle.getValue();
  }

  /**
   *
   * @param title
   */
  public void setArticleTitle(String title)
  {
    title = normalize(title, 2000, "article title");

    m_articleTitle.setValue(title);
  }

  /**
   *
   * @return
   */
  public String getArticleText()
  {
    return m_articleText.getValue();
  }

  /**
   *
   * @param text
   */
  public void setArticleText(String text)
  {
    if (text == null || text.isEmpty())
      text = DB_EMPTY_STRING;

    m_articleText.setValue(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Row fromBean(XmlObject o)
  {
    return fromBean((edu.sdsc.inca.dataModel.article.KbArticle) o);
  }

  /**
   *
   * @param o
   * @return
   */
  public KbArticle fromBean(edu.sdsc.inca.dataModel.article.KbArticle o)
  {
    setEntered(o.getEntered().getTime());
    setErrorMsg(o.getErrorMsg());
    setSeries(o.getSeries());
    setReporter(o.getReporter());
    setAuthorName(o.getAuthorName());
    setAuthorEmail(o.getAuthorEmail());
    setArticleTitle(o.getArticleTitle());
    setArticleText(o.getArticleText());

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlObject toBean()
  {
    edu.sdsc.inca.dataModel.article.KbArticle result = edu.sdsc.inca.dataModel.article.KbArticle.Factory.newInstance();
    Calendar entered = Calendar.getInstance();

    entered.setTime(getEntered());

    result.setEntered(entered);
    result.setErrorMsg(getErrorMsg());
    result.setSeries(getSeries());
    result.setReporter(getReporter());
    result.setAuthorName(getAuthorName());
    result.setAuthorEmail(getAuthorEmail());
    result.setArticleTitle(getArticleTitle());
    result.setArticleText(getArticleText());

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

    if (other instanceof KbArticle == false)
      return false;

    KbArticle otherArticle = (KbArticle) other;

    return getEntered().equals(otherArticle.getEntered()) &&
           getErrorMsg().equals(otherArticle.getErrorMsg()) &&
           getSeries().equals(otherArticle.getSeries()) &&
           getReporter().equals(otherArticle.getReporter()) &&
           getAuthorName().equals(otherArticle.getAuthorName()) &&
           getAuthorEmail().equals(otherArticle.getAuthorEmail()) &&
           getArticleTitle().equals(otherArticle.getArticleTitle()) &&
           getArticleText().equals(otherArticle.getArticleText());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    return 29 * (getEntered().hashCode() + getErrorMsg().hashCode() +
                 getSeries().hashCode() + getReporter().hashCode() +
                 getAuthorName().hashCode() + getAuthorEmail().hashCode() +
                 getArticleTitle().hashCode() + getArticleText().hashCode());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(KbArticle other)
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
  public String toXml()
  {
    edu.sdsc.inca.dataModel.article.KbArticleDocument doc = edu.sdsc.inca.dataModel.article.KbArticleDocument.Factory.newInstance();

    doc.setKbArticle((edu.sdsc.inca.dataModel.article.KbArticle) toBean());

    return doc.xmlText();
  }


  // protected methods


  /**
   * {@inheritDoc}
   */
  @Override
  protected Long findDuplicate(Connection dbConn)
  {
    return null;
  }
}
