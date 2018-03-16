/*
 * SendSuite.java
 */
package edu.sdsc.inca.agent;


import java.io.IOException;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.util.CrypterException;
import edu.sdsc.inca.util.WorkItem;


/**
 * 
 * @author Paul Hoover
 *
 */
class SendSuite implements WorkItem<ReporterManagerController> {

	private final SuiteDocument suiteDoc;


	public SendSuite(SuiteDocument doc)
	{
		this.suiteDoc = doc;
	}


	public void doWork(ReporterManagerController context) throws ConfigurationException, CrypterException, IOException, InterruptedException, ProtocolException
	{
		context.sendSuite(suiteDoc);
	}
}
