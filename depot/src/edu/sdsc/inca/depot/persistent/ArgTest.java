package edu.sdsc.inca.depot.persistent;


/**
 * Perform Basic tests on the Inputs Class.
 */
public class ArgTest extends PersistentTest {

  /**
   * test that raw inputs can be parsed and that malformed inputs are caught.
   */
  public void testConstructors() {
    // test the constructor that takes 2 strings
    Arg arg = new Arg("name","value");
    assertEquals("name",arg.getName());
    assertEquals("value", arg.getValue());
  }

  /**
   * Insert 2 identical name value pairs.  check to see that when the
   * second is saved
   * that it represents the same row in the database.
   */
  public void testPersistence() {

    // this object should not be found
    Arg arg = new Arg("testkey", "testvalue");
    try {
      arg = Arg.find(arg);
    } catch (Exception e) {
      fail(e.toString());
    }
    assertNull(arg);

    // save the arg to the db
    arg = new Arg("testkey", "testvalue");
    try {
      arg.save();
    } catch (Exception e) {
      fail(e.toString());
    }
    assertNotNull(arg);
    assertNotNull(arg.getId());

    Arg arg2 = new Arg("testkey", "testvalue");

    try {
      arg2 = Arg.find(arg2);
    } catch (Exception e) {
      fail(e.toString());
    }
    assertNotNull(arg2);
    assertNotNull(arg2.getId());
    assertTrue(arg.equals(arg2));

  }

  /**
   * Test to see if we can save/query values with quotes.
   */
  public void testQuote() {
    Arg arg = new Arg("quote", "I said 'Who's on third?'");
    try {
      arg.save();
    } catch(Exception e) {
      fail("Save failed:" + e);
    }
    try {
      arg = Arg.find(arg);
      assertNotNull(arg);
    } catch(Exception e) {
      fail("Load failed:" + e);
    }
  }

  /**
   * Test to see that too-long arg names/values are properly truncated.
   */
  public void testTruncation() {
    String fortyLong = "a123456789012345678901234567890123456789";
    String name = fortyLong + fortyLong + fortyLong + fortyLong +
                  fortyLong + fortyLong + fortyLong + fortyLong +
                  fortyLong + fortyLong + fortyLong + fortyLong;
    String value = fortyLong + fortyLong + fortyLong + fortyLong +
                   fortyLong + fortyLong + fortyLong + fortyLong +
                   fortyLong + fortyLong + fortyLong + fortyLong +
                   fortyLong + fortyLong + fortyLong + fortyLong;
    Arg arg = new Arg(name, "");
    arg.setValue(value);
    assertEquals(arg.getName(), name.substring(0, Arg.MAX_DB_STRING_LENGTH));
    assertEquals(arg.getValue(), value.substring(0, Arg.MAX_DB_STRING_LENGTH));
  }

}
