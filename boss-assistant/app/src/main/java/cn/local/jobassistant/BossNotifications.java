package cn.local.jobassistant;

import android.service.notification.*;
import android.os.Bundle;
import cn.local.jobassistant.core.*;

/** Permission is broad at OS level; the first executable branch discards every other app. */
public final class BossNotifications extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification n){
        if(!BossService.PACKAGE.equals(n.getPackageName()))return;
        AssistantApp app=(AssistantApp)getApplication();if(app.vault==null||app.gate.state()!=RunGate.State.RUNNING)return;
        // A notification is a reminder, not trustworthy full chat context for auto-send.
        Notices.attention(app,"收到 BOSS 新通知，请打开对应聊天读取完整内容");app.changed();
    }
}
