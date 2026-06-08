package com.novapulse.mp3;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private static final int REQUEST_AUDIO_PERMISSION = 1001;
    private static final int REQUEST_FOLDER = 1002;

    private final List<Song> songs = new ArrayList<>();
    private final Handler handler = new Handler();
    private final Random random = new Random();

    private TextView trackTitle;
    private TextView trackMeta;
    private TextView durationText;
    private TextView elapsedText;
    private TextView modeText;
    private TextView folderPath;
    private TextView folderCount;
    private ImageView modeIcon;
    private ImageButton playButton;
    private ImageButton favoriteButton;
    private View progressFill;
    private FrameLayout progressTouchArea;
    private FrameLayout progressTrack;
    private LinearLayout playerPage;
    private LinearLayout drawerContent;
    private View scrim;
    private ScrollView menuDrawer;
    private LinearLayout libraryList;
    private LinearLayout favoriteList;
    private LinearLayout panelLibrary;
    private LinearLayout panelFavorites;
    private LinearLayout panelFolders;
    private LinearLayout panelSettings;
    private ImageButton tabLibrary;
    private ImageButton tabFavorites;
    private ImageButton tabFolders;
    private ImageButton tabSettings;

    private MediaPlayer mediaPlayer;
    private int currentIndex;
    private int modeIndex;
    private boolean prepared;

    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.nova_bg));
        window.setNavigationBarColor(getResources().getColor(R.color.nova_bg));
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        setContentView(R.layout.activity_main);

        bindViews();
        applySystemBarInsets();
        bindActions();
        loadSamples();
        requestAudioPermissionIfNeeded();
        renderAll();
        selectSong(0, false);
        handler.post(progressUpdater);
    }

    private void bindViews() {
        playerPage = findViewById(R.id.playerPage);
        drawerContent = findViewById(R.id.drawerContent);
        trackTitle = findViewById(R.id.tvTrackTitle);
        trackMeta = findViewById(R.id.tvTrackMeta);
        durationText = findViewById(R.id.tvDuration);
        elapsedText = findViewById(R.id.tvElapsed);
        modeText = findViewById(R.id.tvMode);
        folderPath = findViewById(R.id.tvFolderPath);
        folderCount = findViewById(R.id.tvFolderCount);
        modeIcon = findViewById(R.id.ivMode);
        playButton = findViewById(R.id.btnPlay);
        favoriteButton = findViewById(R.id.btnFavorite);
        progressFill = findViewById(R.id.progressFill);
        progressTouchArea = findViewById(R.id.progressTouchArea);
        progressTrack = findViewById(R.id.progressTrack);
        scrim = findViewById(R.id.scrim);
        menuDrawer = findViewById(R.id.menuDrawer);
        libraryList = findViewById(R.id.libraryList);
        favoriteList = findViewById(R.id.favoriteList);
        panelLibrary = findViewById(R.id.panelLibrary);
        panelFavorites = findViewById(R.id.panelFavorites);
        panelFolders = findViewById(R.id.panelFolders);
        panelSettings = findViewById(R.id.panelSettings);
        tabLibrary = findViewById(R.id.tabLibrary);
        tabFavorites = findViewById(R.id.tabFavorites);
        tabFolders = findViewById(R.id.tabFolders);
        tabSettings = findViewById(R.id.tabSettings);
    }

    private void applySystemBarInsets() {
        final View root = findViewById(R.id.root);
        final int playerStart = playerPage.getPaddingStart();
        final int playerTop = playerPage.getPaddingTop();
        final int playerEnd = playerPage.getPaddingEnd();
        final int playerBottom = playerPage.getPaddingBottom();
        final int drawerStart = drawerContent.getPaddingStart();
        final int drawerTop = drawerContent.getPaddingTop();
        final int drawerEnd = drawerContent.getPaddingEnd();
        final int drawerBottom = drawerContent.getPaddingBottom();

        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                int topInset = insets.getSystemWindowInsetTop();
                int bottomInset = insets.getSystemWindowInsetBottom();
                playerPage.setPaddingRelative(
                    playerStart,
                    Math.max(playerTop, topInset),
                    playerEnd,
                    Math.max(playerBottom, bottomInset)
                );
                drawerContent.setPaddingRelative(
                    drawerStart,
                    Math.max(drawerTop, topInset),
                    drawerEnd,
                    Math.max(drawerBottom, bottomInset)
                );
                return insets;
            }
        });
        root.requestApplyInsets();
    }

    private void bindActions() {
        findViewById(R.id.btnMenu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDrawer();
            }
        });
        findViewById(R.id.btnCloseDrawer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeDrawer();
            }
        });
        scrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeDrawer();
            }
        });
        findViewById(R.id.btnMode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchMode();
            }
        });
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlay();
            }
        });
        progressTouchArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN
                    || action == MotionEvent.ACTION_MOVE
                    || action == MotionEvent.ACTION_UP) {
                    seekToProgressTouch(event.getX());
                    return true;
                }
                return action == MotionEvent.ACTION_CANCEL;
            }
        });
        findViewById(R.id.btnPrevious).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPrevious();
            }
        });
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNext(true);
            }
        });
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFavorite(currentIndex);
            }
        });
        tabLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPanel(panelLibrary, tabLibrary);
            }
        });
        tabFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPanel(panelFavorites, tabFavorites);
            }
        });
        tabFolders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPanel(panelFolders, tabFolders);
            }
        });
        tabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPanel(panelSettings, tabSettings);
            }
        });
        findViewById(R.id.btnPickFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_FOLDER);
            }
        });
    }

    private void requestAudioPermissionIfNeeded() {
        String permission = Build.VERSION.SDK_INT >= 33
            ? Manifest.permission.READ_MEDIA_AUDIO
            : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { permission }, REQUEST_AUDIO_PERMISSION);
        } else {
            scanAudioStore();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION
            && grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanAudioStore();
        }
    }

    private void scanAudioStore() {
        List<Song> scanned = new ArrayList<>();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        };

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                collection,
                projection,
                MediaStore.Audio.Media.IS_MUSIC + "!=0",
                null,
                MediaStore.Audio.Media.DATE_ADDED + " DESC"
            );

            if (cursor == null) return;

            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int displayColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);

            while (cursor.moveToNext() && scanned.size() < 300) {
                long id = cursor.getLong(idColumn);
                String displayName = cursor.getString(displayColumn);
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                long duration = cursor.getLong(durationColumn);
                long size = cursor.getLong(sizeColumn);
                Uri uri = ContentUris.withAppendedId(collection, id);

                if (duration <= 0) continue;

                String safeTitle = isEmpty(title) ? displayName : title + ".mp3";
                String safeArtist = isEmpty(artist) ? "手机存储" : artist;
                scanned.add(new Song(safeTitle, "手机存储 / Music / " + safeArtist, formatDuration(duration), formatSize(size), uri));
            }
        } catch (SecurityException ignored) {
            Toast.makeText(this, "需要音频读取权限", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }

        if (!scanned.isEmpty()) {
            songs.clear();
            songs.addAll(scanned);
            songs.get(0).favorite = true;
            currentIndex = 0;
            folderCount.setText("已读取 " + songs.size() + " 首音频");
            renderAll();
            selectSong(0, false);
        }
    }

    private void loadSamples() {
        songs.clear();
        Song first = new Song("星际漫游.mp3", "手机存储 / Music / Synthwave", "03:48", "8.6 MB", null);
        first.favorite = true;
        Song third = new Song("月面通讯.mp3", "手机存储 / Music / Ambient", "02:57", "7.1 MB", null);
        third.favorite = true;
        songs.add(first);
        songs.add(new Song("霓虹低频.mp3", "手机存储 / Downloads / Bass", "04:12", "11.2 MB", null));
        songs.add(third);
        songs.add(new Song("反应堆心跳.mp3", "手机存储 / Music / Cyber", "03:26", "9.4 MB", null));
        folderCount.setText("等待授权后读取本机音频");
    }

    private void renderAll() {
        renderList(libraryList, false);
        renderList(favoriteList, true);
    }

    private void renderList(LinearLayout container, boolean favoritesOnly) {
        container.removeAllViews();
        int count = 0;
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            if (favoritesOnly && !song.favorite) continue;
            container.addView(createSongCard(song, i));
            count++;
        }

        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("暂无收藏音乐");
            empty.setTextColor(getResources().getColor(R.color.nova_muted));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(28), dp(18), dp(28));
            empty.setBackgroundResource(R.drawable.bg_song_card);
            container.addView(empty, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
        }
    }

    private View createSongCard(final Song song, final int index) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackgroundResource(index == currentIndex ? R.drawable.bg_control_button_active : R.drawable.bg_song_card);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(64)
        );
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        ImageView art = new ImageView(this);
        art.setImageResource(R.drawable.ic_music);
        art.setBackgroundResource(R.drawable.bg_song_art);
        art.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.addView(art, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        infoParams.setMargins(dp(10), 0, dp(8), 0);

        TextView title = new TextView(this);
        title.setSingleLine(true);
        title.setText(song.title);
        title.setTextColor(getResources().getColor(R.color.nova_ink));
        title.setTextSize(14);
        title.setTypeface(null, 1);

        TextView detail = new TextView(this);
        detail.setSingleLine(true);
        detail.setText(song.category() + " · " + song.size);
        detail.setTextColor(getResources().getColor(R.color.nova_muted));
        detail.setTextSize(12);
        info.addView(title);
        info.addView(detail);
        row.addView(info, infoParams);

        TextView duration = new TextView(this);
        duration.setText(song.duration);
        duration.setTextColor(getResources().getColor(R.color.nova_soft));
        duration.setTextSize(12);
        duration.setGravity(Gravity.END);
        row.addView(duration, new LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.WRAP_CONTENT));

        ImageButton fav = new ImageButton(this);
        fav.setImageResource(R.drawable.ic_heart);
        fav.setBackgroundResource(song.favorite ? R.drawable.bg_favorite_button : R.drawable.bg_control_button);
        fav.setPadding(dp(8), dp(8), dp(8), dp(8));
        fav.setContentDescription("收藏");
        LinearLayout.LayoutParams favParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        favParams.setMargins(dp(8), 0, 0, 0);
        row.addView(fav, favParams);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectSong(index, false);
                closeDrawer();
            }
        });
        fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFavorite(index);
            }
        });
        return row;
    }

    private void selectSong(int index, boolean start) {
        if (index < 0 || index >= songs.size()) return;
        currentIndex = index;
        Song song = songs.get(index);
        trackTitle.setText(song.title);
        trackMeta.setText(song.meta);
        durationText.setText(song.duration);
        elapsedText.setText("00:00");
        favoriteButton.setBackgroundResource(song.favorite ? R.drawable.bg_favorite_button : R.drawable.bg_control_button);
        resetPlayer();
        updateProgressWidth(0);
        renderAll();
        if (start) togglePlay();
    }

    private void toggleFavorite(int index) {
        if (index < 0 || index >= songs.size()) return;
        Song song = songs.get(index);
        song.favorite = !song.favorite;
        favoriteButton.setBackgroundResource(songs.get(currentIndex).favorite ? R.drawable.bg_favorite_button : R.drawable.bg_control_button);
        renderAll();
    }

    private void togglePlay() {
        Song song = songs.get(currentIndex);
        if (song.uri == null) {
            Toast.makeText(this, "授权后可播放手机中的真实音频", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null && prepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updatePlayButton();
            return;
        }

        if (mediaPlayer != null && prepared) {
            mediaPlayer.start();
            updatePlayButton();
            return;
        }

        prepareAndPlay(song);
    }

    private void prepareAndPlay(Song song) {
        resetPlayer();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                prepared = true;
                player.start();
                updatePlayButton();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                playNext(false);
            }
        });

        try {
            mediaPlayer.setDataSource(this, song.uri);
            mediaPlayer.prepareAsync();
        } catch (IOException error) {
            Toast.makeText(this, "无法播放该音频", Toast.LENGTH_SHORT).show();
            resetPlayer();
        }
    }

    private void resetPlayer() {
        prepared = false;
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        updatePlayButton();
    }

    private void updatePlayButton() {
        boolean isPlaying = mediaPlayer != null && prepared && mediaPlayer.isPlaying();
        playButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        playButton.setContentDescription(isPlaying ? "暂停" : "播放");
    }

    private void playPrevious() {
        int next = currentIndex - 1;
        if (next < 0) next = songs.size() - 1;
        selectSong(next, true);
    }

    private void playNext(boolean manual) {
        int next;
        if (!manual && modeIndex == 2) {
            next = currentIndex;
        } else if (modeIndex == 0 && songs.size() > 1) {
            next = random.nextInt(songs.size());
            if (next == currentIndex) next = (next + 1) % songs.size();
        } else {
            next = (currentIndex + 1) % songs.size();
        }
        selectSong(next, true);
    }

    private void switchMode() {
        modeIndex = (modeIndex + 1) % 3;
        if (modeIndex == 0) {
            modeText.setText("随机");
            modeIcon.setImageResource(R.drawable.ic_shuffle);
        } else if (modeIndex == 1) {
            modeText.setText("循环");
            modeIcon.setImageResource(R.drawable.ic_repeat);
        } else {
            modeText.setText("单曲");
            modeIcon.setImageResource(R.drawable.ic_repeat);
        }
    }

    private void updateProgress() {
        if (mediaPlayer == null || !prepared) return;
        int position = mediaPlayer.getCurrentPosition();
        int duration = mediaPlayer.getDuration();
        elapsedText.setText(formatDuration(position));
        if (duration > 0) {
            updateProgressWidth(position / (float) duration);
        }
    }

    private void updateProgressWidth(final float progress) {
        progressTrack.post(new Runnable() {
            @Override
            public void run() {
                int width = Math.max(1, (int) (progressTrack.getWidth() * progress));
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) progressFill.getLayoutParams();
                params.width = width;
                progressFill.setLayoutParams(params);
            }
        });
    }

    private void seekToProgressTouch(float touchX) {
        if (mediaPlayer == null || !prepared) return;
        int duration = mediaPlayer.getDuration();
        int width = progressTrack.getWidth();
        if (duration <= 0 || width <= 0) return;

        float progress = Math.max(0f, Math.min(1f, touchX / width));
        int position = Math.min(duration, Math.max(0, (int) (duration * progress)));
        mediaPlayer.seekTo(position);
        elapsedText.setText(formatDuration(position));
        updateProgressWidth(progress);
    }

    private void openDrawer() {
        scrim.setVisibility(View.VISIBLE);
        menuDrawer.setVisibility(View.VISIBLE);
    }

    private void closeDrawer() {
        scrim.setVisibility(View.GONE);
        menuDrawer.setVisibility(View.GONE);
    }

    private void showPanel(View panel, ImageButton tab) {
        panelLibrary.setVisibility(panel == panelLibrary ? View.VISIBLE : View.GONE);
        panelFavorites.setVisibility(panel == panelFavorites ? View.VISIBLE : View.GONE);
        panelFolders.setVisibility(panel == panelFolders ? View.VISIBLE : View.GONE);
        panelSettings.setVisibility(panel == panelSettings ? View.VISIBLE : View.GONE);
        tabLibrary.setBackgroundResource(tab == tabLibrary ? R.drawable.bg_control_button_active : R.drawable.bg_control_button);
        tabFavorites.setBackgroundResource(tab == tabFavorites ? R.drawable.bg_control_button_active : R.drawable.bg_control_button);
        tabFolders.setBackgroundResource(tab == tabFolders ? R.drawable.bg_control_button_active : R.drawable.bg_control_button);
        tabSettings.setBackgroundResource(tab == tabSettings ? R.drawable.bg_control_button_active : R.drawable.bg_control_button);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                int flags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(treeUri, flags);
                folderPath.setText(treeUri.toString());
                Toast.makeText(this, "已设置音乐目录", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(progressUpdater);
        resetPlayer();
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "未知大小";
        return String.format(Locale.US, "%.1f MB", bytes / 1024f / 1024f);
    }

    private static String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private static class Song {
        final String title;
        final String meta;
        final String duration;
        final String size;
        final Uri uri;
        boolean favorite;

        Song(String title, String meta, String duration, String size, Uri uri) {
            this.title = title;
            this.meta = meta;
            this.duration = duration;
            this.size = size;
            this.uri = uri;
        }

        String category() {
            String[] parts = meta.split(" / ");
            return parts.length == 0 ? "Music" : parts[parts.length - 1];
        }
    }
}
