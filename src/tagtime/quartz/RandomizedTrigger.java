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

import org.quartz.SimpleTrigger;

import tagtime.random.RandomSequenceGenerator;

/**
 * Derived from <code>{@link SimpleTrigger}</code>.
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
	
	public void setRNG(RandomSequenceGenerator rng);
	
	public String getRNGKey();
	
	Date getFireTimeBefore(Date target);
	
	Date getFireTimeBefore(Date target, boolean alwaysReturn);
}
