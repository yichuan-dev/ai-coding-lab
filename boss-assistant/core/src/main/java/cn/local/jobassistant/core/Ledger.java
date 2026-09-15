package cn.local.jobassistant.core;

import org.json.*;
import java.time.*;

public final class Ledger {
    public static JSONObject defaults() {
        return J.obj("schema",1,"profile",J.obj(),"prefs",J.obj("job","","city","","minSalary",0,"maxSalary",100000,"threshold",70,"dailyApplications",10,"dailyCalls",50,"dailyTokens",100000,"dailyCost",0,"intervalSeconds",20,"dedupDays",30),"ai",J.obj("baseUrl","https://api.deepseek.com","model","deepseek-flash","temperature",0.2,"inputPrice",0,"outputPrice",0),"faq",new JSONArray(),"resumes",new JSONArray(),"jobs",new JSONArray(),"chats",new JSONArray(),"outbox",J.obj(),"usage",J.obj(),"bindings",J.obj(),"logs",new JSONArray());
    }
    public static String day(long now) { return Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString(); }
    public static boolean reserve(JSONObject s,String task,String kind,String target,long now,int dailyCap,int days) {
        JSONObject box=J.object(s,"outbox");
        if(box.has(task)) return false;
        int count=0;
        for(java.util.Iterator<String> it=box.keys();it.hasNext();) {
            JSONObject t=box.optJSONObject(it.next()); if(t==null) continue;
            if(kind.equals("apply") && "apply".equals(t.optString("kind"))) {
                if(day(now).equals(t.optString("day"))) count++;
                if(target.equals(t.optString("target")) && (!t.optString("state").equals("SENT")||now-t.optLong("time")<days*86400000L)) return false;
            }
        }
        if(kind.equals("apply")&&count>=dailyCap) return false;
        J.put(box,task,J.obj("kind",kind,"target",target,"state","UNKNOWN","time",now,"day",day(now))); J.put(s,"outbox",box); return true;
    }
    public static void sent(JSONObject s,String task,long now) { JSONObject t=J.object(J.object(s,"outbox"),task); if(t.length()==0) throw new IllegalStateException("任务不存在"); J.put(t,"state","SENT"); J.put(t,"sentAt",now); }
    public static JSONObject usage(JSONObject s,long now) { return J.object(J.object(s,"usage"),day(now)); }
    /** Persist before HTTP. Failed calls still consume a call slot. */
    public static void reserveAi(JSONObject s,long now) {
        JSONObject u=usage(s,now),p=J.object(s,"prefs");
        if(u.optInt("calls")>=p.optInt("dailyCalls",50)||u.optLong("tokens")>=p.optLong("dailyTokens",100000)||(p.optDouble("dailyCost",0)>0 && u.optDouble("cost")>=p.optDouble("dailyCost"))) throw new IllegalStateException("已达到今日 AI 预算");
        J.put(u,"calls",u.optInt("calls")+1); JSONObject all=J.object(s,"usage"); J.put(all,day(now),u); J.put(s,"usage",all);
    }
    public static void account(JSONObject s,long now,long input,long output) {
        JSONObject u=usage(s,now),ai=J.object(s,"ai");
        J.put(u,"tokens",u.optLong("tokens")+Math.max(0,input)+Math.max(0,output));
        J.put(u,"cost",u.optDouble("cost",0)+(Math.max(0,input)*ai.optDouble("inputPrice",0)+Math.max(0,output)*ai.optDouble("outputPrice",0))/1000000.0);
        JSONObject all=J.object(s,"usage"); J.put(all,day(now),u); J.put(s,"usage",all);
    }
    public static JSONObject backup(JSONObject s) { return J.obj("schema",1,"profile",J.copy(J.object(s,"profile")),"prefs",J.copy(J.object(s,"prefs")),"faq",J.array(J.copy(s),"faq")); }
}
