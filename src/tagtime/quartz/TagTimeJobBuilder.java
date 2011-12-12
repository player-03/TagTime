package tagtime.quartz;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.jobs.NoOpJob;
import org.quartz.utils.Key;

import tagtime.TagTime;

public class TagTimeJobBuilder {
	private JobKey key;
	private String description;
	private Class<? extends Job> jobClass = NoOpJob.class;
	private boolean durability;
	private boolean shouldRecover;
	private TagTime tagTimeInstance;
	
	private JobDataMap jobDataMap = new JobDataMap();
	
	private TagTimeJobBuilder() {
	}
	
	/**
	 * Create a TagTimeJobBuilder with which to define a
	 * <code>JobDetail</code>.
	 * @return a new TagTimeJobBuilder
	 */
	public static TagTimeJobBuilder newJob() {
		return new TagTimeJobBuilder();
	}
	
	/**
	 * Create a TagTimeJobBuilder with which to define a
	 * <code>JobDetail</code>, and set the class name of the
	 * <code>Job</code> to be executed.
	 * @return a new TagTimeJobBuilder
	 */
	public static TagTimeJobBuilder newJob(Class<? extends Job> jobClass) {
		TagTimeJobBuilder b = new TagTimeJobBuilder();
		b.ofType(jobClass);
		return b;
	}
	
	/**
	 * Produce the <code>JobDetail</code> instance defined by this
	 * <code>TagTimeJobBuilder</code>.
	 * @return the defined JobDetail.
	 */
	public JobDetail build() {
		
		TagTimeJobDetailImpl job = new TagTimeJobDetailImpl();
		
		job.setJobClass(jobClass);
		job.setDescription(description);
		if(key == null)
			key = new JobKey(Key.createUniqueName(null), null);
		job.setKey(key);
		job.setDurability(durability);
		job.setRequestsRecovery(shouldRecover);
		job.setTagTimeInstance(tagTimeInstance);
		
		if(!jobDataMap.isEmpty())
			job.setJobDataMap(jobDataMap);
		
		return job;
	}
	
	/**
	 * Use a <code>JobKey</code> with the given name and default group to
	 * identify the JobDetail.
	 * <p>
	 * If none of the 'withIdentity' methods are set on the
	 * TagTimeJobBuilder, then a random, unique JobKey will be generated.
	 * </p>
	 * @param name the name element for the Job's JobKey
	 * @return the updated TagTimeJobBuilder
	 * @see JobKey
	 * @see JobDetail#getKey()
	 */
	public TagTimeJobBuilder withIdentity(String name) {
		key = new JobKey(name, null);
		return this;
	}
	
	/**
	 * Use a <code>JobKey</code> with the given name and group to
	 * identify the JobDetail.
	 * <p>
	 * If none of the 'withIdentity' methods are set on the
	 * TagTimeJobBuilder, then a random, unique JobKey will be generated.
	 * </p>
	 * @param name the name element for the Job's JobKey
	 * @param group the group element for the Job's JobKey
	 * @return the updated TagTimeJobBuilder
	 * @see JobKey
	 * @see JobDetail#getKey()
	 */
	public TagTimeJobBuilder withIdentity(String name, String group) {
		key = new JobKey(name, group);
		return this;
	}
	
	/**
	 * Use a <code>JobKey</code> to identify the JobDetail.
	 * <p>
	 * If none of the 'withIdentity' methods are set on the
	 * TagTimeJobBuilder, then a random, unique JobKey will be generated.
	 * </p>
	 * @param jobKey the Job's JobKey
	 * @return the updated TagTimeJobBuilder
	 * @see JobKey
	 * @see JobDetail#getKey()
	 */
	public TagTimeJobBuilder withIdentity(JobKey jobKey) {
		this.key = jobKey;
		return this;
	}
	
	/**
	 * Set the given (human-meaningful) description of the Job.
	 * @param jobDescription the description for the Job
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#getDescription()
	 */
	public TagTimeJobBuilder withDescription(String jobDescription) {
		this.description = jobDescription;
		return this;
	}
	
	public TagTimeJobBuilder withTagTimeInstance(TagTime instance) {
		tagTimeInstance = instance;
		return this;
	}
	
	/**
	 * Set the class which will be instantiated and executed when a
	 * Trigger fires that is associated with this JobDetail.
	 * @param jobClazz a class implementing the Job interface.
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#getJobClass()
	 */
	public TagTimeJobBuilder ofType(Class<? extends Job> jobClazz) {
		this.jobClass = jobClazz;
		return this;
	}
	
	/**
	 * Instructs the <code>Scheduler</code> whether or not the
	 * <code>Job</code> should be re-executed if a 'recovery' or
	 * 'fail-over' situation is encountered.
	 * <p>
	 * If not explicitly set, the default value is <code>false</code>.
	 * </p>
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#requestsRecovery()
	 */
	public TagTimeJobBuilder requestRecovery() {
		this.shouldRecover = true;
		return this;
	}
	
	/**
	 * Instructs the <code>Scheduler</code> whether or not the
	 * <code>Job</code> should be re-executed if a 'recovery' or
	 * 'fail-over' situation is encountered.
	 * <p>
	 * If not explicitly set, the default value is <code>false</code>.
	 * </p>
	 * @param jobShouldRecover
	 * @return the updated TagTimeJobBuilder
	 */
	public TagTimeJobBuilder requestRecovery(boolean jobShouldRecover) {
		this.shouldRecover = jobShouldRecover;
		return this;
	}
	
	/**
	 * Whether or not the <code>Job</code> should remain stored after it
	 * is orphaned (no <code>{@link Trigger}s</code> point to it).
	 * <p>
	 * If not explicitly set, the default value is <code>false</code>.
	 * </p>
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#isDurable()
	 */
	public TagTimeJobBuilder storeDurably() {
		this.durability = true;
		return this;
	}
	
	/**
	 * Whether or not the <code>Job</code> should remain stored after it
	 * is orphaned (no <code>{@link Trigger}s</code> point to it).
	 * <p>
	 * If not explicitly set, the default value is <code>false</code>.
	 * </p>
	 * @param jobDurability the value to set for the durability property.
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#isDurable()
	 */
	public TagTimeJobBuilder storeDurably(boolean jobDurability) {
		this.durability = jobDurability;
		return this;
	}
	
	/**
	 * Add the given key-value pair to the JobDetail's {@link JobDataMap}
	 * .
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#getJobDataMap()
	 */
	public TagTimeJobBuilder usingJobData(String dataKey, String value) {
		jobDataMap.put(dataKey, value);
		return this;
	}
	
	/**
	 * Add the given key-value pair to the JobDetail's {@link JobDataMap}
	 * .
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#getJobDataMap()
	 */
	public TagTimeJobBuilder usingJobData(String dataKey, Integer value) {
		jobDataMap.put(dataKey, value);
		return this;
	}
	
	/**
	 * Add the given key-value pair to the JobDetail's {@link JobDataMap}
	 * .
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#getJobDataMap()
	 */
	public TagTimeJobBuilder usingJobData(String dataKey, Long value) {
		jobDataMap.put(dataKey, value);
		return this;
	}
	
	/**
	 * Add the given key-value pair to the JobDetail's {@link JobDataMap}
	 * .
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#getJobDataMap()
	 */
	public TagTimeJobBuilder usingJobData(String dataKey, Float value) {
		jobDataMap.put(dataKey, value);
		return this;
	}
	
	/**
	 * Add the given key-value pair to the JobDetail's {@link JobDataMap}
	 * .
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#getJobDataMap()
	 */
	public TagTimeJobBuilder usingJobData(String dataKey, Double value) {
		jobDataMap.put(dataKey, value);
		return this;
	}
	
	/**
	 * Add the given key-value pair to the JobDetail's {@link JobDataMap}
	 * .
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#getJobDataMap()
	 */
	public TagTimeJobBuilder usingJobData(String dataKey, Boolean value) {
		jobDataMap.put(dataKey, value);
		return this;
	}
	
	/**
	 * Set the JobDetail's {@link JobDataMap}, adding any values to it
	 * that were already set on this TagTimeJobBuilder using any of the
	 * other 'usingJobData' methods.
	 * @return the updated TagTimeJobBuilder
	 * @see JobDetail#getJobDataMap()
	 */
	public TagTimeJobBuilder usingJobData(JobDataMap newJobDataMap) {
		// add any existing data to this new map
		for(String dataKey : jobDataMap.keySet()) {
			newJobDataMap.put(dataKey, jobDataMap.get(dataKey));
		}
		jobDataMap = newJobDataMap; // set new map as the map to use
		return this;
	}
}
