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

package tagtime.util;

import java.util.Collection;

public class TagMatcher implements ITagMatcher {
	/**
	 * The tags that are accepted. At least one of these must be present
	 * for a given set of tags to be accepted. Exception: if this list is
	 * empty, no tags are required.
	 */
	protected final Collection<String> tagsToAccept;
	
	/**
	 * Tags that are explicitly disallowed. If any of these are present,
	 * a given set of tags will be rejected entirely. This may be null.
	 */
	protected final Collection<String> tagsToReject;
	
	public TagMatcher(Collection<String> tagsToAccept, Collection<String> tagsToReject) {
		this.tagsToAccept = tagsToAccept;
		this.tagsToReject = tagsToReject;
	}
	
	@Override
	public boolean matchesTags(Iterable<String> tags) {
		//the tags are accepted by default if no matches are required
		//(the tag group just has to be checked for rejected tags)
		boolean matches = tagsToAccept.size() == 0;
		
		//iterate through all the tags, checking for matches
		for(String tag : tags) {
			tag = tag.toLowerCase();
			
			//once a tag as been accepted, there is no further need to
			//check acceptance
			if(!matches && tagsToAccept.contains(tag)) {
				matches = true;
				
				//if tagsToReject is empty, stop searching once any tag
				//is accepted
				if(tagsToReject == null || tagsToReject.size() == 0) {
					return true;
				}
			}

			//if any tag is rejected, the entire group is rejected
			else if(tagsToReject != null && tagsToReject.contains(tag)) {
				return false;
			}
		}
		
		return matches;
	}
}
