package cn.local.jobassistant.core;

import org.json.*;
import java.util.regex.*;

public final class JobRules {
    /** Port of boss-zhipin-bot score_hr_activity; upstream MIT attribution in licenses/. */
    public static int hrActivity(String text) {
        if(text==null||text.isEmpty()) return 0;
        if(text.contains("刚刚活跃")) return 100;
        if(text.contains("今日活跃")) return 80;
        if(Pattern.compile("\\d+日内活跃|三天内活跃").matcher(text).find()) return 60;
        if(text.contains("本周活跃")) return 40;
        if(text.contains("本月活跃")) return 30;
        if(text.contains("半年")&&text.contains("活跃")) return 20;
        return text.contains("活跃")?10:0;
    }
    public static String reject(JSONObject j,JSONObject p) {
        String title=j.optString("title"),company=j.optString("company"),jd=j.optString("jd");
        for(String k:new String[]{"blackCompanies","blackJobs"}) for(String b:p.optString(k).split("[,，\\n]")) if(!b.isBlank()&&(k.equals("blackCompanies")?company:title).contains(b.trim())) return "命中黑名单";
        for(String[] pair:new String[][]{{"city","city"},{"education","education"},{"experience","experience"},{"type","jd"},{"keywords","jd"},{"companyKeywords","company"}}) for(String q:p.optString(pair[0]).split("[,，\\n]")) if(!q.isBlank()&&!j.optString(pair[1]).contains(q.trim())) return "未满足条件："+pair[0];
        if(jd.isBlank()||title.isBlank()||company.isBlank()) return "岗位信息不完整";
        if(p.optInt("minSalary")>0||p.optInt("maxSalary",100000)<100000) {
            String salary=j.optString("salary").toLowerCase();
            Matcher m=Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[-~至]\\s*(\\d+(?:\\.\\d+)?)\\s*([k万]?)").matcher(salary);
            if(!m.find()||salary.matches(".*(天|日|时|年).*")) return "月薪范围未知，需要本人核对";
            double mult=m.group(3).equals("k")?1000:m.group(3).equals("万")?10000:1;
            if(Double.parseDouble(m.group(2))*mult<p.optInt("minSalary") || Double.parseDouble(m.group(1))*mult>p.optInt("maxSalary",100000)) return "薪资范围不匹配";
        }
        return "";
    }
}
