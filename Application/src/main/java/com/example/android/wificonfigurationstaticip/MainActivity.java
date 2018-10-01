/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wificonfigurationstaticip;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "logwificonfiguration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_real);







        if (savedInstanceState == null) {
            DevicePolicyManager manager =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (manager.isDeviceOwnerApp(getApplicationContext().getPackageName())) {
                // This app is set up as the device owner. Show the main features.
                Log.d(TAG, "The app is the device owner.");
                showFragment(DeviceOwnerFragment.newInstance());
            } else {
                // This app is not set up as the device owner. Show instructions.
                Log.d(TAG, "The app is not the device owner.");
                showFragment(InstructionFragment.newInstance());
            }
        }
        Log.i(TAG, "context xxx");
        System.out.println("context yyy");
        Context context = getApplicationContext();
        getDeviceNetworkIp(context);
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    public static String getDeviceNetworkIp(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context
                .CONNECTIVITY_SERVICE);
        Log.d(TAG, "getDeviceNetworkIp");
        System.out.println("getDeviceNetworkIp yyy");

        WifiConfiguration wifiConf = null;

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        Log.d(TAG, "networkInfo:" + networkInfo);
        System.out.println("networkInfo:" + networkInfo);


        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {  //wifi
                Log.d(TAG, "getType:" + networkInfo.getType());
                WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifi.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();

                Log.d(TAG, "ip:" + ipAddress);
                System.out.println("ip:" + ipAddress);

                String Ipv4Address = null;
                try {
                    Ipv4Address = InetAddress
                            .getByName(
                                    String.format("%d.%d.%d.%d", (ipAddress & 0xff),
                                            (ipAddress >> 8 & 0xff),
                                            (ipAddress >> 16 & 0xff),
                                            (ipAddress >> 24 & 0xff))).getHostAddress()
                            .toString();
                    Log.d(TAG, "Ipv4Address:" + Ipv4Address);
                    System.out.println("Ipv4Address:" + Ipv4Address);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                List<WifiConfiguration> configuredNetworks = wifi.getConfiguredNetworks();
                Log.d(TAG, "configuredNetworks:" + configuredNetworks);
                System.out.println("configuredNetworks:" + configuredNetworks);
                for (WifiConfiguration conf : configuredNetworks){
                    Log.d(TAG, "WifiConfiguration:" + conf.networkId + "==wifiInfo.getNetworkId:" + wifiInfo.getNetworkId());
                    System.out.println("WifiConfiguration:" + conf.networkId  + "==wifiInfo.getNetworkId:" + wifiInfo.getNetworkId());
                    if (conf.networkId == wifiInfo.getNetworkId()){
                        wifiConf = conf;
                        Log.d(TAG, "wifiConf:" + wifiConf);
                        System.out.println("wifiConf:" + wifiConf);
                        break;
                    }
                }

                /*
                try{
                    setIpAssignment("STATIC", wifiConf); //or "DHCP" for dynamic setting
                    setIpAddress(InetAddress.getByName("192.168.66.200"), 24, wifiConf);
                    setGateway(InetAddress.getByName("192.168.66.1"), wifiConf);
                    setDNS(InetAddress.getByName("4.4.4.4"), wifiConf);
                    wifi.updateNetwork(wifiConf); //apply the setting
                    wifi.saveConfiguration(); //Save it
                }catch(Exception e){
                    e.printStackTrace();
                } */

                MakeStaticIp wifiConfigurationMe = new MakeStaticIp();
                wifiConfigurationMe.test(context, wifiConf);


                return Ipv4Address;
                //return convertToIp(ip);
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) { // gprs
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                         en.hasMoreElements();) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                             enumIpAddr.hasMoreElements();) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && !(inetAddress
                                    instanceof Inet6Address)) {
                                return inetAddress.getHostAddress().toString();
                            }

                        }

                    }
                } catch (SocketException e) {
                    Log.e(TAG, "e", e);
                }
            }
        }
        return "";
    }

    public static void setIpAssignment(String assign , WifiConfiguration wifiConf)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException{

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Object ipConfiguration = null;
            try {
                ipConfiguration = wifiConf.getClass().getMethod("getIpConfiguration").invoke(wifiConf);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            setEnumField(ipConfiguration, assign, "ipAssignment");
        } else {
            setEnumField(wifiConf, assign, "ipAssignment");
        }

    }

    public static void setIpAddress(InetAddress addr, int prefixLength, WifiConfiguration wifiConf)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchMethodException, ClassNotFoundException, InstantiationException, InvocationTargetException{

        Object linkProperties = getField(wifiConf, "linkProperties");

        if(linkProperties == null)return;
        Class laClass = Class.forName("android.net.LinkAddress");
        Constructor laConstructor = laClass.getConstructor(new Class[]{InetAddress.class, int.class});
        Object linkAddress = laConstructor.newInstance(addr, prefixLength);

        ArrayList mLinkAddresses = (ArrayList)getDeclaredField(linkProperties, "mLinkAddresses");
        mLinkAddresses.clear();
        mLinkAddresses.add(linkAddress);
    }


    public static void setGateway(InetAddress gateway, WifiConfiguration wifiConf)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException, InvocationTargetException{
        Object linkProperties = getField(wifiConf, "linkProperties");
        if(linkProperties == null)return;
        Class routeInfoClass = Class.forName("android.net.RouteInfo");
        Constructor routeInfoConstructor = routeInfoClass.getConstructor(new Class[]{InetAddress.class});
        Object routeInfo = routeInfoConstructor.newInstance(gateway);

        ArrayList mRoutes = (ArrayList)getDeclaredField(linkProperties, "mRoutes");
        mRoutes.clear();
        mRoutes.add(routeInfo);
    }

    public static void setDNS(InetAddress dns, WifiConfiguration wifiConf)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException{
        Object linkProperties = getField(wifiConf, "linkProperties");
        if(linkProperties == null)return;

        ArrayList<InetAddress> mDnses = (ArrayList<InetAddress>)getDeclaredField(linkProperties, "mDnses");
        mDnses.clear(); //or add a new dns address , here I just want to replace DNS1
        mDnses.add(dns);
    }

    public static Object getField(Object obj, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        Object out = f.get(obj);
        return out;
    }

    public static Object getDeclaredField(Object obj, String name)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        Object out = f.get(obj);
        return out;
    }

    private static void setEnumField(Object obj, String value, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
    }


}
