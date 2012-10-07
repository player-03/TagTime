/*
 * Copyright 2011-2012 Joseph Cloutier
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

import org.quartz.DateBuilder;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.spi.MutableTrigger;

import tagtime.random.RandomSequenceGenerator;

/**
 * Derived from <code>{@link SimpleScheduleBuilder}</code>.
 */
public class RandomizedScheduleBuilder extends ScheduleBuilder<SimpleTrigger> {
	private long interval = 0;
	private int repeatCount = 0;
	private int misfireInstruction = Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;
	private String rngKey = null;
	
	private RandomizedScheduleBuilder() {
	}
	
	/**
	 * Create a RandomizedScheduleBuilder.
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder randomizedSchedule() {
		return new RandomizedScheduleBuilder();
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat forever with a 1
	 * minute interval.
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatMinutelyForever() {
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInMinutes(1)
					.repeatForever();
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat forever with an
	 * interval of the given number of minutes.
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatMinutelyForever(int minutes) {
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInMinutes(minutes)
					.repeatForever();
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat forever with a 1
	 * second interval.
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatSecondlyForever() {
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInSeconds(1)
					.repeatForever();
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat forever with an
	 * interval of the given number of seconds.
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatSecondlyForever(int seconds) {
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInSeconds(seconds)
					.repeatForever();
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat forever with a 1
	 * hour interval.
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatHourlyForever() {
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInHours(1)
					.repeatForever();
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat forever with an
	 * interval of the given number of hours.
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatHourlyForever(int hours) {
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInHours(hours)
					.repeatForever();
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat the given number
	 * of times - 1 with a 1 minute interval.
	 * <p>
	 * Note: Total count = 1 (at start time) + repeat count
	 * </p>
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatMinutelyForTotalCount(int count) {
		if(count < 1)
			throw new IllegalArgumentException(
						"Total count of firings must be at least one! Given count: " + count);
		
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInMinutes(1)
					.withRepeatCount(count - 1);
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat the given number
	 * of times - 1 with an interval of the given number of minutes.
	 * <p>
	 * Note: Total count = 1 (at start time) + repeat count
	 * </p>
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatMinutelyForTotalCount(int count, int minutes) {
		if(count < 1)
			throw new IllegalArgumentException(
						"Total count of firings must be at least one! Given count: " + count);
		
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInMinutes(minutes)
					.withRepeatCount(count - 1);
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat the given number
	 * of times - 1 with a 1 second interval.
	 * <p>
	 * Note: Total count = 1 (at start time) + repeat count
	 * </p>
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatSecondlyForTotalCount(int count) {
		if(count < 1)
			throw new IllegalArgumentException(
						"Total count of firings must be at least one! Given count: " + count);
		
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInSeconds(1)
					.withRepeatCount(count - 1);
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat the given number
	 * of times - 1 with an interval of the given number of seconds.
	 * <p>
	 * Note: Total count = 1 (at start time) + repeat count
	 * </p>
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatSecondlyForTotalCount(int count, int seconds) {
		if(count < 1)
			throw new IllegalArgumentException(
						"Total count of firings must be at least one! Given count: " + count);
		
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInSeconds(seconds)
					.withRepeatCount(count - 1);
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat the given number
	 * of times - 1 with a 1 hour interval.
	 * <p>
	 * Note: Total count = 1 (at start time) + repeat count
	 * </p>
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatHourlyForTotalCount(int count) {
		if(count < 1)
			throw new IllegalArgumentException(
						"Total count of firings must be at least one! Given count: " + count);
		
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInHours(1)
					.withRepeatCount(count - 1);
		
		return sb;
	}
	
	/**
	 * Create a RandomizedScheduleBuilder set to repeat the given number
	 * of times - 1 with an interval of the given number of hours.
	 * <p>
	 * Note: Total count = 1 (at start time) + repeat count
	 * </p>
	 * @return the new RandomizedScheduleBuilder
	 */
	public static RandomizedScheduleBuilder repeatHourlyForTotalCount(int count, int hours) {
		if(count < 1)
			throw new IllegalArgumentException(
						"Total count of firings must be at least one! Given count: " + count);
		
		RandomizedScheduleBuilder sb = randomizedSchedule()
					.withIntervalInHours(hours)
					.withRepeatCount(count - 1);
		
		return sb;
	}
	
	/**
	 * Build the actual Trigger -- NOT intended to be invoked by end
	 * users, but will rather be invoked by a TriggerBuilder which this
	 * ScheduleBuilder is given to.
	 * @see TriggerBuilder#withSchedule(ScheduleBuilder)
	 */
	@Override
	public MutableTrigger build() {
		RandomizedTriggerImpl rt = new RandomizedTriggerImpl();
		rt.setRepeatInterval(interval);
		rt.setRepeatCount(repeatCount);
		rt.setMisfireInstruction(misfireInstruction);
		
		try {
			rt.setRNG(new RandomSequenceGenerator(rngKey));
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return rt;
	}
	
	/**
	 * Specify a repeat interval in milliseconds.
	 * @param intervalInMillis the number of seconds at which the trigger
	 *            should repeat.
	 * @return the updated RandomizedScheduleBuilder
	 * @see RandomizedTrigger#getRepeatInterval()
	 * @see #withRepeatCount(int)
	 */
	public RandomizedScheduleBuilder withIntervalInMilliseconds(long intervalInMillis) {
		this.interval = intervalInMillis;
		return this;
	}
	
	/**
	 * Specify a repeat interval in seconds - which will then be
	 * multiplied by 1000 to produce milliseconds.
	 * @param intervalInSeconds the number of seconds at which the
	 *            trigger should repeat.
	 * @return the updated RandomizedScheduleBuilder
	 * @see RandomizedTrigger#getRepeatInterval()
	 * @see #withRepeatCount(int)
	 */
	public RandomizedScheduleBuilder withIntervalInSeconds(int intervalInSeconds) {
		this.interval = intervalInSeconds * 1000L;
		return this;
	}
	
	/**
	 * Specify a repeat interval in minutes - which will then be
	 * multiplied by 60 * 1000 to produce milliseconds.
	 * @param intervalInMinutes the number of seconds at which the
	 *            trigger should repeat.
	 * @return the updated RandomizedScheduleBuilder
	 * @see RandomizedTrigger#getRepeatInterval()
	 * @see #withRepeatCount(int)
	 */
	public RandomizedScheduleBuilder withIntervalInMinutes(int intervalInMinutes) {
		this.interval = intervalInMinutes * DateBuilder.MILLISECONDS_IN_MINUTE;
		return this;
	}
	
	/**
	 * Specify a repeat interval in minutes - which will then be
	 * multiplied by 60 * 60 * 1000 to produce milliseconds.
	 * @param intervalInHours the number of seconds at which the trigger
	 *            should repeat.
	 * @return the updated RandomizedScheduleBuilder
	 * @see RandomizedTrigger#getRepeatInterval()
	 * @see #withRepeatCount(int)
	 */
	public RandomizedScheduleBuilder withIntervalInHours(int intervalInHours) {
		this.interval = intervalInHours * DateBuilder.MILLISECONDS_IN_HOUR;
		return this;
	}
	
	/**
	 * Specify a the number of time the trigger will repeat - total
	 * number of firings will be this number + 1.
	 * @param triggerRepeatCount the number of seconds at which the
	 *            trigger should repeat.
	 * @return the updated RandomizedScheduleBuilder
	 * @see RandomizedTrigger#getRepeatCount()
	 * @see #repeatForever()
	 */
	public RandomizedScheduleBuilder withRepeatCount(int triggerRepeatCount) {
		this.repeatCount = triggerRepeatCount;
		return this;
	}
	
	/**
	 * Specify that the trigger will repeat indefinitely.
	 * @return the updated RandomizedScheduleBuilder
	 * @see RandomizedTrigger#getRepeatCount()
	 * @see RandomizedTrigger#REPEAT_INDEFINITELY
	 * @see #withIntervalInMilliseconds(long)
	 * @see #withIntervalInSeconds(int)
	 * @see #withIntervalInMinutes(int)
	 * @see #withIntervalInHours(int)
	 */
	public RandomizedScheduleBuilder repeatForever() {
		this.repeatCount = RandomizedTrigger.REPEAT_INDEFINITELY;
		return this;
	}
	
	public RandomizedScheduleBuilder withRNGKey(String key) {
		this.rngKey = key;
		return this;
	}
	
	/**
	 * If the Trigger misfires, use the
	 * {@link Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY}
	 * instruction.
	 * @return the updated RandomizedScheduleBuilder
	 * @see Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
	 */
	public RandomizedScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires() {
		misfireInstruction = Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
		return this;
	}
	
	/**
	 * If the Trigger misfires, use the
	 * {@link SimpleTrigger#MISFIRE_INSTRUCTION_FIRE_NOW} instruction.
	 * @return the updated RandomizedScheduleBuilder
	 * @see SimpleTrigger#MISFIRE_INSTRUCTION_FIRE_NOW
	 */
	public RandomizedScheduleBuilder withMisfireHandlingInstructionFireNow() {
		misfireInstruction = SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW;
		return this;
	}
	
	/**
	 * If the Trigger misfires, use the
	 * {@link SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT}
	 * instruction.
	 * @return the updated RandomizedScheduleBuilder
	 * @see SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT
	 */
	public RandomizedScheduleBuilder withMisfireHandlingInstructionNextWithExistingCount() {
		misfireInstruction =
					SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT;
		return this;
	}
	
	/**
	 * If the Trigger misfires, use the
	 * {@link SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT}
	 * instruction.
	 * @return the updated RandomizedScheduleBuilder
	 * @see SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT
	 */
	public RandomizedScheduleBuilder withMisfireHandlingInstructionNextWithRemainingCount() {
		misfireInstruction =
					SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT;
		return this;
	}
	
	/**
	 * If the Trigger misfires, use the
	 * {@link SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT}
	 * instruction.
	 * @return the updated RandomizedScheduleBuilder
	 * @see SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT
	 */
	public RandomizedScheduleBuilder withMisfireHandlingInstructionNowWithExistingCount() {
		misfireInstruction =
					SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT;
		return this;
	}
	
	/**
	 * If the Trigger misfires, use the
	 * {@link SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT}
	 * instruction.
	 * @return the updated RandomizedScheduleBuilder
	 * @see SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT
	 */
	public RandomizedScheduleBuilder withMisfireHandlingInstructionNowWithRemainingCount() {
		misfireInstruction =
					SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT;
		return this;
	}
}
