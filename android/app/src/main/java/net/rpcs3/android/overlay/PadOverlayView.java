package net.rpcs3.android.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import net.rpcs3.android.RPCS3;

import java.util.HashMap;
import java.util.Map;

public class PadOverlayView extends View {
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mDigital1 = 0;
    private int mDigital2 = 0;
    private int mLeftStickX = 128;
    private int mLeftStickY = 128;
    private int mRightStickX = 128;
    private int mRightStickY = 128;
    private int mL2Axis = 0;
    private int mR2Axis = 0;

    private float mAlpha = 0.7f;
    private boolean mHapticEnabled = true;
    private Vibrator mVibrator;

    private final Map<Integer, Integer> mPointerToControl = new HashMap<>();

    // Control IDs
    private static final int CTRL_NONE = 0;
    private static final int CTRL_L_STICK = 1;
    private static final int CTRL_R_STICK = 2;
    private static final int CTRL_DPAD_UP = 3;
    private static final int CTRL_DPAD_DOWN = 4;
    private static final int CTRL_DPAD_LEFT = 5;
    private static final int CTRL_DPAD_RIGHT = 6;
    private static final int CTRL_TRIANGLE = 7;
    private static final int CTRL_CIRCLE = 8;
    private static final int CTRL_CROSS = 9;
    private static final int CTRL_SQUARE = 10;
    private static final int CTRL_L1 = 11;
    private static final int CTRL_L2 = 12;
    private static final int CTRL_R1 = 13;
    private static final int CTRL_R2 = 14;
    private static final int CTRL_START = 15;
    private static final int CTRL_SELECT = 16;
    private static final int CTRL_PS = 17;
    private static final int CTRL_L3 = 18;
    private static final int CTRL_R3 = 19;

    // Control bounds
    private float mLeftStickCenterX, mLeftStickCenterY, mStickRadius;
    private float mRightStickCenterX, mRightStickCenterY;
    private float mLeftStickKnobX, mLeftStickKnobY;
    private float mRightStickKnobX, mRightStickKnobY;

    private final RectF mDpadUpRect = new RectF();
    private final RectF mDpadDownRect = new RectF();
    private final RectF mDpadLeftRect = new RectF();
    private final RectF mDpadRightRect = new RectF();

    private final RectF mTriangleRect = new RectF();
    private final RectF mCircleRect = new RectF();
    private final RectF mCrossRect = new RectF();
    private final RectF mSquareRect = new RectF();

    private final RectF mL1Rect = new RectF();
    private final RectF mL2Rect = new RectF();
    private final RectF mR1Rect = new RectF();
    private final RectF mR2Rect = new RectF();

    private final RectF mSelectRect = new RectF();
    private final RectF mStartRect = new RectF();
    private final RectF mPsRect = new RectF();
    private final RectF mL3Rect = new RectF();
    private final RectF mR3Rect = new RectF();

    public PadOverlayView(Context context) {
        super(context);
        init(context);
    }

    public PadOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);

        mActivePaint.setStyle(Paint.Style.FILL);
        mActivePaint.setColor(Color.parseColor("#4080FF"));

        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setFakeBoldText(true);

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void setOverlayAlpha(float alpha) {
        mAlpha = Math.max(0.1f, Math.min(1.0f, alpha));
        invalidate();
    }

    public void setHapticEnabled(boolean enabled) {
        mHapticEnabled = enabled;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutControls(w, h);
    }

    private void layoutControls(int w, int h) {
        float scale = Math.min(w, h) / 720.0f;
        float btnSize = 56 * scale;
        float padMargin = 24 * scale;

        mStickRadius = 70 * scale;

        // Left D-Pad & Left Stick
        float dpadCenterX = padMargin + 100 * scale;
        float dpadCenterY = h * 0.40f;

        mDpadUpRect.set(dpadCenterX - btnSize / 2, dpadCenterY - btnSize * 1.5f, dpadCenterX + btnSize / 2, dpadCenterY - btnSize * 0.5f);
        mDpadDownRect.set(dpadCenterX - btnSize / 2, dpadCenterY + btnSize * 0.5f, dpadCenterX + btnSize / 2, dpadCenterY + btnSize * 1.5f);
        mDpadLeftRect.set(dpadCenterX - btnSize * 1.5f, dpadCenterY - btnSize / 2, dpadCenterX - btnSize * 0.5f, dpadCenterY + btnSize / 2);
        mDpadRightRect.set(dpadCenterX + btnSize * 0.5f, dpadCenterY - btnSize / 2, dpadCenterX + btnSize * 1.5f, dpadCenterY + btnSize / 2);

        mLeftStickCenterX = padMargin + 110 * scale;
        mLeftStickCenterY = h - padMargin - mStickRadius - 30 * scale;
        mLeftStickKnobX = mLeftStickCenterX;
        mLeftStickKnobY = mLeftStickCenterY;

        // Right Action Buttons & Right Stick
        float faceCenterX = w - padMargin - 100 * scale;
        float faceCenterY = h * 0.40f;

        mTriangleRect.set(faceCenterX - btnSize / 2, faceCenterY - btnSize * 1.5f, faceCenterX + btnSize / 2, faceCenterY - btnSize * 0.5f);
        mCrossRect.set(faceCenterX - btnSize / 2, faceCenterY + btnSize * 0.5f, faceCenterX + btnSize / 2, faceCenterY + btnSize * 1.5f);
        mSquareRect.set(faceCenterX - btnSize * 1.5f, faceCenterY - btnSize / 2, faceCenterX - btnSize * 0.5f, faceCenterY + btnSize / 2);
        mCircleRect.set(faceCenterX + btnSize * 0.5f, faceCenterY - btnSize / 2, faceCenterX + btnSize * 1.5f, faceCenterY + btnSize / 2);

        mRightStickCenterX = w - padMargin - 110 * scale;
        mRightStickCenterY = h - padMargin - mStickRadius - 30 * scale;
        mRightStickKnobX = mRightStickCenterX;
        mRightStickKnobY = mRightStickCenterY;

        // Shoulders (L1, L2, R1, R2)
        float shWidth = 80 * scale;
        float shHeight = 44 * scale;
        mL2Rect.set(padMargin, padMargin + 10 * scale, padMargin + shWidth, padMargin + 10 * scale + shHeight);
        mL1Rect.set(padMargin + shWidth + 12 * scale, padMargin + 10 * scale, padMargin + shWidth * 2 + 12 * scale, padMargin + 10 * scale + shHeight);

        mR1Rect.set(w - padMargin - shWidth * 2 - 12 * scale, padMargin + 10 * scale, w - padMargin - shWidth - 12 * scale, padMargin + 10 * scale + shHeight);
        mR2Rect.set(w - padMargin - shWidth, padMargin + 10 * scale, w - padMargin, padMargin + 10 * scale + shHeight);

        // Center buttons (Select, PS, Start, L3, R3)
        float sysWidth = 60 * scale;
        float sysHeight = 36 * scale;
        float centerX = w / 2.0f;
        float bottomY = h - padMargin - sysHeight;

        mSelectRect.set(centerX - sysWidth * 1.8f, bottomY, centerX - sysWidth * 0.8f, bottomY + sysHeight);
        mPsRect.set(centerX - sysWidth * 0.5f, bottomY - 10 * scale, centerX + sysWidth * 0.5f, bottomY + sysHeight - 10 * scale);
        mStartRect.set(centerX + sysWidth * 0.8f, bottomY, centerX + sysWidth * 1.8f, bottomY + sysHeight);

        mL3Rect.set(mLeftStickCenterX - 24 * scale, mLeftStickCenterY - mStickRadius - 35 * scale, mLeftStickCenterX + 24 * scale, mLeftStickCenterY - mStickRadius - 5 * scale);
        mR3Rect.set(mRightStickCenterX - 24 * scale, mRightStickCenterY - mStickRadius - 35 * scale, mRightStickCenterX + 24 * scale, mRightStickCenterY - mStickRadius - 5 * scale);

        mTextPaint.setTextSize(20 * scale);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int baseAlpha = (int) (mAlpha * 120);
        int knobAlpha = (int) (mAlpha * 200);

        // Left Analog Stick Base
        mPaint.setColor(Color.BLACK);
        mPaint.setAlpha(baseAlpha / 2);
        canvas.drawCircle(mLeftStickCenterX, mLeftStickCenterY, mStickRadius, mPaint);
        mPaint.setColor(Color.WHITE);
        mPaint.setAlpha(baseAlpha);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3);
        canvas.drawCircle(mLeftStickCenterX, mLeftStickCenterY, mStickRadius, mPaint);
        mPaint.setStyle(Paint.Style.FILL);

        // Left Stick Knob
        mPaint.setColor((mDigital1 & RPCS3.CELL_PAD_CTRL_L3) != 0 ? Color.CYAN : Color.WHITE);
        mPaint.setAlpha(knobAlpha);
        canvas.drawCircle(mLeftStickKnobX, mLeftStickKnobY, mStickRadius * 0.45f, mPaint);

        // Right Analog Stick Base
        mPaint.setColor(Color.BLACK);
        mPaint.setAlpha(baseAlpha / 2);
        canvas.drawCircle(mRightStickCenterX, mRightStickCenterY, mStickRadius, mPaint);
        mPaint.setColor(Color.WHITE);
        mPaint.setAlpha(baseAlpha);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(mRightStickCenterX, mRightStickCenterY, mStickRadius, mPaint);
        mPaint.setStyle(Paint.Style.FILL);

        // Right Stick Knob
        mPaint.setColor((mDigital1 & RPCS3.CELL_PAD_CTRL_R3) != 0 ? Color.CYAN : Color.WHITE);
        mPaint.setAlpha(knobAlpha);
        canvas.drawCircle(mRightStickKnobX, mRightStickKnobY, mStickRadius * 0.45f, mPaint);

        // D-Pad
        drawButton(canvas, mDpadUpRect, "▲", (mDigital1 & RPCS3.CELL_PAD_CTRL_UP) != 0);
        drawButton(canvas, mDpadDownRect, "▼", (mDigital1 & RPCS3.CELL_PAD_CTRL_DOWN) != 0);
        drawButton(canvas, mDpadLeftRect, "◀", (mDigital1 & RPCS3.CELL_PAD_CTRL_LEFT) != 0);
        drawButton(canvas, mDpadRightRect, "▶", (mDigital1 & RPCS3.CELL_PAD_CTRL_RIGHT) != 0);

        // Face Buttons
        drawButton(canvas, mTriangleRect, "△", (mDigital2 & RPCS3.CELL_PAD_CTRL_TRIANGLE) != 0, Color.parseColor("#00E676"));
        drawButton(canvas, mCircleRect, "○", (mDigital2 & RPCS3.CELL_PAD_CTRL_CIRCLE) != 0, Color.parseColor("#FF5252"));
        drawButton(canvas, mCrossRect, "✕", (mDigital2 & RPCS3.CELL_PAD_CTRL_CROSS) != 0, Color.parseColor("#448AFF"));
        drawButton(canvas, mSquareRect, "□", (mDigital2 & RPCS3.CELL_PAD_CTRL_SQUARE) != 0, Color.parseColor("#FF4081"));

        // Shoulders
        drawButton(canvas, mL1Rect, "L1", (mDigital2 & RPCS3.CELL_PAD_CTRL_L1) != 0);
        drawButton(canvas, mL2Rect, "L2", (mDigital2 & RPCS3.CELL_PAD_CTRL_L2) != 0);
        drawButton(canvas, mR1Rect, "R1", (mDigital2 & RPCS3.CELL_PAD_CTRL_R1) != 0);
        drawButton(canvas, mR2Rect, "R2", (mDigital2 & RPCS3.CELL_PAD_CTRL_R2) != 0);

        // System
        drawButton(canvas, mSelectRect, "SELECT", (mDigital1 & RPCS3.CELL_PAD_CTRL_SELECT) != 0);
        drawButton(canvas, mPsRect, "PS", (mDigital1 & RPCS3.CELL_PAD_CTRL_PS) != 0, Color.parseColor("#3D5AFE"));
        drawButton(canvas, mStartRect, "START", (mDigital1 & RPCS3.CELL_PAD_CTRL_START) != 0);

        drawButton(canvas, mL3Rect, "L3", (mDigital1 & RPCS3.CELL_PAD_CTRL_L3) != 0);
        drawButton(canvas, mR3Rect, "R3", (mDigital1 & RPCS3.CELL_PAD_CTRL_R3) != 0);
    }

    private void drawButton(Canvas canvas, RectF rect, String label, boolean pressed) {
        drawButton(canvas, rect, label, pressed, Color.WHITE);
    }

    private void drawButton(Canvas canvas, RectF rect, String label, boolean pressed, int textColor) {
        int alpha = (int) (mAlpha * (pressed ? 230 : 120));
        mPaint.setColor(pressed ? Color.parseColor("#4080FF") : Color.BLACK);
        mPaint.setAlpha(alpha);
        canvas.drawRoundRect(rect, 14, 14, mPaint);

        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAlpha((int) (mAlpha * 180));
        mPaint.setStrokeWidth(2);
        canvas.drawRoundRect(rect, 14, 14, mPaint);
        mPaint.setStyle(Paint.Style.FILL);

        mTextPaint.setColor(textColor);
        mTextPaint.setAlpha((int) (mAlpha * 255));
        float textY = rect.centerY() - ((mTextPaint.descent() + mTextPaint.ascent()) / 2);
        canvas.drawText(label, rect.centerX(), textY, mTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                float x = event.getX(actionIndex);
                float y = event.getY(actionIndex);
                int ctrl = findControl(x, y);
                mPointerToControl.put(pointerId, ctrl);
                handleControlPress(ctrl, x, y, true);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    float x = event.getX(i);
                    float y = event.getY(i);
                    int ctrl = mPointerToControl.getOrDefault(pId, CTRL_NONE);
                    if (ctrl == CTRL_L_STICK) {
                        updateLeftStick(x, y);
                    } else if (ctrl == CTRL_R_STICK) {
                        updateRightStick(x, y);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                int ctrl = mPointerToControl.remove(pointerId);
                handleControlPress(ctrl, 0, 0, false);
                break;
            }
        }

        RPCS3.sendPadData(mDigital1, mDigital2, mLeftStickX, mLeftStickY, mRightStickX, mRightStickY, mL2Axis, mR2Axis);
        invalidate();
        return true;
    }

    private int findControl(float x, float y) {
        if (distance(x, y, mLeftStickCenterX, mLeftStickCenterY) <= mStickRadius * 1.5f) return CTRL_L_STICK;
        if (distance(x, y, mRightStickCenterX, mRightStickCenterY) <= mStickRadius * 1.5f) return CTRL_R_STICK;

        if (mDpadUpRect.contains(x, y)) return CTRL_DPAD_UP;
        if (mDpadDownRect.contains(x, y)) return CTRL_DPAD_DOWN;
        if (mDpadLeftRect.contains(x, y)) return CTRL_DPAD_LEFT;
        if (mDpadRightRect.contains(x, y)) return CTRL_DPAD_RIGHT;

        if (mTriangleRect.contains(x, y)) return CTRL_TRIANGLE;
        if (mCircleRect.contains(x, y)) return CTRL_CIRCLE;
        if (mCrossRect.contains(x, y)) return CTRL_CROSS;
        if (mSquareRect.contains(x, y)) return CTRL_SQUARE;

        if (mL1Rect.contains(x, y)) return CTRL_L1;
        if (mL2Rect.contains(x, y)) return CTRL_L2;
        if (mR1Rect.contains(x, y)) return CTRL_R1;
        if (mR2Rect.contains(x, y)) return CTRL_R2;

        if (mSelectRect.contains(x, y)) return CTRL_SELECT;
        if (mStartRect.contains(x, y)) return CTRL_START;
        if (mPsRect.contains(x, y)) return CTRL_PS;
        if (mL3Rect.contains(x, y)) return CTRL_L3;
        if (mR3Rect.contains(x, y)) return CTRL_R3;

        return CTRL_NONE;
    }

    private void handleControlPress(int ctrl, float x, float y, boolean pressed) {
        if (pressed && mHapticEnabled && mVibrator != null && mVibrator.hasVibrator()) {
            try {
                mVibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
            } catch (Exception ignored) {}
        }

        switch (ctrl) {
            case CTRL_L_STICK:
                if (pressed) updateLeftStick(x, y);
                else {
                    mLeftStickKnobX = mLeftStickCenterX;
                    mLeftStickKnobY = mLeftStickCenterY;
                    mLeftStickX = 128;
                    mLeftStickY = 128;
                }
                break;
            case CTRL_R_STICK:
                if (pressed) updateRightStick(x, y);
                else {
                    mRightStickKnobX = mRightStickCenterX;
                    mRightStickKnobY = mRightStickCenterY;
                    mRightStickX = 128;
                    mRightStickY = 128;
                }
                break;
            case CTRL_DPAD_UP: setBit1(RPCS3.CELL_PAD_CTRL_UP, pressed); break;
            case CTRL_DPAD_DOWN: setBit1(RPCS3.CELL_PAD_CTRL_DOWN, pressed); break;
            case CTRL_DPAD_LEFT: setBit1(RPCS3.CELL_PAD_CTRL_LEFT, pressed); break;
            case CTRL_DPAD_RIGHT: setBit1(RPCS3.CELL_PAD_CTRL_RIGHT, pressed); break;
            case CTRL_TRIANGLE: setBit2(RPCS3.CELL_PAD_CTRL_TRIANGLE, pressed); break;
            case CTRL_CIRCLE: setBit2(RPCS3.CELL_PAD_CTRL_CIRCLE, pressed); break;
            case CTRL_CROSS: setBit2(RPCS3.CELL_PAD_CTRL_CROSS, pressed); break;
            case CTRL_SQUARE: setBit2(RPCS3.CELL_PAD_CTRL_SQUARE, pressed); break;
            case CTRL_L1: setBit2(RPCS3.CELL_PAD_CTRL_L1, pressed); break;
            case CTRL_L2:
                setBit2(RPCS3.CELL_PAD_CTRL_L2, pressed);
                mL2Axis = pressed ? 255 : 0;
                break;
            case CTRL_R1: setBit2(RPCS3.CELL_PAD_CTRL_R1, pressed); break;
            case CTRL_R2:
                setBit2(RPCS3.CELL_PAD_CTRL_R2, pressed);
                mR2Axis = pressed ? 255 : 0;
                break;
            case CTRL_SELECT: setBit1(RPCS3.CELL_PAD_CTRL_SELECT, pressed); break;
            case CTRL_START: setBit1(RPCS3.CELL_PAD_CTRL_START, pressed); break;
            case CTRL_PS: setBit1(RPCS3.CELL_PAD_CTRL_PS, pressed); break;
            case CTRL_L3: setBit1(RPCS3.CELL_PAD_CTRL_L3, pressed); break;
            case CTRL_R3: setBit1(RPCS3.CELL_PAD_CTRL_R3, pressed); break;
        }
    }

    private void updateLeftStick(float x, float y) {
        float dx = x - mLeftStickCenterX;
        float dy = y - mLeftStickCenterY;
        float dist = (float) Math.hypot(dx, dy);
        if (dist > mStickRadius) {
            dx = (dx / dist) * mStickRadius;
            dy = (dy / dist) * mStickRadius;
        }
        mLeftStickKnobX = mLeftStickCenterX + dx;
        mLeftStickKnobY = mLeftStickCenterY + dy;

        mLeftStickX = (int) ((dx / mStickRadius) * 127.0f + 128.0f);
        mLeftStickY = (int) ((dy / mStickRadius) * 127.0f + 128.0f);
    }

    private void updateRightStick(float x, float y) {
        float dx = x - mRightStickCenterX;
        float dy = y - mRightStickCenterY;
        float dist = (float) Math.hypot(dx, dy);
        if (dist > mStickRadius) {
            dx = (dx / dist) * mStickRadius;
            dy = (dy / dist) * mStickRadius;
        }
        mRightStickKnobX = mRightStickCenterX + dx;
        mRightStickKnobY = mRightStickCenterY + dy;

        mRightStickX = (int) ((dx / mStickRadius) * 127.0f + 128.0f);
        mRightStickY = (int) ((dy / mStickRadius) * 127.0f + 128.0f);
    }

    private void setBit1(int bit, boolean set) {
        if (set) mDigital1 |= bit;
        else mDigital1 &= ~bit;
    }

    private void setBit2(int bit, boolean set) {
        if (set) mDigital2 |= bit;
        else mDigital2 &= ~bit;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x1 - x2, y1 - y2);
    }
}
