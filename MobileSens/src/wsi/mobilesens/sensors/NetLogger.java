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

package wsi.mobilesens.sensors;

import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.TrafficStats;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONException;

import wsi.mobilesens.util.Status;

/**
 * Responsible for monitoring the amount of network traffic that has been made
 * by each application. It stores this information into the database. There
 * should not exist an instance of this class. Instead, its constructor should
 * be called exactly once before the static methods of this class are used. All
 * future interactions should be done through the static methods.
 * 
 * @author Hossein Falaki and John Jenkins
 * @version 1.0
 * 
 */
public class NetLogger {
	private static final String TAG = "NetLogger";

	private static Context mContext;
	private static final double MB = 1048576.0;

	public NetLogger(Context context) {
		mContext = context;
	}

	// Updates the database with the current network statistics collected by the system
	public JSONObject getAppNetUsage() {
	
		List<ApplicationInfo> apps = mContext.getPackageManager().getInstalledApplications(0);	// Get all UIDs
		int appsLen = apps.size();

		JSONObject data;
		JSONObject result = new JSONObject();

		// Update the database with the necessary information.
		ApplicationInfo currApp = null;
		long currRxBytes, currTxBytes;
		for (int i = 0; i < appsLen; i++) {
			currApp = apps.get(i);

			currRxBytes = TrafficStats.getUidRxBytes(currApp.uid);		// Get the number of bytes received through the network for this UID
			currTxBytes = TrafficStats.getUidTxBytes(currApp.uid);		// Get the number of bytes sent through the network for this UID	

			if ((currRxBytes != TrafficStats.UNSUPPORTED) || (currTxBytes != TrafficStats.UNSUPPORTED)) {
				try {
					data = new JSONObject();
					data.put("Rx", currRxBytes);
					data.put("Tx", currTxBytes);

					result.put(currApp.packageName, data);

				} catch (JSONException je) {
					Log.e(TAG, "Exception", je);
				}
			}
		}

		return result;
	}

	public JSONObject getIfNetUsage() {
		
		JSONObject result = new JSONObject();

		try {
			result.put("MobileRxBytes", TrafficStats.getMobileRxBytes());		// through the mobile interface
			result.put("MobileTxBytes", TrafficStats.getMobileTxBytes());
			result.put("MobileRxPackets", TrafficStats.getMobileRxPackets());
			result.put("MobileTxPackets", TrafficStats.getMobileTxPackets());

			result.put("TotalRxBytes", TrafficStats.getTotalRxBytes());			// through all network interfaces
			result.put("TotalTxBytes", TrafficStats.getTotalTxBytes());
			result.put("TotalRxPackets", TrafficStats.getTotalRxPackets());
			result.put("TotalTxPackets", TrafficStats.getTotalTxPackets());
		} catch (JSONException je) {
			Log.e(TAG, "Exception", je);
		}

		Status.setTraffic(TrafficStats.getTotalTxBytes() / MB, // send
				TrafficStats.getTotalRxBytes() / MB); // receive

		return result;

	}
}
