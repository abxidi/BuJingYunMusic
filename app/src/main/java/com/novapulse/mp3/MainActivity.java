package com.novapulse.mp3;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaMetadataRetriever;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQUEST_AUDIO_PERMISSION = 1001;
    private static final int REQUEST_FOLDER = 1002;
    private static final int MAX_TREE_SCAN_COUNT = 500;
    private static final int MAX_TREE_SCAN_DEPTH = 16;
    private static final String PREFS_NAME = "bu_jing_yun_music";
    private static final String PREF_FAVORITE_KEYS = "favorite_song_keys";
    private static final String PREF_UI_STYLE = "ui_style";

    private final List<Song> songs = new ArrayList<>();
    private final List<Integer> activeQueue = new ArrayList<>();
    private final Handler handler = new Handler();
    private final Random random = new Random();
    private final Collator titleCollator = Collator.getInstance(Locale.CHINA);
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
        new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(final int focusChange) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleAudioFocusChange(focusChange);
                    }
                });
            }
        };

    private TextView trackTitle;
    private TextView trackMeta;
    private TextView durationText;
    private TextView elapsedText;
    private View root;
    private View modeButton;
    private TextView folderPath;
    private TextView folderCount;
    private ImageView modeIcon;
    private ImageButton playButton;
    private ImageButton favoriteButton;
    private View discOrbit;
    private View disc;
    private ThemeVisualizerView themeVisualizer;
    private View progressFill;
    private FrameLayout progressTouchArea;
    private FrameLayout progressTrack;
    private LinearLayout visualizer;
    private LinearLayout playerPage;
    private LinearLayout drawerContent;
    private View scrim;
    private ScrollView menuDrawer;
    private TextView drawerTitle;
    private TextView drawerSubtitle;
    private LinearLayout playlistContent;
    private LinearLayout settingsContent;
    private LinearLayout allSongList;
    private LinearLayout favoriteList;
    private LinearLayout panelAllSongs;
    private LinearLayout panelFavoriteSongs;
    private TextView tabAllSongs;
    private TextView tabFavoriteSongs;
    private TextView themeClassic;
    private TextView themeLiquid;
    private TextView themeGalaxy;
    private TextView themeRadar;

    private MediaPlayer mediaPlayer;
    private ObjectAnimator discAnimator;
    private AnimatorSet visualizerAnimator;
    private AudioManager audioManager;
    private MediaSession mediaSession;
    private SharedPreferences preferences;
    private int currentIndex;
    private int activeQueuePosition = -1;
    private int modeIndex;
    private int uiStyle = ThemeVisualizerView.STYLE_CLASSIC;
    private boolean prepared;
    private boolean drawerFromBottom;
    private boolean favoriteQueueActive;
    private boolean hasAudioFocus;
    private boolean resumeOnAudioFocusGain;

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

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        bindViews();
        uiStyle = preferences.getInt(PREF_UI_STYLE, ThemeVisualizerView.STYLE_CLASSIC);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        initMediaSession();
        applySystemBarInsets();
        bindActions();
        applyUiStyle(uiStyle, false);
        loadSamples();
        requestAudioPermissionIfNeeded();
        renderAll();
        selectSong(0, false);
        handler.post(progressUpdater);
    }

    private void bindViews() {
        root = findViewById(R.id.root);
        playerPage = findViewById(R.id.playerPage);
        drawerContent = findViewById(R.id.drawerContent);
        trackTitle = findViewById(R.id.tvTrackTitle);
        trackMeta = findViewById(R.id.tvTrackMeta);
        durationText = findViewById(R.id.tvDuration);
        elapsedText = findViewById(R.id.tvElapsed);
        modeButton = findViewById(R.id.btnMode);
        folderPath = findViewById(R.id.tvFolderPath);
        folderCount = findViewById(R.id.tvFolderCount);
        modeIcon = findViewById(R.id.ivMode);
        playButton = findViewById(R.id.btnPlay);
        favoriteButton = findViewById(R.id.btnFavorite);
        discOrbit = findViewById(R.id.discOrbit);
        disc = findViewById(R.id.disc);
        themeVisualizer = findViewById(R.id.themeVisualizer);
        progressFill = findViewById(R.id.progressFill);
        progressTouchArea = findViewById(R.id.progressTouchArea);
        progressTrack = findViewById(R.id.progressTrack);
        visualizer = findViewById(R.id.visualizer);
        scrim = findViewById(R.id.scrim);
        menuDrawer = findViewById(R.id.menuDrawer);
        drawerTitle = findViewById(R.id.drawerTitle);
        drawerSubtitle = findViewById(R.id.drawerSubtitle);
        playlistContent = findViewById(R.id.playlistContent);
        settingsContent = findViewById(R.id.settingsContent);
        allSongList = findViewById(R.id.allSongList);
        favoriteList = findViewById(R.id.favoriteList);
        panelAllSongs = findViewById(R.id.panelAllSongs);
        panelFavoriteSongs = findViewById(R.id.panelFavoriteSongs);
        tabAllSongs = findViewById(R.id.tabAllSongs);
        tabFavoriteSongs = findViewById(R.id.tabFavoriteSongs);
        themeClassic = findViewById(R.id.themeClassic);
        themeLiquid = findViewById(R.id.themeLiquid);
        themeGalaxy = findViewById(R.id.themeGalaxy);
        themeRadar = findViewById(R.id.themeRadar);
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
                    drawerTop,
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
                openSettingsDrawer();
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
        modeButton.setOnClickListener(new View.OnClickListener() {
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
        findViewById(R.id.btnPlaylist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPlaylistDrawer();
            }
        });
        tabAllSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPlaylistPanel(false);
            }
        });
        tabFavoriteSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPlaylistPanel(true);
            }
        });
        findViewById(R.id.btnPlayAllSongs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playFirstFromList(false);
            }
        });
        findViewById(R.id.btnPlayFavoriteSongs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playFirstFromList(true);
            }
        });
        findViewById(R.id.btnPickFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_FOLDER);
            }
        });
        themeClassic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyUiStyle(ThemeVisualizerView.STYLE_CLASSIC, true);
            }
        });
        themeLiquid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyUiStyle(ThemeVisualizerView.STYLE_LIQUID, true);
            }
        });
        themeGalaxy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyUiStyle(ThemeVisualizerView.STYLE_GALAXY, true);
            }
        });
        themeRadar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyUiStyle(ThemeVisualizerView.STYLE_RADAR, true);
            }
        });
    }

    private void initMediaSession() {
        mediaSession = new MediaSession(this, "BuJingYunMusic");
        mediaSession.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isPlaybackActive()) togglePlay();
                    }
                });
            }

            @Override
            public void onPause() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        pausePlayback(false);
                    }
                });
            }

            @Override
            public void onSkipToPrevious() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        playPrevious();
                    }
                });
            }

            @Override
            public void onSkipToNext() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        playNext(true);
                    }
                });
            }

            @Override
            public void onStop() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        resetPlayer();
                    }
                });
            }
        });
        mediaSession.setActive(true);
        updateMediaSessionState();
    }

    private void applyUiStyle(int style, boolean persist) {
        if (style < ThemeVisualizerView.STYLE_CLASSIC || style > ThemeVisualizerView.STYLE_RADAR) {
            style = ThemeVisualizerView.STYLE_CLASSIC;
        }
        uiStyle = style;
        if (persist) {
            preferences.edit().putInt(PREF_UI_STYLE, uiStyle).apply();
        }
        discOrbit.setPadding(0, 0, 0, 0);

        if (uiStyle == ThemeVisualizerView.STYLE_CLASSIC) {
            root.setBackgroundResource(R.drawable.bg_player);
            discOrbit.setBackgroundColor(Color.TRANSPARENT);
            disc.setVisibility(View.VISIBLE);
            disc.setBackgroundResource(R.drawable.bg_disc);
            progressTrack.setBackgroundResource(R.drawable.bg_progress_track);
            progressFill.setBackgroundResource(R.drawable.bg_progress_fill);
            getWindow().setStatusBarColor(getResources().getColor(R.color.nova_bg));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.nova_bg));
        } else {
            root.setBackground(makeRootBackground());
            discOrbit.setBackgroundColor(Color.TRANSPARENT);
            disc.setVisibility(View.GONE);
            progressTrack.setBackground(makeProgressTrackBackground());
            progressFill.setBackground(makeProgressFillBackground());
            getWindow().setStatusBarColor(themeRootStart());
            getWindow().setNavigationBarColor(themeRootEnd());
        }

        themeVisualizer.setUiStyle(uiStyle);
        themeVisualizer.setPlaying(isPlaybackActive());
        applyStaticThemeBackgrounds();
        updateThemeTabs();
        updatePlayerMotion(isPlaybackActive());
        if (!songs.isEmpty()) {
            applyFavoriteButtonStyle(favoriteButton, songs.get(currentIndex).favorite);
            renderAll();
        }
    }

    private void applyStaticThemeBackgrounds() {
        setControlBackground(findViewById(R.id.btnMenu), false);
        setControlBackground(modeButton, false);
        setControlBackground(findViewById(R.id.btnPrevious), false);
        setControlBackground(findViewById(R.id.btnNext), false);
        setControlBackground(findViewById(R.id.btnPlaylist), false);
        setControlBackground(findViewById(R.id.btnCloseDrawer), false);
        setPlayBackground(findViewById(R.id.btnPlay));
        setPlayBackground(findViewById(R.id.btnPickFolder));
        setPlayBackground(findViewById(R.id.btnPlayAllSongs));
        setPlayBackground(findViewById(R.id.btnPlayFavoriteSongs));
        if (uiStyle == ThemeVisualizerView.STYLE_CLASSIC) {
            settingsContent.setBackgroundResource(R.drawable.bg_panel);
            menuDrawer.setBackgroundResource(R.drawable.bg_drawer);
        } else {
            settingsContent.setBackground(makePanelBackground(false));
            menuDrawer.setBackground(makePanelBackground(false));
        }
        applyVisualizerBarTheme();
        showPlaylistPanel(panelFavoriteSongs.getVisibility() == View.VISIBLE);
    }

    private void updateThemeTabs() {
        setControlBackground(themeClassic, uiStyle == ThemeVisualizerView.STYLE_CLASSIC);
        setControlBackground(themeLiquid, uiStyle == ThemeVisualizerView.STYLE_LIQUID);
        setControlBackground(themeGalaxy, uiStyle == ThemeVisualizerView.STYLE_GALAXY);
        setControlBackground(themeRadar, uiStyle == ThemeVisualizerView.STYLE_RADAR);
    }

    private void setControlBackground(View view, boolean active) {
        if (view == null) return;
        if (uiStyle == ThemeVisualizerView.STYLE_CLASSIC) {
            view.setBackgroundResource(active ? R.drawable.bg_control_button_active : R.drawable.bg_control_button);
            return;
        }
        int fill = active ? themeActiveFill() : themePanelFill();
        int stroke = active ? themeAccent() : themeLine();
        view.setBackground(makeRoundedBackground(fill, stroke, 18));
    }

    private void setPlayBackground(View view) {
        if (view == null) return;
        if (uiStyle == ThemeVisualizerView.STYLE_CLASSIC) {
            view.setBackgroundResource(R.drawable.bg_play_button);
            return;
        }
        GradientDrawable background = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { themeAccent(), themeAccentAlt() }
        );
        background.setCornerRadius(dp(24));
        view.setBackground(background);
    }

    private void setSongCardBackground(View view, boolean active) {
        if (uiStyle == ThemeVisualizerView.STYLE_CLASSIC) {
            view.setBackgroundResource(active ? R.drawable.bg_control_button_active : R.drawable.bg_song_card);
            return;
        }
        view.setBackground(makeRoundedBackground(
            active ? themeActiveFill() : themePanelFill(),
            active ? themeAccent() : withAlpha(themeLine(), 145),
            18
        ));
    }

    private GradientDrawable makeRootBackground() {
        return new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { themeRootStart(), themeRootCenter(), themeRootEnd() }
        );
    }

    private GradientDrawable makePanelBackground(boolean strong) {
        GradientDrawable drawable = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] {
                strong ? themePanelStrongFill() : themePanelFill(),
                withAlpha(themeAccent(), strong ? 76 : 52),
                strong ? themePanelFill() : withAlpha(Color.BLACK, 214)
            }
        );
        drawable.setStroke(dp(1), withAlpha(themeAccent(), strong ? 232 : 205));
        drawable.setCornerRadius(dp(22));
        return drawable;
    }

    private GradientDrawable makeProgressTrackBackground() {
        return makeRoundedBackground(withAlpha(Color.BLACK, 95), withAlpha(themeLine(), 110), 6);
    }

    private GradientDrawable makeProgressFillBackground() {
        GradientDrawable drawable = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] { themeAccent(), themeAccentAlt() }
        );
        drawable.setCornerRadius(dp(6));
        return drawable;
    }

    private GradientDrawable makeRoundedBackground(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setStroke(dp(1), stroke);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private void applyVisualizerBarTheme() {
        for (int i = 0; i < visualizer.getChildCount(); i++) {
            View bar = visualizer.getChildAt(i);
            if (uiStyle == ThemeVisualizerView.STYLE_CLASSIC) {
                bar.setBackgroundResource(R.drawable.bg_progress_fill);
            } else {
                bar.setBackground(makeProgressFillBackground());
            }
        }
    }

    private int themeRootStart() {
        if (uiStyle == ThemeVisualizerView.STYLE_LIQUID) return Color.rgb(3, 2, 8);
        if (uiStyle == ThemeVisualizerView.STYLE_RADAR) return Color.rgb(2, 19, 16);
        return Color.rgb(1, 1, 2);
    }

    private int themeRootCenter() {
        if (uiStyle == ThemeVisualizerView.STYLE_LIQUID) return Color.rgb(18, 6, 28);
        if (uiStyle == ThemeVisualizerView.STYLE_RADAR) return Color.rgb(4, 42, 32);
        return Color.rgb(13, 14, 17);
    }

    private int themeRootEnd() {
        if (uiStyle == ThemeVisualizerView.STYLE_LIQUID) return Color.rgb(8, 3, 16);
        if (uiStyle == ThemeVisualizerView.STYLE_RADAR) return Color.rgb(1, 8, 9);
        return Color.rgb(0, 0, 0);
    }

    private int themePanelFill() {
        if (uiStyle == ThemeVisualizerView.STYLE_LIQUID) return Color.argb(242, 24, 11, 33);
        if (uiStyle == ThemeVisualizerView.STYLE_RADAR) return Color.argb(240, 5, 44, 34);
        return Color.argb(242, 14, 15, 18);
    }

    private int themePanelStrongFill() {
        if (uiStyle == ThemeVisualizerView.STYLE_LIQUID) return Color.argb(250, 37, 14, 48);
        if (uiStyle == ThemeVisualizerView.STYLE_RADAR) return Color.argb(248, 4, 56, 43);
        return Color.argb(250, 24, 25, 29);
    }

    private int themeActiveFill() {
        if (uiStyle == ThemeVisualizerView.STYLE_LIQUID) return Color.argb(94, 255, 48, 218);
        if (uiStyle == ThemeVisualizerView.STYLE_RADAR) return Color.argb(86, 78, 255, 177);
        return Color.argb(96, 245, 247, 250);
    }

    private int themeLine() {
        if (uiStyle == ThemeVisualizerView.STYLE_LIQUID) return Color.argb(205, 230, 120, 255);
        if (uiStyle == ThemeVisualizerView.STYLE_RADAR) return Color.argb(185, 86, 255, 190);
        return Color.argb(172, 214, 218, 224);
    }

    private int themeAccent() {
        if (uiStyle == ThemeVisualizerView.STYLE_LIQUID) return Color.rgb(255, 45, 222);
        if (uiStyle == ThemeVisualizerView.STYLE_RADAR) return Color.rgb(90, 255, 187);
        return Color.rgb(245, 247, 250);
    }

    private int themeAccentAlt() {
        if (uiStyle == ThemeVisualizerView.STYLE_LIQUID) return Color.rgb(255, 205, 102);
        if (uiStyle == ThemeVisualizerView.STYLE_RADAR) return Color.rgb(202, 255, 85);
        return Color.rgb(142, 148, 158);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
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

                String safeTitle = isEmpty(displayName) ? title : displayName;
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
            applyPersistedFavorites();
            currentIndex = 0;
            folderCount.setText("已读取 " + songs.size() + " 首音频");
            renderAll();
            selectSong(0, false);
        }
    }

    private void scanSelectedFolder(Uri treeUri) {
        List<Song> scanned = new ArrayList<>();
        try {
            scanDocumentChildren(treeUri, DocumentsContract.getTreeDocumentId(treeUri), scanned, 0);
        } catch (RuntimeException error) {
            Toast.makeText(this, "无法读取所选目录", Toast.LENGTH_SHORT).show();
        }

        if (scanned.isEmpty()) {
            folderCount.setText("当前目录及子目录未找到音频");
            Toast.makeText(this, "当前目录及子目录未找到音频", Toast.LENGTH_SHORT).show();
            return;
        }

        songs.clear();
        songs.addAll(scanned);
        applyPersistedFavorites();
        currentIndex = 0;
        activeQueue.clear();
        activeQueuePosition = -1;
        folderCount.setText("已读取 " + songs.size() + " 首音频");
        renderAll();
        List<Integer> indices = sortedSongIndices(false);
        selectSong(indices.isEmpty() ? 0 : indices.get(0), false);
    }

    private void scanDocumentChildren(Uri treeUri, String documentId, List<Song> scanned, int depth) {
        if (depth > MAX_TREE_SCAN_DEPTH || scanned.size() >= MAX_TREE_SCAN_COUNT) return;

        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);
        String[] projection = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        };

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(childrenUri, projection, null, null, DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            if (cursor == null) return;

            int idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
            int sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE);

            while (cursor.moveToNext() && scanned.size() < MAX_TREE_SCAN_COUNT) {
                String childId = cursor.getString(idColumn);
                String name = cursor.getString(nameColumn);
                String mimeType = cursor.getString(mimeColumn);
                long size = cursor.isNull(sizeColumn) ? 0 : cursor.getLong(sizeColumn);

                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                    scanDocumentChildren(treeUri, childId, scanned, depth + 1);
                } else if (isAudioDocument(name, mimeType)) {
                    Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId);
                    scanned.add(createSongFromDocument(documentUri, name, size));
                }
            }
        } catch (SecurityException ignored) {
            Toast.makeText(this, "需要目录读取权限", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private Song createSongFromDocument(Uri documentUri, String displayName, long size) {
        String safeName = isEmpty(displayName) ? "未知音频" : displayName;
        return new Song(
            safeName,
            "所选目录 / 子目录",
            readAudioDuration(documentUri),
            formatSize(size),
            documentUri
        );
    }

    private String readAudioDuration(Uri documentUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, documentUri);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (!isEmpty(duration)) {
                return formatDuration(Long.parseLong(duration));
            }
        } catch (RuntimeException ignored) {
            // Some document providers do not expose metadata streams reliably.
        } finally {
            try {
                retriever.release();
            } catch (IOException ignored) {
            }
        }
        return "--:--";
    }

    private static boolean isAudioDocument(String name, String mimeType) {
        if (mimeType != null && mimeType.startsWith("audio/")) return true;
        if (name == null) return false;
        String lowerName = name.toLowerCase(Locale.US);
        return lowerName.endsWith(".mp3")
            || lowerName.endsWith(".m4a")
            || lowerName.endsWith(".aac")
            || lowerName.endsWith(".wav")
            || lowerName.endsWith(".wave")
            || lowerName.endsWith(".flac")
            || lowerName.endsWith(".alac")
            || lowerName.endsWith(".aif")
            || lowerName.endsWith(".aiff")
            || lowerName.endsWith(".ogg")
            || lowerName.endsWith(".oga")
            || lowerName.endsWith(".opus")
            || lowerName.endsWith(".amr")
            || lowerName.endsWith(".3gp")
            || lowerName.endsWith(".wma");
    }

    private void loadSamples() {
        songs.clear();
        Song first = new Song("星际漫游.mp3", "手机存储 / Music / Synthwave", "03:48", "8.6 MB", null);
        Song third = new Song("月面通讯.mp3", "手机存储 / Music / Ambient", "02:57", "7.1 MB", null);
        songs.add(first);
        songs.add(new Song("霓虹低频.mp3", "手机存储 / Downloads / Bass", "04:12", "11.2 MB", null));
        songs.add(third);
        songs.add(new Song("反应堆心跳.mp3", "手机存储 / Music / Cyber", "03:26", "9.4 MB", null));
        applyPersistedFavorites();
        folderCount.setText("等待授权后读取本机音频");
    }

    private void renderAll() {
        renderList(allSongList, false);
        renderList(favoriteList, true);
    }

    private void renderList(LinearLayout container, boolean favoritesOnly) {
        container.removeAllViews();
        List<Integer> indices = sortedSongIndices(favoritesOnly);
        for (int i = 0; i < indices.size(); i++) {
            int songIndex = indices.get(i);
            container.addView(createSongCard(songs.get(songIndex), songIndex, favoritesOnly));
        }

        if (indices.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(favoritesOnly ? "暂无收藏音乐" : "暂无本地音乐");
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

    private List<Integer> sortedSongIndices(final boolean favoritesOnly) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            if (!favoritesOnly || songs.get(i).favorite) {
                indices.add(i);
            }
        }
        Collections.sort(indices, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                Song leftSong = songs.get(left);
                Song rightSong = songs.get(right);
                int result = titleCollator.compare(leftSong.title, rightSong.title);
                return result != 0 ? result : left - right;
            }
        });
        return indices;
    }

    private View createSongCard(final Song song, final int index, final boolean favoritesOnly) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        setSongCardBackground(row, index == currentIndex);

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
        applyFavoriteButtonStyle(fav, song.favorite);
        fav.setPadding(dp(8), dp(8), dp(8), dp(8));
        fav.setContentDescription("收藏");
        LinearLayout.LayoutParams favParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        favParams.setMargins(dp(8), 0, 0, 0);
        row.addView(fav, favParams);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (favoritesOnly) {
                    activateFavoriteQueue(index);
                    selectSong(index, true, true);
                } else {
                    selectSong(index, true);
                }
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
        selectSong(index, start, false);
    }

    private void selectSong(int index, boolean start, boolean keepQueue) {
        if (index < 0 || index >= songs.size()) return;
        if (!keepQueue) {
            activeQueue.clear();
            activeQueuePosition = -1;
            favoriteQueueActive = false;
        }
        currentIndex = index;
        Song song = songs.get(index);
        trackTitle.setText(song.title);
        trackMeta.setText(song.meta);
        durationText.setText(song.duration);
        elapsedText.setText("00:00");
        applyFavoriteButtonStyle(favoriteButton, song.favorite);
        resetPlayer();
        updateProgressWidth(0);
        renderAll();
        if (start) togglePlay();
    }

    private void toggleFavorite(int index) {
        if (index < 0 || index >= songs.size()) return;
        Song song = songs.get(index);
        song.favorite = !song.favorite;
        persistFavorite(song);
        applyFavoriteButtonStyle(favoriteButton, songs.get(currentIndex).favorite);
        if (favoriteQueueActive) {
            refreshFavoriteQueue();
        }
        renderAll();
    }

    private void applyPersistedFavorites() {
        Set<String> favoriteKeys = loadFavoriteKeys();
        for (Song song : songs) {
            song.favorite = favoriteKeys.contains(favoriteKey(song));
        }
    }

    private void persistFavorite(Song song) {
        Set<String> favoriteKeys = loadFavoriteKeys();
        String key = favoriteKey(song);
        if (song.favorite) {
            favoriteKeys.add(key);
        } else {
            favoriteKeys.remove(key);
        }
        preferences.edit().putStringSet(PREF_FAVORITE_KEYS, favoriteKeys).apply();
    }

    private Set<String> loadFavoriteKeys() {
        return new HashSet<>(preferences.getStringSet(PREF_FAVORITE_KEYS, new HashSet<String>()));
    }

    private String favoriteKey(Song song) {
        if (song.uri != null) {
            return song.uri.toString();
        }
        return song.title + "|" + song.meta;
    }

    private void applyFavoriteButtonStyle(ImageButton button, boolean favorite) {
        if (favorite) {
            button.setBackgroundResource(R.drawable.bg_favorite_button);
        } else {
            setControlBackground(button, false);
        }
        button.setColorFilter(getResources().getColor(favorite ? R.color.nova_pink : android.R.color.white));
    }

    private void togglePlay() {
        Song song = songs.get(currentIndex);
        if (song.uri == null) {
            Toast.makeText(this, "授权后可播放手机中的真实音频", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null && prepared && mediaPlayer.isPlaying()) {
            pausePlayback(false);
            return;
        }

        if (mediaPlayer != null && prepared) {
            if (!requestPlaybackFocus()) return;
            mediaPlayer.start();
            updatePlayButton();
            return;
        }

        prepareAndPlay(song);
    }

    private void prepareAndPlay(Song song) {
        resetPlayer();
        if (!requestPlaybackFocus()) return;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build());
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                prepared = true;
                if (hasAudioFocus) {
                    player.start();
                }
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
        abandonPlaybackFocus();
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        updatePlayButton();
    }

    private void pausePlayback(boolean keepAudioFocus) {
        if (mediaPlayer != null && prepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        if (!keepAudioFocus) {
            abandonPlaybackFocus();
        }
        updatePlayButton();
    }

    private boolean isPlaybackActive() {
        return mediaPlayer != null && prepared && mediaPlayer.isPlaying();
    }

    private boolean requestPlaybackFocus() {
        if (audioManager == null || hasAudioFocus) return true;
        int result = audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        );
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if (!hasAudioFocus) {
            Toast.makeText(this, "暂时无法获取音频播放权限", Toast.LENGTH_SHORT).show();
        }
        return hasAudioFocus;
    }

    private void abandonPlaybackFocus() {
        resumeOnAudioFocusGain = false;
        if (audioManager != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
        hasAudioFocus = false;
    }

    private void handleAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            hasAudioFocus = true;
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(1f, 1f);
            }
            if (resumeOnAudioFocusGain && mediaPlayer != null && prepared) {
                resumeOnAudioFocusGain = false;
                mediaPlayer.start();
            }
            updatePlayButton();
            return;
        }

        boolean wasPlaying = isPlaybackActive();
        hasAudioFocus = false;
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
            || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            resumeOnAudioFocusGain = wasPlaying;
            pausePlayback(true);
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            resumeOnAudioFocusGain = false;
            pausePlayback(true);
            abandonPlaybackFocus();
        }
    }

    private void updatePlayButton() {
        boolean isPlaying = isPlaybackActive();
        playButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        playButton.setContentDescription(isPlaying ? "暂停" : "播放");
        updatePlayerMotion(isPlaying);
        updateMediaSessionState();
    }

    private void updateMediaSessionState() {
        if (mediaSession == null) return;
        boolean isPlaying = isPlaybackActive();
        int state = prepared
            ? (isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED)
            : PlaybackState.STATE_STOPPED;
        long position = 0L;
        if (mediaPlayer != null && prepared) {
            try {
                position = mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException ignored) {
                position = 0L;
            }
        }
        long actions = PlaybackState.ACTION_PLAY
            | PlaybackState.ACTION_PAUSE
            | PlaybackState.ACTION_PLAY_PAUSE
            | PlaybackState.ACTION_SKIP_TO_PREVIOUS
            | PlaybackState.ACTION_SKIP_TO_NEXT
            | PlaybackState.ACTION_STOP;
        mediaSession.setPlaybackState(new PlaybackState.Builder()
            .setActions(actions)
            .setState(state, position, isPlaying ? 1f : 0f)
            .build());
    }

    private void updatePlayerMotion(boolean isPlaying) {
        themeVisualizer.setPlaying(isPlaying);
        if (uiStyle == ThemeVisualizerView.STYLE_CLASSIC && isPlaying) {
            startDiscRotation();
        } else {
            stopDiscRotation();
        }
        if (isPlaying) {
            startVisualizerMotion();
        } else {
            stopVisualizerMotion();
        }
    }

    private void startDiscRotation() {
        if (discAnimator != null && discAnimator.isStarted()) return;
        float startRotation = disc.getRotation();
        discAnimator = ObjectAnimator.ofFloat(disc, "rotation", startRotation, startRotation + 360f);
        discAnimator.setDuration(9000);
        discAnimator.setInterpolator(new LinearInterpolator());
        discAnimator.setRepeatCount(ValueAnimator.INFINITE);
        discAnimator.setRepeatMode(ValueAnimator.RESTART);
        discAnimator.start();
    }

    private void stopDiscRotation() {
        if (discAnimator == null) return;
        discAnimator.cancel();
        discAnimator = null;
    }

    private void startVisualizerMotion() {
        if (visualizerAnimator != null && visualizerAnimator.isStarted()) return;
        if (visualizer.getWidth() == 0) {
            visualizer.post(new Runnable() {
                @Override
                public void run() {
                    boolean isPlaying = mediaPlayer != null && prepared && mediaPlayer.isPlaying();
                    if (isPlaying) startVisualizerMotion();
                }
            });
            return;
        }

        List<Animator> animators = new ArrayList<>();
        float[] minScales = {0.45f, 0.7f, 0.35f, 0.58f, 0.5f};
        int[] durations = {360, 420, 330, 390, 450};
        int[] delays = {0, 70, 120, 40, 150};
        int barCount = Math.min(visualizer.getChildCount(), minScales.length);
        for (int index = 0; index < barCount; index++) {
            View bar = visualizer.getChildAt(index);
            bar.setPivotX(bar.getWidth() / 2f);
            bar.setPivotY(bar.getHeight());
            ObjectAnimator scale = ObjectAnimator.ofFloat(bar, "scaleY", minScales[index], 1f);
            scale.setDuration(durations[index]);
            scale.setStartDelay(delays[index]);
            scale.setRepeatCount(ValueAnimator.INFINITE);
            scale.setRepeatMode(ValueAnimator.REVERSE);
            scale.setInterpolator(new AccelerateDecelerateInterpolator());
            animators.add(scale);
        }

        visualizerAnimator = new AnimatorSet();
        visualizerAnimator.playTogether(animators);
        visualizerAnimator.start();
    }

    private void stopVisualizerMotion() {
        if (visualizerAnimator != null) {
            visualizerAnimator.cancel();
            visualizerAnimator = null;
        }
        for (int index = 0; index < visualizer.getChildCount(); index++) {
            View bar = visualizer.getChildAt(index);
            bar.setScaleY(1f);
            bar.setAlpha(1f);
        }
    }

    private void playPrevious() {
        if (favoriteQueueActive) {
            refreshFavoriteQueue();
            if (activeQueue.isEmpty()) {
                Toast.makeText(this, "暂无收藏音乐", Toast.LENGTH_SHORT).show();
                return;
            }
            activeQueuePosition = activeQueuePosition <= 0 ? activeQueue.size() - 1 : activeQueuePosition - 1;
            selectSong(activeQueue.get(activeQueuePosition), true, true);
            return;
        }
        int next = currentIndex - 1;
        if (next < 0) next = songs.size() - 1;
        selectSong(next, true);
    }

    private void playNext(boolean manual) {
        if (favoriteQueueActive) {
            refreshFavoriteQueue();
            if (activeQueue.isEmpty()) {
                Toast.makeText(this, "暂无收藏音乐", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!manual && modeIndex == 2) {
                selectSong(activeQueue.get(activeQueuePosition), true, true);
                return;
            }
            if (modeIndex == 0 && activeQueue.size() > 1) {
                int nextQueuePosition = random.nextInt(activeQueue.size());
                if (nextQueuePosition == activeQueuePosition) {
                    nextQueuePosition = (nextQueuePosition + 1) % activeQueue.size();
                }
                activeQueuePosition = nextQueuePosition;
            } else {
                activeQueuePosition = (activeQueuePosition + 1) % activeQueue.size();
            }
            selectSong(activeQueue.get(activeQueuePosition), true, true);
            return;
        }
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
            modeIcon.setImageResource(R.drawable.ic_shuffle);
            modeButton.setContentDescription("随机播放");
        } else if (modeIndex == 1) {
            modeIcon.setImageResource(R.drawable.ic_repeat);
            modeButton.setContentDescription("循环播放");
        } else {
            modeIcon.setImageResource(R.drawable.ic_repeat_one);
            modeButton.setContentDescription("单曲循环");
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

    private void openSettingsDrawer() {
        configureSettingsDrawer();
        drawerTitle.setText("设置");
        drawerSubtitle.setText("目录 / 播放控制");
        playlistContent.setVisibility(View.GONE);
        settingsContent.setVisibility(View.VISIBLE);
        openDrawer();
    }

    private void openPlaylistDrawer() {
        configurePlaylistDrawer();
        drawerTitle.setText("歌单");
        drawerSubtitle.setText("全部音乐 / 收藏音乐");
        settingsContent.setVisibility(View.GONE);
        playlistContent.setVisibility(View.VISIBLE);
        showPlaylistPanel(false);
        renderAll();
        openDrawer();
    }

    private void showPlaylistPanel(boolean favoritesOnly) {
        panelAllSongs.setVisibility(favoritesOnly ? View.GONE : View.VISIBLE);
        panelFavoriteSongs.setVisibility(favoritesOnly ? View.VISIBLE : View.GONE);
        setControlBackground(tabAllSongs, !favoritesOnly);
        setControlBackground(tabFavoriteSongs, favoritesOnly);
    }

    private void playFirstFromList(boolean favoritesOnly) {
        List<Integer> indices = sortedSongIndices(favoritesOnly);
        if (indices.isEmpty()) {
            Toast.makeText(this, favoritesOnly ? "暂无收藏音乐" : "暂无本地音乐", Toast.LENGTH_SHORT).show();
            return;
        }
        if (favoritesOnly) {
            activateFavoriteQueue(indices.get(0));
            selectSong(activeQueue.get(activeQueuePosition), true, true);
        } else {
            selectSong(indices.get(0), true);
        }
        closeDrawer();
    }

    private void activateFavoriteQueue(int selectedIndex) {
        favoriteQueueActive = true;
        activeQueue.clear();
        activeQueue.addAll(sortedSongIndices(true));
        activeQueuePosition = activeQueue.indexOf(selectedIndex);
        if (activeQueuePosition < 0 && !activeQueue.isEmpty()) {
            activeQueuePosition = 0;
        }
    }

    private void refreshFavoriteQueue() {
        if (!favoriteQueueActive) return;
        activeQueue.clear();
        activeQueue.addAll(sortedSongIndices(true));
        activeQueuePosition = activeQueue.indexOf(currentIndex);
        if (activeQueuePosition < 0 && !activeQueue.isEmpty()) {
            activeQueuePosition = 0;
        }
    }

    private void configureSettingsDrawer() {
        drawerFromBottom = false;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) menuDrawer.getLayoutParams();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        params.width = Math.min(screenWidth - dp(40), dp(340));
        params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER;
        menuDrawer.setLayoutParams(params);
        menuDrawer.setTranslationX(0);
        menuDrawer.setTranslationY(0);
        menuDrawer.smoothScrollTo(0, 0);
    }

    private void configurePlaylistDrawer() {
        drawerFromBottom = true;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) menuDrawer.getLayoutParams();
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        params.width = FrameLayout.LayoutParams.MATCH_PARENT;
        params.height = Math.min(Math.max(dp(420), screenHeight - dp(96)), dp(640));
        params.gravity = Gravity.BOTTOM;
        menuDrawer.setLayoutParams(params);
        menuDrawer.setTranslationX(0);
        menuDrawer.smoothScrollTo(0, 0);
    }

    private void openDrawer() {
        scrim.setVisibility(View.VISIBLE);
        menuDrawer.animate().cancel();
        if (drawerFromBottom) {
            menuDrawer.setTranslationY(getResources().getDisplayMetrics().heightPixels);
        } else {
            menuDrawer.setTranslationY(0);
        }
        menuDrawer.setVisibility(View.VISIBLE);
        if (drawerFromBottom) {
            menuDrawer.post(new Runnable() {
                @Override
                public void run() {
                    menuDrawer.setTranslationY(menuDrawer.getHeight());
                    menuDrawer.animate()
                        .translationY(0)
                        .setDuration(180)
                        .start();
                }
            });
        }
    }

    private void closeDrawer() {
        scrim.setVisibility(View.GONE);
        menuDrawer.animate().cancel();
        if (drawerFromBottom && menuDrawer.getVisibility() == View.VISIBLE) {
            menuDrawer.animate()
                .translationY(menuDrawer.getHeight())
                .setDuration(160)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        menuDrawer.setVisibility(View.GONE);
                        menuDrawer.setTranslationY(0);
                    }
                })
                .start();
        } else {
            menuDrawer.setVisibility(View.GONE);
            menuDrawer.setTranslationY(0);
        }
    }

    @Override
    public void onBackPressed() {
        if (menuDrawer.getVisibility() == View.VISIBLE) {
            closeDrawer();
            return;
        }
        super.onBackPressed();
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
                scanSelectedFolder(treeUri);
                Toast.makeText(this, "已设置音乐目录", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(progressUpdater);
        resetPlayer();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
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
