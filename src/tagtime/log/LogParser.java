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
import tagtime.util.TagMatcher;

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
	
	public static List<DataPoint> parse(File logFile, TagMatcher tagMatcher) {
		//a map of dates and amounts of time on those dates
		//(dates are represented as 
		List<DataPoint> timePerDay = new ArrayList<DataPoint>();
		
		BufferedReader logFileIn;
		try {
			logFileIn = new BufferedReader(new FileReader(logFile));
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		//the current line of text
		String line;
		
		Matcher lineData = lineParser.matcher("");
		Matcher tagData = tagParser.matcher("");
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
			//submit it now
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
				 * of course, it's too late to change their behavior.
				 * 
				 * (The reason we don't just use the average gap between
				 * pings is that the user can change that value at any
				 * time.)
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
			
			//place the tags in the list for easier iteration
			tagData.reset(lineData.group(2));
			tags.clear();
			while(tagData.find()) {
				tags.add(tagData.group());
			}
			
			//check if the ping is accepted, but don't record it until the
			//next iteration
			previousPingAccepted = tagMatcher.matchesTags(tags);
			previousPingTime = currentPingTime;
		}
		
		return timePerDay;
	}
}
