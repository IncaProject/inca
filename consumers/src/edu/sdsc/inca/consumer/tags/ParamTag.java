/*
 * ParamTag.java
 */
package edu.sdsc.inca.consumer.tags;


import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;


/**
 *
 * @author Paul Hoover
 *
 */
@SuppressWarnings("serial")
public class ParamTag extends BodyTagSupport {

  // data fields


  private String m_name;
  private Object m_value;


  // constructors


  public ParamTag()
  {
    m_name = null;
    m_value = null;
  }


  // public methods


  @Override
  public int doEndTag() throws JspException
  {
    Tag tag = findAncestorWithClass(this, TransformTag.class);

    if (tag == null)
      throw new JspTagException("parameter tag used outside of transform tag");

    TransformTag parent = (TransformTag)tag;
    Object value = m_value;

    if (value == null) {
      String body;

      if (bodyContent == null)
        body = null;
      else
        body = bodyContent.getString();

      if (body == null)
        value = "";
      else
        value = body.trim();
    }

    parent.setParameter(m_name, value);

    return EVAL_PAGE;
  }

  public void setName(String name)
  {
    m_name = name;
  }

  public void setValue(Object value)
  {
    m_value = value;
  }
}
