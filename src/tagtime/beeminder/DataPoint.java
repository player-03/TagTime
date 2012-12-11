/*
 * Copyright 2012 Joseph Cloutier
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

package tagtime.beeminder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.http.client.HttpClient;

/**
 * The relevant information about a single data point on Beeminder.
 */
public class DataPoint {
	private static final Calendar calendar = new GregorianCalendar();
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy'/'MM'/'dd");
	
	/**
	 * The identifier for the data point on Beeminder. This will be null
	 * if the data point does not yet exist on Beeminder.
	 */
	public String id;
	
	public boolean toBeRemoved = false;
	public boolean toBeUpdated = false;
	
	/**
	 * The Unix timestamp (in seconds) of the very beginning of the day
	 * on which this data point occurred.
	 */
	public final long timestamp;
	
	/**
	 * The time, in hours, spent doing the relevant activity or
	 * activities on the day of this data point.
	 */
	public double hours;
	
	public String comment;
	
	public DataPoint(long timestamp, double hours) {
		id = null;
		this.timestamp = getStartOfDay(timestamp);
		this.hours = hours;
		comment = "";
	}
	
	public DataPoint(String id, long timestamp, double hours, String comment) {
		this.id = id;
		this.timestamp = getStartOfDay(timestamp);
		this.hours = hours;
		this.comment = comment;
	}
	
	@Override
	public String toString() {
		return (isToBeRemoved() ? "removing "
					: isToBeCreated() ? "creating "
								: isToBeUpdated() ? "updating " : "")
					+ DATE_FORMAT.format(new Date(timestamp * 1000)) + ": " + hours;
	}
	
	/**
	 * If this data point falls on the same day as the given one, this
	 * function copies that data point's data into this one, marks this
	 * one as "to be updated," and marks the other for removal.
	 */
	public void checkMerge(DataPoint other) {
		if(other.timestamp == timestamp) {
			hours += other.hours;
			toBeUpdated = true;
			other.toBeRemoved = true;
			
			//merge the comments
			if(other.comment.length() > 0) {
				if(comment.length() > 0) {
					comment = other.comment + "; " + comment;
				} else {
					comment = other.comment;
				}
			}
		}
	}
	
	/**
	 * Submits this data point's information to Beeminder, if necessary.
	 * @param saveID Whether to save the data point's ID as returned by
	 *            Beeminder. This only applies when creating a new data
	 *            point.
	 * @return Whether the operation completed successfully. If this is
	 *         false, it will most likely be counterproductive to try
	 *         submitting further data.
	 */
	public boolean submit(HttpClient client, BeeminderGraph graph,
				boolean saveID) {
		if(isToBeRemoved()) {
			return BeeminderAPI.deleteDataPoint(client, graph, this);
		} else if(isToBeCreated()) {
			return BeeminderAPI.createDataPoint(client, graph, this, saveID);
		} else if(isToBeUpdated()) {
			return BeeminderAPI.updateDataPoint(client, graph, this);
		}
		
		return true;
	}
	
	public boolean isToBeCreated() {
		return id == null;
	}
	
	public boolean isToBeUpdated() {
		return toBeUpdated;
	}
	
	public boolean isToBeRemoved() {
		return toBeRemoved;
	}
	
	/**
	 * Synchronized just in case, because it shares the calendar object.
	 * TODO: Is this necessary and does it slow anything down?
	 * @return The UNIX timestamp representing midnight at the start of
	 *         the day the given timestamp falls on.
	 */
	public static synchronized long getStartOfDay(long timestamp) {
		//timestamps are stored in seconds, but the Calendar class uses
		//milliseconds
		calendar.setTimeInMillis(timestamp * 1000);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		
		//convert back from milliseconds to seconds
		return calendar.getTimeInMillis() / 1000;
	}
}
