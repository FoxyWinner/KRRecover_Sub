package com.buptnsrc.krrecoversub.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.buptnsrc.krrecoversub.service.BackServer;

public class AlarmReceiver extends BroadcastReceiver
{
    /**
     * 刚广播接收器旨在当到达了BackServer设置的时间间隔时，再次唤醒BackServer
     * 注：
     * BroadcastRecevier不能完成耗时操作，10s内不执行完Android会认为Application No Response.
     * 也不要尝试在BroadcastReceiiver中新建线程完成耗时操作，因为BroadcastReceiver生命周期很短，容易子线程还没运行完BroadcastReceiver就退出了。
     * 如果真的要耗时操作，考虑启动一个Service来进行。
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i("C-AlarmReceiver", "Start BackServer.");
        context.startService(new Intent(context, BackServer.class));
        Log.i("C-AlarmReceiver", "End Start BackServer.");
    }
}