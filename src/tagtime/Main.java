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

package tagtime;

import java.io.File;
import java.io.IOException;

public class Main {
	private static File dataDirectory;
	
	public static void main(String[] args) {
		//create the data directory, if needed
		dataDirectory = new File("./data");
		dataDirectory.mkdir();
		
		if(args.length > 0) {
			//if for any reason multiple people want to use TagTime on
			//the same device at once, they can (though it would most
			//likely be more trouble than it's worth)
			for(String username : args) {
				runTagTime(username);
			}
		} else {
			//TODO: open a window and ask for their username
			runTagTime("default_user");
		}
	}
	
	/**
	 * Creates and runs a new TagTime instance for the given user.
	 */
	private static void runTagTime(String username) {
		try {
			TagTime instance = new TagTime(username);
			instance.start();
		} catch(IOException e) {
			System.err.println("Unable to run TagTime for " + username + ".");
			e.printStackTrace();
		}
	}
	
	/**
	 * @return A file object representing the data directory.
	 */
	public static File getDataDirectory() {
		return dataDirectory;
	}
}
