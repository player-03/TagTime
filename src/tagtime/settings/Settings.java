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

package tagtime.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import tagtime.Main;
import tagtime.util.TagCount;

/**
 * Handles the storage and retrieval of a given user's settings. Refer to
 * {@link SettingType} to see what values can be stored and retrieved.
 */
public class Settings {
	/**
	 * Instances of this class for each user.
	 */
	private static HashMap<String, Settings> instances = new HashMap<String, Settings>();
	
	/**
	 * Returns the settings manager for the given user name.
	 */
	public static Settings getInstance(String userName) {
		Settings instance = instances.get(userName);
		
		if(instance == null) {
			instance = new Settings(userName);
			instances.put(userName, instance);
		}
		
		return instance;
	}
	
	/**
	 * The associated user's username. If the user uses Beeminder, this
	 * is expected to match their Beeminder username.
	 */
	public final String username;
	
	/**
	 * The properties object used to store the settings.
	 */
	private final PropertiesConfiguration properties;
	
	/**
	 * A persistent instance of the TagCount list, so that it doesn't
	 * need to be rebuilt every time.
	 */
	private List<TagCount> tagCounts = null;
	
	/**
	 * Creates a new settings manager for the given user.
	 */
	private Settings(String userName) {
		//setup
		this.username = userName;
		//settingValues = new EnumMap<SettingType, Object>(SettingType.class);
		
		String fileLocation = Main.getDataDirectory().getPath()
					+ "/" + userName + ".properties";
		
		//load the file
		PropertiesConfiguration tempProperties;
		try {
			tempProperties = new PropertiesConfiguration(fileLocation);
		} catch(ConfigurationException e) {
			//file not found
			tempProperties = new PropertiesConfiguration();
			
			tempProperties.setPath(fileLocation);
			try {
				tempProperties.save();
			} catch(ConfigurationException e1) {
				e1.printStackTrace();
			}
		}
		
		properties = tempProperties;
		
		/*if(settingsFile.exists()) {
			//read the save file and load the settings
			BufferedReader in = new BufferedReader(new FileReader(settingsFile));
			readInSettings(in);
			
			//TagTime doesn't bother reading the file from here on; if the
			//user changes it, those changes will be overwritten
			in.close();
		} else {
			applyDefaultSettings();
			flush();
		}*/
	}
	
	/**
	 * Reads lines from the given settings file and determines the
	 * settings accordingly.
	 */
	/*private void readInSettings(BufferedReader in) {
		String currentLine;
		StringBuffer currentData = new StringBuffer();
		
		int firstSpace;
		SettingType currentSettingType = SettingType.WINDOW_X;
		
		boolean parsingCollection = false;
		
		while(true) {
			//read the next line, stopping if there are no more lines
			try {
				currentLine = in.readLine();
				if(currentLine == null) {
					break;
				}
			} catch(IOException e) {
				break;
			}
			
			//if this isn't in the middle of parsing a collection, parse
			//this line normally
			if(!parsingCollection) {
				//attempt to parse the line - a properly formatted line
				//starts with a SettingType, followed by a space, followed
				//by the relevant data
				firstSpace = currentLine.indexOf(' ');
				if(firstSpace < 0 || currentLine.length() < firstSpace + 1) {
					continue;
				}
				
				//find the setting type for the current line
				try {
					currentSettingType =
								SettingType.valueOf(SettingType.class,
													currentLine.substring(0, firstSpace));
				} catch(Exception e) {
					continue;
				}
				
				//find the data on this line
				currentData.delete(0, currentData.length());
				currentData.append(currentLine.substring(firstSpace + 1));
				
				//if the data starts with a bracket, strip the bracket
				//and set parsingCollection to true
				if(currentData.length() != 0
							&& currentData.charAt(0) == '[') {
					parsingCollection = true;
					currentData.deleteCharAt(0);
				}
			} else {
				//if in the middle of parsing a collection, just append
				//this line to the buffer
				currentData.append(currentLine);
			}
			
			//if the current collection ends on this line, strip the
			//final bracket
			if(parsingCollection && currentData.charAt(currentData.length() - 1) == ']') {
				currentData.deleteCharAt(currentData.length() - 1);
				parsingCollection = false;
			}
			
			//if the data is ready to be entered, convert it to the
			//appropriate type and enter it in the map
			if(!parsingCollection) {
				settingValues.put(currentSettingType,
									extractData(currentSettingType, currentData.toString()));
			}
		}
		
		applyDefaultSettings();
	}*/

	/**
	 * If any settings are left undefined, inserts the default value for
	 * those settings.
	 */
	/*private void applyDefaultSettings() {
		for(SettingType type : EnumSet.allOf(SettingType.class)) {
			if(!settingValues.containsKey(type)) {
				if(type.defaultValue != null) {
					settingValues.put(type, type.defaultValue);
				} else {
					//if a default value is not provided, it is because
					//the class is mutable or otherwise unable to be
					//stored statically; in this case, extractData
					//should be able to build an instance given the
					//class type
					settingValues.put(type, extractData(type, ""));
				}
			}

			if(properties.getProperty(type.toString()) == null) {
				properties.setProperty(type.toString(), extractData(type, "").toString());
			}
		}
	}*/

	/**
	 * Parses the given data string, returning the object it represents.
	 * @param type The type of object to return.
	 * @param substring The data to parse.
	 * @return The parsed object, or null if type wasn't recognized.
	 */
	/*private Object extractData(SettingType type, String data) {
		//most settings are numbers, strings, or booleans
		if(type.valueClass == int.class || type.valueClass == Integer.class) {
			try {
				return Integer.parseInt(data);
			} catch(NumberFormatException e) {
				return 0;
			}
		} else if(type.valueClass == double.class || type.valueClass == Double.class) {
			try {
				return Double.parseDouble(data);
			} catch(NumberFormatException e) {
				return 0;
			}
		} else if(type.valueClass == float.class || type.valueClass == Float.class) {
			try {
				return Float.parseFloat(data);
			} catch(NumberFormatException e) {
				return 0;
			}
		} else if(type.valueClass == String.class) {
			return data;
		} else if(type.valueClass == Boolean.class) {
			return data.equals("true");
		}
		
		//more advanced settings have to be handled directly; there's no
		//better way to specify what the generics should be (at least,
		//none that I can think of)
		switch(type) {
			case BEEMINDER_GRAPHS:
				assert type.valueClass == HashSet.class;
				
				if(data.length() == 0) {
					return new HashSet<String>();
				}
				
				return new HashSet<String>(Arrays.asList(data.split(",\\s*")));
			case CACHED_TAGS:
				assert type.valueClass == TreeSet.class;
				
				TreeSet<TagCount> set = new TreeSet<TagCount>();
				List<String> dataList = Arrays.asList(data.split(",\\s*"));
				int delim;
				
				for(String s : dataList) {
					delim = s.indexOf(':');
					if(delim > 0 && delim < s.length() - 1) {
						try {
							set.add(new TagCount(s.substring(0, delim),
										Integer.parseInt(s.substring(delim + 1))));
						} catch(NumberFormatException e) {
							set.add(new TagCount(s.substring(0, delim), 0));
						}
					}
				}
				
				return set;
			default:
		}
		
		System.err.println("Setting type " + type + " unaccounted for.");
		
		return null;
	}*/

	/**
	 * Writes the user's settings to the save file.
	 */
	public void flush() {
		if(tagCounts != null) {
			List<String> tagCountsAsStrings = new ArrayList<String>(tagCounts.size());
			for(TagCount tagCount : tagCounts) {
				tagCountsAsStrings.add(tagCount.toString());
			}
			properties.setProperty(SettingType.CACHED_TAGS.toString(),
						tagCountsAsStrings);
		}
		
		try {
			properties.save();
		} catch(ConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	public int getIntValue(SettingType setting) {
		assert setting.valueClass == int.class;
		
		return properties.getInt(setting.toString(),
						(Integer) setting.defaultValue);
	}
	
	public boolean getBooleanValue(SettingType setting) {
		assert setting.valueClass == boolean.class;
		
		return properties.getBoolean(setting.toString(),
						(Boolean) setting.defaultValue);
	}
	
	public String getStringValue(SettingType setting) {
		assert setting.valueClass == String.class;
		
		return properties.getString(setting.toString(),
						(String) setting.defaultValue);
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getListValue(SettingType setting) {
		assert setting.valueClass == List.class;
		
		return properties.getList(setting.toString(),
							new ArrayList<String>());
	}
	
	public List<TagCount> getTagCounts(SettingType setting) {
		if(tagCounts == null) {
			List<String> list = getListValue(setting);
			tagCounts = new ArrayList<TagCount>(list.size());
			for(String s : list) {
				tagCounts.add(new TagCount(s));
			}
		}
		
		Collections.sort(tagCounts);
		
		return tagCounts;
	}
	
	/**
	 * Sets the value of the given setting.
	 */
	public void setValue(SettingType setting, Object value) {
		properties.setProperty(setting.toString(), value);
	}
	
	public void incrementTagCounts(SettingType setting,
							List<? extends String> values) {
		List<TagCount> tagCounts = getTagCounts(setting);
		
		//this is inefficient, but I can't think of a better working alternative
		for(TagCount tC : tagCounts) {
			if(values.contains(tC.getTag())) {
				tC.increment();
				
				values.remove(tC.getTag());
			}
		}
		
		for(String newTag : values) {
			tagCounts.add(new TagCount(newTag));
		}
	}
}
