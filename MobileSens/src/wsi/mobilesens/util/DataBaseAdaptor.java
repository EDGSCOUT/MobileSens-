package wsi.mobilesens.util;

import org.json.JSONObject;
import org.json.JSONException;

import wsi.mobilesens.MobileSens;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.HashSet;

import android.R.integer;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.PowerManager;
import android.util.Log;

/**
 * Simple database access helper class. Interfaces with the SQLite database to
 * store system information. Written based on sample code provided by Google.
 * 
 * @author Hossein Falaki
 */
public class DataBaseAdaptor {
	private static final String TAG = "DataBaseAdapter";

	private static final String MAC_ADDRESS = MobileSens.MAC_ADDRESS;
	private static final String IMEI=MobileSens.IMEI;

	public static final String KEY_ROWID = "_id";
	public static final String KEY_DATARECORD = "datarecord";
	public static final String KEY_TYPE = "recordtype";
	public static final String KEY_TIME = "recordtime";
	public String fileName;
	public String mStore = "/sdcard/systemlog/";
	public int num;
	public int name;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/** Database creation sql statement */
	private static final String DATABASE_CREATE = "create table systemsens ("
			+ "_id integer primary key  autoincrement, "
			+ "recordtime text not null,  " + "recordtype text not null, "
			+ "datarecord text not null);";

	private static final String DATABASE_NAME = "data"; // database
	private static final String DATABASE_TABLE = "systemsens"; // table
	private static final int DATABASE_VERSION = 4;

	
	private HashSet<ContentValues> mBuffer;
	private HashSet<ContentValues> tempBuffer;

	private boolean mOpenLock = false;
	private boolean mFlushLock = false;

	private final Context mCtx;
	private final PowerManager.WakeLock mWL;

	// helper
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS systemsens");
			onCreate(db);
		}
	}

	// Constructor
	public DataBaseAdaptor(Context ctx) {
		this.mCtx = ctx;
		mBuffer = new HashSet<ContentValues>();

		PowerManager pm = (PowerManager) ctx
				.getSystemService(Context.POWER_SERVICE);
		mWL = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		mWL.setReferenceCounted(false);
	}

	public synchronized DataBaseAdaptor open() throws SQLException { // open
																		// database
		Log.i(TAG, "open()");

		if (!mFlushLock) {
			mDbHelper = new DatabaseHelper(mCtx);
			mDb = mDbHelper.getWritableDatabase(); // write
		}
		mOpenLock = true;
		return this;
	}

	public synchronized void close() { // close database
		Log.i(TAG, "close()");

		if (!mFlushLock) {
			mDb.close();
			mDbHelper.close();
		}
		mOpenLock = false;
	}

	public synchronized void createEntry(JSONObject data, String type) {

		JSONObject dataRecord = new JSONObject();
		// First thing, get the current time
		Calendar cal = Calendar.getInstance();
		String timeStr = "" + cal.get(Calendar.YEAR) + "-"
				+ (cal.get(Calendar.MONTH) + 1) + "-"
				+ cal.get(Calendar.DAY_OF_MONTH) + " "
				+ cal.get(Calendar.HOUR_OF_DAY) + ":"
				+ cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);

		try {
			dataRecord.put("date", timeStr);
//			dataRecord.put("time_stamp", cal.getTimeInMillis());
			dataRecord.put("user", MAC_ADDRESS);
			dataRecord.put("imei", IMEI);
			dataRecord.put("type", type);
			dataRecord.put("data", data);
		}
		catch (JSONException e) {
			Log.e(TAG, "Exception", e);
		}

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TIME, timeStr);
		initialValues.put(KEY_TYPE, type);
		initialValues.put(KEY_DATARECORD, dataRecord.toString());

		BufferedWriter out = null;
		try {
			File file = new File(mStore);
			if (!file.exists()) {
				file.mkdir();
			}
			fileName = mStore + MAC_ADDRESS.replace(':','.') + '_' + type + ".info";
			out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(fileName, true)));
			out.write(initialValues.toString());
			out.newLine();
			out.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		 mBuffer.add(initialValues);

		// Log.i(TAG, "Creating data record" + dataRecord.toString());
		// return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	public void flushDb() {
		Log.i(TAG, "flushDb()");

		synchronized (this) {
			tempBuffer = mBuffer;
			mBuffer = new HashSet<ContentValues>();
		}

		Thread flushThread = new Thread() { // thread
			public void run() {
				mWL.acquire();

				if (!mOpenLock) {
					try {
						mDbHelper = new DatabaseHelper(mCtx);
						mDb = mDbHelper.getWritableDatabase();
					}
					catch (SQLException se) {
						Log.e(TAG, "Could not open DB helper", se);
					}
				}
				mFlushLock = true;

				Log.i(TAG, "Flushing " + tempBuffer.size() + " records.");

				for (ContentValues value : tempBuffer) {
					mDb.insert(DATABASE_TABLE, null, value); // insert
																// contentValues
				}

				if (!mOpenLock) {
					mDb.close();
					mDbHelper.close();
				}

				mFlushLock = false;
				mWL.release();
			}
		};

		flushThread.start(); // start thread
	}

	public synchronized boolean deleteEntry(long rowId) {
		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public synchronized boolean deleteRange(long fromId, long toId) {
		return mDb.delete(DATABASE_TABLE, KEY_ROWID + " BETWEEN " + fromId
				+ " AND " + toId, null) > 0;
	}

	// fetch all
	public Cursor fetchAllEntries() {
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_TIME,
				KEY_TYPE, KEY_DATARECORD }, null, null, null, null, null);
	}
	
	public Cursor fetchCountEntries(int count,int maxId){
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_TIME,
				KEY_TYPE, KEY_DATARECORD }, KEY_ROWID+">"+maxId, null, null, null, null,String.valueOf(count));
	}
	
	public synchronized boolean deleteCountEntries(int count){
		return mDb.delete(DATABASE_TABLE,null, null) > 0;
	}
	
	public int fetchAllEntriesCount(){
		Cursor cursor=mDb.rawQuery("select count(*) from "+DATABASE_TABLE,null);
		if(cursor.moveToFirst()){
			return cursor.getInt(0);
		}
		return 0;
		//return mDb.query(DATABASE_TABLE, columns, selection, selectionArgs, groupBy, having, orderBy)
	}
	
	

	// fetch by id
	public Cursor fetchEntry(long rowId) throws SQLException {
		Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] {
				KEY_ROWID, KEY_TIME, KEY_TYPE, KEY_DATARECORD }, KEY_ROWID
				+ "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

}
