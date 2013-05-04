/*
 * Copyright 2011-2012 Joseph Cloutier
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

package tagtime.settings;

import java.util.List;
import java.util.TreeSet;

/**
 * An enum storing the different types of value that can be saved and
 * retrieved as settings.
 */
public enum SettingType {
	/**
	 * The average gap between pings, in minutes.
	 */
	AVERAGE_GAP(int.class, 45),

	/**
	 * The length of time, in seconds, that ping windows will wait before
	 * timing out.
	 */
	WINDOW_TIMEOUT(int.class, 60),

	/**
	 * The tags the user has used before.
	 */
	CACHED_TAGS(TreeSet.class, null),

	/**
	 * The x position of the user's preferred window location.
	 */
	WINDOW_X(int.class, 20),

	/**
	 * The y position of the user's preferred window location.
	 */
	WINDOW_Y(int.class, 20),

	/**
	 * The user's preferred window width.
	 */
	WINDOW_WIDTH(int.class, 350),

	/**
	 * The user's preferred window height.
	 */
	WINDOW_HEIGHT(int.class, 400),

	/**
	 * Whether ping windows should attempt to steal focus from other
	 * applications.
	 */
	STEAL_FOCUS(Boolean.class, true),

	/**
	 * The encryption key used to generate pseudo-random values.
	 */
	RNG_KEY(String.class, null),

	/**
	 * <p>
	 * Entries for each Beeminder graph the user uses with TagTime. Each
	 * entry in this set is in the format <code>graphname|tags</code>.
	 * Tags may contain any characters except commas, right square
	 * brackets, whitespace, and - signs at the start. (Actually, - signs
	 * at the start are allowed; they just have a different meaning and
	 * aren't parsed as part of the tag.)
	 * </p>
	 * <p>
	 * For example, if a user with username alice had the entry
	 * <code>work|job work</code>, all pings including at least one of
	 * "job" or "work" would be sent to www.beeminder.com/alice/work.
	 * </p>
	 * <p>
	 * Additionally, if any tag is preceded by a - sign, that tag must
	 * <em>not</em> be present for the ping to be submitted to the
	 * specified graph. If some tags have - signs, the ping will be
	 * submitted if <em>none</em> of the - tags are present, and at least
	 * one of the other tags is. For example, the entry
	 * <code>computeridle|afk retro -off</code> will send pings such as
	 * "afk" and "afk RETRO", but it will not send "afk off RETRO" or
	 * "research for work".
	 * </p>
	 * <p>
	 * Note that tag matching is case insensitive: the above entry will
	 * match pings with "Afk Retro" as well as "afk RETRO", but the two
	 * would show up differently in the log.
	 * </p>
	 * <p>
	 * Special case: if every tag has a - sign, the requirement for at
	 * least one matching tag will be waived. For example,
	 * <code>nafk|-afk</code> will submit any ping without "afk" to the
	 * "nafk" graph. Pings can be submitted to any number of graphs, so
	 * "job" would be submitted to both the "work" graph and the "nafk"
	 * graph.
	 * </p>
	 * <p>
	 * To specify data for multiple graphs, separate each entry with a
	 * comma. To include all three previously-given examples, the entry
	 * in your properties file would look like this:
	 * </p>
	 * <p>
	 * BEEMINDER_GRAPHS = work|job work,computeridle|afk retro -off,nafk|-afk
	 * </p>
	 */
	BEEMINDER_GRAPHS(List.class, null),

	/**
	 * Your personal authentication token. Log in to your account, then
	 * visit https://www.beeminder.com/api/v1/auth_token.json to find it.
	 * Without this, TagTime will be unable to submit data.
	 */
	AUTH_TOKEN(String.class, null),

	/**
	 * The number of digits of precision to use when submitting time in
	 * hours.
	 */
	PRECISION(int.class, 2),

	/**
	 * If this is true, the next time you submit data to Beeminder,
	 * TagTime will check the data points on Beeminder's server and make
	 * sure they match your log file. This isn't necessary unless you
	 * manually update your log file, or you change BEEMINDER_GRAPHS.
	 */
	UPDATE_ALL_DATA(Boolean.class, true),

	/**
	 * The sound file to play. This file should be located in the sound
	 * directory. If no such file is found, no sound will be played, so
	 * set this to any non-matching value (for example, by removing the
	 * file extension) to disable the sound.
	 */
	SOUND_TO_PLAY(String.class, "whoosh.wav");
	
	/**
	 * The type of value stored in this setting. For example, the
	 * AVERAGE_GAP setting is a number of minutes, so this would be
	 * int.class.
	 */
	public final Class<?> valueClass;
	
	/**
	 * This setting's default value. For complicated objects like sets,
	 * this will most likely be null. If it is not null, it is required
	 * to be an instance of <code>valueClass</code>.
	 */
	public final Object defaultValue;
	
	<T> SettingType(Class<T> valueClass, T defaultValue) {
		this.valueClass = valueClass;
		
		this.defaultValue = defaultValue;
	}
}
