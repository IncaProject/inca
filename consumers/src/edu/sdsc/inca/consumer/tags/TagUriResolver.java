/*
 * TagUriResolver.java
 */
package edu.sdsc.inca.consumer.tags;


import java.io.InputStream;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;


/**
 *
 * @author Paul Hoover
 *
 */
class TagUriResolver implements URIResolver {

  // data fields


  private static final Pattern m_urlPattern = Pattern.compile("^[a-zA-Z0-9+.-]+:[a-zA-Z0-9+.-/]*");
  private final PageContext m_context;


  // constructors


  public TagUriResolver(PageContext context)
  {
    m_context = context;
  }


  // public methods


  @Override
  public Source resolve(String href, String base) throws TransformerException
  {
    if (href == null)
      return null;

    if (m_urlPattern.matcher(href).matches())
      return null;

    if (base == null)
      base = "";
    else {
      if (m_urlPattern.matcher(base).matches())
        return null;

      int index = base.lastIndexOf("/");

      if (index >= 0)
        base = base.substring(0, index + 1);
      else
        base = "";
    }

    String target = base + href;

    if (!target.startsWith("/")) {
      String pagePath = ((HttpServletRequest)m_context.getRequest()).getServletPath();
      String basePath = pagePath.substring(0, pagePath.lastIndexOf("/"));

      target = basePath + "/" + target;
    }

    InputStream inStream = m_context.getServletContext().getResourceAsStream(target);

    if (inStream == null)
      throw new TransformerException("unable to resolve entity " + target);

    return new StreamSource(inStream);
  }
}
