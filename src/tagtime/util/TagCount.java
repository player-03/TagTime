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

/**
 * Tracks a single tag the user has entered, as well as the number of
 * times that tag was entered.
 */
public class TagCount implements Comparable<TagCount> {
	private final String tag;
	private int count;
	
	public TagCount(String tag, int count) {
		assert tag != null;
		
		this.tag = tag.toLowerCase();
		this.count = count;
	}
	
	public String getTag() {
		return tag;
	}
	
	@Override
	public String toString() {
		return tag + ":" + count;
	}
	
	public int getCount() {
		return count;
	}
	
	public void increment() {
		count++;
	}
	
	@Override
	public int compareTo(TagCount other) {
		if(tag.equals(other.tag)) {
			return 0;
		}
		
		int diff = other.count - count;
		if(diff != 0) {
			return diff;
		}
		
		return tag.compareTo(other.tag);
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof TagCount
					&& tag.equals(((TagCount) other).tag);
	}
	
	@Override
	public int hashCode() {
		return tag.hashCode();
	}
}
