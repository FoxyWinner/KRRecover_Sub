package com.buptnsrc.krrecoversub.service;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.buptnsrc.krrecoversub.activity.ChildActivity;
import com.buptnsrc.krrecoversub.broadcastreceiver.AlarmReceiver;


/**
 * todo 我认为该service没必要无限重启停，定时任务就可以了
 * @author foxywinner
 */
public class BackServer extends Service
{

    public Context context;

    public Handler mHandler;

    AlarmManager alarmManager;

    PendingIntent pendingIntent;

    // 600秒=10分钟，改为了一分钟
    public int time = 60000;

    public BackServer() {
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i("【C-BackServer】", "onCreate");
        super.onCreate();
        this.context = (Context)this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("C-BackServer", "start thread setting");
        this.mHandler = new Mhandler();
        Message message = Message.obtain();
        message.what = 2;
        message.obj = "SetAlarm";
        this.mHandler.sendMessage(message);

        // todo 让mHandler发了一条message之后线程休眠1s？什么操作
        try
        {
            Thread.sleep(1000L);
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
        message = Message.obtain();
        message.what = 1;
        message.obj = "DO";
        this.mHandler.sendMessage(message);
        Log.i("C-BackServer", "end thread setting");
        return super.onStartCommand(intent, flags, startId);
    }

    class Mhandler extends Handler
    {
        @Override
        public void handleMessage(Message message)
        {
            // todo 这段代码可能有bug，message.what=1 和 =2应该严格走两个分支才对，但现在看来suspiciousCount <= 0 时都执行了setAlarm的逻辑
            if(message.what == 1)
            {
                Log.i("【C-BackService】", "handleMesaage");
                try
                {
                    Context context = BackServer.this.createPackageContext("com.buptnsrc.krrecover", CONTEXT_IGNORE_SECURITY);
                    int suspiciousCount = context.getSharedPreferences("suspiciousList", MODE_WORLD_READABLE+MODE_WORLD_WRITEABLE+MODE_MULTI_PROCESS).getInt("suspiciousCount", 0);
                    Log.i("【C-BackService】", String.valueOf(suspiciousCount));
                    if (suspiciousCount > 0)
                    {
                        Log.i("【C-BackService】", "count > 0");
                        // 如果子APK的MainActivity未启动，则启动
                        // MainActivity.context这个context似乎一直为空
                        if (ChildActivity.context == null)
                        {
                            Log.i("【C-BackService】", "MainActivity restart");
                            Intent mainActivityIntent = new Intent();
                            mainActivityIntent.setClass(BackServer.this.context, ChildActivity.class);
                            mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            BackServer.this.startActivity(mainActivityIntent);
                        }

                        ChildActivity.killFocusedWindow(context);
                        return;
                    }
                } catch (android.content.pm.PackageManager.NameNotFoundException nameNotFoundException)
                {
                    nameNotFoundException.printStackTrace();
                    return;
                }

//                Log.i("C-BackServer", "setAlarm-start");
//                BackServer.this.alarmManager = (AlarmManager)BackServer.this.getSystemService(Context.ALARM_SERVICE);
//                long l1 = SystemClock.elapsedRealtime();
//                long l2 = BackServer.this.time;
//                Intent intent = new Intent(BackServer.this.context, AlarmReceiver.class);
//                BackServer.this.pendingIntent = PendingIntent.getBroadcast(BackServer.this.context, 0, intent, 0);
//
//                BackServer.this.alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, l1 + l2, BackServer.this.pendingIntent);
//                Log.i("C-BackServer", "setAlarm-end");

            }else if (message.what == 2) // AlarmManager周期唤醒BackServer
            {
                Log.i("【C-BackServer】", "setAlarm-start");
                BackServer.this.alarmManager = (AlarmManager)BackServer.this.getSystemService(Context.ALARM_SERVICE);
                long l1 = SystemClock.elapsedRealtime();
                long l2 = BackServer.this.time;
                Intent intent = new Intent(BackServer.this.context, AlarmReceiver.class);
                BackServer.this.pendingIntent = PendingIntent.getBroadcast(BackServer.this.context, 0, intent, 0);

                //从Android4.4（API 19）开始，AlarmManager的set方式是非精准激发的，系统会偏移（shift）闹钟来最小化唤醒和电池消耗。6.0以上也有新的方法。
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    BackServer.this.alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, l1 + l2, BackServer.this.pendingIntent);
                }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                {
                    BackServer.this.alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, l1 + l2, BackServer.this.pendingIntent);
                }else
                {
                    BackServer.this.alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, l1 + l2, BackServer.this.pendingIntent);
                }
                Log.i("【C-BackServer】", "setAlarm-end");
            }

            // 如果是1和2之外的数字，不做任何处理

        }
    }
}
