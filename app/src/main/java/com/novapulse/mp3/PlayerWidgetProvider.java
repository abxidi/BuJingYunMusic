package com.novapulse.mp3;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.widget.RemoteViews;

public class PlayerWidgetProvider extends AppWidgetProvider {
    private static final String PREFS_NAME = "bu_jing_yun_music";
    private static final String PREF_UI_STYLE = "ui_style";
    private static final String PREF_WIDGET_TITLE = "widget_title";
    private static final String PREF_WIDGET_META = "widget_meta";
    private static final String PREF_WIDGET_DURATION = "widget_duration";
    private static final String PREF_WIDGET_ELAPSED = "widget_elapsed";
    private static final String PREF_WIDGET_PROGRESS = "widget_progress";
    private static final String PREF_WIDGET_PLAYING = "widget_playing";
    private static final String PREF_WIDGET_FAVORITE = "widget_favorite";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        updateWidgets(context, manager, appWidgetIds);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, PlayerWidgetProvider.class);
        updateWidgets(context, manager, manager.getAppWidgetIds(component));
    }

    private static void updateWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) return;
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        WidgetTheme theme = WidgetTheme.fromStyle(preferences.getInt(PREF_UI_STYLE, ThemeVisualizerView.STYLE_CLASSIC));

        String title = preferences.getString(PREF_WIDGET_TITLE, context.getString(R.string.widget_empty_title));
        String meta = preferences.getString(PREF_WIDGET_META, context.getString(R.string.widget_empty_meta));
        String elapsed = preferences.getString(PREF_WIDGET_ELAPSED, "00:00");
        String duration = preferences.getString(PREF_WIDGET_DURATION, "--:--");
        int progress = Math.max(0, Math.min(1000, preferences.getInt(PREF_WIDGET_PROGRESS, 0)));
        boolean playing = preferences.getBoolean(PREF_WIDGET_PLAYING, false);
        boolean favorite = preferences.getBoolean(PREF_WIDGET_FAVORITE, false);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_player);
            views.setInt(R.id.widgetRoot, "setBackgroundResource", theme.rootBackground);
            views.setInt(R.id.widgetArt, "setBackgroundResource", theme.artBackground);
            views.setInt(R.id.widgetPrevious, "setBackgroundResource", theme.controlBackground);
            views.setInt(R.id.widgetPlay, "setBackgroundResource", theme.playBackground);
            views.setInt(R.id.widgetNext, "setBackgroundResource", theme.controlBackground);
            views.setInt(R.id.widgetFavorite, "setBackgroundResource", favorite ? R.drawable.bg_widget_favorite : theme.controlBackground);

            views.setTextViewText(R.id.widgetTitle, title);
            views.setTextViewText(R.id.widgetMeta, meta);
            views.setTextViewText(R.id.widgetTime, elapsed + " / " + duration);
            views.setTextColor(R.id.widgetBrand, theme.mutedColor);
            views.setTextColor(R.id.widgetTitle, theme.inkColor);
            views.setTextColor(R.id.widgetMeta, theme.softColor);
            views.setTextColor(R.id.widgetTime, theme.mutedColor);
            views.setImageViewResource(R.id.widgetPlay, playing ? R.drawable.ic_pause : R.drawable.ic_play);
            views.setContentDescription(R.id.widgetPlay, playing ? "暂停" : "播放");
            views.setInt(R.id.widgetArtIcon, "setColorFilter", theme.accentColor);
            views.setInt(R.id.widgetPrevious, "setColorFilter", theme.inkColor);
            views.setInt(R.id.widgetNext, "setColorFilter", theme.inkColor);
            views.setInt(R.id.widgetPlay, "setColorFilter", Color.rgb(3, 16, 24));
            views.setInt(R.id.widgetFavorite, "setColorFilter", favorite ? Color.rgb(255, 93, 159) : theme.inkColor);
            views.setProgressBar(R.id.widgetProgress, 1000, progress, false);

            views.setOnClickPendingIntent(R.id.widgetRoot, activityIntent(context, MainActivity.ACTION_WIDGET_OPEN, 10));
            views.setOnClickPendingIntent(R.id.widgetPrevious, activityIntent(context, MainActivity.ACTION_WIDGET_PREVIOUS, 11));
            views.setOnClickPendingIntent(R.id.widgetPlay, activityIntent(context, MainActivity.ACTION_WIDGET_PLAY_TOGGLE, 12));
            views.setOnClickPendingIntent(R.id.widgetNext, activityIntent(context, MainActivity.ACTION_WIDGET_NEXT, 13));
            views.setOnClickPendingIntent(R.id.widgetFavorite, activityIntent(context, MainActivity.ACTION_WIDGET_FAVORITE, 14));
            manager.updateAppWidget(appWidgetId, views);
        }
    }

    private static PendingIntent activityIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags);
    }

    private static class WidgetTheme {
        final int rootBackground;
        final int artBackground;
        final int controlBackground;
        final int playBackground;
        final int inkColor;
        final int softColor;
        final int mutedColor;
        final int accentColor;

        WidgetTheme(
            int rootBackground,
            int artBackground,
            int controlBackground,
            int playBackground,
            int inkColor,
            int softColor,
            int mutedColor,
            int accentColor
        ) {
            this.rootBackground = rootBackground;
            this.artBackground = artBackground;
            this.controlBackground = controlBackground;
            this.playBackground = playBackground;
            this.inkColor = inkColor;
            this.softColor = softColor;
            this.mutedColor = mutedColor;
            this.accentColor = accentColor;
        }

        static WidgetTheme fromStyle(int style) {
            if (style == ThemeVisualizerView.STYLE_RADAR) {
                return new WidgetTheme(
                    R.drawable.bg_widget_radar,
                    R.drawable.bg_widget_art_radar,
                    R.drawable.bg_widget_button_radar,
                    R.drawable.bg_widget_play_radar,
                    Color.rgb(238, 255, 246),
                    Color.argb(214, 218, 255, 234),
                    Color.argb(178, 156, 232, 203),
                    Color.rgb(90, 255, 187)
                );
            }
            if (style == ThemeVisualizerView.STYLE_GALAXY) {
                return new WidgetTheme(
                    R.drawable.bg_widget_galaxy,
                    R.drawable.bg_widget_art_galaxy,
                    R.drawable.bg_widget_button_galaxy,
                    R.drawable.bg_widget_play_galaxy,
                    Color.rgb(245, 247, 250),
                    Color.argb(214, 224, 228, 235),
                    Color.argb(174, 158, 164, 176),
                    Color.rgb(245, 247, 250)
                );
            }
            return new WidgetTheme(
                R.drawable.bg_widget_classic,
                R.drawable.bg_widget_art_classic,
                R.drawable.bg_widget_button_classic,
                R.drawable.bg_widget_play_classic,
                Color.rgb(238, 248, 255),
                Color.argb(218, 184, 238, 248),
                Color.argb(178, 141, 163, 183),
                Color.rgb(100, 232, 255)
            );
        }
    }
}
