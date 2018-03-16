/*
 * DelayedWorkQueue.java
 */
package edu.sdsc.inca.depot;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.sdsc.inca.depot.commands.Insert;
import edu.sdsc.inca.depot.commands.KbArticleDelete;
import edu.sdsc.inca.depot.commands.KbArticleInsert;
import edu.sdsc.inca.depot.commands.SuiteUpdate;


/**
 *
 * @author Paul Hoover
 *
 */
public class DelayedWorkQueue {

  /**
   *
   */
  private static class QueueParser extends DefaultHandler {

    /**
     *
     */
    private interface ParserState {

      /**
       *
       * @param ch
       * @param start
       * @param length
       * @throws SAXException
       */
      void characters(char[] ch, int start, int length) throws SAXException;

      /**
       *
       * @param uri
       * @param localName
       * @param qName
       * @throws SAXException
       */
      void endElement(String uri, String localName, String qName) throws SAXException;

      /**
       *
       * @param uri
       * @param localName
       * @param qName
       * @param atts
       * @throws SAXException
       */
      void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException;
    }

    /**
     *
     */
    private abstract class Capture implements ParserState {

      private StringBuilder m_builder = null;


      // public methods


      /**
       *
       * @param ch
       * @param start
       * @param length
       */
      public void characters(char[] ch, int start, int length)
      {
        if (m_builder != null)
          m_builder.append(new String(ch, start, length));
      }


      // protected methods


      /**
       *
       */
      protected void startTextCapture()
      {
        assert m_builder == null;

        m_builder = new StringBuilder();
      }

      /**
       *
       * @return
       */
      protected String getTextAsString()
      {
        assert m_builder != null;

        String text = m_builder.toString();

        m_builder = null;

        if (text.length() == 0)
          return null;

        return text;
      }
    }

    /**
     *
     */
    private class ParseWorkItem extends Capture {

      private String m_type;
      private String m_data;


      // public methods


      /**
       *
       * @param uri
       * @param localName
       * @param qName
       * @throws SAXException
       */
      public void endElement(String uri, String localName, String qName) throws SAXException
      {
        if (localName.equals("type"))
          m_type = getTextAsString();
        else if (localName.equals("data"))
          m_data = getTextAsString();
        else if (localName.equals("work")) {
          DelayedWork work;

          if (m_type.equals("edu.sdsc.inca.depot.commands.Insert"))
            work = new Insert();
          else if (m_type.equals("edu.sdsc.inca.depot.commands.SuiteUpdate"))
            work = new SuiteUpdate();
          else if (m_type.equals("edu.sdsc.inca.depot.commands.KbArticleInsert"))
            work = new KbArticleInsert();
          else if (m_type.equals("edu.sdsc.inca.depot.commands.KbArticleDelete"))
            work = new KbArticleDelete();
          else
            throw new SAXException("Unknown work type " + m_type);

          try {
            work.loadState(m_data);
          }
          catch (Exception err) {
            throw new SAXException(err);
          }

          m_output.add(work);

          m_logger.debug("Parsed a DelayedWork item of type " + m_type);

          assert m_states.getFirst() == this;

          m_states.removeFirst();
        }
        else
          throw new SAXException("Unexpected end tag for element " + localName);
      }

      /**
       *
       * @param uri
       * @param localName
       * @param qName
       * @param atts
       * @throws SAXException
       */
      public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
      {
        if (localName.equals("type") || localName.equals("data"))
          startTextCapture();
        else
          throw new SAXException("Unexpected start tag for element " + localName);
      }
    }

    /**
     *
     */
    private class Start implements ParserState {

      // public methods


      /**
       *
       * @param ch
       * @param start
       * @param length
       */
      public void characters(char[] ch, int start, int length)
      {
        // do nothing
      }

      /**
       *
       * @param uri
       * @param localName
       * @param qName
       */
      public void endElement(String uri, String localName, String qName)
      {
        // do nothing
      }

      /**
       *
       * @param uri
       * @param localName
       * @param qName
       * @param atts
       * @throws SAXException
       */
      public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
      {
        if (localName.equals("work"))
          m_states.addFirst(new ParseWorkItem());
      }
    }


    private final List<DelayedWork> m_output;
    private final LinkedList<ParserState> m_states = new LinkedList<ParserState>();


    // constructors


    /**
     *
     * @param output
     */
    public QueueParser(List<DelayedWork> output)
    {
      m_output = output;
    }


    // public methods


    /**
     *
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
      m_states.getFirst().characters(ch, start, length);
    }

    /**
     *
     */
    @Override
    public void endDocument()
    {
      m_states.clear();
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
      m_states.getFirst().endElement(uri, localName, qName);
    }

    /**
     *
     * @throws SAXException
     */
    @Override
    public void startDocument() throws SAXException
    {
      m_states.addFirst(new Start());
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      m_states.getFirst().startElement(uri, localName, qName, atts);
    }
  }


  private static final Logger m_logger = Logger.getLogger(DelayedWorkQueue.class);
  private final String m_fileName;


  // constructors


  /**
   *
   * @param fileName
   */
  public DelayedWorkQueue(String fileName)
  {
    m_fileName = fileName;
  }


  // public methods


  /**
   *
   * @throws IOException
   */
  public void createRequestQueue() throws IOException
  {
    createQueueFile("request");
  }

  /**
   *
   * @throws IOException
   */
  public void createResponseQueue() throws IOException
  {
    createQueueFile("response");
  }

  /**
   *
   * @param item
   * @throws IOException
   */
  public synchronized void add(DelayedWork item) throws IOException
  {
    assert (new File(m_fileName)).exists();

    RandomAccessFile workFile = new RandomAccessFile(m_fileName, "rw");

    try {
      workFile.seek(workFile.length() - 8);
      workFile.writeBytes("<work><type>");
      workFile.writeBytes(item.getClass().getName());
      workFile.writeBytes("</type><data><![CDATA[");
      workFile.writeBytes(item.getState().replaceAll("]]>", "]]]]><![CDATA[>"));
      workFile.writeBytes("]]></data></work></queue>");
    }
    finally {
      workFile.close();
    }
  }

  /**
   *
   * @return
   * @throws IOException
   * @throws SAXException
   */
  public synchronized List<DelayedWork> removeAll() throws IOException, SAXException
  {
    assert (new File(m_fileName)).exists();

    List<DelayedWork> result = new ArrayList<DelayedWork>();
    QueueParser handler = new QueueParser(result);
    XMLReader reader = XMLReaderFactory.createXMLReader();

    reader.setContentHandler(handler);
    reader.setDTDHandler(handler);
    reader.setEntityResolver(handler);
    reader.setErrorHandler(handler);

    InputStream inStream = new BufferedInputStream(new FileInputStream(m_fileName));

    try {
      reader.parse(new InputSource(inStream));
    }
    finally {
      inStream.close();
    }

    clear();

    return result;
  }

  /**
   *
   * @throws IOException
   */
  public synchronized void clear() throws IOException
  {
    (new File(m_fileName)).delete();
  }


  // private methods


  /**
   *
   * @param type
   * @throws IOException
   */
  private synchronized void createQueueFile(String type) throws IOException
  {
    PrintWriter writer = new PrintWriter(m_fileName);

    try {
      writer.print("<queue><type>");
      writer.print(type);
      writer.print("</type></queue>");
    }
    finally {
      writer.close();
    }
  }
}
