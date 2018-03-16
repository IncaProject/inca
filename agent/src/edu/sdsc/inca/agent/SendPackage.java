/*
 * SendPackage.java
 */
package edu.sdsc.inca.agent;


import java.io.IOException;

import edu.sdsc.inca.util.WorkItem;


/**
 * 
 * @author Paul Hoover
 *
 */
class SendPackage implements WorkItem<ReporterManagerController> {

	private final String packageName;


	public SendPackage(String name)
	{
		this.packageName = name;
	}


	public void doWork(ReporterManagerController context) throws IOException, InterruptedException
	{
		context.sendPackage(packageName);
	}
}
