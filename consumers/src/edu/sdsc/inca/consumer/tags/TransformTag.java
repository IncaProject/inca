/*
 * TransformTag.java
 */
package edu.sdsc.inca.consumer.tags;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
//import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.TransformerFactoryImpl;


/**
 *
 * @author Paul Hoover
 *
 */
@SuppressWarnings("serial")
public class TransformTag extends BodyTagSupport {

  // nested classes


  /**
   *
   */
  private static class TransformWriter extends Writer {

    private final Writer m_writer;


    public TransformWriter(Writer writer)
    {
      m_writer = writer;
    }


    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
      m_writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException
    {
    }

    @Override
    public void close() throws IOException
    {
    }
  }


  // data fields


  private Object m_doc;
  private Object m_xslt;
  private String m_var;
  private int m_scope;
  private Transformer m_transformer;
  private TagUriResolver m_resolver;


  // constructors


  public TransformTag()
  {
    initialize();
  }


  // public methods


  @Override
  public int doStartTag() throws JspException
  {
    if (m_xslt == null)
      throw new JspTagException("transform xslt attribute is null");

    if (m_xslt instanceof String == false)
      throw new JspTagException("transform xslt attribute is an unsupported type");

    String xslt = (String)m_xslt;

    xslt.trim();

    if (xslt.isEmpty())
      throw new JspTagException("transform xslt attribute is empty");

    Source source = new StreamSource(new StringReader(xslt));
    TransformerFactory factory = new TransformerFactoryImpl();

    try {
      //factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setURIResolver(m_resolver);

      Templates templates = factory.newTemplates(source);

      m_transformer = templates.newTransformer();
    }
    catch (TransformerConfigurationException configErr) {
      throw new JspException(configErr);
    }

    return EVAL_BODY_BUFFERED;
  }

  @Override
  public int doEndTag() throws JspException
  {
    Source source;

    if (m_doc != null) {
      if (m_doc instanceof String == false)
        throw new JspTagException("transform doc attribute is an unsupported type");

      String doc = (String)m_doc;

      doc.trim();

      if (doc.isEmpty())
        throw new JspTagException("transform doc attribute is empty");

      source = new StreamSource(new StringReader(doc));
    }
    else if (bodyContent != null) {
      String body = bodyContent.getString();

      if (body == null)
        throw new JspTagException("transform body content is null");

      body.trim();

      if (body.isEmpty())
        throw new JspTagException("transform body content is empty");

      source = new StreamSource(new StringReader(body));
    }
    else
      throw new JspTagException("no input document provided");

    try {
      if (m_var != null && !m_var.isEmpty()) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        m_transformer.transform(source, new StreamResult(outStream));

        String result = outStream.toString();

        pageContext.setAttribute(m_var, result, m_scope);
      }
      else
        m_transformer.transform(source, new StreamResult(new TransformWriter(pageContext.getOut())));
    }
    catch (TransformerException err) {
      throw new JspException(err);
    }

    return EVAL_PAGE;
  }

  @Override
  public void setPageContext(PageContext pageContext)
  {
    super.setPageContext(pageContext);

    if (pageContext != null)
      m_resolver = new TagUriResolver(pageContext);
    else
      m_resolver = null;
  }

  @Override
  public void release()
  {
    super.release();

    initialize();
  }

  public void setDoc(Object doc)
  {
    m_doc = doc;
  }

  public void setXslt(Object xslt)
  {
    m_xslt = xslt;
  }

  public void setVar(String var)
  {
    m_var = var;
  }

  public void setScope(String scope)
  {
    if (scope.equalsIgnoreCase("request"))
      m_scope = PageContext.REQUEST_SCOPE;
    else if (scope.equalsIgnoreCase("session"))
      m_scope = PageContext.SESSION_SCOPE;
    else if (scope.equalsIgnoreCase("application"))
      m_scope = PageContext.APPLICATION_SCOPE;
    else
      m_scope = PageContext.PAGE_SCOPE;
  }

  public void setParameter(String name, Object value)
  {
    m_transformer.setParameter(name, value);
  }


  // private methods


  private void initialize()
  {
    m_doc = null;
    m_xslt = null;
    m_var = null;
    m_scope = PageContext.PAGE_SCOPE;
    m_transformer = null;
    m_resolver = null;
  }
}
