package lk.raminsenanayake.thefeast.listener;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeListener implements SensorEventListener {

    private static final int FORCE_THRESHOLD = 500;
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 5;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float mLastX=-1.0f, mLastY=-1.0f, mLastZ=-1.0f;
    private long mLastTime;
    private OnShakeListener mShakeListener;
    private Context context;
    private int mShakeCount = 0;
    private long mLastShake;
    private long mLastForce;
    public ShakeListener(Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        resume();
    }

    public interface OnShakeListener    {
        public void onShake();
    }


    public void setOnShakeListener(OnShakeListener listener)    {
        mShakeListener = listener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = System.currentTimeMillis();

        if ((now - mLastForce) > SHAKE_TIMEOUT) {
            mShakeCount = 0;
        }

        if ((now - mLastTime) > TIME_THRESHOLD) {
            long diff = now - mLastTime;
            float speed = Math.abs(event.values[0] + event.values[1] + event.values[2] - mLastX - mLastY - mLastZ) / diff * 10000;
            if (speed > FORCE_THRESHOLD) {
                if ((++mShakeCount >= SHAKE_COUNT) && (now - mLastShake > SHAKE_DURATION)) {
                    mLastShake = now;
                    mShakeCount = 0;
                    if (mShakeListener != null) {
                        mShakeListener.onShake();
                    }
                }
                mLastForce = now;
            }
            mLastTime = now;
            mLastX = event.values[0];
            mLastY = event.values[1];
            mLastZ = event.values[2];
        }
    }

    public void resume() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void pause() {
        if (accelerometer != null) {
            sensorManager.unregisterListener(this);
        }
    }
}
