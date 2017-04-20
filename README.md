To determine how you spend your time, TagTime randomly samples you. At random times it pops up and asks what you're doing right at that moment. You answer with tags.

For more on the idea behind this project, see [messymatters.com/tagtime](http://messymatters.com/tagtime).

# Usage:

0. Download the [latest release](https://github.com/player-03/TagTime/releases) and extract it somewhere convenient.
1. Run the app by double-clicking TagTime.jar, or by typing `java TagTime.jar` on the command line.
2. Enter your Beeminder username, or make up a name if you don't plan to get a Beeminder account.
3. If you want to change your settings, exit the app by right-clicking the icon in the system tray. Find your settings file in the newly-created data folder. Update your settings (see [SettingType.java](https://github.com/player-03/TagTime/blob/master/src/tagtime/settings/SettingType.java) for documentation), and start the app again when you're done.
4. Answer the pings! (Always answer with what it caught you at right at that moment.)

Beeminder users will need to update [these two settings](https://github.com/player-03/TagTime/blob/master/src/tagtime/settings/SettingType.java#L77-L130) in particular.

# Differences between this implementation and the original:

- This library uses a different random number generator, so even if you copy the seed, it will ping you at different times.
- When using this library, you can safely change the ping frequency (AVERAGE_GAP) at any time.

# Credits:

TagTime was conceived of, designed, and [implemented](https://github.com/dreeves/TagTime) by Dreeves and Bethany Soule. This Java version was created by Joseph Cloutier (player_03).

This implementation uses libraries licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). These are [Quartz Scheduler](http://www.quartz-scheduler.org/), [json-simple](http://code.google.com/p/json-simple/), and various libraries from the [Apache Commons](http://commons.apache.org/codec/).

The system tray icon comes from the [Silk icon set](http://www.famfamfam.com/lab/icons/silk/) by Mark James, available under a [Creative Commons Attribution 2.5 license](http://creativecommons.org/licenses/by/2.5/).