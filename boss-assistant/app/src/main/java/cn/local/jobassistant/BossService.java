package cn.local.jobassistant;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.os.*;
import android.view.accessibility.*;
import cn.local.jobassistant.core.*;
import org.json.*;
import java.util.*;

/** BOSS adapter: screenshots/CAPTCHA solvers/private endpoints/device spoofing are absent. */
public final class BossService extends AccessibilityService {
    public static final String PACKAGE="com.hpbr.bosszhipin";
    public static final String[] ROLES={"searchInput","searchButton","jobCard","jobList","jobTitle","jobCompany","jobCity","jobSalary","jobExperience","jobEducation","jobHr","jobJd","applyButton","chatHr","chatCompany","chatJob","incoming","outgoing","messageInput","sendButton"};
    public static final String[] LABELS={"搜索输入框","搜索按钮","列表岗位卡片","岗位列表容器","详情岗位名","详情公司","详情城市","详情薪资","详情经验","详情学历","详情HR","详情JD","立即沟通按钮","聊天HR姓名","聊天公司","聊天岗位","HR消息气泡","本人消息气泡","消息输入框","发送按钮"};
    public static BossService current;
    public static volatile boolean captureRequested;
    public static List<String> inventory=List.of();
    private AssistantApp app;
    private long typingUntil,armedUntil;
    private String armedChat="",armedJob="",searchQuery="";
    private final Set<String> visited=new HashSet<>();
    private final Runnable tick=this::inspect;
    private boolean sending;
    @Override protected void onServiceConnected(){app=(AssistantApp)getApplication();current=this;}
    @Override public void onDestroy(){current=null;if(app!=null){app.main.removeCallbacks(tick);app.stop(RunGate.State.PAUSED,"BOSS 辅助服务已关闭");}super.onDestroy();}
    @Override public void onInterrupt(){if(app!=null)app.stop(RunGate.State.PAUSED,"BOSS 辅助服务已中断");}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(e.getPackageName()==null||!PACKAGE.contentEquals(e.getPackageName()))return;
        if(app==null)app=(AssistantApp)getApplication();
        if(!captureRequested&&app.gate.state()!=RunGate.State.RUNNING)return;
        if(e.getEventType()==AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED&&SystemClock.elapsedRealtime()>typingUntil){AccessibilityNodeInfo n=e.getSource();try{if(n!=null&&n.isEditable()&&!sending){app.stop(RunGate.State.PAUSED,"检测到本人输入，已暂停自动化");return;}}finally{if(n!=null)n.recycle();}}
        app.main.removeCallbacks(tick);app.main.postDelayed(tick,1200);
    }
    private BossTree tree(){if(getSystemService(KeyguardManager.class).isKeyguardLocked())return null;AccessibilityNodeInfo r=getRootInActiveWindow();if(r==null)return null;if(!PACKAGE.contentEquals(r.getPackageName())){r.recycle();return null;}return new BossTree(r);}
    private JSONObject bindings(){return J.object(app.vault.read(),"bindings");}
    private String selector(String role){return bindings().optString(role);}
    private long interval(){return Math.max(5,J.object(app.vault.read(),"prefs").optInt("intervalSeconds",20))*1000L;}
    private boolean rate(){return app.gate.rate(SystemClock.elapsedRealtime(),interval());}
    private void later(){app.main.removeCallbacks(tick);app.main.postDelayed(tick,interval());}
    public void begin(){visited.clear();searchQuery="";later();}
    public void armChat(String id){armedChat=id;armedUntil=SystemClock.elapsedRealtime()+60000;}
    public void armJob(String id){armedJob=id;armedUntil=SystemClock.elapsedRealtime()+60000;}
    private String chatId(BossTree t){String hr=t.value(selector("chatHr")),company=t.value(selector("chatCompany")),job=t.value(selector("chatJob"));if(hr.isBlank()||company.isBlank()||job.isBlank())return "";return Ids.hash(hr,company,job);}
    private String incomingId(BossTree t){List<String> a=t.values(selector("incoming"));return a.isEmpty()?"":Ids.hash(a.toArray(new String[0]));}
    private String latestHr(BossTree t){List<String>a=t.values(selector("incoming"));return a.isEmpty()?"":String.join("\n",a.subList(Math.max(0,a.size()-4),a.size()));}
    private void inspect(){
        if(app.vault==null)return;
        try(BossTree t=tree()){
            if(t==null){if(app.gate.state()==RunGate.State.RUNNING)app.stop(RunGate.State.SCREEN_HIDDEN,"BOSS 不可见或手机已锁屏，请打开 BOSS 后继续");return;}
            if(captureRequested){inventory=t.inventory();captureRequested=false;Notices.attention(app,"控件列表已读取，请返回助手完成绑定");return;}
            if(app.gate.state()!=RunGate.State.RUNNING||sending)return;
            if(!app.key.present()){app.stop(RunGate.State.KEY_REQUIRED,"请重新输入 API Key");return;}
            RunGate.State obstruction=RunGate.obstruction(t.allText());if(obstruction!=null){app.stop(obstruction,"请本人完成 BOSS 登录或安全验证后再继续");return;}
            String cid=chatId(t);if(!cid.isEmpty()){
                if(SystemClock.elapsedRealtime()<armedUntil&&cid.equals(armedChat)){armedChat="";send(cid,true);return;}
                readChat(t,cid);return;
            }
            if(t.one(selector("jobTitle"))!=null&&t.one(selector("jobCompany"))!=null){readJob(t);return;}
            search(t);
        }catch(Exception e){app.stop(RunGate.State.UI_CHANGED,"页面识别或保存失败，请核对 BOSS 页面及控件绑定");}
    }
    private void readJob(BossTree t){
        JSONObject job=J.obj();String[][] fields={{"title","jobTitle"},{"company","jobCompany"},{"city","jobCity"},{"salary","jobSalary"},{"experience","jobExperience"},{"education","jobEducation"},{"hr","jobHr"},{"jd","jobJd"}};
        for(String[] p:fields)J.put(job,p[0],t.value(selector(p[1])));
        String id=Ids.job(job);if(id.isEmpty()||job.optString("jd").isBlank()){app.stop(RunGate.State.UI_CHANGED,"岗位身份或 JD 不完整，已暂停");return;}
        JSONObject old=app.item("jobs",id);
        if(old.length()==0){J.put(job,"id",id);J.put(job,"time",System.currentTimeMillis());J.put(job,"status","发现岗位");app.vault.update(s->J.array(s,"jobs").put(job));app.engine.score(id);later();return;}
        if(old.optString("status").equals("发现岗位")){later();return;}
        boolean confirmed=id.equals(armedJob)&&SystemClock.elapsedRealtime()<armedUntil;
        if(old.optString("status").equals("待投递")&&(app.gate.mode()==RunGate.Mode.AUTO||confirmed)){apply(t,id,confirmed);return;}
        if(rate()){performGlobalAction(GLOBAL_ACTION_BACK);later();}else later();
    }
    private void search(BossTree t){
        JSONObject p=J.object(app.vault.read(),"prefs");String query=(p.optString("job")+" "+p.optString("city")).trim();
        AccessibilityNodeInfo input=t.one(selector("searchInput"));
        if(input!=null&&!query.equals(searchQuery)){
            if(query.isEmpty()){app.stop(RunGate.State.PAUSED,"请先设置目标岗位和城市");return;}
            if(!rate()){later();return;}typingUntil=SystemClock.elapsedRealtime()+3000;
            if(!BossTree.enter(input,query)||!BossTree.click(t.one(selector("searchButton")))){app.stop(RunGate.State.UI_CHANGED,"搜索控件未匹配，请重新绑定");return;}searchQuery=query;later();return;
        }
        for(AccessibilityNodeInfo n:t.find(selector("jobCard"))){String key=BossTree.text(n);if(key.isBlank()||visited.contains(key))continue;if(!rate()){later();return;}visited.add(key);if(!BossTree.click(n))app.stop(RunGate.State.UI_CHANGED,"岗位卡片不可点击");else later();return;}
        AccessibilityNodeInfo list=t.one(selector("jobList"));if(list!=null&&rate()){if(!list.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))app.stop(RunGate.State.PAUSED,"当前岗位列表已到末尾");else later();return;}
        app.stop(RunGate.State.UI_CHANGED,"未识别到岗位列表或聊天，请核对绑定并进入对应页面");
    }
    private void apply(BossTree t,String id,boolean confirmed){
        long epoch=app.gate.epoch();if(!app.gate.maySend(epoch,confirmed,false))return;
        if(!J.object(app.vault.read(),"prefs").optBoolean("allowBossGreeting")){app.stop(RunGate.State.WAITING,"请先在设置中确认：立即沟通可能发送 BOSS 官方默认招呼");return;}
        AccessibilityNodeInfo button=t.one(selector("applyButton"));if(button==null||!BossTree.text(button).equals("立即沟通")){app.stop(RunGate.State.UI_CHANGED,"投递按钮状态无法确认");return;}
        if(!rate()){later();return;}String task=Ids.hash("apply",id,Long.toString(System.currentTimeMillis()));final boolean[] ok={false};
        app.vault.update(s->{JSONObject p=J.object(s,"prefs");ok[0]=Ledger.reserve(s,task,"apply",id,System.currentTimeMillis(),p.optInt("dailyApplications",10),p.optInt("dedupDays",30));});
        if(!ok[0]){app.stop(RunGate.State.WAITING,"该岗位已处理、结果待核对，或达到每日上限");return;}
        armedJob="";if(app.gate.valid(epoch))BossTree.click(button);
        app.editItem("jobs",id,j->{J.put(j,"status","发送结果待核对");J.put(j,"task",task);});
        app.stop(RunGate.State.WAITING,"请到 BOSS 核对首次沟通结果；不会自动重试或标记投递成功");
    }
    private void readChat(BossTree t,String id){
        String mid=incomingId(t),q=latestHr(t);if(mid.isEmpty()||q.isBlank())return;
        JSONObject old=app.item("chats",id);if(mid.equals(old.optString("messageId")))return;
        // Preserve cumulative HR bubbles for burst handling. Require user-confirmed full context on first capture.
        app.vault.update(s->{JSONArray chats=J.array(s,"chats");JSONObject c=null;for(JSONObject x:J.list(chats))if(id.equals(x.optString("id")))c=x;
            if(c==null){c=J.obj("id",id,"hr",t.value(selector("chatHr")),"company",t.value(selector("chatCompany")),"title",t.value(selector("chatJob")),"contextConfirmed",false,"history",new JSONArray(),"audit",new JSONArray());chats.put(c);}
            J.put(c,"messageId",mid);J.put(c,"lastHr",q);J.put(c,"time",System.currentTimeMillis());
            JSONArray h=J.array(c,"history");h.put(J.obj("role","hr","text",q));J.put(c,"history",h);J.put(c,"state","等待本人处理");J.put(s,"chats",chats);
        });app.changed();
        JSONObject c=app.item("chats",id);if(c.optBoolean("waiting")||c.optBoolean("takeover")||!c.optBoolean("contextConfirmed")){Notices.attention(app,"请核对 HR 上下文或补充答案");return;}app.engine.reply(id);
    }
    public void send(String id,boolean confirmed){
        if(sending)return;long epoch=app.gate.epoch();JSONObject chat=app.item("chats",id);
        if(!app.key.present()||!app.gate.maySend(epoch,confirmed,chat.optBoolean("takeover"))||!chat.optBoolean("contextConfirmed"))return;
        String text=chat.optString("draft");if(text.isBlank()||text.length()>350||Privacy.hasSecret(text)||Privacy.sensitive(text)){app.stop(RunGate.State.WAITING,"回复包含敏感信息或过长，请本人处理");return;}
        if(!chat.optString("messageId").equals(chat.optString("draftMessageId"))){app.stop(RunGate.State.WAITING,"HR 已发来新消息，请重新生成回复");return;}
        String mid=chat.optString("messageId");
        try(BossTree t=tree()){
            if(t==null||!id.equals(chatId(t))||!mid.equals(incomingId(t))||RunGate.obstruction(t.allText())!=null){app.stop(RunGate.State.WAITING,"聊天身份或内容已变化，请核对");return;}
            if(selector("incoming").equals(selector("outgoing"))||selector("outgoing").isBlank()){app.stop(RunGate.State.UI_CHANGED,"请分别绑定双方消息气泡");return;}
            AccessibilityNodeInfo input=t.one(selector("messageInput")),button=t.one(selector("sendButton"));
            if(input==null||button==null||!BossTree.text(input).isEmpty()){app.stop(RunGate.State.WAITING,"输入框不为空或发送控件不匹配，已暂停");return;}
            if(!rate()){app.main.postDelayed(()->{if(app.gate.valid(epoch))send(id,confirmed);},interval());return;}
            int previous=Collections.frequency(t.values(selector("outgoing")),text);
            String purpose=chat.optBoolean("edited")?"answer":chat.optBoolean("holding")?"holding":"reply",task=Ids.hash(id,mid,purpose);final boolean[] ok={false};
            app.vault.update(s->ok[0]=Ledger.reserve(s,task,"message",id,System.currentTimeMillis(),0,30));
            if(!ok[0]){app.stop(RunGate.State.WAITING,"这条回复已处理或结果待核对，不会重复发送");return;}
            sending=true;typingUntil=SystemClock.elapsedRealtime()+5000;
            if(!app.gate.valid(epoch)||!BossTree.enter(input,text)){sending=false;app.stop(RunGate.State.WAITING,"输入结果待核对，已暂停");return;}
            app.main.postDelayed(()->finishClick(id,mid,text,task,previous,epoch,confirmed),500);
        }catch(Exception e){sending=false;app.stop(RunGate.State.WAITING,"发送结果不确定，请本人核对");}
    }
    private void finishClick(String id,String mid,String text,String task,int previous,long epoch,boolean confirmed){
        try(BossTree t=tree()){
            if(t==null||!app.gate.maySend(epoch,confirmed,app.item("chats",id).optBoolean("takeover"))||!app.key.present()||!id.equals(chatId(t))||!mid.equals(incomingId(t))||!text.equals(t.value(selector("messageInput")))||RunGate.obstruction(t.allText())!=null||!BossTree.click(t.one(selector("sendButton")))){sending=false;app.stop(RunGate.State.WAITING,"发送前状态变化，已停止，请核对输入框");return;}
        }catch(Exception e){sending=false;app.stop(RunGate.State.WAITING,"发送结果待核对");return;}
        app.main.postDelayed(()->{
            try(BossTree t=tree()){
                if(t!=null&&id.equals(chatId(t))&&Collections.frequency(t.values(selector("outgoing")),text)>previous&&t.value(selector("messageInput")).isEmpty())app.engine.recordSent(id,task,text,!confirmed);
                else app.stop(RunGate.State.WAITING,"发送结果不确定，请核对；不会自动重试");
            }catch(Exception e){app.stop(RunGate.State.WAITING,"发送记录保存失败，请本人核对");}finally{sending=false;}
        },2000);
    }
}
