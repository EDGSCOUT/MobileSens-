package com.nevin.dmeo.jnimac.jni;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import android.content.Context;

public class MacAddr {

	static{
		System.loadLibrary("macAddr");
	}
		
	public static native String getMacAddr(Context context); 
	
	public static String getMacAddress(Context context) 
	{
		String mac = "0";
        try 
        {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) 
            {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) 
                {
                    mac = "0";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) 
                {
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) 
                {
                    res1.deleteCharAt(res1.length() - 1);
                }
                mac = res1.toString();
            }
        } 
        catch (Exception ex) 
        {
        	mac = "0";
        }
        return mac;
		
	}
}
