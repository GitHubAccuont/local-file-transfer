package org.example.ui.element;

import org.example.transfer.dto.Peer;

import javax.swing.*;
import java.awt.*;

public class PeerListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Peer) {
            Peer p = (Peer) value;
            setText(String.format("%s: (%s:%d)", p.getName(), p.getAddress(), p.getTcpPort()));
        }
        return this;
    }
}
