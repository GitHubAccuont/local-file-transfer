package org.example.ui.element;

import org.example.transfer.dto.Peer;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class DiscoveryPanel extends JPanel {

    private final DefaultListModel<Peer> model = new DefaultListModel<>();
    private final JList<Peer> peerList = new JList<>(model);

    public DiscoveryPanel(Consumer<Peer> onPeerSelected) {
        super(new BorderLayout());
        peerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        peerList.setCellRenderer(new PeerListCellRenderer());

        peerList.addListSelectionListener(e -> {

            if (e.getValueIsAdjusting()) return;
            Peer p = peerList.getSelectedValue();
            if (p != null) onPeerSelected.accept(p);
        });
        add(new JScrollPane(peerList), BorderLayout.CENTER);
    }

    public void addPeer(Peer p) {
        model.addElement(p);
    }

    public void removePeer(Peer p) {
        model.removeElement(p);
    }

    public JList<Peer> getPeers() {
        return peerList;
    }
}
