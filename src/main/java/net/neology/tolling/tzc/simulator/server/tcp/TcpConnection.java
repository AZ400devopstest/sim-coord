package net.neology.tolling.tzc.simulator.server.tcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
@RequiredArgsConstructor
public class TcpConnection {
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private Socket socket;
    private final List<Listener> listeners;

    public TcpConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.listeners = new ArrayList<>();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void close() throws IOException {
        socket.close();
    }

    public InetAddress getAddress() {
        return socket.getInetAddress();
    }

    public void send(Object object) throws IOException {
        if (object instanceof byte[]) {
            outputStream.write((byte[]) object);
        }
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                byte[] buffer = new byte[1024 * 64];
                try {
                    int inbound = inputStream.read(buffer);
                    if (inbound > 0) {
                        byte[] data = Arrays.copyOf(buffer, inbound);
                    }
                } catch (IOException ex) {

                }
            }
        }).start();
    }

    private final class Listener {
        void messageReceived(TcpConnection connection, Object message) {

        }
        void connected(TcpConnection connection) {

        }
        void disconnected(TcpConnection connection) {

        }
    }
}
