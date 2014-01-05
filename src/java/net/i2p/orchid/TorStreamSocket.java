package net.i2p.orchid;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.TorClient;


public class TorStreamSocket extends Socket {

    private final Stream _stream;

    public TorStreamSocket(TorClient client, String host, int port) throws Exception {
        _stream = client.openExitStreamTo(host, port);
    }

    @Override
    public InputStream getInputStream() { return _stream.getInputStream(); }

    @Override
    public OutputStream getOutputStream() { return _stream.getOutputStream(); }

    @Override
    public void close() { _stream.close(); }


    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public String toString() {
        return ("Tor outproxy socket");
    }

    // ignored stuff
    /** warning - unsupported */
    @Override
    public void setSoTimeout(int timeout) {}

    @Override
    public int getSoTimeout () {
        return 0;
    }

    // everything below here unsupported
    /** @deprecated unsupported */
    @Override
    public void bind(SocketAddress endpoint) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void connect(SocketAddress endpoint) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void connect(SocketAddress endpoint, int timeout) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public SocketChannel getChannel() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public InetAddress getInetAddress() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public boolean getKeepAlive() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public InetAddress getLocalAddress() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public int getLocalPort() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public SocketAddress getLocalSocketAddress() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public boolean getOOBInline() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public int getPort() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public int getReceiveBufferSize() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public SocketAddress getRemoteSocketAddress() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public boolean getReuseAddress() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public int getSendBufferSize() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public int getSoLinger() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public boolean getTcpNoDelay() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public int getTrafficClass() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public boolean isBound() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public boolean isInputShutdown() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public boolean isOutputShutdown() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void sendUrgentData(int data) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void setKeepAlive(boolean on) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void setOOBInline(boolean on) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void setReceiveBufferSize(int size) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void setReuseAddress(boolean on) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void setSendBufferSize(int size) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void setSoLinger(boolean on, int linger) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void setTcpNoDelay(boolean on) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void setTrafficClass(int cize) {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void shutdownInput() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated unsupported */
    @Override
    public void shutdownOutput() {
        throw new UnsupportedOperationException();
    }


}
