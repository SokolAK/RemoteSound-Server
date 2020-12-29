package pl.sokolak.remotesoundserver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

@SuppressLint("SetTextI18n")
@Getter
public class Player {
    private final Activity activity;
    private Status status = Status.STOPPED;
    private Sound currentSound;
    private MediaPlayer mediaPlayer;
    private final SoundButton button1 = new SoundButton();
    private final SoundButton button2 = new SoundButton();
    private final SoundButton button3 = new SoundButton();
    private final SoundButton button4 = new SoundButton();
    private final SoundButton button5 = new SoundButton();
    private final SoundButton button6 = new SoundButton();
    private EditText etRepeat;
    private TextView tvRepeat;
    private ImageButton buttonForward, buttonBackward;
    private ImageButton buttonStop;
    private ImageButton volumeDownButton, volumeUpButton;
    private TextView tvPlayerStatus;
    private TextView tvTime;
    private TextView tvVolume;
    private Handler timeHandler;
    private Handler rewindHandler;
    private List<SoundButton> buttons = new ArrayList<>();
    private static int TIMESTEP = 5000;

    public Player(Activity activity) {
        this.activity = activity;
    }

    public void init() {
        tvPlayerStatus = activity.findViewById(R.id.playerStatus);
        tvTime = activity.findViewById(R.id.time);

        buttons = Arrays.asList(button1, button2, button3);
        prepareButtons();
        prepareControlButtons();
        prepareRepeat();
        prepareVolumeControl();

        controlTimeUpdate(true);

        etRepeat.clearFocus();
    }

    private void prepareRepeat() {
        etRepeat = activity.findViewById(R.id.repeatValue);
        etRepeat.clearFocus();
        etRepeat.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_DONE){
                etRepeat.clearFocus();
            }
            return false;
        });

        tvRepeat = activity.findViewById(R.id.repeatLabel);
        tvRepeat.setText(activity.getString(R.string.repeat) + ":");
    }

    private void prepareButtons() {
        button1.setButton(activity.findViewById(R.id.button1));
        button2.setButton(activity.findViewById(R.id.button2));
        button3.setButton(activity.findViewById(R.id.button3));
        button4.setButton(activity.findViewById(R.id.button4));
        button5.setButton(activity.findViewById(R.id.button5));
        button6.setButton(activity.findViewById(R.id.button6));

        button1.setSound(new Sound(R.raw.dog1, activity.getString(R.string.dog1)));
        button2.setSound(new Sound(R.raw.dog2, activity.getString(R.string.dog2)));
        button3.setSound(new Sound(R.raw.door1, activity.getString(R.string.door1)));

        for (SoundButton soundButton : buttons) {
            soundButton.getButton().setOnClickListener(b -> {
                play(soundButton.getSound());
                switch (status) {
                    case PLAYING:
                        soundButton.getButton().setText("||");
                        break;
                    case PAUSED:
                        soundButton.getButton().setText(">>");
                        break;
                }
            });
        }

        buttonStop = activity.findViewById(R.id.buttonStop);
        buttonStop.setOnClickListener(b -> stop());
        refreshButtons();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void prepareControlButtons() {
        buttonForward = activity.findViewById(R.id.buttonForward);
        buttonForward.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (rewindHandler != null) return true;
                    rewindHandler = new Handler();
                    rewindHandler.postDelayed(rewindForward, 0);
                    break;
                case MotionEvent.ACTION_UP:
                    if (rewindHandler == null) return true;
                    rewindHandler.removeCallbacks(rewindForward);
                    rewindHandler = null;
                    break;
            }
            return false;
        });

        buttonBackward = activity.findViewById(R.id.buttonBackward);
        buttonBackward.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (rewindHandler != null) return true;
                    rewindHandler = new Handler();
                    rewindHandler.postDelayed(rewindBackward, 0);
                    break;
                case MotionEvent.ACTION_UP:
                    if (rewindHandler == null) return true;
                    rewindHandler.removeCallbacks(rewindBackward);
                    rewindHandler = null;
                    break;
            }
            return false;
        });
    }

    private void play(Sound sound) {
        if (!sound.equals(currentSound)) {
            if (mediaPlayer != null)
                mediaPlayer.stop();
            //mediaPlayer.reset();
            mediaPlayer = MediaPlayer.create(activity, sound.getSoundId());
            mediaPlayer.start();
            currentSound = sound;
            status = Status.PLAYING;
        } else {
            if (status == Status.PLAYING) {
                mediaPlayer.pause();
                status = Status.PAUSED;
            } else {
                mediaPlayer.start();
                status = Status.PLAYING;
            }
        }

        updateStatusText();
        refreshButtons();
    }

    private void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            status = Status.STOPPED;

            updateStatusText();
            refreshButtons();
        }
    }

    private void controlTimeUpdate(boolean run) {
        if(!run) {
            if (timeHandler != null) {
                timeHandler.removeCallbacks(updateSoundTime);
                timeHandler = null;
            }
        }
        else {
            timeHandler = new Handler();
            timeHandler.postDelayed(updateSoundTime, 0);
        }
    }

    private void refreshButtons() {
        for (SoundButton soundButton : buttons) {
            soundButton.getButton().setText(soundButton.getSound().getSoundName());
        }
    }

    private void updateStatusText() {
        String statusText;
        switch (status) {
            case PLAYING:
                statusText = activity.getString(R.string.playing);
                break;
            case PAUSED:
                statusText = activity.getString(R.string.paused);
                break;
            default:
                statusText = activity.getString(R.string.stopped);
        }

        String status = statusText + ": " + currentSound.toString();
        tvPlayerStatus.setText(status);
    }

    private final Runnable updateSoundTime = new Runnable() {
        @SuppressLint("DefaultLocale")
        public void run() {
            if (mediaPlayer != null) {
                int time = mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition();
                tvTime.setText(String.format("-%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(time),
                        TimeUnit.MILLISECONDS.toSeconds(time) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)))
                );
                if (time <= 10) {
                    if (Integer.parseInt(etRepeat.getText().toString()) <= 1) {
                        stop();
                    } else {
                        int repeatTime = Integer.parseInt(etRepeat.getText().toString());
                        etRepeat.setText(String.valueOf(repeatTime - 1));
                        mediaPlayer.seekTo(0);
                        mediaPlayer.start();
                    }
                }
            }
            timeHandler.postDelayed(this, 1000);
        }
    };

    private final Runnable rewindForward = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                int time = mediaPlayer.getCurrentPosition();
                int maxTime = mediaPlayer.getDuration();
                time = Math.min(time + TIMESTEP, maxTime);
                mediaPlayer.seekTo(time);
                rewindHandler.postDelayed(this, 200);
            }
        }
    };

    private final Runnable rewindBackward = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                int time = mediaPlayer.getCurrentPosition();
                time = Math.max(time - TIMESTEP, 0);
                mediaPlayer.seekTo(time);
                rewindHandler.postDelayed(this, 200);
            }
        }
    };

    private void prepareVolumeControl() {
        AudioManager audioManager = (AudioManager) activity.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        tvVolume = activity.findViewById(R.id.volumeValue);
        tvVolume.setText(String.valueOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));

        volumeUpButton = activity.findViewById(R.id.volumeUp);
        volumeUpButton.setOnClickListener(v -> {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
            tvVolume.setText(String.valueOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
        });

        volumeDownButton = activity.findViewById(R.id.volumeDown);
        volumeDownButton.setOnClickListener(v -> {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
            tvVolume.setText(String.valueOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
        });
    }

    enum Status {
        PLAYING,
        STOPPED,
        PAUSED
    }
}
