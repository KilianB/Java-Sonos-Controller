package com.vmichalak.protocol.ssdp;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represent a Device discovered by SSDP.
 */
public class Device {
    private final String ip;
    private final String descriptionUrl;
    private final String server;
    private final String serviceType;
    private final String usn;

    public Device(String ip, String descriptionUrl, String server, String serviceType, String usn) {
        this.ip = ip;
        this.descriptionUrl = descriptionUrl;
        this.server = server;
        this.serviceType = serviceType;
        this.usn = usn;
    }

    /**
     * Instantiate a new Device Object from a SSDP discovery response packet.
     * @param ssdpResult SSDP Discovery Response packet.
     * @return Device
     */
    public static Device parse(DatagramPacket ssdpResult) {
        HashMap<String, String> headers = new HashMap<String, String>();
        Pattern pattern = Pattern.compile("(.*): (.*)");

        String[] lines = new String(ssdpResult.getData()).split("\r\n");

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if(matcher.matches()) {
                headers.put(matcher.group(1).toUpperCase(), matcher.group(2));
            }
        }

        return new Device(
                ssdpResult.getAddress().getHostAddress(),
                headers.get("LOCATION"),
                headers.get("SERVER"),
                headers.get("ST"),
                headers.get("USN"));
    }

    public String getIPAddress() {
        return ip;
    }

    public String getDescriptionUrl() {
        return descriptionUrl;
    }

    public String getServer() {
        return server;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getUSN() {
        return usn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Device device = (Device) o;

        if (ip != null ? !ip.equals(device.ip) : device.ip != null) return false;
        if (descriptionUrl != null ? !descriptionUrl.equals(device.descriptionUrl) : device.descriptionUrl != null)
            return false;
        if (server != null ? !server.equals(device.server) : device.server != null) return false;
        if (serviceType != null ? !serviceType.equals(device.serviceType) : device.serviceType != null) return false;
        return usn != null ? usn.equals(device.usn) : device.usn == null;

    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + (descriptionUrl != null ? descriptionUrl.hashCode() : 0);
        result = 31 * result + (server != null ? server.hashCode() : 0);
        result = 31 * result + (serviceType != null ? serviceType.hashCode() : 0);
        result = 31 * result + (usn != null ? usn.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Device{" +
                "ip='" + ip + '\'' +
                ", descriptionUrl='" + descriptionUrl + '\'' +
                ", server='" + server + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", usn='" + usn + '\'' +
                '}';
    }
}
