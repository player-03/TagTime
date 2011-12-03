To determine how you spend your time, TagTime randomly samples you. At random times it pops up and asks what you're doing right at that moment. You answer with tags.

For more on the idea behind this project, see  [messymatters.com/tagtime](http://messymatters.com/tagtime).

# Usage:

This implementation is not packaged for distribution, but you should still be able to run it.

0. Clone the repository on Github.
1. Compile the project, specifying tagtime.Main as the main class and specifying the jar files as libraries. (Sorry; I can't tell you how to do this from the command line, but I do suggest looking in your IDE's build settings.)
2. Run the app, preferably with your Beeminder username as an argument. (Again, this should be in your IDE's settings, and I can't really help if not.)
3. If you want to change your settings, exit the app and find your settings file in the newly-created data folder. Update your settings (see SettingType.java for documentation), and start the app again when you're done.
4. Answer the pings! (Always answer with what it caught you at right at that moment.)

# Credits:

TagTime was conceived of, designed, and [implemented](https://github.com/dreeves/TagTime) by Dreeves and Bethany Soule. This Java version was created by Joseph Cloutier (player_03).

The Java implementation uses [Quartz Scheduler](http://www.quartz-scheduler.org/), which is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) and created by Terracotta, Inc.

The Java implementation additionally uses [content](http://commons.apache.org/codec/) from the Apache Commons, also licensed under the Apache License 2.0.

The system tray icon comes from the [Silk icon set](http://www.famfamfam.com/lab/icons/silk/) by Mark James, available under a [Creative Commons Attribution 2.5 license](http://creativecommons.org/licenses/by/2.5/).