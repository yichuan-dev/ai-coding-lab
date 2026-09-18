package cn.local.jobassistant;

import android.app.*;
import android.os.*;
import android.content.*;
import cn.local.jobassistant.core.*;
import org.json.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class AssistantApp extends Application {
    public final SessionKey key=new SessionKey(SystemClock::elapsedRealtime);
    public final RunGate gate=new RunGate();
    public final ExecutorService io=Executors.newSingleThreadExecutor();
    public final Handler main=new Handler(Looper.getMainLooper());
    public Vault vault; public AiClient ai; public Engine engine;
    public volatile long dataVersion;
    public String storageFailure="";public String notice="";
    private int foreground;
    @Override public void onCreate() {
        super.onCreate();try { vault=new Vault(this); }catch(Exception e) { storageFailure=e.getMessage(); }
        ai=new AiClient(this);engine=new Engine(this);Notices.channels(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            public void onActivityStarted(Activity a) { if(++foreground==1)key.foreground(); }
            public void onActivityStopped(Activity a) { if(--foreground==0)key.background(); }
            public void onActivityCreated(Activity a,Bundle b){} public void onActivityResumed(Activity a){} public void onActivityPaused(Activity a){} public void onActivitySaveInstanceState(Activity a,Bundle b){} public void onActivityDestroyed(Activity a){}
        });
        main.postDelayed(new Runnable(){public void run(){ if(!key.present()&&gate.state()==RunGate.State.RUNNING)stop(RunGate.State.KEY_REQUIRED,"请重新输入 API Key");main.postDelayed(this,15000); }},15000);
    }
    public void stop(RunGate.State state,String text) { gate.halt(state);ai.cancel();notice=text;changed();if(state!=RunGate.State.STOPPED)Notices.attention(this,text); }
    public void changed() { sendBroadcast(new Intent(getPackageName()+".CHANGED").setPackage(getPackageName())); }
    public synchronized void clearLocalData() {
        dataVersion++;
        key.clear();stop(RunGate.State.STOPPED,"本地数据已清除");
        Vault.erase(this);vault=new Vault(this);
    }
    @FunctionalInterface public interface DataWork { void run(Vault vault) throws Exception; }
    public synchronized void withData(long version,DataWork work) throws Exception {
        if(version!=dataVersion)throw new java.io.IOException("资料已清除，已取消旧操作");
        work.run(vault);
    }
    public void work(Callable<?> task,Runnable success) {
        io.execute(()->{try {task.call();main.post(()->{changed();if(success!=null)success.run();});}catch(Exception e){main.post(()->{notice=safeError(e);Notices.attention(this,notice);changed();});}});
    }
    public static String safeError(Exception e) {
        String m=e.getMessage();
        if(m!=null && m.length()<100 && !m.contains("http") && !Privacy.hasSecret(m))return m;
        return "操作失败，请检查连接或本地数据后重试";
    }
    public String status() {
        return switch(gate.state()) {case STOPPED->"已停止";case RUNNING->switch(gate.mode()){case TEST->"测试模式运行中";case CONFIRM->"半自动运行中";case AUTO->"自动模式运行中";};case VERIFY->"等待本人完成 BOSS 安全验证";case LOGIN->"BOSS 登录失效";case KEY_REQUIRED->"DeepSeek 未连接";case UI_CHANGED->"BOSS 页面需重新适配";case WAITING->"等待本人核对";case SCREEN_HIDDEN->"BOSS 页面不可见";case PAUSED->"已暂停";};
    }
    public JSONObject context(JSONObject job,JSONObject chat) {
        JSONObject s=vault.read();JSONArray rs=new JSONArray();for(JSONObject r:J.list(J.array(s,"resumes"))) if(r.optBoolean("approved"))rs.put(J.obj("name",r.optString("name"),"text",r.optString("text")));
        return J.obj("profile",J.object(s,"profile"),"resumes",rs,"preferences",J.object(s,"prefs"),"job",job,"conversation",chat,"approvedAnswers",ReplyPolicy.facts(s));
    }
    public void editItem(String list,String id,Consumer<JSONObject> edit) { vault.update(s->{for(JSONObject item:J.list(J.array(s,list)))if(id.equals(item.optString("id"))) {edit.accept(item);return;}throw new IllegalArgumentException("记录不存在");});changed(); }
    public JSONObject item(String list,String id) { for(JSONObject j:J.list(J.array(vault.read(),list)))if(id.equals(j.optString("id")))return j;return J.obj(); }
}
