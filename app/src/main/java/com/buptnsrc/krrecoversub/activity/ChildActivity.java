package com.buptnsrc.krrecoversub.activity;


import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;


import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.buptnsrc.krrecoversub.R;
import com.buptnsrc.krrecoversub.enums.ChildAPKStatusEnum;
import com.buptnsrc.krrecoversub.service.BackServer;

public class ChildActivity extends AppCompatActivity
{
    private static final String TAG = "C-ChildActivity";

    public static Context context;


    public Handler mainHandler; // todo 该无用参数是干什么的？


    /**
     * 该方法根据检测结果为用户提供弹窗并做出相应逻辑
     * 由 checkPackageName()方法调用
     */
    public static void alertAndDeal(final String pkg, String ransomWareName, final Context userContext, boolean isTrustedSoftWare)
    {
        if (isTrustedSoftWare)
        {
            return;
        }

        // 1. 简历警告框，弹出警告
        String warningMsg = "<" + ransomWareName + ">疑似勒索软件！已被我们强制停止运行，且建议您将其进行卸载。您可以选择手动加入以下名单。若您选择白名单，您将失去我们提供的守护。";
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("警告！");
        builder.setMessage(warningMsg);
        builder.setPositiveButton("可疑名单", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // 因为只要弹窗肯定是已经在可疑名单里了，所以不需要任何处理
            }
        });
        builder.setNegativeButton("白名单", new DialogInterface.OnClickListener()
        {
            @SuppressLint("ApplySharedPref")
            public void onClick(DialogInterface param1DialogInterface, int suspiciousCount)
            {
                SharedPreferences sharedPreferences = userContext.getSharedPreferences("whiteList", MODE_WORLD_READABLE + MODE_WORLD_WRITEABLE + MODE_MULTI_PROCESS);
                if (!sharedPreferences.getBoolean(pkg, false))
                {
                    sharedPreferences.edit().putBoolean(pkg, true).commit();
                }
                sharedPreferences = userContext.getSharedPreferences("suspiciousList", MODE_WORLD_READABLE + MODE_WORLD_WRITEABLE + MODE_MULTI_PROCESS);

                suspiciousCount = sharedPreferences.getInt("suspiciousCount", 0);
                if (suspiciousCount > 0 && sharedPreferences.getBoolean(pkg, false))
                {
                    sharedPreferences.edit().remove(pkg).commit();
                    sharedPreferences.edit().putInt("suspiciousCount", suspiciousCount - 1).commit();
                }
            }
        });
        builder.setNeutralButton("默认", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface param1DialogInterface, int param1Int)
            {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
        alertDialog.show();
    }

    /**
     * 查找该app存不存在于可疑列表，如果存在于可疑列表，则强停并询问用户是否加入白名单
     *
     * @param packageName       用于am force-stop
     * @param ransomWareName    只做弹窗的警告信息用
     * @param context
     * @param isTrustedSoftWare
     * @return
     */
    public static boolean checkPackageNameInLists(String packageName, String ransomWareName, Context context, boolean isTrustedSoftWare)
    {
        boolean isTrustedInWhite = context.getSharedPreferences("whiteList", MODE_WORLD_READABLE + MODE_WORLD_WRITEABLE + MODE_MULTI_PROCESS).getBoolean(packageName, false);
        boolean isSuspicious = context.getSharedPreferences("suspiciousList", MODE_WORLD_READABLE + MODE_WORLD_WRITEABLE + MODE_MULTI_PROCESS).getBoolean(packageName, false);

        // 如果在白名单或者可疑名单中
//        if (isTrustedInWhite || isSuspicious)
        // 之前的逻辑应该是错误的，按理说如果在可疑名单中才进行
        if (isSuspicious)
        {
            // 强行终止
            String command = "am force-stop " + packageName + "\n";
            int result = execRootCmdSilent(command);
            Log.i(TAG, result == 0 ? "强行停止成功" : "强行停止失败");

            rmPassword();
            openADB();
            alertAndDeal(packageName, ransomWareName, context, isTrustedSoftWare);
            return true;
        }
        return false;
    }

    /**
     * 这段代码没有被反编译成功，只得出了中间码。
     * 猜想这段代码里包含了强制关闭窗口的方法，而且调用了checkPackageNameInLists方法，最终成功复现
     */
    public static void killFocusedWindow(Context context)
    {
        // 1. 先get了processes和service列表
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfoList = activityManager.getRunningAppProcesses();

        if (processInfoList != null)
        {
            Iterator procListIterator = processInfoList.iterator();
            while (procListIterator.hasNext())
            {
                ActivityManager.RunningAppProcessInfo runningAppProcessInfo = (ActivityManager.RunningAppProcessInfo) procListIterator.next();
                // 这里最后一个值的传递存疑，这个bool变量原先应该是机器学习算法计算出的，由父APK存入SP的？
                checkPackageNameInLists(runningAppProcessInfo.processName, runningAppProcessInfo.processName, context, false);
            }
        }

        List<ActivityManager.RunningServiceInfo> runningServices = ((ActivityManager) ChildActivity.context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE);

        if (runningServices != null)
        {
            Iterator serviceListIterator = runningServices.iterator();
            while (serviceListIterator.hasNext())
            {
                ActivityManager.RunningServiceInfo runningServiceInfo = (ActivityManager.RunningServiceInfo) serviceListIterator.next();
                // 这里最后一个值的传递存疑，这个bool变量原先应该是机器学习算法计算出的，由父APK存入SP的？
                checkPackageNameInLists(runningServiceInfo.process, runningServiceInfo.service.getPackageName(), context, false);
            }
        }
    }

    public static void openADB()
    {

        String command = "setprop persist.sys.usb.config adb\n";
        int result = execRootCmdSilent(command);
        Log.i(TAG, result == 0 ? "打开ADB成功" : "打开ADB失败");
        return;

    }

    /**
     * 擦去锁屏密码
     */
    public static void rmPassword()
    {
        String cmd = "rm /data/system/password.key\n";
        int result = execRootCmdSilent(cmd);
        Log.i(TAG, result == 0 ? "Rm password成功" : "Rm password失败");
        // 这里报失败是正常的，因为勒索软件并不一定设置了password，若没有设置，肯定就没有这个文件
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        moveTaskToBack(true);
        Log.i(TAG, "moveTaskToBack");

        // 将静态变量context赋值为this，是为了让外部知道ChildActivity是否已经Create过
        context = (Context) this;
        try
        {
            SharedPreferences whiteListSP= ChildActivity.this.createPackageContext("com.buptnsrc.krrecover", CONTEXT_IGNORE_SECURITY).getSharedPreferences("whiteList", MODE_WORLD_READABLE + MODE_WORLD_WRITEABLE + MODE_MULTI_PROCESS);
            int childAPKState = whiteListSP.getInt("childAPKState", 0);
            Log.i(TAG, "onCreate: childAPKState = "+ childAPKState);

            // 必须得要权限
            if (childAPKState != ChildAPKStatusEnum.GRANTED.getInt())
            {
                // 为子APK进程获取超级用户权限
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dos;
                dos = new DataOutputStream(p.getOutputStream());
                dos.writeBytes("exit\n");
                dos.flush();

                p.waitFor();
                int result = p.exitValue();
                Log.i(TAG, result == 0 ? "成功" : "失败");

                if (result == 0)
                {
                    // 即便设置了"世界可读可写"，也会在写的时候报错。Android早已经不建议这种方式了，如果非要用，换成ContentProvider
//                    whiteListSP.edit().putInt("childAPKState", ChildAPKStatusEnum.GRANTED.getInt()).commit();

//                    SharedPreferences.Editor editor = whiteListSP.edit();
//                    editor.putInt("childAPKState", ChildAPKStatusEnum.GRANTED.getInt());
//                    editor.apply();
                }
            }
        } catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }


        this.mainHandler = new MyHandler();

        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                Message message = Message.obtain();
                message.what = 3;
                message.obj = "suspiciousListNotNull";
                ChildActivity.this.mainHandler.sendMessage(message);
            }
        };

        // todo 线程最好由线程池提供
        new Thread(runnable).start();

    }
    public static int execRootCmdSilent(String cmd)
    {
        int result = -1;
        DataOutputStream dos = null;
        try
        {
            Process p = Runtime.getRuntime().exec("su");
            dos = new DataOutputStream(p.getOutputStream());
            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();
            p.waitFor();
            result = p.exitValue();
            Log.i(TAG, "Success execRootCmdSilent(" + cmd + ")=" + result);
        } catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG,
                    "execRootCmdSilent(" + cmd + "),Exception:"
                            + e.getMessage());
        } finally
        {
            if (dos != null)
            {
                try
                {
                    dos.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    class MyHandler extends Handler
    {
        @Override
        public void handleMessage(Message message)
        {
            if (message.what == 3)
            {
                try
                {
                    Log.i(TAG, "Handle message:3，根据可疑列表准备开始启动BackServer");
                    // 如果可疑列表中
                    int suspiciousNum = ChildActivity.this.createPackageContext("com.buptnsrc.krrecover", CONTEXT_IGNORE_SECURITY).getSharedPreferences("suspiciousList", MODE_WORLD_READABLE + MODE_WORLD_WRITEABLE + MODE_MULTI_PROCESS).getInt("suspiciousCount", 0);
                    Log.i(TAG, "Suspicious number: "+suspiciousNum);

                    if (suspiciousNum > 0)
                    {
                        List list = ((ActivityManager) ChildActivity.context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE);
                        if (list != null)
                        {
                            Iterator iterator = list.iterator();
                            while (iterator.hasNext())
                            {
                                // 如果service所属进程为子APK，先停止一下BackServer的运行，下面将进行重启
                                if (((ActivityManager.RunningServiceInfo) iterator.next()).process.equals("com.buptnsrc.krrecoversub"))
                                {
                                    Intent backServerIntent = new Intent(ChildActivity.context, BackServer.class);
                                    backServerIntent.setAction("android.intent.action.RESPOND_VIA_MESSAGE");
                                    ChildActivity.this.stopService(backServerIntent);
                                    Log.i(TAG, "Stop to restart BackServer.");
                                }
                            }
                        }

                        Log.i(TAG, "Start BackServer.");
                        Intent intent = new Intent(ChildActivity.context, BackServer.class);
                        intent.setAction("android.intent.action.RESPOND_VIA_MESSAGE");
                        ChildActivity.this.startService(intent);
                        Log.i(TAG, "end BackServer");
                        return;
                    }
                } catch (android.content.pm.PackageManager.NameNotFoundException nameNotFoundException)
                {
                    nameNotFoundException.printStackTrace();
                    return;
                }
            }

        }
    }
}