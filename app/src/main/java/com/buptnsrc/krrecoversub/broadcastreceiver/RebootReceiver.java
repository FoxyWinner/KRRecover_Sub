package com.buptnsrc.krrecoversub.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.buptnsrc.krrecoversub.activity.ChildActivity;


public class RebootReceiver extends BroadcastReceiver
{
    /**
     * 刚广播接收器旨在android.intent.action.BOOT_COMPLETED事件发生后，启动ChildActivity。
     * @param paramContext
     * @param paramIntent
     */
    @Override
    public void onReceive(Context paramContext, Intent paramIntent) {
        if (paramIntent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            paramIntent = new Intent();
            paramIntent.setClass(paramContext, ChildActivity.class);
            paramIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            paramContext.startActivity(paramIntent);
            Log.i("C-RebootReceiver", "Received reboot and start ChildActivity");
        }
    }
}