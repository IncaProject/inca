package edu.sdsc.inca.util;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: cmills
 * Date: Feb 4, 2005
 * Time: 1:14:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class WorkQueueTest extends TestCase {

    /**
     * just testing to see that it gets through the constructor without an
     * exception.
     */
    public void testConstructorSimple() {
        WorkQueue<IntegerReader> wq = null;
        try {
            wq = new WorkQueue<IntegerReader>();
        } catch (Exception e) {
            fail("constructor failed");
        }
        Assert.assertNotNull(wq);
    }

    /**
     * Tests the addition of one task and the removal of that task.
     */
    public void testAddWork() {
        WorkQueue<IntegerReader> wq = null;
        try {
            wq = new WorkQueue<IntegerReader>();
            wq.addWork(new IntegerWork(1));
            Assert.assertFalse(wq.isEmpty());
            final WorkItem<IntegerReader> i = wq.getWork();
            Assert.assertTrue(wq.isEmpty());
            Assert.assertTrue(i instanceof IntegerWork);
            Assert.assertEquals(((IntegerWork) i).work.intValue(), 1);
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    /**
     * Adding null to the queue should throw a nullPointerException.
     */
    public void testAddNull() {
        WorkQueue<IntegerReader> wq = null;
        try {
            wq = new WorkQueue<IntegerReader>();
            wq.addWork(null);
            fail("should have thrown Null Pointer");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Adds 10 integers and then uses 5 threads to retrieve them.  Merges all
     * the results to make sure that each object was removed once and only
     * once.
     */
    public void testAddRemoveSameWork() {
        final Integer ins[] = new Integer[10];
        final WorkQueue<IntegerReader> wq = new WorkQueue<IntegerReader>();
        final IntegerReader[] workers = new IntegerReader[5];
        final Integer outs[] = new Integer[10];

        for (int i = 0; i < 10; i++) {
            ins[i] = new Integer(i);
            wq.addWork(new IntegerWork(ins[i]));
        }
        for (int i = 0; i < 5; i++) {
            workers[i] = new IntegerReader(wq);
            workers[i].start();
        }

        // wait for all the work to be done
        while (!wq.isEmpty()) ;

        // join the results
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                if (workers[i].results[j] != null) {
                    if (outs[j] == null) {
                        outs[j] = workers[i].results[j];
                    } else {
                        fail("2 workers got same work");
                    }
                }
            }
        }
        for (int i = 0; i < 10; i++) {
            assertNotNull(outs[i]);
        }

    }

    /**
     * Tests clear
     */
    public void testClear() {
        WorkQueue<IntegerReader> wq = null;
        try {
            wq = new WorkQueue<IntegerReader>();
            wq.addWork( new IntegerWork(1) );
            wq.addWork( new IntegerWork(5) );
            wq.addWork( new IntegerWork(10) );
            Assert.assertFalse(wq.isEmpty());
            wq.clear();
            Assert.assertTrue(wq.isEmpty());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    /**
     * Class to assist with testing the queue.
     */
    private class IntegerReader extends Thread {

        public final Integer[] results = new Integer[10];
    	private final WorkQueue<IntegerReader> wq;


        /**
         * create this worker attached to the indicated Q.
         *
         * @param wq
         */
        public IntegerReader(WorkQueue<IntegerReader> wq) {
            this.wq = wq;
        }


        public void run() {
            try {
                while (!this.isInterrupted()) {
                	WorkItem<IntegerReader> o = wq.getWork();

                	try {
                		o.doWork(this);
                    }
                	catch (Exception err) {
                    	// do nothing
                    }
                }
            } catch (InterruptedException e) {
            	// do nothing
            }
        }

        public void addResult(Integer in) {
            results[in.intValue()] = in;
        }
    }

    /**
     * Class to assist with testing the queue.
     */
    private class IntegerWork implements WorkItem<IntegerReader> {

    	public final Integer work;


    	public IntegerWork(Integer in)
    	{
    		this.work = in;
    	}

    	public void doWork(IntegerReader context)
    	{
    		context.addResult(work);
    	}
    }

}
