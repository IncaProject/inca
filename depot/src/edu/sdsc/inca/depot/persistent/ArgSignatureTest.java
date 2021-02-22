package edu.sdsc.inca.depot.persistent;


import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;


/**
 * Perform basic tests of the Arg Signature.
 */
public class ArgSignatureTest extends PersistentTest {

  public void testConstructors() throws Exception {

    // create a few Arguments that can be used repeatedly
    Arg arg1 = new Arg("name","value1");
    Arg arg2 = new Arg("name","value2");
    Arg arg3 = new Arg("key","value");
    Arg arg4 = new Arg("key1","value");
    Arg arg5 = new Arg("totally","different");

    // put the args in a set
    Set<Arg> args = new HashSet<Arg>();
    args.add(arg1);
    args.add(arg2);
    args.add(arg3);
    args.add(arg4);

    // try the constructor
    ArgSignature argSig = new ArgSignature();
    assertNotNull(argSig);
    argSig.setArgs(args);
    assertNotNull(argSig.getSignature());

    ArgSignature argSig2 = new ArgSignature();
    for(Iterator<Arg> i = args.iterator(); i.hasNext(); ) {
      argSig2.getArgs().add(i.next());
    }
    assertTrue(argSig2.equals(argSig));
    argSig.getArgs().add(arg1);
    assertTrue(argSig2.equals(argSig));
    argSig.getArgs().add(arg5);
    assertFalse(argSig2.equals(argSig));

  }

  public void testPersistence() throws Exception {

    // create a few Arguments that can be used repeatedly
    Arg arg1 = new Arg("name","value1");
    Arg arg2 = new Arg("name","value2");
    Arg arg3 = new Arg("key","value");
    Arg arg4 = new Arg("key1","value");
    Arg arg5 = new Arg("totally","different");
    Arg arg6 = new Arg( "verbose", "0" );
    Arg arg7 = new Arg( "help", "no" );
    Arg arg8 = new Arg( "verbose", "0" );

    // put the args in a set
    Set<Arg> args = new HashSet<Arg>();
    args.add(arg1);
    args.add(arg2);
    args.add(arg3);
    args.add(arg4);

    // try to load before it exists
    ArgSignature argSig = new ArgSignature(args);
    try {
      argSig = ArgSignature.find(argSig);
    } catch(Exception e) {
      fail("exception on load");
    }
    assertNull(argSig);

    // try to save
    argSig = new ArgSignature(args);
    try {
      argSig.save();
    } catch (Exception e) {
      fail("save should have worked");
    }
    assertNotNull(argSig);

    ArgSignature argSig2 = new ArgSignature(args);
    argSig2.getArgs().add(arg5);
    try {
      argSig2.save();
      assertNotNull(argSig2);
    } catch (Exception e) {
      fail("save should have worked: " + e);

    }
    assertEquals(5,args.size());
    argSig2 = new ArgSignature(args);
    ArgSignature argSig3 = new ArgSignature(args);
    assertTrue(argSig2.getArgs().containsAll(argSig3.getArgs()));

    assertTrue(argSig2.getSignature().equals(argSig3.getSignature()));
    assertTrue(argSig2.equals(argSig3));

    logger.debug( "\nSignature help=no,verbose=0");
    Set<Arg> args1 = new HashSet<Arg>();
    args1.add(arg6);
    args1.add(arg7);
    ArgSignature argSig4 = new ArgSignature(args1);
    try {
      argSig4.save();
      assertNotNull(argSig4);
    } catch (Exception e) {
      fail("save should have worked: " + e);
    }

    logger.debug( "\nSignature verbose=0");
    Set<Arg> args2 = new HashSet<Arg>();
    args2.add(arg8);
    argSig4 = new ArgSignature(args2);
    try {
      argSig4.save();
      assertNotNull(argSig4);
    } catch (Exception e) {
      fail("save should have worked: " + e);
    }

  }



}
