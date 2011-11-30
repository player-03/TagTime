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

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import tagtime.log.BeeminderAPI;
import tagtime.log.Log;
import tagtime.ping.PingJob;
import tagtime.quartz.RandomizedScheduleBuilder;
import tagtime.quartz.RandomizedTrigger;
import tagtime.settings.SettingType;
import tagtime.settings.Settings;
import tagtime.util.HMSTimeFormatter;

public class Main {
	public static final String START_MESSAGE = "TagTime started RETRO";
	
	private static File dataDirectory;
	private static Settings settings;
	
	public static Image iconImage;
	private static TrayIcon trayIcon;
	
	private static Scheduler scheduler;
	protected static JobDetail jobDetail;
	protected static RandomizedTrigger trigger;
	
	private static BeeminderAPI api;
	
	public static void main(String[] args) {
		String username;
		if(args.length > 0) {
			username = args[0];
		} else {
			//TODO: open a window and ask for their name
			username = "default_user";
		}
		
		dataDirectory = new File("./data");
		
		//this does nothing if the directory is already there
		dataDirectory.mkdir();
		
		try {
			settings = Settings.getInstance(username);
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
			return;
		}
		
		Log.getInstance().log(System.currentTimeMillis(), START_MESSAGE);
		api = new BeeminderAPI(settings);
		
		String rngKey = (String) settings.getValue(SettingType.RNG_KEY);
		
		try {
			//define a job
			jobDetail = JobBuilder.newJob(PingJob.class)
							.withIdentity("tagJob1", "group1")
							.build();
			
			//set up the trigger and schedule for the job
			RandomizedScheduleBuilder scheduleBuilder = RandomizedScheduleBuilder
						.repeatMinutelyForever((Integer)
									settings.getValue(SettingType.AVERAGE_GAP))
						.withRNGKey(rngKey);
			trigger = (RandomizedTrigger) TriggerBuilder.newTrigger()
						.withIdentity("trigger1", "group1")
						.withSchedule(scheduleBuilder)
						.build();
			
			//start the scheduler
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
			scheduler.scheduleJob(jobDetail, trigger);
		} catch(SchedulerException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		//if the RNG key wasn't defined, save the automatically-generated one
		if(rngKey.equals("")) {
			settings.setValue(SettingType.RNG_KEY, trigger.getRNGKey());
		}
		
		//create a system tray icon
		if(SystemTray.isSupported()) {
			//get a reference to the tray
			SystemTray systemTray = SystemTray.getSystemTray();
			
			//the settings menu item
			ActionListener settingsListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getSettings().flush();
					System.out.println("Settings manager not implemented; sorry. " +
								"Any changes have been saved, though.");
				}
			};
			MenuItem settingsMenuItem = new MenuItem("Settings", new MenuShortcut(KeyEvent.VK_S));
			settingsMenuItem.addActionListener(settingsListener);
			
			//the "submit now" menu item
			ActionListener submitListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getAPI().submit();
				}
			};
			MenuItem submitMenuItem = new MenuItem("Submit data now");
			submitMenuItem.addActionListener(submitListener);
			
			//the quit menu item
			ActionListener quitListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					exit();
				}
			};
			MenuItem quitMenuItem = new MenuItem("Quit", new MenuShortcut(KeyEvent.VK_Q));
			quitMenuItem.addActionListener(quitListener);
			
			//create and populate the icon's popup menu
			PopupMenu popupMenu = new PopupMenu();
			popupMenu.add(settingsMenuItem);
			popupMenu.add(submitMenuItem);
			popupMenu.add(quitMenuItem);
			
			//create the tray icon and attempt to add it to the tray
			iconImage = Toolkit.getDefaultToolkit().createImage("tray_icon.png");
			trayIcon = new TrayIcon(iconImage, "TagTime", popupMenu);
			try {
				systemTray.add(trayIcon);
			} catch(AWTException e) {
				trayIcon = null;
			}
		}
		
		if(trayIcon == null) {
			System.err.println("Could not create system tray icon - " +
						"you will have to kill this process manually.");
			//TODO: Implement an alternative to the system tray.
		}
		
		Date now = new Date();
		long timeDiff = (now.getTime() - trigger.getFireTimeBefore(now, true).getTime()) / 1000;
		assert timeDiff > 0;
		
		System.out.println("TagTime is watching you! Last ping would've been "
					+ HMSTimeFormatter.format(timeDiff)
					+ " ago.");
	}
	
	/**
	 * Saves, cleans up everything used by this app, and exits.
	 */
	public static void exit() {
		settings.flush();
		
		if(trayIcon != null) {
			SystemTray.getSystemTray().remove(trayIcon);
		}
		
		try {
			scheduler.shutdown();
		} catch(SchedulerException e) {
			e.printStackTrace();
		}
		
		Window[] windows = Window.getWindows();
		for(Window window : windows) {
			for(WindowListener listener : window.getWindowListeners()) {
				window.removeWindowListener(listener);
			}
			window.dispose();
		}
		
		//The app should exit even with this line commented. Uncomment it
		//for release builds, but it might be best to leave it commented
		//while debugging, just to make sure everything really has been
		//cleaned up.
		//System.exit(0);
	}
	
	/**
	 * @return The settings manager for the current user.
	 */
	public static Settings getSettings() {
		return settings;
	}
	
	/**
	 * @return A file object representing the data directory.
	 */
	public static File getDataDirectory() {
		return dataDirectory;
	}
	
	/**
	 * @return The Beeminder API object for the current user.
	 */
	public static BeeminderAPI getAPI() {
		return api;
	}
}
