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
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
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

/**
 * An instance of TagTime for one particular user. This class contains
 * all user-specific data and handles all scheduling for that user.
 */
public class TagTime {
	public static final String TAG_TIME_INSTANCE = "TagTime Instance";
	
	public final String username;
	
	public final Settings settings;
	public final Log log;
	
	public final TrayIcon trayIcon;
	
	protected Scheduler scheduler;
	protected final JobDetail jobDetail;
	public final RandomizedTrigger trigger;
	
	public final BeeminderAPI api;
	
	/**
	 * Runs an instance of TagTime for the given user.
	 * @param username The current user's username. This will be used to
	 *            find the settings file and submit data to Beeminder,
	 *            and it cannot be changed without creating a new TagTime
	 *            object.
	 */
	public TagTime(String username) throws IOException {
		this.username = username;
		
		//get the settings file
		settings = Settings.getInstance(username);
		
		//create the log file
		log = new Log(this);
		
		api = new BeeminderAPI(this, settings);
		
		String rngKey = (String) settings.getValue(SettingType.RNG_KEY);
		
		try {
			//define a job
			jobDetail = JobBuilder.newJob(PingJob.class)
									.withIdentity(username)
									.build();
			
			//store a reference to this
			jobDetail.getJobDataMap().put(TAG_TIME_INSTANCE, this);
			
			//set up the trigger and schedule for the job
			RandomizedScheduleBuilder scheduleBuilder =
						RandomizedScheduleBuilder.repeatMinutelyForever(
										(Integer) settings.getValue(SettingType.AVERAGE_GAP))
									.withRNGKey(rngKey)
									.withMisfireHandlingInstructionIgnoreMisfires();
			trigger = (RandomizedTrigger) TriggerBuilder.newTrigger()
									.withIdentity("trigger for " + username,
												"group for " + username)
									.withSchedule(scheduleBuilder)
									.build();
			
			scheduler = StdSchedulerFactory.getDefaultScheduler();
		} catch(SchedulerException e) {
			e.printStackTrace();
			throw new ExceptionInInitializerError("Unable to create the " +
						"scheduler for " + username + ".");
		}
		
		//if the RNG key wasn't defined, save the automatically-generated one
		if(rngKey.equals("")) {
			settings.setValue(SettingType.RNG_KEY, trigger.getRNGKey());
		}
		
		//create a system tray icon
		if(SystemTray.isSupported()) {
			//the settings menu item
			ActionListener settingsListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("Settings manager not implemented; sorry. " +
								"Currently, you have to exit TagTime, manually edit " +
								settings.username + "_settings.txt, and restart TagTime.");
				}
			};
			MenuItem settingsMenuItem = new MenuItem("Settings", new MenuShortcut(KeyEvent.VK_S));
			settingsMenuItem.addActionListener(settingsListener);
			
			//the "submit now" menu item
			ActionListener submitListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					api.submit();
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
			
			//create the tray icon
			trayIcon = new TrayIcon(Main.getIconImage(), "TagTime (" + username + ")", popupMenu);
		} else {
			throw new ExceptionInInitializerError("System tray not supported; TagTime " +
						"(currently) requires a system tray icon to function.");
			//TODO: Implement an alternative to the system tray.
		}
	}
	
	/**
	 * Registers the system tray icon and starts the application.
	 */
	public void start() {
		if(!SystemTray.isSupported()) {
			return;
		}
		
		//put the icon in the tray
		SystemTray systemTray = SystemTray.getSystemTray();
		try {
			systemTray.add(trayIcon);
		} catch(AWTException e) {
			System.err.println("Could not register the system tray " +
							"icon for " + username + ".");
			
			//cancel
			return;
		}
		
		//start the scheduler
		try {
			//if the scheduler has been shut down, we need a new one
			if(scheduler.isShutdown()) {
				//untested, but based to the source, this _should_
				//produce a new scheduler
				scheduler = StdSchedulerFactory.getDefaultScheduler();
			}
			
			scheduler.start();
			scheduler.scheduleJob(jobDetail, trigger);
		} catch(SchedulerException e) {
			e.printStackTrace();
			
			//cancel
			systemTray.remove(trayIcon);
			return;
		}
		
		//record all the pings that were missed while TagTime wasn't running
		Date now = new Date();
		log.logMissedPings("off", now.getTime());
		
		long timeDiff = (now.getTime() - trigger.getFireTimeBefore(now, true).getTime()) / 1000;
		assert timeDiff > 0;
		
		System.out.println("TagTime is watching you, " + username + "!" +
					" Last ping would've been " +
					HMSTimeFormatter.format(timeDiff) +
					" ago.");
	}
	
	/**
	 * Saves and cleans up everything used by this instance.
	 */
	public void exit() {
		settings.flush();
		
		if(trayIcon != null) {
			//TODO: Figure out why removing one tray icon causes the
			//second's popup menu to stop working.
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
		
		System.out.println("Bye, " + username + "!");
		
		//if this was the final TagTime object, the application should
		//exit at this point
	}
}
