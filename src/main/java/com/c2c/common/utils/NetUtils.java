package com.c2c.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 网络工具：识别本机可用的局域网 IPv4 地址。
 *
 * @author wzh
 */
@Slf4j
public final class NetUtils {

    private NetUtils() {
    }

    /**
     * 取首个可用的局域网 IPv4（跳过回环/虚拟/未启用网卡与链路本地地址），
     * 多网卡按 192.168 > 10. > 172.16~31 > 其他 排序；探测失败返回 null。
     */
    public static String detectLanIp() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                String ifName = ni.getName() == null ? "" : ni.getName().toLowerCase();
                String display = ni.getDisplayName() == null ? "" : ni.getDisplayName().toLowerCase();
                if (ifName.contains("vmnet") || ifName.contains("veth") || ifName.contains("wsl")
                        || display.contains("vmware") || display.contains("virtualbox") || display.contains("vbox")
                        || display.contains("hyper-v") || display.contains("vpn")) {
                    continue;
                }
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()
                            && !addr.getHostAddress().startsWith("169.254.")) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("识别局域网 IP 失败: {}", e.getMessage());
            return null;
        }
        ips.sort((a, b) -> rank(a) - rank(b));
        return ips.isEmpty() ? null : ips.get(0);
    }

    private static int rank(String ip) {
        if (ip.startsWith("192.168.")) return 0;
        if (ip.startsWith("10.")) return 1;
        if (ip.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*")) return 2;
        return 3;
    }
}
