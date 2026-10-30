package org.example.transfer.dto;

import org.example.transfer.dto.entity.frame.Frame;

public interface ConnectionListener {
    void onFrame(Frame f);

    void onClosed();
}
