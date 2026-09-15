package cn.local.jobassistant.core;

import java.util.*;
import org.json.*;

/** The model selects evidence; it is never trusted to invent the candidate's answer. */
public final class ReplyPolicy {
    public static final String[] CATEGORIES={"问候","求职状态","岗位意向","简历","毕业时间","学历专业","技能","项目","经历","薪资","工作地点","面试","到岗","加班","出差","联系方式","Offer合同","离职原因","其他"};
    public record Decision(String category,String text,boolean holding,boolean notifyUser,boolean allowed) {}
    public static String classify(String q) {
        if(Privacy.sensitive(q)) return "敏感资料";
        String[][] patterns={{"简历","简历"},{"毕业时间","毕业"},{"学历专业","学历|专业|学校"},{"项目","项目"},{"技能","Java|java|Python|技术|技能|编程"},{"经历","工作经历|实习|经验"},{"薪资","薪资|工资|薪酬|薪水"},{"到岗","到岗|入职|开始工作"},{"面试","面试"},{"加班","加班|大小周|夜班"},{"出差","出差"},{"联系方式","微信|电话|联系\u65b9式|手机号"},{"Offer合同","(?i)offer|合同"},{"离职原因","离职|为什么找"},{"工作地点","地点|外地|哪个城市"},{"求职状态","找工作|在职|看机会"},{"岗位意向","考虑|意向"},{"问候","^在吗[？?]?|方便聊|你好|您好|^hi$"}};
        for(String[] p:patterns) if(java.util.regex.Pattern.compile(p[1]).matcher(q).find()) return p[0];
        return "其他";
    }
    public static boolean important(String cat) { return Set.of("面试","到岗","薪资","Offer合同","敏感资料","联系方式","简历").contains(cat); }
    public static Decision decide(String q,JSONObject evidence,String selectedId,boolean pending,boolean takeover) {
        String cat=classify(q);
        if(takeover||pending) return new Decision(cat,"",false,true,false);
        if(cat.equals("敏感资料")) return hold(cat,q);
        // Compound questions need review rather than answering only the convenient half.
        if(q.matches("(?s).*[？?].+[？?].*") || q.matches("(?s).*(以及|另外|同时还|还想问).*")) return hold(cat,q);
        JSONObject fact=evidence.optJSONObject(selectedId);
        if(fact!=null && cat.equals(fact.optString("category")) && !Set.of("其他","面试","Offer合同","联系方式","简历").contains(cat)) {
            String a=fact.optString("answer").trim();
            if(!a.isEmpty()&&a.length()<=350&&!Privacy.hasSecret(a)&&!Privacy.sensitive(a)) return new Decision(cat,a,false,important(cat),true);
        }
        if(cat.equals("问候")) return new Decision(cat,"您好，方便的，您请说。",false,false,true);
        return hold(cat,q);
    }
    private static Decision hold(String cat,String q) {
        String[] a={"收到，我确认一下具体情况，稍后回复您。","好的，我这边核实一下，再和您说。","这个我先确认一下，稍后给您回复。"};
        return new Decision(cat,a[Math.floorMod(q.hashCode(),a.length)],true,true,true);
    }
    public static JSONObject facts(JSONObject state) {
        JSONObject r=J.obj();
        for(JSONObject f:J.list(J.array(state,"faq"))) if(f.optBoolean("approved")) J.put(r,f.optString("id"),J.obj("category",f.optString("category"),"answer",f.optString("answer"),"question",f.optString("question")));
        JSONObject p=J.object(state,"profile");
        String[][] fields={{"毕业时间","毕业时间"},{"学历专业","学历专业"},{"技能","技能"},{"项目","项目"},{"经历","经历"},{"薪资","期望工资"},{"到岗","到岗时间"},{"加班","加班安排"},{"出差","出差安排"},{"求职状态","求职状态"},{"工作地点","目标城市"},{"离职原因","离职原因"}};
        for(String[] f:fields) if(!p.optString(f[1]).isBlank()) J.put(r,"profile:"+f[1],J.obj("category",f[0],"answer",p.optString(f[1])));
        return r;
    }
}
