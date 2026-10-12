package org.example.transfer.connection.entity;

import java.net.InetAddress;
import java.util.Objects;

public class Peer {

    private String name;
    private final InetAddress address;
    private final int port;

    public Peer(InetAddress address, int port) {
        // Start with no name (user sets it later)
        this.name = null;
        this.address = address;
        this.port = port;
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof Peer) {
            Peer p = (Peer) o;
            return this.address.equals(p.address) && this.port == p.port;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(address,port);
    }
}
