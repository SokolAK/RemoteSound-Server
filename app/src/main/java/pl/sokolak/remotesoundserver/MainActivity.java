package pl.sokolak.remotesoundserver;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Player player = new Player(this);
        player.init();

        ServerConnector serverConnector = new ServerConnector(this);
        serverConnector.init();
    }

}