package tagtime.beeminder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.http.client.HttpClient;

public class DataPoint {
	private static final Calendar calendar = new GregorianCalendar();
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy'/'MM'/'dd");
	
	/**
	 * The identifier for the data point on Beeminder. This will be null
	 * if the data point does not yet exist on Beeminder.
	 */
	public String id;
	
	public boolean toBeRemoved = false;
	public boolean toBeUpdated = false;
	
	public final long timestamp;
	public double hours;
	
	public String comment;
	
	public DataPoint(long timestamp, double hours) {
		id = null;
		this.timestamp = getStartOfDay(timestamp);
		this.hours = hours;
		comment = "";
	}
	
	public DataPoint(String id, long timestamp, double hours, String comment) {
		this.id = id;
		this.timestamp = getStartOfDay(timestamp);
		this.hours = hours;
		this.comment = comment;
	}
	
	@Override
	public String toString() {
		return (isToBeRemoved() ? "removing "
					: isToBeCreated() ? "creating "
								: isToBeUpdated() ? "updating " : "")
					+ DATE_FORMAT.format(new Date(timestamp * 1000)) + ": " + hours;
	}
	
	public void checkMerge(DataPoint other) {
		if(other.timestamp == timestamp) {
			hours += other.hours;
			toBeUpdated = true;
			other.toBeRemoved = true;
			
			//merge the comments
			if(other.comment.length() > 0) {
				if(comment.length() > 0) {
					comment = other.comment + "; " + comment;
				} else {
					comment = other.comment;
				}
			}
		}
	}
	
	/**
	 * Submits this data point's information to Beeminder, if necessary.
	 * @param saveID Whether to save the data point's ID as returned by
	 *            Beeminder. This only applies when creating a new data
	 *            point.
	 * @return Whether it is ok to continue submitting data.
	 */
	public boolean submit(HttpClient client, BeeminderGraph graph,
				boolean saveID) {
		if(isToBeRemoved()) {
			return BeeminderAPI.deleteDataPoint(client, graph, this);
		} else if(isToBeCreated()) {
			return BeeminderAPI.createDataPoint(client, graph, this, saveID);
		} else if(isToBeUpdated()) {
			return BeeminderAPI.updateDataPoint(client, graph, this);
		}
		
		return true;
	}
	
	public boolean isToBeCreated() {
		return id == null;
	}
	
	public boolean isToBeUpdated() {
		return toBeUpdated;
	}
	
	public boolean isToBeRemoved() {
		return toBeRemoved;
	}
	
	/**
	 * Synchronized just in case. TODO: Is this necessary and does it
	 * slow anything down?
	 * @return The UNIX timestamp representing midnight of the day the
	 *         given timestamp.
	 */
	public static synchronized long getStartOfDay(long timestamp) {
		//timestamps are stored in seconds, but the Calendar class uses
		//milliseconds
		calendar.setTimeInMillis(timestamp * 1000);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		
		//convert back from milliseconds to seconds
		return calendar.getTimeInMillis() / 1000;
	}
}
