package qianfeng.a9_4sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private long lastTime = 0;

    private SensorEventListener listener = new SensorEventListener() {
        // 当手机加速度发生变化时触发该方法
        @Override
        public void onSensorChanged(SensorEvent event) {

            // 因为手机加速度是只要你一摇动就会发生变化的，所以我肯定要给它设定一些门槛，
            // 让它没有那么容易触发，否则你轻轻一摇或者没摇就触发了，这种用户体验肯定不好。
            // 而且我的动画加载也需要时间(1000毫秒)，如果你在我动画正在播放的时候，再来一段动画，肯定不好，所以这里让它每隔1000毫秒才执行一次。
            // 其他那些例如一个按钮，如果暂时没有反应，用户在没反应的时候点击了很多次，导致原本没bug现在有bug，
            // 这种解决思路也是一样的，让用户每隔1秒单击一次，这样才有效，一秒内多次点击就没有效果，处理的方式都是像这样的.

            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastTime < 1000) {
                return;
            }
            lastTime = currentTimeMillis;

            // 在这里做当加速度传感器被触发的操作,先判断手机加速度传感器在x，y，z轴上的加速度(矢量),注意加速度是有正负的,所以判断的时候，用绝对值
            float[] values = event.values; // 获取加速度传感器的在三个轴上的加速度矢量(注意是有方向的)

            float valueX = Math.abs(values[0]);
            float valueY = Math.abs(values[1]);
            float valueZ = Math.abs(values[2]);

            if (valueX > 15 || valueY > 15 || valueZ > 15) {
                // 如果达到加速度传感器要触发的值，即"摇一摇"的力度和加速度
                startAnim(); // 开启摇一摇动画
                startSound(); // 开启摇一摇的声音
            }

        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    private SoundPool soundPool;
    private int playId;
    private Vibrator vibrator;

    private void startSound() {
        //1.要播放的音频文件
        //2。3左右声道音量
        //4.优先级
        //5.是否循环，0表示不循环，-1表示一直循环
        //6.播放速率，取值0.5-2之间，1表示正常速率
        soundPool.play(playId, 1, 1, 0, 0, 1);

        // 在声音播放的时候，开启振动功能!
        //开启振动（***添加振动权限***）
        //1.振动节奏
        //2.重复次数
        vibrator.vibrate(new long[]{300, 200, 300, 200}, -1);
    }

    private void startAnim() {

        // 写一个补间组合动画，以自身为参照物,注意这段动画是平移动画
        AnimationSet upSet = new AnimationSet(false);
        TranslateAnimation upUp = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 0, TranslateAnimation.RELATIVE_TO_SELF, 0,
                TranslateAnimation.RELATIVE_TO_SELF, 0, TranslateAnimation.RELATIVE_TO_SELF, -1);
        upUp.setDuration(1000);

        TranslateAnimation upDown = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 0, TranslateAnimation.RELATIVE_TO_SELF, 0,
                TranslateAnimation.RELATIVE_TO_SELF, 0, TranslateAnimation.RELATIVE_TO_SELF, 1);
        upDown.setDuration(1000);
        upDown.setStartOffset(1000); // 延迟1秒执行这个动画

        upSet.addAnimation(upUp);
        upSet.addAnimation(upDown);

        up.startAnimation(upSet);

        AnimationSet downSet = new AnimationSet(false);

        TranslateAnimation downUp = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 0, TranslateAnimation.RELATIVE_TO_SELF, 0,
                TranslateAnimation.RELATIVE_TO_SELF, 0, TranslateAnimation.RELATIVE_TO_SELF, -1);
        downUp.setDuration(1000);
        downUp.setStartOffset(1000); // 延迟1秒执行这个动画(先下去再上来，延迟1秒再执行这段上来的动画)

        TranslateAnimation downDown = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 0, TranslateAnimation.RELATIVE_TO_SELF, 0,
                TranslateAnimation.RELATIVE_TO_SELF, 0, TranslateAnimation.RELATIVE_TO_SELF, 1);
        downDown.setDuration(1000);


        downSet.addAnimation(downUp);
        downSet.addAnimation(downDown);

        down.startAnimation(downSet);


    }

    private ImageView down;
    private ImageView up;
    private SensorManager manager; // 获取一个传感器管理器
    private Sensor sensor;  // 获取一个加速度传感器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        down = ((ImageView) findViewById(R.id.down));
        up = ((ImageView) findViewById(R.id.up));

        //在onCreate()方法中获取一个振动服务
        vibrator = ((Vibrator) getSystemService(VIBRATOR_SERVICE));

    }

    @Override
    protected void onResume() {
        super.onResume();
        // 为什么要选择在onResume上初始化传感器呢？
        // 因为一旦home键出去了，我就要解除对传感器监听的注册，一回来我就要注册到传感器的监听，
        // 所以要在onResume方法中，另外，如果是在onCreate方法中初始化的话，那么我按home键退出的话，
        // 回来的话传感器监听就没有注册了，那我就不能使用传感器来摇一摇了。

        // 初始化传感器
        initSensor();

        initSoundPool();

    }

    private void initSoundPool() {
        // 初始化声音用的是声音池，因为MediaPlayer是重量级的播放器，而播放这么小的声音文件(只有10几k)，用声音池就够了。
        // 声音池的获取现在有两种方法，根据手机的最低版本不同来进行区分。废弃的那个方法是刚刚从Android 5.0才废弃的
        soundPool = null;
        if (Build.VERSION.SDK_INT > 20) {
            SoundPool.Builder builder = new SoundPool.Builder();
            // 设置最大并发流数量
            builder.setMaxStreams(3);
            AudioAttributes.Builder audioAttributesBuilder = new AudioAttributes.Builder();
            // new SoundPool的第二个参数
            audioAttributesBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            builder.setAudioAttributes(audioAttributesBuilder.build());
            soundPool = builder.build(); // 建立声音池

        } else {
            // 1.设置最大并发流数量
            // 2. 设置声音的来源
            // 3. 这个是质量，设置了没用,看源码(ctrl)，描述说给0就可以了.
            soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        }
        // 读取一个音频文件
        // 最后一个参数是优先级，并不影响
        playId = soundPool.load(this, R.raw.awe, 1);
    }

    private void initSensor() {
        // 获取一个传感器管理器
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // 获取一个加速度传感器 (加速度传感器是传感器的一种，可以通过传感器管理器获得)
        sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // 注册传感器监听
        // 1.监听器
        // 2.要监听的传感器
        // 3.传感器的灵敏度，分如下四个级别，从上往下灵敏度依次降低
//        SENSOR_DELAY_FASTEST = 0;
//        SENSOR_DELAY_GAME = 1;
//        SENSOR_DELAY_UI = 2;
//        SENSOR_DELAY_NORMAL = 3;

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // 注意是在onPause中解除对加速度传感器的监听。按home键切换出去也要解除对这个的监听。否则，退出了Activity，或者home键到达桌面了，摇一摇还是有声音的！
        manager.unregisterListener(listener);
    }
}
