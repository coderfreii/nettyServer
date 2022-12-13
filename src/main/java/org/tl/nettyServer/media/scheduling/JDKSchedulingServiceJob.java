package org.tl.nettyServer.media.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Scheduled job that is registered in the Quartz scheduler.
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class JDKSchedulingServiceJob implements Runnable {

    private Logger log = LoggerFactory.getLogger(JDKSchedulingServiceJob.class);

    /**
     * Job data map
     */
    private Map<String, Object> jobDataMap;

    public void setJobDataMap(Map<String, Object> jobDataMap) {
        log.debug("Set job data map: {}", jobDataMap);
        this.jobDataMap = jobDataMap;
    }

    public void run() {
        log.debug("execute");
        IScheduledJob job = null;
        try {
            ISchedulingService service = (ISchedulingService) jobDataMap.get(ISchedulingService.SCHEDULING_SERVICE);
            job = (IScheduledJob) jobDataMap.get(ISchedulingService.SCHEDULED_JOB);
            job.execute(service);
        } catch (Throwable e) {
            if (job == null) {
                log.warn("Job not found");
            } else {
                log.warn("Job {} execution failed", job.toString(), e);
            }
        }
    }

}
