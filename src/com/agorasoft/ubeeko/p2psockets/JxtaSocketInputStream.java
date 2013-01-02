package com.agorasoft.ubeeko.p2psockets;

import java.io.IOException;
import java.io.InputStream;

class JxtaSocketInputStream extends InputStream {

    private JxtaSocket socket;

    JxtaSocketInputStream(JxtaSocket socket) throws IOException {
        this.socket = socket;
    }

    /**
     * {@inheritDoc}
     */
    public int available() throws IOException {
        return socket.available();
    }

    /**
     * {@inheritDoc}
     */
    public int read() throws IOException {
        return socket.read();
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte b[], int off, int len) throws IOException {
        return socket.read(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        socket.close();
    }
}