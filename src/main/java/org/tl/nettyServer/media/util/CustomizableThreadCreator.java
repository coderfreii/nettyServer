package org.tl.nettyServer.media.util;


public class CustomizableThreadCreator {

    private String threadNamePrefix;

    private int threadPriority = Thread.NORM_PRIORITY;

    private boolean daemon = false;

    private ThreadGroup threadGroup;

    private int threadCount = 0;

    private final Object threadCountMonitor = new Object();

    /**
     * Create a new CustomizableThreadCreator with default thread name prefix.
     */

    public CustomizableThreadCreator() {

        this.threadNamePrefix = getDefaultThreadNamePrefix();
    }

    /**
     * Create a new CustomizableThreadCreator with the given thread name prefix.
     *
     * @param threadNamePrefix the prefix to use for the names of newly created threads
     */

    public CustomizableThreadCreator(String threadNamePrefix) {

        this.threadNamePrefix = (threadNamePrefix != null ? threadNamePrefix : getDefaultThreadNamePrefix());
    }

    /**
     * Specify the prefix to use for the names of newly created threads.
     * <p>
     * Default is "SimpleAsyncTaskExecutor-".
     */

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = (threadNamePrefix != null ? threadNamePrefix : getDefaultThreadNamePrefix());
    }

    /**
     * Return the thread name prefix to use for the names of newly
     * <p>
     * created threads.
     */

    public String getThreadNamePrefix() {

        return this.threadNamePrefix;
    }

    /**
     * Set the priority of the threads that this factory creates.
     * <p>
     * Default is 5.
     *
     * @see Thread#NORM_PRIORITY
     */

    public void setThreadPriority(int threadPriority) {

        this.threadPriority = threadPriority;
    }

    /**
     * Return the priority of the threads that this factory creates.
     */

    public int getThreadPriority() {

        return this.threadPriority;
    }

    /**
     * Set whether this factory is supposed to create daemon threads,
     * <p>
     * just executing as long as the application itself is running.
     *
     * <p>
     * Default is "false": Concrete factories usually support explicit
     * <p>
     * cancelling. Hence, if the application shuts down, Runnables will
     * <p>
     * by default finish their execution.
     *
     * <p>
     * Specify "true" for eager shutdown of threads which still
     * <p>
     * actively execute a Runnable.
     *
     * @see Thread#setDaemon
     */

    public void setDaemon(boolean daemon) {

        this.daemon = daemon;
    }

    /**
     * Return whether this factory should create daemon threads.
     */

    public boolean isDaemon() {

        return this.daemon;
    }

    /**
     * Specify the name of the thread group that threads should be created in.
     *
     * @see #setThreadGroup
     */

    public void setThreadGroupName(String name) {

        this.threadGroup = new ThreadGroup(name);
    }

    /**
     * Specify the thread group that threads should be created in.
     *
     * @see #setThreadGroupName
     */

    public void setThreadGroup(ThreadGroup threadGroup) {

        this.threadGroup = threadGroup;
    }

    /**
     * Return the thread group that threads should be created in
     * <p>
     * (or <code>null</code>) for the default group.
     */

    public ThreadGroup getThreadGroup() {
        return this.threadGroup;
    }

    /**
     * Template method for the creation of a Thread.
     *
     * <p>
     * Default implementation creates a new Thread for the given
     * <p>
     * Runnable, applying an appropriate thread name.
     *
     * @param runnable the Runnable to execute
     * @see #nextThreadName()
     */

    public Thread createThread(Runnable runnable) {

        Thread thread = new Thread(getThreadGroup(), runnable, nextThreadName());
        thread.setPriority(getThreadPriority());
        thread.setDaemon(isDaemon());
        return thread;
    }

    /**
     * Return the thread name to use for a newly created thread.
     *
     * <p>
     * Default implementation returns the specified thread name prefix
     * <p>
     * with an increasing thread count appended: for example,
     * <p>
     * "SimpleAsyncTaskExecutor-0".
     *
     * @see #getThreadNamePrefix()
     */

    protected String nextThreadName() {

        int threadNumber = 0;

        synchronized (this.threadCountMonitor) {

            this.threadCount++;
            threadNumber = this.threadCount;

        }
        return getThreadNamePrefix() + threadNumber;
    }

    /**
     * Build the default thread name prefix for this factory.
     *
     * @return the default thread name prefix (never <code>null</code>)
     */

    protected String getDefaultThreadNamePrefix() {

        return getClass().getSimpleName() + "-";
    }

}
