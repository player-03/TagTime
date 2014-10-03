To determine how you spend your time, TagTime randomly samples you. At random times it pops up and asks what you're doing right at that moment. You answer with tags. For more on the idea behind this project, see [messymatters.com/tagtime](http://messymatters.com/tagtime).

# Usage:

0. Clone the repository from Github or download and extract the [jar](http://www.mediafire.com/download.php?ndaaq5p3r6z7u45). Feel free to move the directory they're in.
1. Assuming you have Java installed, run the app by double-clicking TagTime.jar. Enter your Beeminder username, or make up a name if you don't plan to get a Beeminder account.
2. Answer the pings! (Always answer, tag style, with what it caught you at right at that moment.)
3. If you want to change your settings, exit the app by right-clicking the icon in the system tray. Find your settings file in the newly-created data folder. Update your settings in the [name].properties file (SettingType.java documents their meaning), and start the app again when you're done.

# Credits:

TagTime was conceived of, designed, and [implemented](https://github.com/dreeves/TagTime) by Dreeves and Bethany Soule. This Java version was created by Joseph Cloutier (player_03). I, Nzen, am making a few gui changes.

This implementation uses libraries licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). These are [Quartz Scheduler](http://www.quartz-scheduler.org/), [json-simple](http://code.google.com/p/json-simple/), and various libraries from the [Apache Commons](http://commons.apache.org/codec/). The system tray icon comes from the [Silk icon set](http://www.famfamfam.com/lab/icons/silk/) by Mark James, available under a [Creative Commons Attribution 2.5 license](http://creativecommons.org/licenses/by/2.5/).

## Improvement?

This is pretty great software. I much appreciate that player_03 ported this from perl. But, perhaps I can fill in some functionality that _I_ fancy. It seems that this version totally relies on beeminder to provide views. I'd rather process & see it here.

Now with a progress bar to show the time left.

![tagtime with progressbar](https://farm3.staticflickr.com/2946/15429286301_f711c33b6d_o_d.jpg)