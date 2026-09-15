package cn.local.jobassistant;

import android.app.*;
import android.content.*;
import android.os.*;
import cn.local.jobassistant.core.RunGate;

public final class RunService extends Service {
    private AssistantApp app;
    private final Runnable tick=new Runnable(){ public void run(){getSystemService(NotificationManager.class).notify(1,Notices.running(app));app.main.postDelayed(this,5000);}};
    @Override public void onCreate(){super.onCreate();app=(AssistantApp)getApplication();}
    @Override public int onStartCommand(Intent i,int flags,int id) {
        String action=i==null?"STOP":i.getAction();
        if("STOP".equals(action)){app.stop(RunGate.State.STOPPED,"任务已停止");stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
        if("PAUSE".equals(action)){app.stop(RunGate.State.PAUSED,"任务已暂停");return START_NOT_STICKY;}
        startForeground(1,Notices.running(app));app.main.removeCallbacks(tick);app.main.post(tick);return START_NOT_STICKY;
    }
    @Override public void onTaskRemoved(Intent root){app.key.clear();app.stop(RunGate.State.STOPPED,"已退出并清除内存 Key");stopSelf();}
    @Override public void onDestroy(){app.main.removeCallbacks(tick);app.stop(RunGate.State.STOPPED,"任务已停止");super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
