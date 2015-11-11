/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import edu.buffalo.cse.phonelab.json.StrictJSONObject;

import java.util.List;
import android.os.MaybeManager;
import java.lang.Exception;

public class KeyguardPatternView extends LinearLayout implements KeyguardSecurityView,
        AppearAnimationCreator<LockPatternView.CellState> {

    private Context mContext;

    private static final String TAG = "SecurityPatternView";
    private static final boolean DEBUG = KeyguardConstants.DEBUG;

    // how long before we clear the wrong pattern
    private static final int PATTERN_CLEAR_TIMEOUT_MS = 2000;

    // how long we stay awake after each key beyond MIN_PATTERN_BEFORE_POKE_WAKELOCK
    private static final int UNLOCK_PATTERN_WAKE_INTERVAL_MS = 7000;

    // how many cells the user has to cross before we poke the wakelock
    private static final int MIN_PATTERN_BEFORE_POKE_WAKELOCK = 2;

    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final AppearAnimationUtils mAppearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtils;

    private CountDownTimer mCountdownTimer = null;
    private LockPatternUtils mLockPatternUtils;
    private LockPatternView mLockPatternView;
    private KeyguardSecurityCallback mCallback;

    /**
     * Keeps track of the last time we poked the wake lock during dispatching of the touch event.
     * Initialized to something guaranteed to make us poke the wakelock when the user starts
     * drawing the pattern.
     * @see #dispatchTouchEvent(android.view.MotionEvent)
     */
    private long mLastPokeTime = -UNLOCK_PATTERN_WAKE_INTERVAL_MS;

    /**
     * Useful for clearing out the wrong pattern after a delay
     */
    private Runnable mCancelPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
        }
    };
    private Rect mTempRect = new Rect();
    private SecurityMessageDisplay mSecurityMessageDisplay;
    private View mEcaView;
    private Drawable mBouncerFrame;
    private ViewGroup mKeyguardBouncerFrame;
    private KeyguardMessageArea mHelpMessage;
    private int mDisappearYTranslation;

    enum FooterMode {
        Normal,
        ForgotLockPattern,
        VerifyUnlocked
    }

    public KeyguardPatternView(Context context) {
        this(context, null);
    }

    public KeyguardPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
        mAppearAnimationUtils = new AppearAnimationUtils(context,
                AppearAnimationUtils.DEFAULT_APPEAR_DURATION, 1.5f /* translationScale */,
                2.0f /* delayScale */, AnimationUtils.loadInterpolator(
                        mContext, android.R.interpolator.linear_out_slow_in));
        mDisappearAnimationUtils = new DisappearAnimationUtils(context,
                125, 1.2f /* translationScale */,
                0.8f /* delayScale */, AnimationUtils.loadInterpolator(
                        mContext, android.R.interpolator.fast_out_linear_in));
        mDisappearYTranslation = getResources().getDimensionPixelSize(
                R.dimen.disappear_y_translation);
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mCallback = callback;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLockPatternUtils = mLockPatternUtils == null
                ? new LockPatternUtils(mContext) : mLockPatternUtils;

        mLockPatternView = (LockPatternView) findViewById(R.id.lockPatternView);
        mLockPatternView.setSaveEnabled(false);
        mLockPatternView.setFocusable(false);
        mLockPatternView.setOnPatternListener(new UnlockPatternListener());

        // stealth mode will be the same for the life of this screen
        mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled());

        // vibrate mode will be the same for the life of this screen
        mLockPatternView.setTactileFeedbackEnabled(mLockPatternUtils.isTactileFeedbackEnabled());

        setFocusableInTouchMode(true);

        mSecurityMessageDisplay = new KeyguardMessageArea.Helper(this);
        mEcaView = findViewById(R.id.keyguard_selector_fade_container);
        View bouncerFrameView = findViewById(R.id.keyguard_bouncer_frame);
        if (bouncerFrameView != null) {
            mBouncerFrame = bouncerFrameView.getBackground();
        }

        mKeyguardBouncerFrame = (ViewGroup) findViewById(R.id.keyguard_bouncer_frame);
        mHelpMessage = (KeyguardMessageArea) findViewById(R.id.keyguard_message_area);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        // as long as the user is entering a pattern (i.e sending a touch event that was handled
        // by this screen), keep poking the wake lock so that the screen will stay on.
        final long elapsed = SystemClock.elapsedRealtime() - mLastPokeTime;
        if (result && (elapsed > (UNLOCK_PATTERN_WAKE_INTERVAL_MS - 100))) {
            mLastPokeTime = SystemClock.elapsedRealtime();
        }
        mTempRect.set(0, 0, 0, 0);
        offsetRectIntoDescendantCoords(mLockPatternView, mTempRect);
        ev.offsetLocation(mTempRect.left, mTempRect.top);
        result = mLockPatternView.dispatchTouchEvent(ev) || result;
        ev.offsetLocation(-mTempRect.left, -mTempRect.top);
        return result;
    }

    private void maybeHitFactor() {
        
        float hitFactor;
        
        int __hit_factor__208 = 0;
        
        MaybeManager maybeManager;
        
        try {
          maybeManager = (MaybeManager) mContext.getSystemService(Context.MAYBE_SERVICE);
        } catch (Exception e) {
          Log.e("MaybeService-hit_factor", "Failed to get maybe service.", e);
          return;
        };
        
        try {
          __hit_factor__208 = maybeManager.getMaybeAlternative("com.android.keyguard", "hit_factor");
        } catch (Exception e) {
          Log.e("MaybeService-hit_factor", "Failed to get maybe alternative.", e);
        };
        switch (__hit_factor__208) {
          
          case 4: {
                    hitFactor = 0.2f;
                    break;
          }  
          case 3: {
                    hitFactor = 0.4f;
                    break;
          }  
          case 2: {
                    hitFactor = 1.0f;
                    break;
          }  
          case 1: {
                    hitFactor = 0.8f;
                    break;
          }  
          default: {
                    hitFactor = 0.6f;
                    if (__hit_factor__208 != 0) {
                      try {
                        maybeManager.badMaybeAlternative("com.android.keyguard", "hit_factor", __hit_factor__208);
                      } catch (Exception e) {
                        Log.e("MaybeService-hit_factor", "Failed to report bad maybe alternative.", e);
                      }
                    }
                    break;
          }
        }
        mLockPatternView.setHitFactor(hitFactor);
    }

    public void reset() {
        // reset lock pattern
        mLockPatternView.enableInput();
        mLockPatternView.setEnabled(true);
        mLockPatternView.clearPattern();
        maybeHitFactor();

        // if the user is currently locked out, enforce it.
        long deadline = mLockPatternUtils.getLockoutAttemptDeadline();
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        } else {
            displayDefaultSecurityMessage();
        }
    }

    private void displayDefaultSecurityMessage() {
        if (mKeyguardUpdateMonitor.getMaxBiometricUnlockAttemptsReached()) {
            mSecurityMessageDisplay.setMessage(R.string.faceunlock_multiple_failures, true);
        } else {
            mSecurityMessageDisplay.setMessage(R.string.kg_pattern_instructions, false);
        }
    }

    @Override
    public void showUsabilityHint() {
    }

    /** TODO: hook this up */
    public void cleanUp() {
        if (DEBUG) Log.v(TAG, "Cleanup() called on " + this);
        mLockPatternUtils = null;
        mLockPatternView.setOnPatternListener(null);
    }

    private class UnlockPatternListener implements LockPatternView.OnPatternListener {
        private static final String MAYBE_TAG = "Maybe-Keyguard-PhoneLab";
        private static final String UNLOCK_ACTION = "unlock";
        private static final String START_ACTION = "start";
        private static final String END_ACTION = "end";
        private StrictJSONObject log;

        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mCancelPatternRunnable);
            log = new StrictJSONObject(MAYBE_TAG)
                    .put(StrictJSONObject.KEY_ACTION, UNLOCK_ACTION)
                    .put("hitFactor", mLockPatternView.getHitFactor())
                    .put("startElapsedTime", SystemClock.elapsedRealtimeNanos())
                    .put("startTimestamp", System.currentTimeMillis());
        }

        public void onPatternCleared() {
            log.put("status", "abort").log();
        }

        public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
            mCallback.userActivity();
        }

        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
            log.put("patternSize", pattern.size());
            if (mLockPatternUtils.checkPattern(pattern)) {
                mCallback.reportUnlockAttempt(true);
                mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                mCallback.dismiss(true);
                log.put("status", "success");
            } else {
                log.put("status", "fail");
                if (pattern.size() > MIN_PATTERN_BEFORE_POKE_WAKELOCK) {
                    mCallback.userActivity();
                }
                mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                boolean registeredAttempt =
                        pattern.size() >= LockPatternUtils.MIN_PATTERN_REGISTER_FAIL;
                if (registeredAttempt) {
                    mCallback.reportUnlockAttempt(false);
                }
                int attempts = mKeyguardUpdateMonitor.getFailedUnlockAttempts();
                log.put("attempts", attempts);
                if (registeredAttempt &&
                        0 == (attempts % LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT)) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline();
                    handleAttemptLockout(deadline);
                } else {
                    mSecurityMessageDisplay.setMessage(R.string.kg_wrong_pattern, true);
                    mLockPatternView.postDelayed(mCancelPatternRunnable, PATTERN_CLEAR_TIMEOUT_MS);
                }
            }
            log.log();
        }
    }

    private void handleAttemptLockout(long elapsedRealtimeDeadline) {
        mLockPatternView.clearPattern();
        mLockPatternView.setEnabled(false);
        final long elapsedRealtime = SystemClock.elapsedRealtime();

        mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                final int secondsRemaining = (int) (millisUntilFinished / 1000);
                mSecurityMessageDisplay.setMessage(
                        R.string.kg_too_many_failed_attempts_countdown, true, secondsRemaining);
            }

            @Override
            public void onFinish() {
                mLockPatternView.setEnabled(true);
                displayDefaultSecurityMessage();
            }

        }.start();
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
    }

    @Override
    public void onResume(int reason) {
        reset();
    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return mCallback;
    }

    @Override
    public void showBouncer(int duration) {
        KeyguardSecurityViewHelper.
                showBouncer(mSecurityMessageDisplay, mEcaView, mBouncerFrame, duration);
    }

    @Override
    public void hideBouncer(int duration) {
        KeyguardSecurityViewHelper.
                hideBouncer(mSecurityMessageDisplay, mEcaView, mBouncerFrame, duration);
    }

    @Override
    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1f);
        setTranslationY(mAppearAnimationUtils.getStartTranslation());
        animate()
                .setDuration(500)
                .setInterpolator(mAppearAnimationUtils.getInterpolator())
                .translationY(0);
        mAppearAnimationUtils.startAnimation(
                mLockPatternView.getCellStates(),
                new Runnable() {
                    @Override
                    public void run() {
                        enableClipping(true);
                    }
                },
                this);
        if (!TextUtils.isEmpty(mHelpMessage.getText())) {
            mAppearAnimationUtils.createAnimation(mHelpMessage, 0,
                    AppearAnimationUtils.DEFAULT_APPEAR_DURATION,
                    mAppearAnimationUtils.getStartTranslation(),
                    true /* appearing */,
                    mAppearAnimationUtils.getInterpolator(),
                    null /* finishRunnable */);
        }
    }

    @Override
    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        mLockPatternView.clearPattern();
        enableClipping(false);
        setTranslationY(0);
        animate()
                .setDuration(300)
                .setInterpolator(mDisappearAnimationUtils.getInterpolator())
                .translationY(-mDisappearAnimationUtils.getStartTranslation());
        mDisappearAnimationUtils.startAnimation(mLockPatternView.getCellStates(),
                new Runnable() {
                    @Override
                    public void run() {
                        enableClipping(true);
                        if (finishRunnable != null) {
                            finishRunnable.run();
                        }
                    }
                }, KeyguardPatternView.this);
        if (!TextUtils.isEmpty(mHelpMessage.getText())) {
            mDisappearAnimationUtils.createAnimation(mHelpMessage, 0,
                    200,
                    - mDisappearAnimationUtils.getStartTranslation() * 3,
                    false /* appearing */,
                    mDisappearAnimationUtils.getInterpolator(),
                    null /* finishRunnable */);
        }
        return true;
    }

    private void enableClipping(boolean enable) {
        setClipChildren(enable);
        mKeyguardBouncerFrame.setClipToPadding(enable);
        mKeyguardBouncerFrame.setClipChildren(enable);
    }

    @Override
    public void createAnimation(final LockPatternView.CellState animatedCell, long delay,
            long duration, float translationY, final boolean appearing,
            Interpolator interpolator,
            final Runnable finishListener) {
        if (appearing) {
            animatedCell.scale = 0.0f;
            animatedCell.alpha = 1.0f;
        }
        animatedCell.translateY = appearing ? translationY : 0;
        ValueAnimator animator = ValueAnimator.ofFloat(animatedCell.translateY,
                appearing ? 0 : translationY);
        animator.setInterpolator(interpolator);
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedFraction = animation.getAnimatedFraction();
                if (appearing) {
                    animatedCell.scale = animatedFraction;
                } else {
                    animatedCell.alpha = 1 - animatedFraction;
                }
                animatedCell.translateY = (float) animation.getAnimatedValue();
                mLockPatternView.invalidate();
            }
        });
        if (finishListener != null) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    finishListener.run();
                }
            });

            // Also animate the Emergency call
            mAppearAnimationUtils.createAnimation(mEcaView, delay, duration, translationY,
                    appearing, interpolator, null);
        }
        animator.start();
        mLockPatternView.invalidate();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}

