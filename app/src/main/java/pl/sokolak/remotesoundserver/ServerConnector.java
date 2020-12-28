package pl.sokolak.remotesoundserver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.MediaPlayer;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.SystemClock;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import lombok.SneakyThrows;

import static android.content.Context.WIFI_SERVICE;

@SuppressLint("SetTextI18n")
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
    private Handler sendStatusHandler;
    private Player player;
    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;


    public ServerConnector(Activity activity) {
        this.activity = activity;
    }

    public void init(Player player) {
        this.player = player;
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

        sendStatusHandler = new Handler();
        sendStatusHandler.postDelayed(sendStatus, 0);
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
                    connectionStatus = ConnectionStatus.DISCONNECTED;
                    String stat = activity.getString(R.string.status) + ": " + activity.getString(R.string.disconnected);
                    activity.runOnUiThread(() -> tvConnection.setText(stat));
                    tvIP.setText("IP: " + SERVER_IP);
                    tvPort.setText("Port: " + SERVER_PORT);

//                    if(sendStatus != null) {
//                        sendStatusHandler.removeCallbacks(sendStatus);
//                        sendStatusHandler = null;
//                    }

                });
                try {
                    socket = serverSocket.accept();
                    output = new PrintWriter(socket.getOutputStream());
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    connectionStatus = ConnectionStatus.CONNECTED;
                    String stat = activity.getString(R.string.status) + ": " + activity.getString(R.string.connected);
                    activity.runOnUiThread(() -> tvConnection.setText(stat));

                    new Thread(new ThreadRead()).start();

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
                    String message = input.readLine();
                    if (message == null) {
                        serverSocket.close();
                        threadConnection = new Thread(new ThreadConnection());
                        threadConnection.start();
                        break;
                    } else {
                        activity.runOnUiThread(() -> {
                                    @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("HH:mm:ss");
                                    String time = df.format(Calendar.getInstance().getTime());
                                    activity.runOnUiThread(() -> tvMessages.setText("Client [" + time + "]:\n" + message));
                                    performAction(message.toLowerCase());
                                }
                        );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final Runnable sendStatus = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                StringBuilder message = new StringBuilder();
                if(connectionStatus == ConnectionStatus.CONNECTED)
                    message.append("1");
                else
                    message.append("0");

                message.append("_");
                message.append(player.getTvPlayerStatus().getText());
                message.append("_");
                message.append(player.getTvTime().getText());

                message.append("_");
                message.append(player.getTvRepeat().getText());
                message.append("_");
                message.append(player.getEtRepeat().getText());

                message.append("_");
                message.append(player.getTvVolume().getText());

                message.append("_");
                message.append(player.getButton1().getButton().getText());
                message.append("_");
                message.append(player.getButton2().getButton().getText());
                message.append("_");
                message.append(player.getButton3().getButton().getText());
                message.append("_");
                message.append(player.getButton4().getButton().getText());
                message.append("_");
                message.append(player.getButton5().getButton().getText());
                message.append("_");
                message.append(player.getButton6().getButton().getText());

                if (output != null) {
                    output.println(message.toString());
                    output.flush();
                }
                sendStatusHandler.postDelayed(this, 500);
            }
        }
    };


    @SneakyThrows
    private void performAction(String message) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;

        switch (message) {
            case "1":
                player.getButton1().getButton().callOnClick();
                break;
            case "2":
                player.getButton2().getButton().callOnClick();
                break;
            case "3":
                player.getButton3().getButton().callOnClick();
                break;
            case "4":
                player.getButton4().getButton().callOnClick();
                break;
            case "5":
                player.getButton5().getButton().callOnClick();
                break;
            case "6":
                player.getButton6().getButton().callOnClick();
                break;

//            case 100:
//                activity.findViewById(R.id.buttonBackward).onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0));
//                break;
            case "stop":
                player.getButtonStop().callOnClick();
                break;

            case "fd":
                player.getButtonForward().dispatchTouchEvent(MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, 0,0,0));
                break;
            case "fu":
                player.getButtonForward().dispatchTouchEvent(MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, 0,0,0));
                break;
            case "bd":
                player.getButtonBackward().dispatchTouchEvent(MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, 0,0,0));
                break;
            case "bu":
                player.getButtonBackward().dispatchTouchEvent(MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, 0,0,0));
                break;

            case "vd":
                player.getVolumeDownButton().callOnClick();
                break;
            case "vu":
                player.getVolumeUpButton().callOnClick();
                break;
        }

        if (message.startsWith("r")) {
            EditText repeatValue = player.getEtRepeat();
            repeatValue.setText(message.substring(1));
        }

        //new Thread(new ThreadWrite(message)).start();
    }

    enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED
    }
}
