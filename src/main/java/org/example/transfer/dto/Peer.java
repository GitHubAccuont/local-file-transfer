package org.example.transfer.dto;

import java.net.InetAddress;

/**
 * Class representing peers stored inside the Discovery service for displaying all found LAN instances of this app.
 * The idea was to use {@code Record} class but the version limitation forced to use getters and final fields with full args constructor.
 *
 */
public class Peer {
    private final InetAddress address;
    private final int tcpPort;
    private final String name;

    public Peer(InetAddress address, int tcpPort, String name) {
        this.address = address;
        this.tcpPort = tcpPort;
        this.name = name;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public String getName() {
        return name;
    }
}
