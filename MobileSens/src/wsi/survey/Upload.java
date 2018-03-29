package wsi.survey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;
import android.widget.Toast;

public class Upload {

	/** 需要将信息发送给该URL */
	// public static String postUrl =
	// "http://ccpl.psych.ac.cn:10004/mobi_submit.php";
	public static String postUrl = "http://ccpl.psych.ac.cn/global_cfg/mobile.php";
	private static String uploadUrl = null;
	public static String apkVersion = null;
	public static String apkDownLoadUrl = null;

	/**
	 * 
	 * @param val
	 *            要上传的串
	 * @return
	 */
	public static String doUpload(String val) {
		if(uploadUrl==null){
			doPostUploadUrl();
			if(uploadUrl==null){
				return null;
			}
		}		
		// 封装数据
		Map<String, String> parmas = new HashMap<String, String>();
		parmas.put("STR", val);

		DefaultHttpClient client = new DefaultHttpClient();// http客户端
		HttpPost httpPost = new HttpPost(uploadUrl);

		ArrayList<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
		if (parmas != null) {
			Set<String> keys = parmas.keySet();
			for (Iterator<String> i = keys.iterator(); i.hasNext();) {
				String key = (String) i.next();
				pairs.add(new BasicNameValuePair(key, parmas.get(key)));
			}
		}
		String returnString = null;
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
			}

			// show.setText(returnConnection);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return returnString;
	}

	/*
	 * 请求当前信息
	 */
	public static String doRequest(String parameter) {
		// 封装数据
		Map<String, String> parmas = new HashMap<String, String>();
		parmas.put("PARAMETER", parameter);

		DefaultHttpClient client = new DefaultHttpClient();// http客户端
		HttpPost httpPost = new HttpPost(postUrl);

		ArrayList<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
		if (parmas != null) {
			Set<String> keys = parmas.keySet();
			for (Iterator<String> i = keys.iterator(); i.hasNext();) {
				String key = (String) i.next();
				pairs.add(new BasicNameValuePair(key, parmas.get(key)));
			}
		}
		String returnString = null;
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
			}

			// show.setText(returnConnection);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return returnString;
	}

	private static String convertStreamToString(InputStream is) {
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
	
	public static void doPostUploadUrl(){
		String result ="[\"Mobi_Quiz_Post_Url\"]";
		result = doRequest(result);
		JSONObject jsonObject;
		try{
			jsonObject=new JSONObject(result);
			uploadUrl=jsonObject.getString("Mobi_Quiz_Post_Url");
			
		}catch (Exception e) {
			// TODO: handle exception
		}	
	} 
	
	public static void doPostUpdate(){
		String result ="[\"Mobi_APK_Version\",\"Mobi_APK_Download\"]";
		result = doRequest(result);
		JSONObject jsonObject;
		try{
			jsonObject=new JSONObject(result);
			apkVersion=jsonObject.getString("Mobi_APK_Version");
			apkDownLoadUrl=jsonObject.getString("Mobi_APK_Download");	
		}catch (Exception e) {
			// TODO: handle exception
		}	
	}
	
	public static void doPostAll(){
		String result ="[\"Mobi_APK_Version\",\"Mobi_APK_Download\",\"Mobi_Quiz_Post_Url\"]";
		result = doRequest(result);
		JSONObject jsonObject;
		try {
			jsonObject=new JSONObject(result);
			uploadUrl=jsonObject.getString("Mobi_Quiz_Post_Url");
			apkVersion=jsonObject.getString("Mobi_APK_Version");
			apkDownLoadUrl=jsonObject.getString("Mobi_APK_Download");	
			
		} catch (Exception e) {
			// TODO: handle exception
		}		
	}

}
