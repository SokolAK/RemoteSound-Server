package pl.sokolak.remotesoundserver;

import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.content.Context.WIFI_SERVICE;

public class Connector {
    private final Activity activity;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 8080;
    private TextView tvIP, tvPort, tvConnection;
    private ServerSocket serverSocket;
    private Thread threadConnection = null;
    private PrintWriter output;
    private BufferedReader input;

    public Connector(Activity activity) {
        this.activity = activity;
    }

    public void init() {
        tvIP = activity.findViewById(R.id.tvIP);
        tvPort = activity.findViewById(R.id.tvPort);
        tvConnection = activity.findViewById(R.id.tvConnection);

        try {
            SERVER_IP = getLocalIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        threadConnection = new Thread(new ThreadSocket());
        threadConnection.start();

        tvIP.setText("IP: " + SERVER_IP);
    }

    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    class ThreadSocket implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                activity.runOnUiThread(() -> {
                    tvConnection.setText("Not connected");
                    tvIP.setText("IP: " + SERVER_IP);
                    tvPort.setText("Port: " + SERVER_PORT);
                });
                try {
                    socket = serverSocket.accept();
                    output = new PrintWriter(socket.getOutputStream());
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvConnection.setText("Connected\n");
                        }
                    });
                    //new Thread(new Thread2()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
