/** 
 *
 * Copyright (c) 2011, The Regents of the University of California. All
 * rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *   * Redistributions of source code must retain the above copyright
 *   * notice, this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright
 *   * notice, this list of conditions and the following disclaimer in
 *   * the documentation and/or other materials provided with the
 *   * distribution.
 *
 *   * Neither the name of the University of California nor the names of
 *   * its contributors may be used to endorse or promote products
 *   * derived from this software without specific prior written
 *   * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT
 * HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package wsi.mobilesens.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.database.Cursor;
import android.util.Log;
import wsi.mobilesens.MobileSens;

public class Uploader {
	private static final String TAG = "Uploader";

	private DataBaseAdaptor mDbAdaptor;

	private static final int MAX_UPLOAD_SIZE = 100; // Maximum number of records
													// that will be read and
													// deleted at a time
	private static final int MAX_FAIL_COUNT = 2; // After this number of
													// failiurs upload will
													// abort
	private static final String CUSTOM_URL = "HTTP://ccpl.psych.ac.cn:10005/CellPhone/index.php"; // Upload
																							// location
																							// of
																							// the
																							// SystemSens
																							// server

	class Node {
		ArrayList<String> content;
		int min;
		int max;
	}

	public Uploader(DataBaseAdaptor dbAdaptor) {
		this.mDbAdaptor = dbAdaptor;
	}

	public void tryUpload() {
		Log.i(TAG, "tryUpload() start...");

		// Cursor cursor = null;
		boolean postResult = false;

		try {
			mDbAdaptor.open(); // open database

			// cursor = mDbAdaptor.fetchAllEntries();
			int dbSize = mDbAdaptor.fetchAllEntriesCount();
			Log.e(TAG, "db Size is:" + dbSize);
			// int dataIndex =
			// cursor.getColumnIndex(DataBaseAdaptor.KEY_DATARECORD);
			// int idIndex = cursor.getColumnIndex(DataBaseAdaptor.KEY_ROWID);
			// int timeIndex = cursor.getColumnIndex(DataBaseAdaptor.KEY_TIME);
			// int typeIndex = cursor.getColumnIndex(DataBaseAdaptor.KEY_TYPE);
//
//			Integer id;
//			String newRecord;
//			ArrayList<String> content;
//			HashSet<Integer> keySet = new HashSet<Integer>();

			int failCount = 0;

			// cursor.moveToFirst();
			// && MobileSens.isControlPlugged()
			Log.i("MobileSens.isPlugged()",
					Boolean.toString(MobileSens.isPlugged()));
			while ((dbSize > 100) && MobileSens.isPlugged()) {
				Log.i(TAG, "Total DB size is: " + dbSize);
				int maxCount = (MAX_UPLOAD_SIZE > dbSize) ? dbSize
						: MAX_UPLOAD_SIZE; // 鏈�ぇMAX_UPLOAD_SIZE锛�00锛�
				Node node = getNode(maxCount);
				dbSize -= maxCount;
				do {
					Log.i(TAG, "upload number:"+node.content.size()+" maxid:"+node.max+" minId:"+node.min);
					postResult = doPost("data=" + node.content.toString(),
							CUSTOM_URL);
					postResult=doRequest(CUSTOM_URL,node.content.toString());
					postResult = doPost(node.content.toString(), CUSTOM_URL);
					if (postResult) {
						failCount = 0;
						long fromId = node.min;
						long toId = node.max;
						Log.i(TAG, "Deleting [" + fromId + ", " + toId + "]");
						boolean flag = mDbAdaptor.deleteRange(fromId, toId);
						if (!flag) { // 鎴愬姛涓婁紶鏈嶅姟鍣ㄥ悗锛屽垹鍘绘暟鎹簱鐨勬暟鎹紝doPostdelete
										// Range of database
							Log.e(TAG, "Error deleting rows");
						}
					} else {
						Log.e(TAG, "Post failed");
						failCount++;
					}
					// keySet.clear();
				} while ((!postResult) && (failCount < MAX_FAIL_COUNT));

				if (failCount > MAX_FAIL_COUNT) {
					Log.e(TAG, "Too many post failiurs. "
							+ "Will try at another time");
					mDbAdaptor.close();
					return;
				}
			}

			mDbAdaptor.close(); // close database

		} catch (Exception e) {
			Log.e(TAG, "Exception", e);
			Log.i(TAG, "Will resume upload later");
			mDbAdaptor.close();
		}

		Log.i(TAG, "tryUpload() done!");
	}

	private Node getNode(int count) {
		int num=count;
		Node node = new Node();
		node.content = new ArrayList<String>();
		node.min = Integer.MAX_VALUE;
		node.max = 0;
		int dbFetchCount = 100;
		while (num > 0) {
			Cursor cursor = mDbAdaptor.fetchCountEntries(dbFetchCount,node.max);			
			if (cursor != null && cursor.moveToFirst()) {
				num -= cursor.getCount();
				for (int i = 0; i < cursor.getCount(); i++) {
					int id = cursor.getInt(cursor
							.getColumnIndex(DataBaseAdaptor.KEY_ROWID));
					if (node.max < id) {
						node.max = id;
					}
					if (node.min > id) {
						node.min = id;
					}
					String newRecord = cursor.getString(cursor
							.getColumnIndex(DataBaseAdaptor.KEY_DATARECORD));
					// content.add(URLEncoder.encode(newRecord));
					node.content.add(newRecord);

					cursor.moveToNext();
				}
				if(cursor!=null){
					cursor.close();
				}
			} else {
				Log.e(TAG, "get data from db error!");
			}
		}
		return node;
	}
	
	private void writesd(String s, String path) {
		File file = null;
		
		try {
			file = new File(path); 		
			if(!file.exists()){
				file.createNewFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(file));
		} catch (FileNotFoundException e3) {
			e3.printStackTrace();
			return;
		}
		BufferedWriter br = null;
		try {
			br = new BufferedWriter(new OutputStreamWriter(out, "gbk"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		try {
			if(br != null){
				br.write(s);
				br.close();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean doPost(String content, String urlPost) {
		Log.i(TAG, "doPost()");
		Log.i(TAG, "doPost()  ----- content = " + content);

		int respCode;
		String respMsg = "";

		URL url;
		HttpURLConnection con;

		try {
			url = new URL(urlPost); // url
		} catch (MalformedURLException e) {
			Log.e(TAG, "Exception", e);
			return false;
		}

		try {
			con = (HttpURLConnection) url.openConnection(); // open url
		} catch (IOException e) {
			Log.e(TAG, "Exception: " + e.getMessage());
			return false;
		}

		try {
			con.setRequestMethod("POST"); // 璁剧疆URL璇锋眰鏂规硶
			con.setDoOutput(true); // 浣跨敤 URL 杩炴帴杩涜杈撳嚭
			con.setDoInput(true); // 浣跨敤 URL 杩炴帴杩涜杈撳叆
			con.setRequestProperty("Content-type",
					"application/x-www-form-urlencoded");
		} catch (java.net.ProtocolException e) {
			Log.e(TAG, "Exception", e);
			return false;
		}

		try {
			Log.i(TAG, "doPost() begin to connect HTTP = " + urlPost);
			con.connect();
			OutputStream out = con.getOutputStream();
			byte[] buff = content.getBytes("gbk");
			out.write(buff); // 涓婁紶瀛楃涓插唴瀹癸紝 content 鍒版湇鍔″櫒
			out.flush();

			respMsg = con.getResponseMessage();
			respCode = con.getResponseCode();
		} catch (IOException e) {
			Log.e(TAG, "Exception", e);
			con.disconnect();
			return false;
		}

		if (respCode == HttpURLConnection.HTTP_OK) {
			Log.i(TAG, "doPost() successful!!!");
			con.disconnect();
			MobileSens.setControlPlugged(false);
			return true;
		} else {
			Log.e(TAG, "post failed with error: " + respMsg);
			con.disconnect();
			return false;
		}
	}
	
	public boolean doRequest(String url,String context) {
		Log.i(TAG, "doPost()");
		Log.i(TAG, "doPost()"+context);
		Log.i(TAG,"doPost()"+url);
		
		//需要上传的数据写入upload_data.txt文件
		//writesd(context,"/sdcard/upload_data.txt");
		
		String returnString="";
		String reasonString="";
		DefaultHttpClient client = new DefaultHttpClient();// http客户端
		HttpPost httpPost = new HttpPost(url);
		ArrayList<BasicNameValuePair> pairs=new ArrayList<BasicNameValuePair>();
		pairs.add(new BasicNameValuePair("data", context));
		try {
			UrlEncodedFormEntity p_entity = new UrlEncodedFormEntity(pairs,
					"utf-8");
			/*
			 * 将POST数据放入HTTP请求
			 */
			httpPost.setEntity(p_entity);
			/*
			 * 发出实际的HTTP POST请求
			 */
			HttpResponse response = client.execute(httpPost);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				returnString = convertStreamToString(content);
				reasonString=returnString;
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			reasonString=e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			reasonString=e.getMessage();
		}
		if("1".equals(returnString)){
			Log.i(TAG,"doPost()"+"post sucess");
			return true;
		}else{
			Log.i(TAG,"doPost()"+"post fail:"+reasonString);
			return false;
		}
	}
	
	private String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

}
