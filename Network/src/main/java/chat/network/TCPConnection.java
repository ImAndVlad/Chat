package chat.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class TCPConnection {

    private final Socket socket;
    private final Thread rxThread;
    private final TCPConnectionListener evenListener;
    private final BufferedReader in;
    private final BufferedWriter out;

    public TCPConnection(TCPConnectionListener evenListener, String ipAddr, int port) throws IOException {
        this(evenListener, new Socket(ipAddr, port));
    }

    public TCPConnection(TCPConnectionListener evenListener, Socket socket) throws IOException {
        this.evenListener = evenListener;
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    evenListener.onConnectionReady(TCPConnection.this);
                    while (!rxThread.isInterrupted()) {
                        evenListener.onReceiveString(TCPConnection.this, in.readLine());
                    }
                } catch (IOException e) {
                    evenListener.onException(TCPConnection.this, e);
                } finally {
                    evenListener.onDisconnect(TCPConnection.this);
                }
            }
        });
        rxThread.start();
    }

    public synchronized void sendString(String value) {
        try {
            out.write(value + "\r\n");
            out.flush();
        } catch (IOException e) {
            evenListener.onException(TCPConnection.this, e);
            disconnect();
        }

    }

    private synchronized void disconnect() {
        rxThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            evenListener.onException(TCPConnection.this, e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection: " + socket.getInetAddress() + ": " + socket.getPort();
    }
}
