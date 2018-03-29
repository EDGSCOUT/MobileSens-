package wsi.mobilesens.sensors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.EventLog;
import android.util.EventLog.Event;
import android.util.Log;

/**
 * Reads the logs for which applications have been started, restarted, resumed,
 * or paused to get an idea for what applications have been used.
 * 
 * There should never exist an instance of this class. Its constructor should be
 * called exactly once to initialize the class. All subsequent calls should be
 * to the static functions.
 * 
 * @author Hossein Falaki and John Jenkins
 * @version 1.0
 */
public class EventLogger {
	private static final String TAG = "EventLogger";

	private static final String EVENT_LOG_TAGS_FILE = "/system/etc/event-log-tags";

	private Set<String> mActivityEvents; // activity
	private Set<String> mServiceEvents; // service

	private HashMap<Integer, TagInfo> mTags;

	private long mLastUpdate;

	private JSONObject mActivity, mService;

	public EventLogger() {
		Log.i(TAG, "EventLogger()");

		mTags = new HashMap<Integer, TagInfo>();

		mActivityEvents = new HashSet<String>();
		mServiceEvents = new HashSet<String>();

		mActivityEvents.add("am_create_activity");
		mActivityEvents.add("am_restart_activity");
		mActivityEvents.add("am_resume_activity");
		mActivityEvents.add("am_pause_activity");
		mActivityEvents.add("am_destroy_activity");
		mActivityEvents.add("am_relaunch_activity");
		mActivityEvents.add("am_finish_activity");

		mServiceEvents.add("am_create_service");
		mServiceEvents.add("am_destroy_service");

		mLastUpdate = 0L;

		getTags();
	}

	/**
	 * Reads the event log tags file to populate the internal information about
	 * the structure of the logs. This information is then used later when
	 * retrieving logging information.
	 * 
	 * This file follows the following format: <tag_id> <tag_name>
	 * <tag_descriptor>
	 * 
	 * We parse this file based on the 'tag_name's we want to get the 'tag_id'
	 * and 'tag_descriptor'. We then further parse the 'tag_descriptor' to get
	 * the specific descriptor we want. Android provides a similar function that
	 * parses this file, based on a 'tag_name', for the 'tag_id', but it is very
	 * slow; therefore, we do it once and remember it to speed things up. For
	 * more information see 'readAndParseFile()' and 'getComponentIndex()'.
	 * 
	 * @see readAndParseFile
	 * @see getComponentIndex
	 */
	private void getTags() {
		File tagsFile = new File(EVENT_LOG_TAGS_FILE); // EVENT_LOG_TAGS_FILE =
														// "/system/etc/event-log-tags";

		if (!tagsFile.exists()) {
			Log.e(TAG, "There is no event logs in tags file.");
			return;
		}
		else if (!tagsFile.canRead()) {
			Log.e(TAG, "Cannot read the event logs tags file.");
			return;
		}
		else if (tagsFile.length() == 0L) {
			Log.e(TAG, "The logs tags file claims to have a length of zero.");
			return;
		}

		readAndParseFile(tagsFile);
	}

	/**
	 * Parses the event log tags file by finding the relevant tags and
	 * populating the local information with the structure of these logs.
	 * 
	 * @param theFile
	 *            A File object that should already be attached to the event log
	 *            tags file.
	 */
	private void readAndParseFile(File theFile) {
		Log.i(TAG, "readAndParseFile()");

		try {
			BufferedReader reader = new BufferedReader(new FileReader(theFile)); // 读取文件
																					// EVENT_LOG_TAGS_FILE
																					// =
																					// "/system/etc/event-log-tags";
			String currLine;
			ActivityTagInfo appTagInfo;
			ServiceTagInfo serviceTagInfo;

			while ((currLine = reader.readLine()) != null) {
				String[] lineInfo = currLine.split(" ", 3);

				String currType = lineInfo[1]; // type such as
												// "am_create_activity",
												// "am_create_service"
				if (mActivityEvents.contains(currType)) // activity
				{
					appTagInfo = new ActivityTagInfo(currType);

					appTagInfo.id = Integer.parseInt(lineInfo[0]);
					appTagInfo.componentNameIndex = getIndex(lineInfo[2],
							"Component Name");
					appTagInfo.actionIndex = getIndex(lineInfo[2], "Action");
					appTagInfo.taskIndex = getIndex(lineInfo[2], "Task ID");

					mTags.put(appTagInfo.id, appTagInfo);

				}
				else if (mServiceEvents.contains(currType)) { // service

					serviceTagInfo = new ServiceTagInfo(currType);

					serviceTagInfo.id = Integer.parseInt(lineInfo[0]);
					serviceTagInfo.recordIndex = getIndex(lineInfo[2],
							"Service Record");
					serviceTagInfo.nameIndex = getIndex(lineInfo[2], "Name");
					serviceTagInfo.pidIndex = getIndex(lineInfo[2], "PID");
					serviceTagInfo.intentIndex = getIndex(lineInfo[2], "Intent");

					mTags.put(serviceTagInfo.id, serviceTagInfo);
				}
			}
		}
		catch (FileNotFoundException e) {
			Log.e(TAG, "The event logs tags file was not found.");
		}
		catch (IOException e) {
			Log.e(TAG, "Couldn't read the entire event logs tags file.");
		}
	}

	/**
	 * Parses the component descriptor string to find the index among the list
	 * of descriptions that matches "Component Name". This is the value that is
	 * used by Android to describe the Application and Activity in that
	 * Application and is the value we store in the database to associate who is
	 * calling this event.
	 * 
	 * @param componentDescriptor
	 *            A String pulled from the event log tags file that describes
	 *            all the types of information that Android records when an
	 *            event takes place.
	 * 
	 * @return The index of the "Component Name" in the descriptor.
	 */
	private int getIndex(String componentDescriptor, String component) {
		String[] params = componentDescriptor.split(",");
		int currIndex = 0;
		int compLen = component.length();

		while (currIndex < params.length) {
			try {
				if (params[currIndex].substring(1, compLen + 1).equals(
						component)) {
					return currIndex;
				}
			}
			catch (StringIndexOutOfBoundsException e) {
			}
			currIndex++;
		}

		return -1;
	}

	/**
	 * Updates the database with the events that have taken place since the last
	 * call to this function.
	 */
	public void update() {
		mActivity = new JSONObject();
		mService = new JSONObject();

		int[] tagsToRetrieve = new int[mTags.size()];

		int index = 0;
		for (TagInfo curTag : mTags.values())
			tagsToRetrieve[index++] = curTag.id;

		Collection<EventLog.Event> log = new LinkedList<EventLog.Event>();

		try {
			EventLog.readEvents(tagsToRetrieve, log); // 根据 id[], 读取 log
		}
		catch (IOException e) {
			Log.e(TAG, "Exception when trying to readEvents()", e);
			return;
		}

		Log.i(TAG, "Get " + log.size() + " events");

		long greatestEventTime = 0L;
		long eventTime;
		int eventID;

		TagInfo refTag; // tag info
		ActivityTagInfo activityTag; // tag info for activity
		ServiceTagInfo serviceTag; // tag info for service

		Object[] data;

		String componentNameStr;
		String serviceName = "";
		JSONObject eventObject;

		for (Event currEvent : log) {
			eventTime = System.currentTimeMillis();//currEvent.getTimeNanos();			
			eventID = currEvent.getTag();

			if (eventTime > mLastUpdate) {
				if (eventTime > greatestEventTime)
					greatestEventTime = eventTime;

				refTag = mTags.get(eventID); // TagInfo
				data = (Object[]) currEvent.getData(); //

				if (refTag.type == TagInfo.ACTIVITY) // activity
				{
					activityTag = (ActivityTagInfo) refTag;

					componentNameStr = "";
					if (activityTag.componentNameIndex != -1)
						componentNameStr = (String) ((Object[]) data)[activityTag.componentNameIndex];

					eventObject = new JSONObject();
					if (componentNameStr != null && componentNameStr.length()!=0) {
						try {

							eventObject.put("Event", activityTag.tagName);
							eventObject.put("Activity", componentNameStr);
							mActivity.put(String.valueOf(eventTime),
									eventObject);
						}
						catch (JSONException je) {
							Log.e(TAG, "JSON Exception", je);
						}
					}
				}
				else if (refTag.type == TagInfo.SERVICE) { // service

					serviceTag = (ServiceTagInfo) refTag;
					serviceName = (String) ((Object[]) data)[serviceTag.nameIndex];

					eventObject = new JSONObject();
					if (serviceName != null && serviceName.length()!=0) {

						try {
							eventObject.put("Event", serviceTag.tagName);
							eventObject.put("Name", serviceName);
							mService.put(String.valueOf(eventTime), eventObject);
						}
						catch (JSONException je) {
							Log.e(TAG, "JSON Exception", je);
						}
					}
				}
			}
		}

		if (greatestEventTime != 0) {
			mLastUpdate = greatestEventTime;
		}
	}

	public JSONObject getActivityEvents() {
		return mActivity;
	}

	public JSONObject getServiceEvents() {
		return mService;
	}
}

class TagInfo {
	public String tagName;
	public int id;
	public int type;

	public final static int ACTIVITY = 1;
	public final static int SERVICE = 2;

	public TagInfo(String name, int inType) {
		tagName = name;
		type = inType;
		id = -1;
	}
}

class ActivityTagInfo extends TagInfo {
	public int componentNameIndex;
	public int actionIndex;
	public int taskIndex;

	public ActivityTagInfo(String name) {
		super(name, TagInfo.ACTIVITY);
		componentNameIndex = actionIndex = taskIndex = -1;
	}
}

class ServiceTagInfo extends TagInfo {
	public int recordIndex;
	public int nameIndex;
	public int intentIndex;
	public int pidIndex;

	public ServiceTagInfo(String name) {
		super(name, TagInfo.SERVICE);
		recordIndex = nameIndex = intentIndex = pidIndex = -1;
	}
}
