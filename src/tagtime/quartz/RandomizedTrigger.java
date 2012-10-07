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

import java.util.Date;

import org.quartz.SimpleTrigger;

import tagtime.random.RandomSequenceGenerator;

/**
 * A trigger that fires at randomized times, with a given average gap
 * between firing.
 * @see RandomizedTriggerImpl
 */
public interface RandomizedTrigger extends SimpleTrigger {
	/**
	 * This represents the time at index 0 in the random sequence. It
	 * should be a time significantly earlier than when the user will run
	 * the program, but it doesn't need to be the start of the Unix
	 * epoch. (The program will iterate from this time to the current
	 * time, which would take too long if it started from 1970.)
	 */
	//1319500800000 is October 25, 2011 at midnight
	public static final long CALENDAR_START = 1319500800000L;
	
	/**
	 * Sets the random number generator. Caution: changing the random
	 * number generator after the trigger has been started will not
	 * update the next scheduled fire time, which will probably result in
	 * temporary erratic behavior.
	 */
	public void setRNG(RandomSequenceGenerator rng);
	
	/**
	 * Returns the key used by the random number generator.
	 */
	public String getRNGKey();
	
	/**
	 * <p>
	 * Returns the first time at which the <code>Trigger</code> will
	 * fire, after the given time. If the trigger will not fire after the
	 * given time and <code>alwaysReturn</code> is false,
	 * <code>null</code> will be returned.
	 * </p>
	 */
	Date getFireTimeAfter(Date target, boolean alwaysReturn);
	
	/**
	 * <p>
	 * Returns the last time at which the <code>Trigger</code> will fire,
	 * before the given time. If the trigger will not fire before the
	 * given time, <code>null</code> will be returned.
	 * </p>
	 */
	Date getFireTimeBefore(Date target);
	
	/**
	 * <p>
	 * Returns the last time at which the <code>Trigger</code> will fire,
	 * before the given time. If the trigger will not fire before the
	 * given time and <code>alwaysReturn</code> is false,
	 * <code>null</code> will be returned.
	 * </p>
	 */
	Date getFireTimeBefore(Date target, boolean alwaysReturn);
}
