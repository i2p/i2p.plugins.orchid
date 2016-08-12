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
    //private final String _host;
    private final int _port;

    public TorStreamSocket(TorClient client, String host, int port) throws Exception {
        _stream = client.openExitStreamTo(host, port);
        //_host = host;
        _port = port;
    }

    @Override
    public InputStream getInputStream() { return _stream.getInputStream(); }

    @Override
    public OutputStream getOutputStream() { return _stream.getOutputStream(); }

    @Override
    public void close() { _stream.close(); }


    @Override
    public boolean isClosed() {
        return _stream.isClosed();
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

    // several below here unsupported

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

    /**
     *  @return null since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public SocketChannel getChannel() {
        return null;
    }

    /**
     *  We could return InetAddress.getByName(), but that's not
     *  necessarily the actual IP of the socket.
     *  @return null since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public InetAddress getInetAddress() {
        return null;
    }

    /** @deprecated unsupported */
    @Override
    public boolean getKeepAlive() {
        throw new UnsupportedOperationException();
    }

    /**
     *  @return null since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public InetAddress getLocalAddress() {
        return null;
    }

    /**
     *  @return 0 since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public int getLocalPort() {
        return 0;
    }

    /**
     *  @return null since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public SocketAddress getLocalSocketAddress() {
        return null;
    }

    /**
     *  @return false since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public boolean getOOBInline() {
        return false;
    }

    /**
     *  @return remote port since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public int getPort() {
        return _port;
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

    /**
     *  @return false since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public boolean getReuseAddress() {
        return false;
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

    /**
     *  @return 0 since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public int getTrafficClass() {
        return 0;
    }

    /**
     *  @return true since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public boolean isBound() {
        return true;
    }

    /**
     *  Supported since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public boolean isConnected() {
        return !_stream.isClosed();
    }

    /**
     *  Supported since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public boolean isInputShutdown() {
        return _stream.isClosed();
    }

    /**
     *  Supported since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public boolean isOutputShutdown() {
        return _stream.isClosed();
    }

    /** @deprecated unsupported */
    @Override
    public void sendUrgentData(int data) {
        throw new UnsupportedOperationException();
    }

    /**
     *  Does nothing since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public void setKeepAlive(boolean on) {}

    /**
     *  On is unsupported
     *  @throws UnsupportedOperationException if on is true
     */
    @Override
    public void setOOBInline(boolean on) {
        if (on)
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

    /**
     *  Closes both sides since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public void shutdownInput() throws IOException {
        close();
    }

    /**
     *  Closes both sides since 1.2.2-0.2, prior to that threw UnsupportedOperationException
     */
    @Override
    public void shutdownOutput() throws IOException {
        close();
    }


}
