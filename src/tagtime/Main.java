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

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Main {
	private static final Pattern BEEMINDER_USERNAME_MATCHER = Pattern.compile("[a-z0-9]+");
	
	private static File dataDirectory;
	private static File soundDirectory;
	private static Image iconImage;
	
	public static void main(String[] args) {
		//create the data directory, if needed
		dataDirectory = new File("./data");
		dataDirectory.mkdir();
		
		//create the sound directory, if needed
		soundDirectory = new File("./sound");
		if(soundDirectory.mkdir()) {
			System.out.println("No sound directory found; a new " +
						"directory has been created, but it will need " +
						"to be populated before any sounds can be " +
						"played. Directory location:");
			System.out.println(soundDirectory.getAbsolutePath());
		}
		
		//create the icon
		iconImage = Toolkit.getDefaultToolkit().createImage("tray_icon.png");
		
		ArrayList<String> usernames = null;
		if(args.length > 0) {
			usernames = new ArrayList<String>(args.length);
			for(String username : args) {
				usernames.add(username);
			}
		} else {
			//attempt to load the "usernames.txt" file
			File usernameFile = new File("./usernames.txt");
			
			if(usernameFile.exists()) {
				usernames = new ArrayList<String>();
				try {
					BufferedReader usernameReader = new BufferedReader(
								new FileReader(usernameFile));
					
					usernames = new ArrayList<String>(1);
					
					for(String username = usernameReader.readLine(); username != null; username =
								usernameReader.readLine()) {
						usernames.add(username);
					}
				} catch(FileNotFoundException e) {
					e.printStackTrace();
				} catch(IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					usernameFile.createNewFile();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		if(usernames != null) {
			//if for any reason multiple people want to use TagTime on
			//the same device at once, they can (though it would most
			//likely be more trouble than it's worth)
			for(String username : usernames) {
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
		if(username == null
					|| !BEEMINDER_USERNAME_MATCHER.matcher(username).matches()) {
			return;
		}
		
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
	
	/**
	 * @return A file object representing the sound directory.
	 */
	public static File getSoundDirectory() {
		return soundDirectory;
	}
	
	public static Image getIconImage() {
		return iconImage;
	}
}
