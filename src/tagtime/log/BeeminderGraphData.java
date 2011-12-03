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

package tagtime.log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tagtime.Main;
import tagtime.settings.SettingType;

/**
 * A set of data for a single Beeminder graph. This class handles the
 * actual parsing and submission of data.
 */
public class BeeminderGraphData {
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
	 * Submit time spent as hours, rounded to two decimal places.
	 */
	private static final DecimalFormat hourFormatter =
										new DecimalFormat("#.##");
	
	/**
	 * The name of the Beeminder graph.
	 */
	public final String graphName;
	
	/**
	 * The url of the Beeminder graph.
	 */
	private URL graphURL;
	
	/**
	 * The tags that are accepted to this graph. At least one of these
	 * must be present for a given ping to be sent to this graph.
	 * Exception: if this list is empty, no tags are required.
	 */
	private final ArrayList<String> acceptedTags;
	
	/**
	 * The tags that aren't accepted by this graph. If one or more of
	 * these are present, a given ping will not be sent to this graph.
	 */
	private final ArrayList<String> rejectedTags;
	
	/**
	 * A list of timestamps corresponding to pings that have been
	 * submitted. (Pings should occur one by one, so a single time value
	 * should be sufficient to identify a ping.)
	 */
	private final ArrayList<Long> pingsSubmitted;
	
	/**
	 * The .bee file for storing the pings submitted.
	 */
	private final File beeFile;
	
	/**
	 * Parses a graph's data and sets up a .bee file to track which tags
	 * have been submitted to the graph.
	 * @param username The user's Beeminder username.
	 * @param dataEntry The data entry for the current graph. This must
	 *            be in the format "graphName|tags".
	 */
	public BeeminderGraphData(String username, String dataEntry) {
		if(username == null || dataEntry == null) {
			throw new IllegalArgumentException("Both parameters to the " +
						"BeeminderGraphData constructor must be defined.");
		}
		
		//the URL will be determined based on whether the data is to be
		//overwritten
		boolean overwriteAllData = (Boolean) Main.getSettings()
													.getValue(SettingType.OVERWRITE_ALL_DATA);
		
		//find the graph url
		int graphDelim = dataEntry.indexOf('|');
		if(graphDelim < 0 || graphDelim >= dataEntry.length() - 1) {
			throw new IllegalArgumentException("dataEntry must be in " +
						"format \"graphname|tags\"");
		}
		graphName = dataEntry.substring(0, graphDelim);
		try {
			graphURL = new URL((String) Main.getSettings().getValue(SettingType.SUBMISSION_URL) +
						"/" + username +
						"/goals/" +
						graphName +
						"/datapoints/" +
						(overwriteAllData ? "tagtime_update" : "create_all"));
		} catch(MalformedURLException e) {
			e.printStackTrace();
			graphURL = null;
		}
		
		//get the tags
		String[] tags = dataEntry.substring(graphDelim + 1).split("\\s+");
		
		acceptedTags = new ArrayList<String>(3);
		rejectedTags = new ArrayList<String>(0);
		
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
		
		//create the list of timestamps for pings that have been
		//submitted to this graph
		pingsSubmitted = new ArrayList<Long>(20);
		
		//if a .bee file exists for this user and graph, load the data
		//from it unless that data is going to be overwritten
		//(yes, it might be more space-efficient to use one file for all
		//graphs, but that would be harder to process)
		beeFile = new File(Main.getDataDirectory().getName()
					+ "/" + username + "_" + graphName + ".bee");
		if(beeFile.exists() && !overwriteAllData) {
			BufferedReader beeDataIn;
			String timestampString;
			try {
				beeDataIn = new BufferedReader(new FileReader(beeFile));
				
				while(true) {
					try {
						timestampString = beeDataIn.readLine();
					} catch(IOException e) {
						break;
					}
					if(timestampString == null) {
						break;
					}
					
					pingsSubmitted.add(Long.parseLong(timestampString));
				}
			} catch(FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			try {
				beeFile.createNewFile();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Submits all matching pings from the given file that have not yet
	 * been submitted. Currently, it estimates the time by assuming the
	 * user spent all their time between two pings on the activity
	 * recorded by the second ping.
	 * @param logFile A reference to the log file to read from.
	 */
	public void submitPings(File logFile) {
		//a map of dates and amounts of time on those dates
		TreeMap<String, Long> timeToSubmit = new TreeMap<String, Long>();
		
		//a formatter for converting a Date object to a string that can
		//be submitted (this string will also be used as the key for
		//values in timeToSubmit)
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MM dd", Locale.US);
		
		BufferedReader logFileIn;
		try {
			logFileIn = new BufferedReader(new FileReader(logFile));
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		//the current index in pingsSubmitted
		int pingIndex = 0;
		
		String line;
		Matcher lineData;
		
		long previousPingTime = -1;
		long currentPingTime;
		
		boolean previousPingToBeSubmitted = false;
		boolean pingAccepted;
		
		String tag;
		String dateString;
		
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
			lineData = lineParser.matcher(line);
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
			if(previousPingToBeSubmitted) {
				//record the ping as having been submitted
				//(normally this will add the ping at the end, but if
				//the user has modified the settings for this list,
				//pingIndex may be in the middle)
				pingsSubmitted.add(pingIndex, previousPingTime);
				
				//the pings are stored in seconds, but the Date class
				//uses milliseconds
				dateString = dateFormat.format(new Date(previousPingTime * 1000));
				
				//add the time elapsed to the running total for the day
				if(!timeToSubmit.containsKey(dateString)) {
					//the time for the previous ping is the length of time
					//after that ping and before this one; previously,
					//the amount of time _before_ a ping was used, but
					//this opened up an exploit:
					
					//if a user went an hour without seeing any pings,
					//they could start working until the next ping, and
					//they would get credit for an hour of work they
					//didn't actually do
					
					//this way, the user has no information about the
					//next ping, so they can't game the system; sure, they
					//can go back and edit the log file if enough time
					//passes, but they could do that anyway
					
					//(note that the reason we don't just use the average
					//gap is that the average gap have changed)
					timeToSubmit.put(dateString, currentPingTime - previousPingTime);
				} else {
					//note that these times are in seconds
					timeToSubmit.put(dateString, timeToSubmit.get(dateString)
								+ (currentPingTime - previousPingTime));
				}
			}
			
			//find this ping in pingsSubmitted
			while(pingIndex < pingsSubmitted.size() &&
						currentPingTime > pingsSubmitted.get(pingIndex)) {
				pingIndex++;
			}
			
			//if the ping has already been submitted, skip it
			if(pingIndex < pingsSubmitted.size() &&
						pingsSubmitted.get(pingIndex) == currentPingTime) {
				previousPingTime = currentPingTime;
				previousPingToBeSubmitted = false;
				pingIndex++;
				continue;
			}
			
			//if there are no required tags, pingAccepted defaults to true
			pingAccepted = acceptedTags.size() == 0;
			
			//iterate through all the tags, checking for matches
			String tags = lineData.group(2);
			Matcher tagData = tagParser.matcher(tags);
			while(tagData.find()) {
				tag = tagData.group().toLowerCase();
				
				//check if this tag is accepted, unless a match has
				//already been found, in which case all that needs to be
				//done is check for rejected tags
				if(!pingAccepted && acceptedTags.contains(tag)) {
					pingAccepted = true;
					
					//if rejectedTags is empty, there's nothing left to
					//search for
					if(rejectedTags.size() == 0) {
						break;
					}
				} else if(rejectedTags.contains(tag)) {
					//if any tag is rejected, the entire ping is rejected
					pingAccepted = false;
					break;
				}
			}
			
			previousPingToBeSubmitted = pingAccepted;
			previousPingTime = currentPingTime;
		}
		
		if(timeToSubmit.size() == 0) {
			return;
		}
		
		//combine all data to be submitted into a single string
		boolean firstEntry = true;
		StringBuffer data = new StringBuffer("origin=tgt");
		if(!(Boolean) Main.getSettings().getValue(SettingType.OVERWRITE_ALL_DATA)) {
			data.append("&sendmail=false");
		}
		data.append("&datapoints_text=");
		for(Entry<String, Long> dataToSubmit : timeToSubmit.entrySet()) {
			if(dataToSubmit.getValue() <= 0) {
				continue;
			}
			
			try {
				data.append(URLEncoder.encode((firstEntry ? "" : "\n") +
							dataToSubmit.getKey() + " " +
							hourFormatter.format((double) dataToSubmit.getValue() / 3600),
												"UTF-8"));
				firstEntry = false;
			} catch(UnsupportedEncodingException e) {
				e.printStackTrace();
				return;
			}
		}
		
		//submit the data
		try {
			//open the connection
			HttpURLConnection connection = (HttpURLConnection) graphURL.openConnection();
			
			//apply the connection settings
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Length", Integer.toString(data.length()));
			
			//post the data
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			out.write(data.toString());
			out.flush();
			
			out.close();
			
			//For debugging:
			System.out.println("Request: " + graphURL.toString() + "?" + data.toString());
			System.out.println("Response: " + connection.getResponseCode() +
						" " + connection.getResponseMessage());
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Unable to submit your data to Beeminder " +
						"graph " + graphName + ". Please try again later.");
			return;
		}
		
		//record the submitted data
		try {
			BufferedWriter beeFileOut = new BufferedWriter(new FileWriter(beeFile));
			
			for(Long pingTime : pingsSubmitted) {
				beeFileOut.write(pingTime.toString() + "\n");
			}
			
			beeFileOut.flush();
		} catch(IOException e) {
			System.err.println("Unable to record which data points were " +
						"submitted. This may cause those data points to " +
						"be submitted again next time. To prevent the " +
						"creation of duplicate values, OVERWRITE_ALL_DATA " +
						"will be set to true. (Feel free to undo this " +
						"after you submit successfully.)");
			
			Main.getSettings().setValue(SettingType.OVERWRITE_ALL_DATA, true);
		}
	}
}
