package org.tl.nettyServer.servers.scheduling;


import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.servers.jmx.mxbeans.JDKSchedulingServiceMXBean;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduling service that uses JDK ScheduledExecutor as backend.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */

@Slf4j
public class JDKSchedulingService implements ISchedulingService, JDKSchedulingServiceMXBean {


    /**
     * Service scheduler
     */
    protected ScheduledExecutorService scheduler;

    protected int threadCount = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * Storage for job futures keyed by name
     */
    protected ConcurrentMap<String, ScheduledFuture<?>> keyMap = new ConcurrentHashMap<>();

    protected AtomicInteger jobDetailCounter = new AtomicInteger();

    private boolean interruptOnRemove = true;

    /**
     * Constructs a new QuartzSchedulingService.
     */
    public void afterPropertiesSet() throws Exception {
        log.debug("Initializing...");
        scheduler = Executors.newScheduledThreadPool(threadCount);
    }

    /**
     * @return the threadCount
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * @param threadCount the threadCount to set
     */
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * {@inheritDoc}
     */
    public String addScheduledJob(int interval, IScheduledJob job) {
        String name = getJobName();
        // Store reference to applications job and service
        Map<String, Object> jobData = new HashMap<>();
        jobData.put(ISchedulingService.SCHEDULING_SERVICE, this);
        jobData.put(ISchedulingService.SCHEDULED_JOB, job);
        // runnable task
        JDKSchedulingServiceJob schedJob = new JDKSchedulingServiceJob();
        schedJob.setJobDataMap(jobData);
        // schedule it to run at interval
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(schedJob, interval, interval, TimeUnit.MILLISECONDS);
        // add to the key map
        keyMap.put(name, future);
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String addScheduledOnceJob(Date date, IScheduledJob job) {
        String name = getJobName();
        // Store reference to applications job and service
        Map<String, Object> jobData = new HashMap<>();
        jobData.put(ISchedulingService.SCHEDULING_SERVICE, this);
        jobData.put(ISchedulingService.SCHEDULED_JOB, job);
        // runnable task
        JDKSchedulingServiceJob schedJob = new JDKSchedulingServiceJob();
        schedJob.setJobDataMap(jobData);
        // calculate the delay
        long delay = date.getTime() - System.currentTimeMillis();
        // schedule it to run once after the specified delay
        ScheduledFuture<?> future = scheduler.schedule(schedJob, delay, TimeUnit.MILLISECONDS);
        // add to the key map
        keyMap.put(name, future);
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String addScheduledOnceJob(long timeDelta, IScheduledJob job) {
        // Create trigger that fires once in <timeDelta> milliseconds
        String name = getJobName();
        // Store reference to applications job and service
        Map<String, Object> jobData = new HashMap<>();
        jobData.put(ISchedulingService.SCHEDULING_SERVICE, this);
        jobData.put(ISchedulingService.SCHEDULED_JOB, job);
        // runnable task
        JDKSchedulingServiceJob schedJob = new JDKSchedulingServiceJob();
        schedJob.setJobDataMap(jobData);
        // schedule it to run once after the specified delay
        ScheduledFuture<?> future = scheduler.schedule(schedJob, timeDelta, TimeUnit.MILLISECONDS);
        // add to the key map
        keyMap.put(name, future);
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String addScheduledJobAfterDelay(int interval, IScheduledJob job, int delay) {
        String name = getJobName();
        // Store reference to applications job and service
        Map<String, Object> jobData = new HashMap<>();
        jobData.put(ISchedulingService.SCHEDULING_SERVICE, this);
        jobData.put(ISchedulingService.SCHEDULED_JOB, job);
        // runnable task
        JDKSchedulingServiceJob schedJob = new JDKSchedulingServiceJob();
        schedJob.setJobDataMap(jobData);
        // schedule it to run at interval
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(schedJob, delay, interval, TimeUnit.MILLISECONDS);
        // add to the key map
        keyMap.put(name, future);
        return name;
    }

    /**
     * Getter for job name.
     *
     * @return Job name
     */
    public String getJobName() {
        return String.format("ScheduledJob_%d", jobDetailCounter.getAndIncrement());
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getScheduledJobNames() {
        if (scheduler != null) {
            return new ArrayList<>(keyMap.keySet());
        } else {
            log.warn("No scheduler is available");
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    public void pauseScheduledJob(String name) {
        throw new RuntimeException("Pause is not supported for ScheduledFuture");
    }

    /**
     * {@inheritDoc}
     */
    public void resumeScheduledJob(String name) {
        throw new RuntimeException("Pause/resume is not supported for ScheduledFuture");
    }

    /**
     * {@inheritDoc}
     */
    public void removeScheduledJob(String name) {
        try {
            ScheduledFuture<?> future = keyMap.remove(name);
            if (future != null) {
                future.cancel(interruptOnRemove);
            } else {
                log.debug("No key found for job: {}", name);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void destroy() throws Exception {
        if (scheduler != null) {
            log.debug("Destroying...");
            scheduler.shutdownNow();
        }
        keyMap.clear();
    }

    public boolean isInterruptOnRemove() {
        return interruptOnRemove;
    }

    public void setInterruptOnRemove(boolean interruptOnRemove) {
        this.interruptOnRemove = interruptOnRemove;
    }

}
