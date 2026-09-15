package cn.local.jobassistant;

import android.app.*;
import android.content.*;

public final class Notices {
    static void channels(Context c) {
        NotificationManager n=c.getSystemService(NotificationManager.class);
        n.createNotificationChannel(new NotificationChannel("run","求职任务运行状态",NotificationManager.IMPORTANCE_LOW));
        n.createNotificationChannel(new NotificationChannel("attention","需要本人处理",NotificationManager.IMPORTANCE_DEFAULT));
    }
    static Notification running(AssistantApp app) {
        PendingIntent open=PendingIntent.getActivity(app,0,new Intent(app,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(app,1,new Intent(app,RunService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pause=PendingIntent.getService(app,2,new Intent(app,RunService.class).setAction("PAUSE"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(app,"run").setSmallIcon(R.drawable.ic_app).setContentTitle("求职助手 · "+app.status()).setContentText("点击返回；可随时暂停或立即停止").setContentIntent(open).setOngoing(true).setVisibility(Notification.VISIBILITY_PRIVATE).addAction(new Notification.Action.Builder(null,"暂停",pause).build()).addAction(new Notification.Action.Builder(null,"立即停止",stop).build()).build();
    }
    static void attention(AssistantApp app,String reason) {
        app.notice=reason; // The notification intentionally contains no HR text, identity, or secrets.
        try { app.getSystemService(NotificationManager.class).notify(20,new Notification.Builder(app,"attention").setSmallIcon(R.drawable.ic_app).setContentTitle("求职助手需要你处理").setContentText("请打开 APP 查看详情").setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(PendingIntent.getActivity(app,0,new Intent(app,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).build()); }catch(SecurityException ignored){}
    }
}
