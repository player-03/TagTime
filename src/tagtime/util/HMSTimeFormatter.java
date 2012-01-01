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

package tagtime.util;

public class HMSTimeFormatter {
	/**
	 * @param time An amount of time in seconds.
	 * @return A string representing the given amount of time, formatted
	 *         appropriately based on the number of hours, minutes, and
	 *         seconds. Sample return values are
	 *         "4 minutes and 1 second", "41 seconds", and "1 hour, 19
	 *         minutes, and 53 seconds". If 0 or a negative value is
	 *         provided, "0 seconds" will be returned.
	 */
	public static String format(long time) {
		long hours = time / 3600;
		long minutes = (time % 3600) / 60;
		long seconds = (time % 60);
		
		int itemsInList = (hours > 0 ? 1 : 0)
						+ (minutes > 0 ? 1 : 0)
						+ (seconds > 0 ? 1 : 0);
		
		if(itemsInList == 0) {
			return "0 seconds";
		}
		
		String formattedTime = "";
		
		if(hours > 0) {
			//use the correct pluralization
			if(hours != 1) {
				formattedTime = String.format("%d hours", hours);
			} else {
				formattedTime = "1 hour";
			}
			
			//only use commas if all three items are in the list
			if(itemsInList == 3) {
				formattedTime += ", ";
			}

			//otherwise, use "and" unless only hours are in the list
			else if(itemsInList == 2) {
				formattedTime += " and ";
			}
		}
		
		if(minutes > 0) {
			if(minutes != 1) {
				formattedTime += String.format("%d minutes", minutes);
			} else {
				formattedTime += "1 minute";
			}
			
			//again, only use commas if all three are present
			if(itemsInList == 3) {
				formattedTime += ", and ";
			}

			//only use "and" if seconds are in the list
			else if(seconds > 0) {
				formattedTime += " and ";
			}
		}
		
		if(seconds > 0) {
			if(seconds != 1) {
				formattedTime += String.format("%d seconds", seconds);
			} else {
				formattedTime += "1 second";
			}
		}
		
		return formattedTime;
	}
}
