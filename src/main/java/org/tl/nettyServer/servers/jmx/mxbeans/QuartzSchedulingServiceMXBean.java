package org.tl.nettyServer.servers.jmx.mxbeans;

import javax.management.MXBean;
import java.util.List;

@MXBean
public interface QuartzSchedulingServiceMXBean {

	/**
	 * Getter for job name.
	 *
	 * @return  Job name
	 */
	public String getJobName();

	public void removeScheduledJob(String name);

	public List<String> getScheduledJobNames();

}
