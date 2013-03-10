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

package tagtime.beeminder;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import tagtime.Main;
import tagtime.TagTime;
import tagtime.settings.SettingType;
import tagtime.settings.Settings;

/**
 * Contains static methods for sending/retrieving information to/from
 * Beeminder, used by the BeeminderGraph class. Instances of this class
 * manage a user's BeeminderGraph objects. <br>
 * TODO: Refactor one of these two responsibilities into a new class.
 */
public class BeeminderAPI {
	private static final JSONParser JSON_PARSER = new JSONParser();
	private static final String API_BASE_URL = "https://www.beeminder.com/api/v1";
	
	private final Settings userSettings;
	private final List<BeeminderGraph> graphData;
	
	public BeeminderAPI(TagTime tagTimeInstance, Settings userSettings) throws ClassCastException {
		this.userSettings = userSettings;
		
		String username = tagTimeInstance.username;
		
		Collection<String> graphDataEntries = userSettings
					.getListValue(SettingType.BEEMINDER_GRAPHS);
		
		graphData = new ArrayList<BeeminderGraph>(graphDataEntries.size());
		for(String dataEntry : graphDataEntries) {
			graphData.add(new BeeminderGraph(tagTimeInstance, username, dataEntry));
		}
	}
	
	/**
	 * Submits the current user's data to each registered graph.
	 */
	public void submit() {
		String username = userSettings.username;
		
		File logFile = new File(Main.getDataDirectory().getPath()
					+ "/" + username + ".log");
		
		for(BeeminderGraph data : graphData) {
			data.submitPings(logFile);
		}
	}
	
	public static long fetchResetDate(HttpClient client,
				String graphName, TagTime tagTimeInstance) {
		//this functionality is not implemented and may never be
		/*List<JSONObject> parsedArray = runGetRequest(client,
					getGraphURL(tagTimeInstance, graphName),
					tagTimeInstance);
		if(parsedArray == null) {
			return 0;
		}
		
		//Unless the API is updated, the request will return one object.
		JSONObject data = parsedArray.get(0);
		if(data.containsKey("reset")) {
			return (Long) data.get("reset");
		}*/
		
		return 0;
	}
	
	/**
	 * @param id The id of the data point to retrieve.
	 * @param timestamp The timestamp to use. Required because Beeminder
	 *            will not provide it, unlike when fetching all points.
	 */
	public static DataPoint fetchDataPoint(HttpClient client, String graphName,
				TagTime tagTimeInstance, String id, long timestamp) {
		if(id == null || id.length() == 0) {
			return null;
		}
		
		List<JSONObject> parsedData = runGetRequest(client,
					getDataPointURL(tagTimeInstance, graphName, id),
					tagTimeInstance);
		
		if(parsedData == null) {
			return null;
		}
		
		JSONObject jsonDataPoint = parsedData.get(0);
		
		return new DataPoint((String) jsonDataPoint.get("id"),
					timestamp,
					(Double) jsonDataPoint.get("value"),
					(String) jsonDataPoint.get("comment"));
	}
	
	public static List<DataPoint> fetchAllDataPoints(HttpClient client,
				String graphName, TagTime tagTimeInstance) {
		List<JSONObject> parsedData = runGetRequest(client,
					getDataURL(tagTimeInstance, graphName),
					tagTimeInstance);
		if(parsedData == null) {
			return null;
		}
		
		//convert the data to a set of data points
		List<DataPoint> dataPoints = new ArrayList<DataPoint>();
		DataPoint dataPoint;
		DataPoint prevDataPoint = null;
		int insertIndex;
		
		JSONObject jsonDataPoint;
		for(Object inputDataPoint : parsedData) {
			if(inputDataPoint instanceof JSONObject) {
				jsonDataPoint = (JSONObject) inputDataPoint;
				dataPoint = new DataPoint((String) jsonDataPoint.get("id"),
										(Long) jsonDataPoint.get("timestamp"),
										(Double) jsonDataPoint.get("value"),
										(String) jsonDataPoint.get("comment"));
				
				//insert the new data point such that all data points are
				//in order, iterating backwards so that the most common
				//case (already being in order) is handled fastest
				if(prevDataPoint != null && prevDataPoint.timestamp > dataPoint.timestamp) {
					for(insertIndex = dataPoints.size() - 2; insertIndex >= 0; insertIndex--) {
						if(dataPoints.get(insertIndex).timestamp <= dataPoint.timestamp) {
							break;
						}
					}
					dataPoints.add(insertIndex + 1, dataPoint);
				} else {
					dataPoints.add(dataPoint);
				}
				
				prevDataPoint = dataPoint;
			}
		}
		
		return dataPoints;
	}
	
	/**
	 * Creates a new data point on Beeminder.
	 * @param saveID
	 * @return Whether the request completed successfully. If this is
	 *         false, then Beeminder is probably inaccessible, and no
	 *         more requests should be sent for now.
	 */
	public static boolean createDataPoint(HttpClient client,
				BeeminderGraph graph, DataPoint dataPoint,
				boolean saveID) {
		HttpResponse response = runPostRequest(client, graph.tagTimeInstance,
					getDataURL(graph.tagTimeInstance, graph.graphName),
					buildPostData(new String[] {
								"timestamp", Long.toString(dataPoint.timestamp),
								"value", graph.hourFormatter.format(dataPoint.hours)}));
		
		if(response != null) {
			if(saveID) {
				List<JSONObject> parsedResponse = parseResponse(response);
				
				if(parsedResponse.size() > 0) {
					graph.writeToBeeFile((String) parsedResponse.get(0).get("id"),
								dataPoint.timestamp, dataPoint.hours,
								dataPoint.comment);
				}
			} else {
				//parseResponse consumes the response, so this only needs
				//to be called if parseResponse isn't
				try {
					EntityUtils.consume(response.getEntity());
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
			
			return true;
		}
		
		System.err.println("Unable to submit your data to Beeminder " +
					"graph " + graph.graphName + ". Please try again later.");
		return false;
	}
	
	/**
	 * Updates an existing data point on Beeminder.
	 * @return Whether the request completed successfully. If this is
	 *         false, then Beeminder is probably inaccessible, and no
	 *         more requests should be sent for now.
	 */
	public static boolean updateDataPoint(HttpClient client, BeeminderGraph graph,
				DataPoint dataPoint) {
		List<NameValuePair> postData = buildPostData(new String[] {
					"timestamp", Long.toString(dataPoint.timestamp),
					"value", graph.hourFormatter.format(dataPoint.hours)});
		
		if(dataPoint.comment.length() > 0) {
			postData.add(new BasicNameValuePair("comment", dataPoint.comment));
		}
		
		if(runPutRequest(client, graph.tagTimeInstance,
					getDataPointURL(graph.tagTimeInstance, graph.graphName, dataPoint.id),
					postData)) {
			return true;
		}
		
		System.err.println("Unable to submit your data to Beeminder " +
					"graph " + graph.graphName + ". Please try again later.");
		return false;
	}
	
	/**
	 * Removes a data point from Beeminder.
	 * @return Whether the request completed successfully. If this is
	 *         false, then Beeminder is probably inaccessible, and no
	 *         more requests should be sent for now.
	 */
	public static boolean deleteDataPoint(HttpClient client, BeeminderGraph graph,
				DataPoint dataPoint) {
		if(runDeleteRequest(client, graph.tagTimeInstance,
					getDataPointURL(graph.tagTimeInstance, graph.graphName, dataPoint.id))) {
			return true;
		}
		
		System.err.println("Unable to submit your data to Beeminder " +
					"graph " + graph.graphName + ". Please try again later.");
		return false;
	}
	
	private static boolean runDeleteRequest(HttpClient client, TagTime tagTimeInstance,
				String targetURL) {
		HttpDelete deleteRequest = new HttpDelete(targetURL
					+ getAuthTokenToAppend(tagTimeInstance));
		
		System.out.println("DELETE " + deleteRequest.getURI());
		
		HttpResponse response;
		try {
			response = client.execute(deleteRequest);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		
		StatusLine status = response.getStatusLine();
		
		System.out.println("Response: " + status.getStatusCode()
					+ " " + status.getReasonPhrase());
		
		try {
			EntityUtils.consume(response.getEntity());
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	/**
	 * Runs a put request.
	 * @return Whether the request completed successfully. If this is
	 *         false, then Beeminder is probably inaccessible, and no
	 *         more requests should be sent for now.
	 */
	private static boolean runPutRequest(HttpClient client, TagTime tagTimeInstance,
				String dataURL, List<NameValuePair> postData) {
		//add the authorization token
		postData.add(new BasicNameValuePair("auth_token",
					tagTimeInstance.settings.getStringValue(SettingType.AUTH_TOKEN)));
		
		//build the request
		HttpPut putRequest = new HttpPut(dataURL);
		HttpResponse response;
		
		try {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postData);
			System.out.println("PUT " + dataURL + "?"
						+ new BufferedReader(new InputStreamReader(entity.getContent()))
									.readLine());
			
			putRequest.setEntity(entity);
			
			response = client.execute(putRequest);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		
		StatusLine status = response.getStatusLine();
		
		System.out.println("Response: " + status.getStatusCode()
					+ " " + status.getReasonPhrase());
		
		try {
			EntityUtils.consume(response.getEntity());
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	/**
	 * Runs a post request.
	 * @return The response if the request completed successfully, or
	 *         null otherwise. If this is null, then Beeminder is
	 *         probably inaccessible, and no more requests should be sent
	 *         for now. Otherwise, make sure to run EntityUtils.consume()
	 *         on it.
	 */
	private static HttpResponse runPostRequest(HttpClient client, TagTime tagTimeInstance,
				String dataURL, List<NameValuePair> postData) {
		//add the authorization token
		postData.add(new BasicNameValuePair("auth_token",
					tagTimeInstance.settings.getStringValue(SettingType.AUTH_TOKEN)));
		
		//build the request
		HttpPost postRequest = new HttpPost(dataURL);
		HttpResponse response;
		
		try {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postData);
			System.out.println("POST " + dataURL + "?"
						+ new BufferedReader(new InputStreamReader(entity.getContent()))
									.readLine());
			
			postRequest.setEntity(entity);
			
			response = client.execute(postRequest);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
		StatusLine status = response.getStatusLine();
		
		System.out.println("Response: " + status.getStatusCode()
					+ " " + status.getReasonPhrase());
		
		return response;
	}
	
	private static List<JSONObject> runGetRequest(HttpClient client, String url,
				TagTime tagTimeInstance) {
		HttpGet getRequest = new HttpGet(
					url + getAuthTokenToAppend(tagTimeInstance));
		HttpResponse response;
		
		//retrieve the data
		try {
			response = client.execute(getRequest);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
		if(response.getStatusLine().getStatusCode() == 401) {
			System.err.println("Invalid authorization token. Visit " +
						"https://www.beeminder.com/api/v1/auth_token.json to " +
						"get your token, then add it to your settings file.");
			return null;
		}
		
		return parseResponse(response);
	}
	
	private static List<JSONObject> parseResponse(HttpResponse response) {
		OutputStream data;
		try {
			data = new ByteArrayOutputStream(response.getEntity().getContent().available());
			response.getEntity().writeTo(data);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
		Object parseResult;
		try {
			parseResult = JSON_PARSER.parse(data.toString());
		} catch(ParseException e) {
			System.err.println(data.toString());
			e.printStackTrace();
			parseResult = null;
		}
		
		//close the stream used to read the response
		try {
			data.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		//This might be redundant, but leave it even so, in case the HTTP
		//library changes.
		try {
			EntityUtils.consume(response.getEntity());
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		//JSONParser.parse() can return multiple object types, depending
		//on the JSON itself
		List<JSONObject> parsedArray = null;
		if(parseResult instanceof JSONArray) {
			parsedArray = new ArrayList<JSONObject>();
			for(Object element : (JSONArray) parseResult) {
				if(element instanceof JSONObject) {
					parsedArray.add((JSONObject) element);
				} else {
					//Currently, it's safe to ignore any sub-arrays.
					//(Sorry if this messes things up in the future.)
				}
			}
		} else if(parseResult instanceof JSONObject) {
			parsedArray = new ArrayList<JSONObject>(1);
			
			parsedArray.add((JSONObject) parseResult);
		} else {
			System.out.println("Unknown result from JSON parser: " + parseResult);
			parsedArray = new ArrayList<JSONObject>();
		}
		
		return parsedArray;
	}
	
	/**
	 * @param dataPairs The parameters and values to post, in string
	 *            form. They will be parsed in order, like so: [param0,
	 *            value0, param1, value1, ...], and if there are an odd
	 *            number, the last will be ignored. These parameters do
	 *            not need to include the authorization token; that will
	 *            be added.
	 * @return The data pairs, in the correct format to be used as an
	 *         HTML entity.
	 */
	private static List<NameValuePair> buildPostData(String[] dataPairs) {
		List<NameValuePair> postData = new ArrayList<NameValuePair>();
		for(int i = 0; i < dataPairs.length - 1; i += 2) {
			postData.add(new BasicNameValuePair(dataPairs[i], dataPairs[i + 1]));
		}
		
		return postData;
	}
	
	//unused
	/*private static String getGraphURL(TagTime tagTimeInstance, String graphName) {
		return API_BASE_URL + "/users/" + tagTimeInstance.username
					+ "/goals/" + graphName + ".json";
	}*/
	
	private static String getDataURL(TagTime tagTimeInstance, String graphName) {
		return API_BASE_URL + "/users/" + tagTimeInstance.username
					+ "/goals/" + graphName + "/datapoints.json";
	}
	
	private static String getDataPointURL(TagTime tagTimeInstance, String graphName,
									String dataPointID) {
		return API_BASE_URL + "/users/" + tagTimeInstance.username
					+ "/goals/" + graphName
					+ "/datapoints/" + dataPointID + ".json";
	}
	
	private static String getAuthTokenToAppend(TagTime tagTimeInstance) {
		return "?auth_token=" + tagTimeInstance.settings.getStringValue(SettingType.AUTH_TOKEN);
	}
}
