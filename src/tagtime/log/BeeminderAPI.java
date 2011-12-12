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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import tagtime.Main;
import tagtime.TagTime;
import tagtime.settings.SettingType;
import tagtime.settings.Settings;

public class BeeminderAPI {
	private final Settings userSettings;
	private final List<BeeminderGraphData> graphData;
	
	public BeeminderAPI(TagTime tagTimeInstance, Settings userSettings) throws ClassCastException {
		this.userSettings = userSettings;
		
		String username = tagTimeInstance.username;
		
		@SuppressWarnings("unchecked")
		Collection<String> graphDataEntries = (Collection<String>) userSettings
					.getValue(SettingType.BEEMINDER_GRAPHS);
		
		graphData = new ArrayList<BeeminderGraphData>(graphDataEntries.size());
		for(String dataEntry : graphDataEntries) {
			graphData.add(new BeeminderGraphData(tagTimeInstance, username, dataEntry));
		}
	}
	
	/**
	 * Submits the current user's data to each registered graph.
	 */
	public void submit() {
		String username = userSettings.username;
		
		File logFile = new File(Main.getDataDirectory().getPath()
					+ "/" + username + ".log");
		
		/*BufferedWriter beeFile = null;
		try {
			beeFile = new BufferedWriter(new FileWriter(
						new File(Main.getDataDirectory().getName()
									+ "/" + username + ".bee")));
		} catch(IOException e) {
			e.printStackTrace();
		}*/

		for(BeeminderGraphData data : graphData) {
			data.submitPings(logFile);
		}
	}
}
