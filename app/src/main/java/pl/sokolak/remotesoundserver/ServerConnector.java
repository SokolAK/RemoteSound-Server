package pl.sokolak.remotesoundserver;

import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.content.Context.WIFI_SERVICE;

public class ServerConnector {
    private final Activity activity;
    public static String SERVER_IP = "";
    public static int SERVER_PORT = 8080;
    private TextView tvIP, tvPort, tvConnection;
    private ServerSocket serverSocket;
    private Thread threadConnection = null;
    private PrintWriter output;
    private BufferedReader input;
    private TextView tvMessages;

    public ServerConnector(Activity activity) {
        this.activity = activity;
    }

    public void init() {
        tvIP = activity.findViewById(R.id.tvIP);
        tvPort = activity.findViewById(R.id.tvPort);
        tvConnection = activity.findViewById(R.id.tvConnection);
        tvMessages = activity.findViewById(R.id.tvMessages);

        try {
            SERVER_IP = getLocalIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        threadConnection = new Thread(new ThreadConnection());
        threadConnection.start();

        //tvIP.setText("IP: " + SERVER_IP);
    }

    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    class ThreadConnection implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(SERVER_PORT));
                activity.runOnUiThread(() -> {
                    String stat = activity.getString(R.string.status) + ": " + activity.getString(R.string.disconnected);
                    activity.runOnUiThread(() -> tvConnection.setText(stat));
                    tvIP.setText("IP: " + SERVER_IP);
                    tvPort.setText("Port: " + SERVER_PORT);
                    performAction(500);
                });
                try {
                    socket = serverSocket.accept();
                    System.out.println(socket);
                    output = new PrintWriter(socket.getOutputStream());
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String stat = activity.getString(R.string.status) + ": " + activity.getString(R.string.connected);
                    activity.runOnUiThread(() -> tvConnection.setText(stat));
                    new Thread(new ThreadRead()).start();
                    performAction(501);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ThreadRead implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    int message = input.read();
                    if (message == -1) {
                        serverSocket.close();
                        threadConnection = new Thread(new ThreadConnection());
                        threadConnection.start();
                        break;
                    } else {
                        activity.runOnUiThread(() -> {
                                    tvMessages.setText("Client: " + message);
                                    performAction(message);
                                }
                        );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class ThreadWrite implements Runnable {
        private int message;

        ThreadWrite(int c) {
            this.message = c;
        }

        @Override
        public void run() {
            if (output != null) {
                output.write(message);
                output.flush();
            }
        }
    }

    private void performAction(int message) {
        switch (message) {
            case 1:
                Button button1 = activity.findViewById(R.id.button1);
                button1.callOnClick();
                break;
            case 2:
                activity.findViewById(R.id.button2).callOnClick();
                break;
            case 3:
                activity.findViewById(R.id.button3).callOnClick();
                break;

            case 100:
                activity.findViewById(R.id.buttonBackward).onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0));
                break;
            case 101:
                activity.findViewById(R.id.buttonBackward).onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0, 0, 0));
                break;
            case 102:
                activity.findViewById(R.id.buttonForward).onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0));
                break;
            case 103:
                activity.findViewById(R.id.buttonForward).onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0, 0, 0));
                break;
            case 104:
                activity.findViewById(R.id.buttonStop).callOnClick();
                break;
        }

        if (message < 0) {
            EditText repeatValue = activity.findViewById(R.id.repeatValue);
            repeatValue.setText(-message);
        }

        new Thread(new ThreadWrite(message)).start();
    }
}
