package org.example.transfer.service;

import org.example.transfer.dto.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple implementation of discovery service. Since we dont really have any
 * need for packages to be fed or sent properly udp is used here. Messages could in theory be entangled
 * but since its small project made just for LAN transfer it shouldn't be happening in most cases.
 * <p>
 * Discovery service used to add automatically new discovered peers (with same app running) over LAN
 * by transferring packets with UDP across {@code 255.255.255.255} address which should send it to anyone in net.
 * the stored found {@link Peer} instances are stored within the simple map where they added as {@code <Peer,lastSeen> entries}
 * (the self-caught broadcast is ignored). One of constructor parameters for it calls for instance of the
 * inlined listener interface (same as for Connection) to allow the feedback to UI without storing logic in there instead.
 *
 */
public class DiscoveryService {

    private static final int DISCOVERY_PORT = 9001;
    private static final long ANNOUNCE_INTERVAL_MS = 2000;
    private static final long PEER_TIMEOUT_MS = 6000;
    private static final int EXPECTED_MESSAGE_LENGTH_LIMIT = 1024;
    private static final String LAN_ADRESS = "255.255.255.255";
    private final String myName;
    private final int myTcpPort;
    private final Listener listener;
    private final Map<Peer, Long> peers = new ConcurrentHashMap<>();
    private DatagramSocket socket;
    private Thread listenThread;
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    public DiscoveryService(String myName, int myTcpPort, Listener listener) {
        this.myName = myName;
        this.myTcpPort = myTcpPort;
        this.listener = listener;
    }

    public void start() throws IOException {
        running = true;
        socket = new DatagramSocket(DISCOVERY_PORT);
        socket.setBroadcast(true);

        listenThread = new Thread(this::listenLoop, "discovery-listen");
        listenThread.setDaemon(true);
        listenThread.start();

        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::announce, 0, ANNOUNCE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::expire, 0, ANNOUNCE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void listenLoop() {
        while (running) {
            byte[] buffer = new byte[EXPECTED_MESSAGE_LENGTH_LIMIT];
            DatagramPacket p = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(p);
                String msg = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                if (msg.startsWith("HELLO")) {
                    String[] parts = msg.split(" ");
                    if (parts.length != 3) throw new IOException("Wrong message was recieved for connection");
                    int port = Integer.parseInt(parts[2]);
                    String name = parts[1];
                    Peer peer = new Peer(p.getAddress(), port, name);
                    Long previous = peers.put(peer, System.currentTimeMillis());
                    if (previous == null) {
                        listener.onPeerDiscovered(peer);
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void announce() {
        byte[] messageBytes = String.format("HELLO %s %d", myName, myTcpPort).getBytes(StandardCharsets.UTF_8);
        try {
            DatagramPacket helloMessage =
                    new DatagramPacket(
                            messageBytes,
                            messageBytes.length,
                            InetAddress.getByName(LAN_ADRESS),
                            DISCOVERY_PORT
                    );
            socket.send(helloMessage);
        } catch (IOException ignored) {
        }

    }

    private void expire() {
        long now = System.currentTimeMillis();
        List<Peer> expired = new ArrayList<>();
        peers.entrySet().removeIf(e -> {
            if (PEER_TIMEOUT_MS + e.getValue() < now) {
                expired.add(e.getKey());
                return true;
            }
            return false;
        });
        expired.forEach(listener::onPeerLost);
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
        if (scheduler != null) scheduler.shutdownNow();
    }

    public interface Listener {
        void onPeerDiscovered(Peer p);

        void onPeerLost(Peer p);
    }
}
