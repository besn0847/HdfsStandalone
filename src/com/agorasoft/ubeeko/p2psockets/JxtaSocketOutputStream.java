package com.agorasoft.ubeeko.p2psockets;

import java.io.IOException;
import java.io.OutputStream;

public class JxtaSocketOutputStream extends OutputStream {

    /**
     *  Data buffer
     */
    protected byte buf[];

    /**
     *  byte count in buffer
     */
    protected int count;

    /**
     *  JxtaSocket associated with this stream
     */
    protected JxtaSocket socket;

    /**
     *  Constructor for the JxtaSocketOutputStream object
     *
     *@param  socket  JxtaSocket associated with this stream
     */
    public JxtaSocketOutputStream(JxtaSocket socket) {
        this(socket, 16384);
    }

    /**
     *  Constructor for the JxtaSocketOutputStream object
     *
     *@param  socket  JxtaSocket associated with this stream
     *@param  size    buffer size in bytes
     */
    public JxtaSocketOutputStream(JxtaSocket socket, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
        this.socket = socket;
    }

    /**
     *  Flush the internal buffer
     *
     *@exception  IOException  if an i/o error occurs
     */
    private void flushBuffer() throws IOException {
        if (count > 0) {
            // send the message
            socket.write(buf, 0, count);
            count = 0;
        }
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized void write(int b) throws IOException {
        if (count >= buf.length) {
            flushBuffer();
        }
        buf[count++] = (byte) b;
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized void write(byte b[], int off, int len)  throws IOException {
        int left = buf.length - count;
        if (len > left) {
            System.arraycopy(b, off, buf, count, left);
            len -= left;
            off += left;
            count += left;
            flushBuffer();
        }
        // chunk data if larger than buf.length
        while (len >= buf.length) {
            socket.write(b, off, buf.length);
            len -= buf.length;
            off += buf.length;
        }
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized void flush() throws IOException {
        flushBuffer();
    }
    
    //jxl modification start
    public void close() throws IOException{
        super.close();
        socket.shutdownOutput(); 
        
    }
    //jxl modification end
}
