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

package tagtime.ping;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import tagtime.TagTime;
import tagtime.settings.SettingType;
import tagtime.util.HMSTimeFormatter;
import tagtime.util.TagCount;

/**
 * A job that gets executed by the Quartz scheduler when it is time to
 * ping the user. This creates and displays a ping window, then hides it
 * if it times out.
 */
public class PingJob implements Job {
	private TagTime tagTimeInstance;
	
	protected PingWindow window;
	
	private long scheduledTime;
	private boolean dataLogged = false;
	
	public PingJob() throws ClassCastException {
	}
	
	@Override
	public void execute(JobExecutionContext context) {
		tagTimeInstance = (TagTime) context.getJobDetail().getJobDataMap()
									.getWrappedMap().get(TagTime.TAG_TIME_INSTANCE);
		assert tagTimeInstance != null;
		
		if(context.getPreviousFireTime() == null) {
			//the first job is run immediately at the start of the
			//session, but this doesn't match the actual time it should
			//have been run, so skip it
			return;
		}
		
		List<TagCount> cachedTags =
					tagTimeInstance.settings.getTagCounts(SettingType.CACHED_TAGS);
		window = new PingWindow(tagTimeInstance, this, cachedTags);
		
		scheduledTime = context.getScheduledFireTime().getTime();
		
		long windowTimeout = (tagTimeInstance.settings
							.getIntValue(SettingType.WINDOW_TIMEOUT)) * 1000;
		
		//if this job was executed too long after it was scheduled,
		//log it as "afk off RETRO"
		if(checkTimedOut(windowTimeout, "afk off")) {
			return;
		}
		
		long timeSincePreviousPing = scheduledTime - context.getPreviousFireTime().getTime();
		System.out.println("Dispatching ping after a wait of "
						+ HMSTimeFormatter.format(timeSincePreviousPing / 1000)
						+ ".");
		
		window.setVisible(true);
		
		do {
			try {
				Thread.sleep(windowTimeout);
			} catch(InterruptedException e) {}
			checkTimedOut(windowTimeout, "afk");
		} while(!dataLogged);
	}
	
	public void cancel() {
		window.dispose();
	}
	
	private boolean checkTimedOut(long timeoutLength, String timeoutMessage) {
		if(!dataLogged) {
			if(System.currentTimeMillis() - scheduledTime > timeoutLength) {
				dataLogged = true;
				tagTimeInstance.log.logRetro(scheduledTime, timeoutMessage);
				cancel();
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Submits the given text as the tag(s) entered.
	 */
	public void submit(String tags) {
		if(!dataLogged) {
			dataLogged = true;
			
			tagTimeInstance.log.log(scheduledTime, tags);
			tagTimeInstance.settings.incrementTagCounts(SettingType.CACHED_TAGS,
						new LinkedList<String>(Arrays.asList(tags.split(" "))));
		}
	}
	
	/**
	 * Submits the ping canceled message, unless data has already been
	 * submitted.
	 */
	public void submitCanceled() {
		if(!dataLogged) {
			dataLogged = true;
			
			tagTimeInstance.log.logRetro(scheduledTime, "canceled");
		}
	}
}
