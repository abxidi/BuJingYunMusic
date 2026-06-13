package com.novapulse.mp3;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Random;

public class ThemeVisualizerView extends View {
    public static final int STYLE_CLASSIC = 0;
    public static final int STYLE_LIQUID = 1;
    public static final int STYLE_GALAXY = 2;
    public static final int STYLE_RADAR = 3;

    private static final int PARTICLE_COUNT = 520;
    private static final int DUST_COUNT = 160;
    private static final int RADAR_TARGET_COUNT = 20;
    private static final float RADAR_SWEEP_PLAYING_STEP = 0.44f;
    private static final float RADAR_SWEEP_IDLE_STEP = 0.055f;
    private static final String[] RADAR_NOTE_SYMBOLS = {"♪", "♫", "♬", "♩", "♭"};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path liquidPath = new Path();
    private final Path spiralPath = new Path();
    private final Path dustPath = new Path();
    private final Path radarPath = new Path();
    private final GalaxyParticle[] particles = new GalaxyParticle[PARTICLE_COUNT];
    private final GalaxyParticle[] dust = new GalaxyParticle[DUST_COUNT];
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
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            GalaxyParticle particle = new GalaxyParticle();
            particle.arm = i % 4;
            particle.angle = random.nextFloat() * (float) Math.PI * 2f;
            particle.radius = 0.05f + random.nextFloat() * 0.96f;
            particle.size = 0.55f + random.nextFloat() * 2.9f;
            particle.alpha = 0.28f + random.nextFloat() * 0.72f;
            particle.spread = (random.nextFloat() - 0.5f) * 0.68f;
            particle.twinkle = random.nextFloat() * (float) Math.PI * 2f;
            particle.depth = random.nextFloat();
            particles[i] = particle;
        }
        for (int i = 0; i < DUST_COUNT; i++) {
            GalaxyParticle mote = new GalaxyParticle();
            mote.arm = i % 4;
            mote.angle = random.nextFloat() * (float) Math.PI * 2f;
            mote.radius = 0.16f + random.nextFloat() * 0.78f;
            mote.size = 1.2f + random.nextFloat() * 4.2f;
            mote.alpha = 0.22f + random.nextFloat() * 0.46f;
            mote.spread = (random.nextFloat() - 0.5f) * 0.45f;
            mote.twinkle = random.nextFloat() * (float) Math.PI * 2f;
            mote.depth = random.nextFloat();
            dust[i] = mote;
        }
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
        float size = Math.min(width, height);
        float beat = playing ? 0.012f * wave(phase * 2.2f) : 0.006f * wave(phase);
        float baseRadius = size * (0.425f + beat);

        liquidPath.reset();
        int points = 112;
        for (int i = 0; i <= points; i++) {
            float unit = i / (float) points;
            float angle = unit * (float) Math.PI * 2f;
            float ripple = 1f
                + 0.052f * (float) Math.sin(angle * 3f + phase * 1.3f)
                + 0.035f * (float) Math.sin(angle * 7f - phase * 0.85f)
                + (playing ? 0.025f * (float) Math.sin(angle * 11f + phase * 2.6f) : 0f);
            float radius = baseRadius * ripple;
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;
            if (i == 0) {
                liquidPath.moveTo(x, y);
            } else {
                liquidPath.lineTo(x, y);
            }
        }
        liquidPath.close();

        int pink = Color.rgb(255, 93, 159);
        int gold = Color.rgb(255, 211, 119);
        int mint = Color.rgb(109, 255, 191);
        int cyan = Color.rgb(88, 222, 255);
        int violet = Color.rgb(178, 118, 255);
        paint.setShader(new SweepGradient(centerX, centerY, new int[] {
            gold, pink, violet, cyan, mint, gold
        }, null));
        paint.setAlpha(245);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(liquidPath, paint);
        paint.setAlpha(255);
        paint.setShader(null);

        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setShader(new RadialGradient(
            centerX - baseRadius * 0.28f,
            centerY - baseRadius * 0.38f,
            baseRadius * 0.72f,
            Color.argb(150, 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        ));
        scratchOval.set(
            centerX - baseRadius * 0.68f,
            centerY - baseRadius * 0.72f,
            centerX + baseRadius * 0.18f,
            centerY + baseRadius * 0.12f
        );
        canvas.drawOval(scratchOval, glowPaint);
        glowPaint.setShader(null);
    }

    private void drawGalaxy(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float size = Math.min(width, height);
        float maxRadius = size * 0.47f;
        float pulse = playing ? 1f + 0.11f * wave(phase * 1.75f) : 1f;
        float rotation = phase * (playing ? 0.055f : 0.025f);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(
            centerX,
            centerY,
            maxRadius * 1.08f,
            new int[] {
                Color.argb(245, 255, 252, 236),
                Color.argb(190, 140, 170, 255),
                Color.argb(105, 80, 232, 255),
                Color.argb(42, 34, 38, 92),
                Color.TRANSPARENT
            },
            new float[] {0f, 0.14f, 0.42f, 0.76f, 1f},
            Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX, centerY, maxRadius * 0.94f * pulse, paint);
        paint.setShader(null);

        drawBackgroundStars(canvas, centerX, centerY, maxRadius, pulse);

        canvas.save();
        canvas.rotate(-18f, centerX, centerY);
        canvas.scale(1f, 0.58f, centerX, centerY);
        drawSpiralGlow(canvas, centerX, centerY, maxRadius, rotation, pulse);
        drawDustLanes(canvas, centerX, centerY, maxRadius, rotation);

        for (int i = 0; i < particles.length; i++) {
            GalaxyParticle particle = particles[i];
            float armOffset = particle.arm * ((float) Math.PI / 2f);
            float radius = particle.radius * maxRadius * pulse;
            float angle = armOffset + rotation + particle.radius * 5.35f + particle.spread;
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;
            float twinkle = 0.62f + 0.38f * wave(phase * 1.7f + particle.twinkle);
            int alpha = Math.min(255, Math.max(18, (int) (255f * particle.alpha * twinkle)));
            if (particle.depth < 0.34f) {
                paint.setColor(Color.argb(alpha, 112, 232, 255));
            } else if (particle.depth < 0.68f) {
                paint.setColor(Color.argb(alpha, 200, 126, 255));
            } else {
                paint.setColor(Color.argb(alpha, 255, 246, 220));
            }
            canvas.drawCircle(x, y, particle.size * (playing ? 1.25f : 1f), paint);
        }
        drawDustParticles(canvas, centerX, centerY, maxRadius, rotation);
        canvas.restore();

        drawCoreBurst(canvas, centerX, centerY, maxRadius, pulse);
    }

    private void drawBackgroundStars(Canvas canvas, float centerX, float centerY, float maxRadius, float pulse) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 72; i++) {
            float seed = i * 19.733f;
            float angle = seed;
            float radius = maxRadius * (0.48f + ((i * 37) % 100) / 100f * 0.62f);
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle * 1.37f) * radius;
            float twinkle = wave(phase * 0.9f + i * 0.31f);
            int alpha = (int) (28 + 72 * twinkle);
            paint.setColor(Color.argb(alpha, 210, 232, 255));
            canvas.drawCircle(x, y, (0.45f + (i % 5) * 0.18f) * pulse, paint);
        }
    }

    private void drawSpiralGlow(Canvas canvas, float centerX, float centerY, float maxRadius, float rotation, float pulse) {
        int[] colors = {
            Color.argb(150, 92, 232, 255),
            Color.argb(130, 180, 120, 255),
            Color.argb(120, 255, 209, 126),
            Color.argb(115, 255, 94, 178)
        };
        for (int arm = 0; arm < 4; arm++) {
            buildSpiralPath(spiralPath, centerX, centerY, maxRadius * pulse, arm, rotation, 0f);
            glowPaint.setShader(null);
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeCap(Paint.Cap.ROUND);
            glowPaint.setStrokeJoin(Paint.Join.ROUND);
            glowPaint.setColor(colors[arm]);
            glowPaint.setStrokeWidth(maxRadius * 0.18f);
            glowPaint.setMaskFilter(new BlurMaskFilter(maxRadius * 0.08f, BlurMaskFilter.Blur.NORMAL));
            canvas.drawPath(spiralPath, glowPaint);

            glowPaint.setMaskFilter(null);
            glowPaint.setStrokeWidth(maxRadius * 0.055f);
            glowPaint.setColor(withAlpha(colors[arm], 165));
            canvas.drawPath(spiralPath, glowPaint);
        }
        glowPaint.setMaskFilter(null);
    }

    private void drawDustLanes(Canvas canvas, float centerX, float centerY, float maxRadius, float rotation) {
        glowPaint.setShader(null);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeJoin(Paint.Join.ROUND);
        glowPaint.setColor(Color.argb(105, 4, 5, 14));
        glowPaint.setStrokeWidth(maxRadius * 0.046f);
        glowPaint.setMaskFilter(new BlurMaskFilter(maxRadius * 0.028f, BlurMaskFilter.Blur.NORMAL));
        for (int arm = 0; arm < 4; arm++) {
            buildSpiralPath(dustPath, centerX, centerY, maxRadius * 0.95f, arm, rotation, 0.22f);
            canvas.drawPath(dustPath, glowPaint);
        }
        glowPaint.setMaskFilter(null);
    }

    private void drawDustParticles(Canvas canvas, float centerX, float centerY, float maxRadius, float rotation) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < dust.length; i++) {
            GalaxyParticle mote = dust[i];
            float armOffset = mote.arm * ((float) Math.PI / 2f);
            float radius = mote.radius * maxRadius;
            float angle = armOffset + rotation + mote.radius * 5.15f + mote.spread + 0.16f;
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;
            int alpha = Math.min(145, Math.max(28, (int) (255f * mote.alpha)));
            paint.setColor(Color.argb(alpha, 5, 7, 18));
            canvas.drawCircle(x, y, mote.size, paint);
        }
    }

    private void drawCoreBurst(Canvas canvas, float centerX, float centerY, float maxRadius, float pulse) {
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setMaskFilter(new BlurMaskFilter(maxRadius * 0.055f, BlurMaskFilter.Blur.NORMAL));
        glowPaint.setColor(Color.argb(185, 255, 246, 210));
        canvas.drawCircle(centerX, centerY, maxRadius * 0.16f * pulse, glowPaint);
        glowPaint.setMaskFilter(null);

        paint.setShader(new RadialGradient(
            centerX,
            centerY,
            maxRadius * 0.32f,
            new int[] {
                Color.argb(255, 255, 255, 244),
                Color.argb(210, 255, 215, 142),
                Color.argb(90, 124, 232, 255),
                Color.TRANSPARENT
            },
            new float[] {0f, 0.18f, 0.56f, 1f},
            Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX, centerY, maxRadius * 0.34f * pulse, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(maxRadius * 0.012f);
        paint.setColor(Color.argb(150, 255, 255, 255));
        float ray = maxRadius * (playing ? 0.64f : 0.48f);
        for (int i = 0; i < 8; i++) {
            float angle = phase * 0.08f + i * ((float) Math.PI / 4f);
            canvas.drawLine(
                centerX + (float) Math.cos(angle) * maxRadius * 0.08f,
                centerY + (float) Math.sin(angle) * maxRadius * 0.08f,
                centerX + (float) Math.cos(angle) * ray,
                centerY + (float) Math.sin(angle) * ray,
                paint
            );
        }
    }

    private void buildSpiralPath(Path path, float centerX, float centerY, float maxRadius, int arm, float rotation, float offset) {
        path.reset();
        float armOffset = arm * ((float) Math.PI / 2f) + offset;
        int steps = 86;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float eased = t * t * (3f - 2f * t);
            float radius = maxRadius * (0.08f + eased * 0.92f);
            float angle = armOffset + rotation + eased * 5.35f;
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void drawRadar(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float size = Math.min(width, height);
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
            int alpha = ring == 4 ? 145 : 58;
            paint.setColor(Color.argb(alpha, 80, 255, 184));
            paint.setStrokeWidth(radius * (ring == 4 ? 0.004f : 0.0022f));
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

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(radius * 0.0035f);
            paint.setColor(Color.argb(Math.min(170, alpha), 91, 255, 187));
            canvas.drawCircle(x, y, target.size * (2.2f + sweepBoost), paint);
            paint.setStyle(Paint.Style.FILL);
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
