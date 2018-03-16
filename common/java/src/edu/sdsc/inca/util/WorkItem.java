/*
 * WorkItem.java
 */
package edu.sdsc.inca.util;


import java.io.IOException;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.protocol.ProtocolException;


/**
 * 
 * @author Paul Hoover
 *
 */
public interface WorkItem<T> {

	void doWork(T context) throws IOException, InterruptedException, ConfigurationException, CrypterException, ProtocolException;
}
