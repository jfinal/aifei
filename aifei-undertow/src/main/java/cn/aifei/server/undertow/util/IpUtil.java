/*
 * Copyright 2011-2035 詹波 (aifei.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aifei.server.undertow.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * IpUtil
 */
public class IpUtil {

    public static List<String> getLocalIp() {
        List<String> ipList = new ArrayList<>();
        try {
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
                NetworkInterface networkInterface = e.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }

                for (Enumeration<InetAddress> ele = networkInterface.getInetAddresses(); ele.hasMoreElements();) {
                    InetAddress ip = ele.nextElement();
                    if (ip instanceof Inet4Address) {
                        ipList.add(ip.getHostAddress());
                    }
                }
            }
            // return InetAddress.getLocalHost().getHostAddress();
            return ipList;
        } catch (Exception e) {
            // return "127.0.0.1";
            return ipList;
        }
    }
}



