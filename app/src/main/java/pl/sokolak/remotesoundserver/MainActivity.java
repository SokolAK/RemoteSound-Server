package pl.sokolak.remotesoundserver;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Player player = new Player(this);
        player.init();

        ServerConnector serverConnector = new ServerConnector(this);
        serverConnector.init(player);
    }

}