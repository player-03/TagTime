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

package tagtime.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tagtime.beeminder.DataPoint;
import tagtime.util.ITagMatcher;

public class LogParser {
	/**
	 * A regular expression for the first pass of parsing a line: getting
	 * the timestamp at the start and stripping the timestamp at the end.
	 */
	private static final Pattern lineParser =
				Pattern.compile("^(\\d+) (.+)\\[[a-zA-Z0-9 :,\\.]+\\]$");
	
	/**
	 * A regular expression for finding individual tags. Tags are
	 * separated by spaces and may consist of any characters but commas,
	 * ]s, whitespace, and - signs at the start.
	 */
	private static final Pattern tagParser =
				Pattern.compile("[^\\]\\s,\\-][^\\]\\s,]+");
	
	/**
	 * @return The list of data points that <em>would</em> exist on a
	 *         Beeminder graph that was populated according to the given
	 *         ITagMatcher, if the graph was up-to-date.
	 */
	public static List<DataPoint> parse(File logFile, ITagMatcher tagMatcher) {
		//an ordered list of data points, with each data point
		//representing the time spent on a separate day
		List<DataPoint> timePerDay = new ArrayList<DataPoint>();
		
		BufferedReader logFileIn;
		try {
			logFileIn = new BufferedReader(new FileReader(logFile));
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		String line;
		
		//matchers can be initialized on empty strings, then updated to
		//match new strings
		Matcher lineData = lineParser.matcher("");
		Matcher tagData = tagParser.matcher("");
		
		//the list of discovered tags, to be passed to the ITagMatcher
		List<String> tags = new ArrayList<String>();
		
		long previousPingTime = -1;
		long currentPingTime;
		
		boolean previousPingAccepted = false;
		
		DataPoint dataPoint;
		int dataPointIndex;
		
		while(true) {
			//read the current line
			try {
				line = logFileIn.readLine();
			} catch(IOException e) {
				break;
			}
			if(line == null) {
				break;
			}
			
			//attempt to parse the line
			lineData.reset(line);
			if(!lineData.matches()) {
				continue;
			}
			
			//once the match succeeds, group 0 will be the entire line,
			//group 1 will be the timestamp, and group 2 will contain all
			//the tags (plus a bunch of whitespace at the end)
			
			//record the ping time
			currentPingTime = Long.parseLong(lineData.group(1));
			
			//if the previous ping was accepted and needs to be submitted,
			//add it to the list
			if(previousPingAccepted) {
				/* The time for the previous ping is the length of time
				 * (in hours) after that ping and before this one; it
				 * might be easier just to use the gap _before_ a ping,
				 * but this would open up an exploit.
				 * 
				 * For example, a user could slack off for an hour after
				 * each ping (resetting the timer if they got pinged
				 * again), then start working once the hour was up.
				 * Assuming they kept working until the next ping, they'd
				 * get credit for the work they did, PLUS the hour they
				 * slacked off for.
				 * 
				 * It is better to use the amount of time _after_ each
				 * ping, because this way the user has no way of knowing
				 * a ping's value until the ping after it. At that point,
				 * of course, it's too late to change their plans.
				 * 
				 * (The reason this implementation doesn't just use the
				 * average gap between pings is that the user can change
				 * that value at any time.)
				 */
				dataPoint = new DataPoint(previousPingTime,
							(currentPingTime - previousPingTime) / 3600.0);
				
				//add the time elapsed to the running total for the day,
				//or create a new data point if necessary
				for(dataPointIndex = timePerDay.size() - 1; dataPointIndex >= 0; dataPointIndex--) {
					if(timePerDay.get(dataPointIndex).timestamp == dataPoint.timestamp) {
						timePerDay.get(dataPointIndex).hours += dataPoint.hours;
						break;
					} else if(timePerDay.get(dataPointIndex).timestamp < dataPoint.timestamp) {
						timePerDay.add(dataPointIndex + 1, dataPoint);
						break;
					}
				}
				
				if(dataPointIndex < 0) {
					timePerDay.add(0, dataPoint);
				}
			}
			
			//parse the tags and place them in the list
			tagData.reset(lineData.group(2));
			tags.clear();
			while(tagData.find()) {
				tags.add(tagData.group());
			}
			
			//check if the tags match, but don't record it the ping until
			//the next iteration (the most recent ping cannot be recorded,
			//no matter what tags it has)
			previousPingAccepted = tagMatcher.matchesTags(tags);
			previousPingTime = currentPingTime;
		}
		
		return timePerDay;
	}
}
