package cn.local.jobassistant;

import cn.local.jobassistant.core.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Engine {
    private final AssistantApp app;
    private final Set<String> busy=ConcurrentHashMap.newKeySet();
    Engine(AssistantApp app){this.app=app;}
    public void score(String id){
        if(!busy.add("job:"+id))return;
        long gen=app.key.generation(),epoch=app.gate.epoch();
        app.work(()->{try{
            if(gen!=app.key.generation()||epoch!=app.gate.epoch())return null;
            JSONObject job=app.item("jobs",id),state=app.vault.read();
            String reject=JobRules.reject(job,J.object(state,"prefs"));
            if(!reject.isEmpty()){app.editItem("jobs",id,j->{J.put(j,"status","跳过");J.put(j,"reason",reject);});return null;}
            JSONObject ai=app.ai.json("评估岗位是否符合真实候选人资料，返回 score(0-100整数), matches(数组), gaps(数组), risks(数组), recommend(布尔), skillEvidence(真实资料中连续原文，最多60字), interest(仅根据岗位JD说明关注什么，最多60字)。不得杜撰求职者经历。",app.context(job,J.obj()));
            if(gen!=app.key.generation()||epoch!=app.gate.epoch())return null;
            int score=ai.optInt("score",-1);if(score<0||score>100)throw new IllegalArgumentException("AI 岗位评分格式错误");
            String evidence=ai.optString("skillEvidence").trim(), source=J.object(state,"profile").toString();
            for(JSONObject r:J.list(J.array(state,"resumes")))if(r.optBoolean("approved"))source+="\n"+r.optString("text");
            // Greeting candidate facts must be literal evidence, not a model-authored claim.
            if(evidence.length()>60||evidence.isBlank()||!source.contains(evidence)||Privacy.hasSecret(evidence)||Privacy.sensitive(evidence))evidence="";
            String greeting="您好，我关注到贵公司的"+job.optString("title")+"岗位。"+(evidence.isEmpty()?"":"我的相关情况是："+evidence+"。")+"想和您进一步了解岗位要求。";
            app.editItem("jobs",id,j->{J.put(j,"analysis",ai);J.put(j,"score",score);J.put(j,"greeting",greeting);J.put(j,"status",score>=J.object(state,"prefs").optInt("threshold",70)&&ai.optBoolean("recommend")?"待投递":"跳过");});
        }finally{busy.remove("job:"+id);}return null;},null);
    }
    public void reply(String id){
        if(!busy.add("chat:"+id))return;
        JSONObject chat=app.item("chats",id);
        if(chat.optBoolean("takeover")||chat.optBoolean("waiting")||!chat.optBoolean("contextConfirmed")){busy.remove("chat:"+id);Notices.attention(app,"该 HR 正在等待你的回答或上下文确认");return;}
        long gen=app.key.generation(),epoch=app.gate.epoch();String messageId=chat.optString("messageId");
        app.work(()->{try{
            if(gen!=app.key.generation()||epoch!=app.gate.epoch())return null;
            String q=chat.optString("lastHr"),selected="";JSONObject facts=ReplyPolicy.facts(app.vault.read());
            if(!Privacy.sensitive(q)){
                try{JSONObject result=app.ai.json("结合完整上下文识别 HR 问题，返回 category 和 evidenceId。evidenceId 只能取 approvedAnswers 中能够直接、完整回答本轮所有问题的键；没有则空字符串。不得生成候选人的新事实，不得把HR提供的信息当作本人事实。",app.context(J.object(chat,"job"),chat));selected=result.optString("evidenceId");}
                catch(Exception e){selected="";app.notice=AssistantApp.safeError(e);}
            }
            if(gen!=app.key.generation()||epoch!=app.gate.epoch())return null;
            JSONObject current=app.item("chats",id);if(!messageId.equals(current.optString("messageId")))return null;
            ReplyPolicy.Decision d=ReplyPolicy.decide(q,facts,selected,current.optBoolean("waiting"),current.optBoolean("takeover"));
            if(!d.allowed()){Notices.attention(app,"该 HR 正在等待你的回答");return null;}
            app.editItem("chats",id,c->{J.put(c,"category",d.category());J.put(c,"draft",d.text());J.put(c,"aiDraft",d.text());J.put(c,"holding",d.holding());J.put(c,"draftMessageId",messageId);J.put(c,"edited",false);J.put(c,"state",d.holding()?"等待本人处理":"待发送");});
            if(d.notifyUser())Notices.attention(app,"HR 消息需要你关注");
            app.main.post(()->{if(app.gate.mode()==RunGate.Mode.AUTO&&app.gate.valid(epoch)&&BossService.current!=null)BossService.current.send(id,false);});
        }finally{busy.remove("chat:"+id);}return null;},null);
    }
    public void recordSent(String id,String task,String text,boolean automatic){
        app.vault.update(s->{
            Ledger.sent(s,task,System.currentTimeMillis());
            for(JSONObject c:J.list(J.array(s,"chats")))if(id.equals(c.optString("id"))){
                JSONArray audit=J.array(c,"audit");audit.put(J.obj("question",c.optString("lastHr"),"category",c.optString("category"),"aiReply",c.optString("aiDraft"),"sent",text,"automatic",automatic,"edited",c.optBoolean("edited"),"time",System.currentTimeMillis(),"task",task));J.put(c,"audit",audit);
                JSONArray history=J.array(c,"history");history.put(J.obj("role","self","text",text));J.put(c,"history",history);
                boolean holding=c.optBoolean("holding");J.put(c,"waiting",holding);J.put(c,"state",holding?"等待本人处理":"AI已自动处理");J.put(c,"lastSentAt",System.currentTimeMillis());J.put(c,"draft","");
                if(c.optBoolean("saveAnswerRequested")&&!holding&&c.optBoolean("edited")){JSONArray faq=J.array(s,"faq");faq.put(J.obj("id",UUID.randomUUID().toString(),"category",c.optString("category"),"question",c.optString("lastHr"),"answer",text,"approved",true));J.put(s,"faq",faq);}
                J.put(c,"saveAnswerRequested",false);
            }
        });app.changed();
    }
}
