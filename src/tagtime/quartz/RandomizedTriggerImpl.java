/*
 * Copyright 2011 Joseph Cloutier, Daniel Reeves, Bethany Soule
 * 
 * This file is part of TagTime.
 * 
 * TagTime is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * TagTime is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TagTime. If not, see <http://www.gnu.org/licenses/>.
 */

package tagtime.quartz;

import java.util.Date;

import org.quartz.Calendar;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import tagtime.random.RandomSequenceGenerator;

/**
 * An implementation of {@link RandomizedTrigger}.
 */
public class RandomizedTriggerImpl extends SimpleTriggerImpl
			implements RandomizedTrigger {
	private static final long serialVersionUID = 5800564916338048242L;
	
	/**
	 * The pseudo-random generator used to determine the length of time
	 * the trigger waits in between being triggered.
	 */
	private RandomSequenceGenerator rng = null;
	
	/**
	 * The index corresponding to the cached time value. Note that this
	 * is not required to match the number of times the trigger has been
	 * fired, as long as it corresponds to the cached time.
	 * @see RandomizedTriggerImpl#cachedTimeOfExecution
	 */
	private long cachedIndex = 0;
	
	/**
	 * <p>
	 * The time corresponding to the cached index. For example, an index
	 * of 0 would represent a trigger that has been fired once, in which
	 * case <code>cachedTimeOfExecution</code> would equal
	 * <code>getStartTime()</code> (remember, the trigger is always fired
	 * at the start time). An index of 2 would represent a trigger that
	 * was fired three times, and the cached time would be when it was
	 * fired the third time.
	 * </p>
	 * <p>
	 * If this is null, then no value is cached and
	 * <code>cachedIndex</code> is invalid. (This is a <code>Long</code>
	 * rather than a <code>long</code> specifically so that it can be
	 * null.) In this case, the index will be set to 0 and the time set
	 * to the start of the TagTime calendar.
	 * </p>
	 * @see RandomizedTriggerImpl#cachedIndex
	 * @see RandomizedTrigger#CALENDAR_START
	 */
	private Long cachedTimeOfExecution = null;
	
	/**
	 * Creates a <code>RandomizedTrigger</code> with the default
	 * settings.
	 */
	public RandomizedTriggerImpl() {
		super();
	}
	
	/**
	 * Increments <code>cachedIndex</code> and updates
	 * <code>cachedTimeOfExecution</code> accordingly.
	 */
	private void incrementCachedValues() {
		if(cachedTimeOfExecution == null) {
			resetCachedValues();
		}
		
		cachedTimeOfExecution += getTimeElapsedAfter(cachedIndex);
		cachedIndex++;
	}
	
	/**
	 * Decrements <code>cachedIndex</code> and updates
	 * <code>cachedTimeOfExecution</code> accordingly.
	 */
	private void decrementCachedValues() {
		if(cachedTimeOfExecution == null) {
			resetCachedValues();
		}
		
		cachedIndex--;
		cachedTimeOfExecution -= getTimeElapsedAfter(cachedIndex);
	}
	
	/**
	 * Resets and/or initializes the cached values to the default.
	 */
	private void resetCachedValues() {
		cachedIndex = 0;
		cachedTimeOfExecution = RandomizedTrigger.CALENDAR_START;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRNG(RandomSequenceGenerator rng) {
		this.rng = rng;
		resetCachedValues();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRNGKey() {
		return rng.getKey();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void triggered(Calendar calendar) {
		super.triggered(calendar);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * Additionally, if this trigger has been triggered the specified
	 * number of times, this function will return null, regardless of
	 * whether the given time is valid.
	 * </p>
	 */
	@Override
	public Date getFireTimeAfter(Date target) {
		if((getTimesTriggered() > getRepeatCount())
					&& (getRepeatCount() != REPEAT_INDEFINITELY)) {
			return null;
		}
		
		if(target == null) {
			target = new Date();
		}
		
		if(getRepeatCount() == 0 && target.compareTo(getStartTime()) >= 0) {
			return null;
		}
		
		long startTime = getStartTime().getTime();
		long targetTime = target.getTime();
		long endTime = (getEndTime() == null) ? Long.MAX_VALUE : getEndTime().getTime();
		
		if(targetTime >= endTime) {
			return null;
		}
		
		if(targetTime < startTime) {
			return new Date(startTime);
		}
		
		findFireTimeAfter(targetTime);
		
		if(cachedTimeOfExecution >= endTime) {
			return null;
		}
		
		return new Date(cachedTimeOfExecution);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Date getFireTimeAfter(Date target, boolean alwaysReturn) {
		if(!alwaysReturn) {
			return getFireTimeAfter(target);
		}
		
		findFireTimeAfter(target.getTime());
		
		return new Date(cachedTimeOfExecution);
	}
	
	/**
	 * Updates the cached values to point to the first fire time after
	 * the given time. Unlike <code>getFireTimeAfter()</code>, this will
	 * always update the values.
	 */
	private void findFireTimeAfter(long target) {
		if(cachedTimeOfExecution == null) {
			resetCachedValues();
		}
		
		//if the current cached value is currently after the target time,
		//decrement it until it goes past
		while(cachedTimeOfExecution > target) {
			decrementCachedValues();
		}
		
		//increment the time until it is after the target time
		while(cachedTimeOfExecution <= target) {
			incrementCachedValues();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Date getFireTimeBefore(Date target) {
		if(target.getTime() < getStartTime().getTime()) {
			return null;
		}
		
		findFireTimeBefore(target.getTime());
		
		return new Date(cachedTimeOfExecution);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Date getFireTimeBefore(Date target, boolean alwaysReturn) {
		if(!alwaysReturn) {
			return getFireTimeBefore(target);
		}
		
		findFireTimeBefore(target.getTime());
		
		return new Date(cachedTimeOfExecution);
	}
	
	/**
	 * Updates the cached values to point to the last fire time before
	 * the given time. Unlike <code>getFireTimeBefore()</code>, this will
	 * always update the values.
	 */
	private void findFireTimeBefore(long target) {
		if(cachedTimeOfExecution == null) {
			resetCachedValues();
		}
		
		//if the current cached value is currently before the target time,
		//increment it until it goes past
		while(cachedTimeOfExecution < target) {
			incrementCachedValues();
		}
		
		//decrement the time until it is before the target time
		while(cachedTimeOfExecution >= target) {
			decrementCachedValues();
		}
	}
	
	@Override
	public int computeNumTimesFiredBetween(Date start, Date end) {
		if(getRepeatInterval() < 1) {
			return 0;
		}
		
		int count = 0;
		long endTime = end.getTime();
		
		//start at the first fire time in range
		findFireTimeAfter(start.getTime());
		
		//count the remaining fire times
		for(; cachedTimeOfExecution < endTime; incrementCachedValues()) {
			count++;
		}
		
		return count;
	}
	
	/**
	 * Get a {@link ScheduleBuilder} that is configured to produce a
	 * schedule identical to this trigger's schedule.
	 */
	@Override
	public ScheduleBuilder<SimpleTrigger> getScheduleBuilder() {
		RandomizedScheduleBuilder sb = RandomizedScheduleBuilder.randomizedSchedule()
					.withIntervalInMilliseconds(getRepeatInterval())
					.withRepeatCount(getRepeatCount())
					.withRNGKey(getRNGKey());
		
		switch(getMisfireInstruction()) {
			case MISFIRE_INSTRUCTION_FIRE_NOW:
				sb.withMisfireHandlingInstructionFireNow();
				break;
			case MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT:
				sb.withMisfireHandlingInstructionNextWithExistingCount();
				break;
			case MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT:
				sb.withMisfireHandlingInstructionNextWithRemainingCount();
				break;
			case MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT:
				sb.withMisfireHandlingInstructionNowWithExistingCount();
				break;
			case MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT:
				sb.withMisfireHandlingInstructionNowWithRemainingCount();
				break;
		}
		
		return sb;
	}
	
	/**
	 * Converts random values between 0 and 1 to an exponential
	 * distribution with a mean of this trigger's repeat interval.
	 */
	private long convertToExponentialDistribution(double randomValue) {
		//values too close to 0 will produce absurdly large numbers
		//(and 0 itself will produce Infinity)
		//TODO: (Possibly) implement a more elegant way of enforcing an upper bound.
		if(randomValue < 0.00000001) {
			randomValue = 0.00000001;
		}
		
		return (long) (getRepeatInterval() * -1 * Math.log(randomValue));
	}
	
	/**
	 * <p>
	 * Finds the amount of time in seconds that the trigger will wait,
	 * once it is triggered the given number of times. For example, if
	 * <code>index</code> is 2, this returns how much time will pass
	 * between the second and third times the trigger is triggered.
	 * </p>
	 * <p>
	 * <b>Caution:</b> If this trigger uses a calendar and skips being
	 * triggered as a result, this method will fail to take that into
	 * account. For example, if the trigger is triggered five times and
	 * then skipped twice, getTimesTriggered() will return 5, but passing
	 * 5 to timeElapsedAfter() will return a value corresponding to part
	 * of the skipped time period.
	 * </p>
	 */
	public long getTimeElapsedAfter(long index) {
		return convertToExponentialDistribution(rng.getValue(index));
	}
}
