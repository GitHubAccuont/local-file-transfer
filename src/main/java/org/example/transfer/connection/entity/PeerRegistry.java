package org.example.transfer.connection.entity;

import org.example.transfer.connection.entity.Peer;

import java.util.concurrent.ConcurrentHashMap;

public class PeerRegistry {

    public ConcurrentHashMap<String, Peer> getRegistry() {
        return registry;
    }

    public void setRegistry(ConcurrentHashMap<String, Peer> registry) {
        this.registry = registry;
    }

    private ConcurrentHashMap<String, Peer> registry;

    public PeerRegistry(){
        registry = new ConcurrentHashMap<>();
    }

    public void addPeer(Peer peer, String key){
        registry.put(key, peer);
    }

    public void removePeer(String key){
        registry.remove(key);
    }

    public void clearRegistry(){
        registry.clear();
    }

    public Peer findByKey(String key){
        return registry.get(key);
    }
}
