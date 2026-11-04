package org.example.transfer.dto;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;

/**
 * Class representing peers stored inside the Discovery service for displaying all found LAN instances of this app.
 * The idea was to use {@code Record} class but the version limitation forced to use getters and final fields with full args constructor.
 *
 */
public class Peer {
    private final InetAddress address;
    private final int tcpPort;
    private final String name;
    private final UUID id;

    public Peer(InetAddress address, int tcpPort, String name, UUID id) {
        this.address = address;
        this.tcpPort = tcpPort;
        this.name = name;
        this.id = id;
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

    public UUID getId() {
        return id;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return tcpPort == peer.tcpPort && address.equals(peer.address) && id.equals(peer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, tcpPort, id);
    }

}
