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

package tagtime.beeminder;

import java.io.File;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import tagtime.TagTime;
import tagtime.log.LogParser;
import tagtime.settings.SettingType;
import tagtime.util.TagMatcher;

/**
 * A set of data for a single Beeminder graph. This class handles the
 * actual parsing and submission of data.
 */
public class BeeminderGraphData {
	private static final List<String> RESET_STRINGS =
				Arrays.asList(new String[] {"Reset today", "Unfroze today"});
	
	/**
	 * Submit time spent as hours, rounded to the given number of decimal
	 * places.
	 */
	private final DecimalFormat hourFormatter;
	
	/**
	 * 10 ^ (the number of decimal places being used)
	 * <p>
	 * To compare two amounts of time, multiply them by this value, then
	 * round them and see if the rounded values are equal.
	 * </p>
	 */
	private final int roundingMultiplier;
	
	public final TagTime tagTimeInstance;
	
	/**
	 * The name of the Beeminder graph.
	 */
	public final String graphName;
	
	private final TagMatcher tagMatcher;
	
	/**
	 * Parses a graph's data and sets up a .bee file to track which tags
	 * have been submitted to the graph.
	 * @param username The user's Beeminder username.
	 * @param dataEntry The data entry for the current graph. This must
	 *            be in the format "graphName|tags".
	 */
	public BeeminderGraphData(TagTime tagTimeInstance, String username, String dataEntry) {
		if(username == null || dataEntry == null) {
			throw new IllegalArgumentException("Both parameters to the " +
						"BeeminderGraphData constructor must be defined.");
		}
		
		this.tagTimeInstance = tagTimeInstance;
		
		int decimalDigits = tagTimeInstance.settings
					.getIntValue(SettingType.PRECISION);
		hourFormatter = new DecimalFormat();
		hourFormatter.setGroupingUsed(false);
		hourFormatter.setRoundingMode(RoundingMode.HALF_UP);
		hourFormatter.setMaximumFractionDigits(decimalDigits);
		roundingMultiplier = (int) Math.pow(10, decimalDigits);
		
		//find the graph name
		int graphDelim = dataEntry.indexOf('|');
		if(graphDelim < 0 || graphDelim >= dataEntry.length() - 1) {
			throw new IllegalArgumentException("dataEntry must be in " +
						"format \"graphname|tags\"");
		}
		graphName = dataEntry.substring(0, graphDelim);
		
		//get the tags
		String[] tags = dataEntry.substring(graphDelim + 1).split("\\s+");
		
		List<String> acceptedTags = new ArrayList<String>(3);
		List<String> rejectedTags = new ArrayList<String>(0);
		
		//enter the tags in the correct lists
		for(String tag : tags) {
			if(tag.charAt(0) == '-') {
				rejectedTags.add(tag.substring(1).toLowerCase());
			} else {
				acceptedTags.add(tag.toLowerCase());
			}
		}
		
		//make sure some tags were entered
		if(acceptedTags.size() == 0 && rejectedTags.size() == 0) {
			throw new IllegalArgumentException("No tags provided.");
		}
		
		tagMatcher = new TagMatcher(acceptedTags, rejectedTags);
	}
	
	/**
	 * Submits all matching pings from the given file that have not yet
	 * been submitted. Currently, it estimates the time by assuming the
	 * user spent all their time between two pings on the activity
	 * recorded by the second ping.
	 * @param logFile A reference to the log file to read from.
	 */
	public void submitPings(File logFile) {
		HttpClient client = new DefaultHttpClient();
		
		DataPoint beeminderDataPoint;
		List<DataPoint> beeminderDataPoints = BeeminderAPI.fetchAllDataPoints(client,
											graphName, tagTimeInstance);
		if(beeminderDataPoints == null) {
			client.getConnectionManager().shutdown();
			return;
		}
		
		long resetDate = Math.max(beeminderDataPoints.get(0).timestamp,
					BeeminderAPI.fetchResetDate(client, graphName,
								tagTimeInstance));
		resetDate = DataPoint.getStartOfDay(resetDate);
		
		//check the Beeminder list for data points that fall on the same
		//day, and mark those data points as needing to be merged (the
		//first point gets marked as needing to be removed, and the
		//second gets marked as needing to be updated)
		if(beeminderDataPoints.size() > 1) {
			for(int i = 1; i < beeminderDataPoints.size(); i++) {
				beeminderDataPoint = beeminderDataPoints.get(i);
				
				//Special case: each time the graph is reset, Beeminder
				//adds a "Reset today" data point. Do not remove these,
				//and use them to set the reset date if necessary.
				if(RESET_STRINGS.contains(beeminderDataPoint.comment)) {
					if(beeminderDataPoint.timestamp > resetDate) {
						resetDate = beeminderDataPoint.timestamp;
					}
					
					//skip the check after this one as well
					i++;
					
					//if this data point has any "hours" value besides 0,
					//insert a new data point with that value, and set
					//this one back to 0
					if(beeminderDataPoint.hours != 0) {
						//i is already 1 greater than the reset data
						//point's index
						beeminderDataPoints.add(i, new DataPoint(beeminderDataPoint.timestamp,
									beeminderDataPoint.hours));
						beeminderDataPoint.hours = 0;
						beeminderDataPoint.toBeUpdated = true;
					}
				} else {
					beeminderDataPoint.checkMerge(beeminderDataPoints.get(i - 1));
				}
			}
		}
		
		DataPoint actualDataPoint;
		List<DataPoint> actualDataPoints = LogParser.parse(logFile, tagMatcher);
		
		/*
		 * Merge actualDataPoints into beeminderDataPoints to produce a
		 * single list with all relevant data points.
		 * 
		 * This resulting list will consist of two types of data points:
		 * those that should be on Beeminder (whether or not they already
		 * are), and those that are currently on Beeminder and need to be
		 * removed.
		 * 
		 * In other words, this process annotates beeminderDataPoints
		 * with all the changes needed to make it match actualDataPoints.
		 * 
		 * (Note that both lists are already sorted by timestamp.)
		 */

		//skipping the first Beeminder data point
		int i1 = 1, i2 = 0;
		
		//first part of the merge: compare data points from the two lists
		//until one list runs out
		while(i1 < beeminderDataPoints.size() && i2 < actualDataPoints.size()) {
			beeminderDataPoint = beeminderDataPoints.get(i1);
			
			//skip data points already marked as "to be removed," and
			//data points representing the graph being reset
			if(beeminderDataPoint.isToBeRemoved()
						|| RESET_STRINGS.contains(beeminderDataPoint.comment)) {
				i1++;
				continue;
			}
			
			actualDataPoint = actualDataPoints.get(i2);
			
			//if the data points are for the same day, make sure the time
			//values match, then advance both indices
			if(beeminderDataPoint.timestamp == actualDataPoint.timestamp) {
				//if the time values don't match, update the Beeminder
				//data point
				if(!roundedValuesEqual(beeminderDataPoint.hours, actualDataPoint.hours)) {
					beeminderDataPoint.hours = actualDataPoint.hours;
					beeminderDataPoint.toBeUpdated = true;
				}
				
				i1++;
				i2++;
			}

			//if the Beeminder data point comes before the actual data
			//point, then that Beeminder data point corresponds to a data
			//point that no longer exists
			else if(beeminderDataPoint.timestamp < actualDataPoint.timestamp) {
				beeminderDataPoint.toBeRemoved = true;
				i1++;
			}

			//if the actual data point comes first, then it needs to be
			//created in front of the current Beeminder data point,
			//UNLESS this is before the graph was last reset
			else if(actualDataPoint.timestamp >= resetDate) {
				beeminderDataPoints.add(i1, actualDataPoint);
				
				//this produces a match
				i1++;
				i2++;
			} else {
				//skip all pre-reset data points
				i2++;
			}
		}
		
		//second part of the merge: remove unmatched Beeminder data
		//points, except for the first
		for(i1 = i1 == 0 ? 1 : i1; i1 < beeminderDataPoints.size(); i1++) {
			beeminderDataPoint = beeminderDataPoints.get(i1);
			
			//do not remove "reset" data points
			if(!RESET_STRINGS.contains(beeminderDataPoint.comment)) {
				beeminderDataPoints.get(i1).toBeRemoved = true;
			}
		}
		
		//third part of the merge: add unmatched actual data points that
		//come after the reset date
		for(; i2 < actualDataPoints.size(); i2++) {
			actualDataPoint = actualDataPoints.get(i2);
			if(actualDataPoint.timestamp >= resetDate) {
				beeminderDataPoints.add(actualDataPoints.get(i2));
			}
		}
		
		//submit the changes to all data points in the merged list
		for(DataPoint p : beeminderDataPoints) {
			if(!p.submit(client, tagTimeInstance, graphName, hourFormatter)) {
				break;
			}
		}
		
		client.getConnectionManager().shutdown();
	}
	
	private boolean roundedValuesEqual(double time1, double time2) {
		return Math.round(time1 * roundingMultiplier) == Math.round(time2 * roundingMultiplier);
	}
}
