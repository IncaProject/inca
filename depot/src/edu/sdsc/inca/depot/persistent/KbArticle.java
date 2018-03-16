/*
 * KbArticle.java
 */
package edu.sdsc.inca.depot.persistent;


import java.util.Calendar;
import java.util.Date;

import org.apache.xmlbeans.XmlObject;


/**
 *
 * @author Paul Hoover
 *
 */
public class KbArticle extends PersistentObject {

	private Long id;
	private Date entered;
	private String errorMsg;
	private String series;
	private String reporter;
	private String authorName;
	private String authorEmail;
	private String articleTitle;
	private String articleText;


	/**
	 *
	 * @return
	 */
	public Long getId()
	{
		return id;
	}

	/**
	 *
	 * @param i
	 */
	public void setId(Long i)
	{
		id = i;
	}

	/**
	 *
	 * @return
	 */
	public Date getEntered()
	{
		return entered;
	}

	/**
	 *
	 * @param e
	 */
	public void setEntered(Date e)
	{
		entered = e;
	}

	/**
	 *
	 * @return
	 */
	public String getErrorMsg()
	{
		return errorMsg;
	}

	/**
	 *
	 * @param msg
	 */
	public void setErrorMsg(String msg)
	{
		if (msg == null || msg.length() < 1)
			errorMsg = DB_EMPTY_STRING;
		else
			errorMsg = truncate(msg, 4000, "error message");
	}

	/**
	 *
	 * @return
	 */
	public String getSeries()
	{
		return series;
	}

	/**
	 *
	 * @param s
	 */
	public void setSeries(String s)
	{
		if (s == null || s.length() < 1)
			series = DB_EMPTY_STRING;
		else
			series = truncate(s, 255, "series name");
	}

	/**
	 *
	 * @return
	 */
	public String getReporter()
	{
		return reporter;
	}

	/**
	 *
	 * @param r
	 */
	public void setReporter(String r)
	{
		if (r == null || r.length() < 1)
			reporter = DB_EMPTY_STRING;
		else
			reporter = truncate(r, 255, "reporter name");
	}

	/**
	 *
	 * @return
	 */
	public String getAuthorName()
	{
		return authorName;
	}

	/**
	 *
	 * @param name
	 */
	public void setAuthorName(String name)
	{
		if (name == null || name.length() < 1)
			authorName = DB_EMPTY_STRING;
		else
			authorName = truncate(name, 255, "author name");
	}

	/**
	 *
	 * @return
	 */
	public String getAuthorEmail()
	{
		return authorEmail;
	}

	/**
	 *
	 * @param email
	 */
	public void setAuthorEmail(String email)
	{
		if (email == null || email.length() < 1)
			authorEmail = DB_EMPTY_STRING;
		else
			authorEmail = truncate(email, 255, "author email");
	}

	/**
	 *
	 * @return
	 */
	public String getArticleTitle()
	{
		return articleTitle;
	}

	/**
	 *
	 * @param title
	 */
	public void setArticleTitle(String title)
	{
		if (title == null || title.length() < 1)
			articleTitle = DB_EMPTY_STRING;
		else
			articleTitle = truncate(title, 2000, "article title");
	}

	/**
	 *
	 * @return
	 */
	public String getArticleText()
	{
		return articleText;
	}

	/**
	 *
	 * @param text
	 */
	public void setArticleText(String text)
	{
		if (text == null || text.length() < 1)
			articleText = DB_EMPTY_STRING;
		else
			articleText = text;
	}

	/**
	 *
	 * @param o
	 * @return
	 */
	public PersistentObject fromBean(XmlObject o)
	{
		return fromBean((edu.sdsc.inca.dataModel.article.KbArticle)o);
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
	 *
	 * @return
	 */
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
	 *
	 * @return
	 */
	@Override
	public String toXml()
	{
		edu.sdsc.inca.dataModel.article.KbArticleDocument doc = edu.sdsc.inca.dataModel.article.KbArticleDocument.Factory.newInstance();

		doc.setKbArticle((edu.sdsc.inca.dataModel.article.KbArticle) toBean());

		return doc.xmlText();
	}
}
