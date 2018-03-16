package edu.sdsc.inca.util;

import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;

/**
 * @author Jim Hayes
 */
public class StringMethodsTest extends TestCase {

  private static Logger logger = Logger.getLogger(StringMethodsTest.class);

  protected String tableRow(String tag, String[] cells) {
    StringBuffer result = new StringBuffer("<tr>");
    for(int i = 0; i < cells.length; i++) {
      result.append("<" + tag + ">").append(cells[i]).append("</" + tag + ">");
    }
    result.append("</tr>");
    return result.toString();
  }

  public void testMimeMessage() throws Exception {
    File tempFile1 = new File( "/tmp/testMime1.txt" );
    FileWriter out1 = new FileWriter( tempFile1 );
    out1.write( "<suite/>" );
    out1.close();
    File tempFile2 = new File( "/tmp/testMime2.txt");
    FileWriter out2 = new FileWriter( tempFile2 );
    out2.write( "<inca/>" );
    out2.close();

    String mime = StringMethods.mimeMessage(
      "ssmallen@sdsc.edu",
      "Fwd: inca notification",
      "test message",
      new File[] { tempFile1, tempFile2 },
      new String[] { "text/xml", "text/xml" }
    );
    tempFile1.delete();
    tempFile2.delete();
    String expected =
      "To: ssmallen@sdsc.edu\n" +
      "Mime-Version: 1.0\n" +
      "Content-Type: multipart/mixed; boundary=Inca-2-by-SDSC\n" +
      "Subject: Fwd: inca notification \n" +
      "\n" +
      "\n" +
      "--Inca-2-by-SDSC\n" +
      "Content-Transfer-Encoding: 7bit\n" +
      "Content-Type: text/plain;\n" +
      "\tcharset=US-ASCII;\n" +
      "\tdelsp=yes;\n" +
      "\tformat=flowed\n" +
      "\n" +
      "test message\n" +
      "--Inca-2-by-SDSC\n" +
      "Content-Transfer-Encoding: 7bit\n" +
      "Content-Type: text/xml;\n" +
      "\tx-unix-mode=0644;\n" +
      "\tname=testMime1.txt\n" +
      "Content-Disposition: attachment;\n" +
      "\tfilename=testMime1.txt\n" +
      "\n" +
      "<suite/>\n" +
      "--Inca-2-by-SDSC\n" +
      "Content-Transfer-Encoding: 7bit\n" +
      "Content-Type: text/xml;\n" +
      "\tx-unix-mode=0644;\n" +
      "\tname=testMime2.txt\n" +
      "Content-Disposition: attachment;\n" +
      "\tfilename=testMime2.txt\n" +
      "\n" +
      "<inca/>\n" +
      "--Inca-2-by-SDSC--\n" +
      "\n";
    assertEquals( "mime message format is correct", expected, mime );    
  }

  protected static final String TABLE_END = "</table>";
  protected static final String TABLE_START = "<table[^>]*>";

  public void testXmlContentToHtml() throws Exception {
    String html = StringMethods.xmlContentToHtml("<a>7</a>", "").
                  replaceAll(">\\s+", ">");
    String pat =
      TABLE_START +
      tableRow("th", new String[] {"a"}) +
      tableRow("td", new String[] {"7"}) +
      TABLE_END;
    assertTrue(html.matches(pat));
    html = StringMethods.xmlContentToHtml("<a>8</a><b>9</b>", "").
           replaceAll(">\\s+", ">");
    pat =
      TABLE_START +
      tableRow("th", new String[] {"a", "b"}) +
      tableRow("td", new String[] {"8", "9"}) +
      TABLE_END;
    assertTrue(html.matches(pat));
    html = StringMethods.xmlContentToHtml("<z><a>10</a><b>11</b></z>", "").
           replaceAll(">\\s+", ">");
    pat =
      TABLE_START +
      tableRow("th", new String[] {"z"}) +
      tableRow("td", new String[] {
        TABLE_START +
          tableRow("th", new String[] {"a", "b"}) +
          tableRow("td", new String[] {"10", "11"}) +
        TABLE_END
      }) +
      TABLE_END;
    assertTrue(html.matches(pat));
    html = StringMethods.xmlContentToHtml("<a>12</a><a>13</a>", "").
           replaceAll(">\\s+", ">");
    pat =
      TABLE_START +
      tableRow("th", new String[] {"a"}) +
      tableRow("td", new String[] {
        TABLE_START +
          tableRow("td", new String[] {"12"}) +
          tableRow("td", new String[] {"13"}) +
        TABLE_END
      }) +
      TABLE_END;
    assertTrue(html.matches(pat));
    html = StringMethods.xmlContentToHtml
      ("<z><a>14</a><b>15</b></z><z><a>16</a><b>17</b></z>", "").
           replaceAll(">\\s+", ">");
    pat =
      TABLE_START +
      tableRow("th", new String[] {"z"}) +
      tableRow("td", new String[] {
        TABLE_START +
        tableRow("td", new String[] {
          TABLE_START +
            tableRow("th", new String[] {"a", "b"}) +
            tableRow("td", new String[] {"14", "15"}) +
          TABLE_END
        }) +
        tableRow("td", new String[] {
          TABLE_START +
            tableRow("th", new String[] {"a", "b"}) +
            tableRow("td", new String[] {"16", "17"}) +
          TABLE_END
        }) +
        TABLE_END
      }) +
      TABLE_END;
    assertTrue(html.matches(pat));
  }

}
