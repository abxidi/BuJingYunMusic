package com.novapulse.mp3;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Random;

public class ThemeVisualizerView extends View {
    public static final int STYLE_CLASSIC = 0;
    public static final int STYLE_LIQUID = 1;
    public static final int STYLE_GALAXY = 2;
    public static final int STYLE_RADAR = 3;

    private static final int RADAR_TARGET_COUNT = 20;
    private static final float RADAR_SWEEP_PLAYING_STEP = 0.44f;
    private static final float RADAR_SWEEP_IDLE_STEP = 0.055f;
    private static final String[] RADAR_NOTE_SYMBOLS = {"♪", "♫", "♬", "♩", "♭"};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path liquidPath = new Path();
    private final Path radarPath = new Path();
    private final GalaxyParticle[] radarTargets = new GalaxyParticle[RADAR_TARGET_COUNT];
    private final RectF scratchOval = new RectF();

    private ValueAnimator animator;
    private int uiStyle = STYLE_CLASSIC;
    private float phase;
    private float radarSweepAngle;
    private boolean playing;

    public ThemeVisualizerView(Context context) {
        super(context);
        init();
    }

    public ThemeVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThemeVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        Random random = new Random(4217L);
        for (int i = 0; i < RADAR_TARGET_COUNT; i++) {
            GalaxyParticle target = new GalaxyParticle();
            target.arm = i % RADAR_NOTE_SYMBOLS.length;
            target.angle = random.nextFloat() * (float) Math.PI * 2f;
            target.radius = 0.18f + random.nextFloat() * 0.76f;
            target.size = 2.2f + random.nextFloat() * 4.8f;
            target.alpha = 0.5f + random.nextFloat() * 0.5f;
            target.spread = random.nextFloat() * 0.55f + 0.12f;
            target.twinkle = random.nextFloat() * (float) Math.PI * 2f;
            target.depth = random.nextFloat();
            radarTargets[i] = target;
        }
    }

    public void setUiStyle(int style) {
        uiStyle = style;
        if (uiStyle == STYLE_CLASSIC) {
            stopAnimator();
            setVisibility(View.GONE);
        } else {
            setVisibility(View.VISIBLE);
            startAnimator();
        }
        invalidate();
    }

    public void setPlaying(boolean isPlaying) {
        playing = isPlaying;
        if (uiStyle != STYLE_CLASSIC) {
            startAnimator();
        }
        invalidate();
    }

    private void startAnimator() {
        if (animator != null && animator.isStarted()) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16000L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                phase += playing ? 0.075f : 0.022f;
                if (uiStyle == STYLE_RADAR) {
                    radarSweepAngle = (radarSweepAngle + (playing ? RADAR_SWEEP_PLAYING_STEP : RADAR_SWEEP_IDLE_STEP)) % 360f;
                }
                postInvalidateOnAnimation();
            }
        });
        animator.start();
    }

    private void stopAnimator() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (uiStyle == STYLE_LIQUID) {
            drawLiquid(canvas);
        } else if (uiStyle == STYLE_GALAXY) {
            drawGalaxy(canvas);
        } else if (uiStyle == STYLE_RADAR) {
            drawRadar(canvas);
        }
    }

    private void drawLiquid(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float size = safeVisualSize(width, height);
        float beat = playing ? wave(phase * 2.4f) : wave(phase * 0.7f);
        float radius = size * (0.43f + (playing ? 0.022f * beat : 0.006f * beat));
        float drift = playing ? phase * 0.09f : phase * 0.018f;

        liquidPath.reset();
        liquidPath.addCircle(centerX, centerY, radius, Path.Direction.CW);

        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setMaskFilter(new BlurMaskFilter(radius * 0.09f, BlurMaskFilter.Blur.NORMAL));
        glowPaint.setColor(Color.argb(135 + (int) (46f * beat), 200, 24, 255));
        canvas.drawCircle(centerX, centerY, radius * 1.02f, glowPaint);
        glowPaint.setColor(Color.argb(70 + (int) (40f * beat), 72, 170, 255));
        canvas.drawCircle(centerX - radius * 0.04f, centerY + radius * 0.02f, radius * 0.96f, glowPaint);
        glowPaint.setMaskFilter(null);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(
            centerX - radius * 0.42f,
            centerY + radius * 0.1f,
            radius * 1.38f,
            new int[] {
                Color.argb(255, 245, 252, 238),
                Color.argb(244, 160, 226, 244),
                Color.argb(232, 218, 38, 232),
                Color.argb(224, 88, 12, 116),
                Color.argb(236, 10, 4, 20)
            },
            new float[] {0f, 0.22f, 0.48f, 0.73f, 1f},
            Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setShader(null);

        canvas.save();
        canvas.clipPath(liquidPath);

        paint.setShader(new LinearGradient(
            centerX - radius,
            centerY - radius * 0.76f + (float) Math.sin(drift) * radius * 0.24f,
            centerX + radius,
            centerY + radius * 0.86f + (float) Math.cos(drift * 0.8f) * radius * 0.2f,
            new int[] {
                Color.argb(22, 0, 0, 0),
                Color.argb(120, 120, 55, 255),
                Color.argb(168, 255, 35, 210),
                Color.argb(180, 226, 246, 232),
                Color.argb(126, 255, 171, 62),
                Color.argb(64, 70, 8, 32)
            },
            new float[] {0f, 0.18f, 0.35f, 0.56f, 0.76f, 1f},
            Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX, centerY, radius * 1.02f, paint);
        paint.setShader(null);

        paint.setShader(new RadialGradient(
            centerX + (float) Math.sin(drift * 1.7f) * radius * 0.38f,
            centerY - radius * 0.04f + (float) Math.cos(drift * 1.2f) * radius * 0.24f,
            radius * 0.75f,
            Color.argb(100 + (int) (44f * beat), 255, 0, 226),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX, centerY, radius * 0.95f, paint);
        paint.setShader(null);

        paint.setShader(new RadialGradient(
            centerX + (float) Math.cos(drift * 1.35f) * radius * 0.32f,
            centerY + radius * 0.26f + (float) Math.sin(drift * 1.05f) * radius * 0.2f,
            radius * 0.58f,
            Color.argb(96 + (int) (54f * beat), 255, 172, 58),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX, centerY, radius * 0.92f, paint);
        paint.setShader(null);

        paint.setShader(new RadialGradient(
            centerX - radius * 0.52f,
            centerY + radius * 0.08f,
            radius * 0.56f,
            Color.argb(210, 244, 255, 236),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX - radius * 0.28f, centerY + radius * 0.08f, radius * 0.78f, paint);
        paint.setShader(null);

        canvas.restore();

        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setShader(new RadialGradient(
            centerX - radius * 0.42f,
            centerY - radius * 0.32f,
            radius * 0.56f,
            Color.argb(118, 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        ));
        scratchOval.set(
            centerX - radius * 0.72f,
            centerY - radius * 0.68f,
            centerX + radius * 0.08f,
            centerY + radius * 0.12f
        );
        canvas.drawOval(scratchOval, glowPaint);
        glowPaint.setShader(null);
    }

    private void drawGalaxy(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float size = safeVisualSize(width, height);
        float radius = size * 0.49f;
        float beat = playing ? wave(phase * 2.25f) : wave(phase * 0.55f);
        float rotation = phase * (playing ? 0.042f : 0.01f);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(
            centerX,
            centerY,
            radius * 1.08f,
            new int[] {
                Color.argb(24 + (int) (22f * beat), 245, 247, 250),
                Color.argb(28, 132, 136, 146),
                Color.argb(12, 42, 44, 50),
                Color.TRANSPARENT
            },
            new float[] {0f, 0.22f, 0.64f, 1f},
            Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX, centerY, radius * 0.78f, paint);
        paint.setShader(null);

        drawRippleDotField(canvas, centerX, centerY, radius, rotation, beat);
    }

    private void drawRippleDotField(Canvas canvas, float centerX, float centerY, float radius, float rotation, float beat) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setMaskFilter(null);

        float dotGap = radius * 0.039f;
        float waveSpeed = playing ? phase * 0.027f : phase * 0.008f;
        float maxGrid = radius * 1.02f;

        for (float y = -maxGrid; y <= maxGrid; y += dotGap) {
            for (float x = -maxGrid; x <= maxGrid; x += dotGap) {
                float distance = (float) Math.sqrt(x * x + y * y);
                if (distance > radius || distance < radius * 0.015f) continue;

                float strength = rippleExpansionStrength(distance, radius, waveSpeed);
                if (strength <= 0.01f) continue;

                float angle = (float) Math.atan2(y, x);
                float traveling = wave(angle * 2.35f - rotation * 14f + distance / radius * 5.8f);
                float shimmer = wave(phase * (playing ? 1.8f : 0.45f) + x * 0.013f + y * 0.017f);
                float glow = 0.42f + 0.58f * traveling;
                float alphaUnit = strength * (0.28f + 0.46f * glow + 0.26f * shimmer);
                alphaUnit *= playing ? (0.84f + 0.34f * beat) : 0.78f;
                int alpha = Math.min(245, Math.max(16, (int) (205f * alphaUnit)));
                float dotRadius = radius * (0.0065f + 0.0038f * strength + (playing ? 0.002f * beat * glow : 0f));

                paint.setColor(Color.argb(alpha, 232, 235, 238));
                canvas.drawCircle(centerX + x, centerY + y, dotRadius, paint);

                if (alpha > 155) {
                    paint.setColor(Color.argb((int) (alpha * 0.22f), 255, 255, 255));
                    canvas.drawCircle(centerX + x, centerY + y, dotRadius * 1.65f, paint);
                }
            }
        }
    }

    private float rippleExpansionStrength(float distance, float radius, float waveOffset) {
        float best = 0f;
        float inner = radius * 0.035f;
        float span = radius * 0.925f;
        float thickness = radius * (playing ? 0.13f : 0.105f);
        for (int i = 0; i < 3; i++) {
            float progress = fract(waveOffset + i / 3f);
            float bandRadius = inner + span * progress;
            float band = rippleBandStrength(distance, bandRadius, thickness);
            float edgeFade = (float) Math.sin(progress * Math.PI);
            float centerFade = Math.min(1f, progress / 0.12f);
            float strength = band * edgeFade * centerFade;
            if (strength > best) best = strength;
        }
        return best;
    }

    private float rippleBandStrength(float distance, float bandRadius, float thickness) {
        float unit = Math.abs(distance - bandRadius) / thickness;
        if (unit >= 1f) return 0f;
        return 1f - unit * unit * (3f - 2f * unit);
    }

    private float fract(float value) {
        return value - (float) Math.floor(value);
    }

    private void drawRadar(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float size = safeVisualSize(width, height);
        float radius = size * 0.485f;
        float sweepAngle = radarSweepAngle;

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(
            centerX,
            centerY,
            radius * 1.12f,
            new int[] {
                Color.argb(92, 91, 255, 180),
                Color.argb(58, 18, 111, 76),
                Color.argb(18, 0, 32, 26),
                Color.TRANSPARENT
            },
            new float[] {0f, 0.36f, 0.72f, 1f},
            Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX, centerY, radius * 1.02f, paint);
        paint.setShader(null);

        drawRadarInnerRings(canvas, centerX, centerY, radius);
        drawRadarGrid(canvas, centerX, centerY, radius);
        drawRadarSweep(canvas, centerX, centerY, radius, sweepAngle);
        drawRadarTargets(canvas, centerX, centerY, radius, sweepAngle);
        drawRadarCenter(canvas, centerX, centerY, radius);
    }

    private void drawRadarInnerRings(Canvas canvas, float centerX, float centerY, float radius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setShader(null);
        paint.setStrokeCap(Paint.Cap.BUTT);
        for (int ring = 1; ring <= 4; ring++) {
            float unit = ring / 4f;
            int alpha = ring == 4 ? 120 : 58;
            paint.setColor(Color.argb(alpha, 80, 255, 184));
            paint.setStrokeWidth(radius * (ring == 4 ? 0.0032f : 0.0022f));
            canvas.drawCircle(centerX, centerY, radius * unit, paint);
        }
    }

    private void drawRadarGrid(Canvas canvas, float centerX, float centerY, float radius) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(radius * 0.0035f);
        paint.setColor(Color.argb(150, 91, 255, 187));
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, paint);
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, paint);

        for (int i = 0; i < 72; i++) {
            float angle = (float) Math.toRadians(i * 5f);
            float outer = radius;
            float inner = radius * (i % 6 == 0 ? 0.9f : 0.955f);
            paint.setStrokeWidth(i % 6 == 0 ? radius * 0.0048f : radius * 0.0025f);
            paint.setColor(Color.argb(i % 6 == 0 ? 190 : 115, 113, 255, 196));
            canvas.drawLine(
                centerX + (float) Math.cos(angle) * inner,
                centerY + (float) Math.sin(angle) * inner,
                centerX + (float) Math.cos(angle) * outer,
                centerY + (float) Math.sin(angle) * outer,
                paint
            );
        }
    }

    private void drawRadarSweep(Canvas canvas, float centerX, float centerY, float radius, float sweepAngle) {
        scratchOval.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        for (int i = 0; i < 9; i++) {
            float start = sweepAngle - 42f + i * 4.7f;
            int alpha = 12 + i * 13;
            radarPath.reset();
            radarPath.moveTo(centerX, centerY);
            radarPath.arcTo(scratchOval, start, 4.9f);
            radarPath.close();
            paint.setColor(Color.argb(alpha, 91, 255, 187));
            canvas.drawPath(radarPath, paint);
        }

        float angle = (float) Math.toRadians(sweepAngle);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(radius * 0.012f);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setColor(Color.argb(210, 110, 255, 196));
        glowPaint.setMaskFilter(new BlurMaskFilter(radius * 0.018f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawLine(
            centerX,
            centerY,
            centerX + (float) Math.cos(angle) * radius,
            centerY + (float) Math.sin(angle) * radius,
            glowPaint
        );
        glowPaint.setMaskFilter(null);
    }

    private void drawRadarTargets(Canvas canvas, float centerX, float centerY, float radius, float sweepAngle) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < radarTargets.length; i++) {
            GalaxyParticle target = radarTargets[i];
            float drift = phase * 0.012f * target.spread;
            float angle = target.angle + drift;
            float x = centerX + (float) Math.cos(angle) * radius * target.radius;
            float y = centerY + (float) Math.sin(angle) * radius * target.radius;
            float targetDegrees = (float) Math.toDegrees(angle);
            float delta = Math.abs(normalizedDegrees(sweepAngle - targetDegrees));
            float sweepBoost = delta < 28f ? 1f + (28f - delta) / 28f * 1.7f : 1f;
            float blink = 0.56f + 0.44f * wave(phase * 1.9f + target.twinkle);
            int alpha = Math.min(255, (int) (180f * target.alpha * blink * sweepBoost));
            if (target.depth < 0.18f) {
                paint.setColor(Color.argb(alpha, 255, 92, 66));
            } else if (target.depth < 0.46f) {
                paint.setColor(Color.argb(alpha, 255, 224, 66));
            } else {
                paint.setColor(Color.argb(alpha, 230, 255, 244));
            }
            drawRadarNote(canvas, x, y, target.arm, target.size * sweepBoost, paint, alpha);
        }
    }

    private void drawRadarNote(Canvas canvas, float x, float y, int noteIndex, float size, Paint sourcePaint, int alpha) {
        sourcePaint.setStyle(Paint.Style.FILL);
        sourcePaint.setTextAlign(Paint.Align.CENTER);
        sourcePaint.setFakeBoldText(true);
        sourcePaint.setTextSize(size * 6.2f);
        sourcePaint.setShadowLayer(size * 1.25f, 0f, 0f, Color.argb(Math.min(220, alpha), 91, 255, 187));
        Paint.FontMetrics metrics = sourcePaint.getFontMetrics();
        float baseline = y - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(RADAR_NOTE_SYMBOLS[noteIndex % RADAR_NOTE_SYMBOLS.length], x, baseline, sourcePaint);
        sourcePaint.clearShadowLayer();
        sourcePaint.setFakeBoldText(false);
    }

    private void drawRadarCenter(Canvas canvas, float centerX, float centerY, float radius) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.01f);
        paint.setColor(Color.argb(230, 110, 255, 196));
        canvas.drawCircle(centerX, centerY, radius * 0.13f, paint);
        paint.setStrokeWidth(radius * 0.004f);
        canvas.drawCircle(centerX, centerY, radius * 0.055f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(radius * 0.05f);
        paint.setColor(Color.argb(175, 190, 255, 220));
        canvas.drawText("RADAR", centerX, centerY - radius * 0.18f, paint);
        paint.setTextSize(radius * 0.035f);
        paint.setColor(Color.argb(130, 202, 255, 85));
        canvas.drawText("AUDIO SCAN", centerX, centerY + radius * 0.2f, paint);
    }

    private float normalizedDegrees(float value) {
        float result = value % 360f;
        if (result > 180f) result -= 360f;
        if (result < -180f) result += 360f;
        return result;
    }

    private float safeVisualSize(float width, float height) {
        return Math.min(width, height) * 0.78f;
    }

    private float wave(float value) {
        return (float) ((Math.sin(value) + 1d) / 2d);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimator();
        super.onDetachedFromWindow();
    }

    private static class GalaxyParticle {
        int arm;
        float angle;
        float radius;
        float size;
        float alpha;
        float spread;
        float twinkle;
        float depth;
    }
}
