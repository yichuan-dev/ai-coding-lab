package cn.local.jobassistant.core;

import org.json.JSONObject;

/** One allowlist and validation path for both forms and imported backups. */
public final class Preferences {
    private Preferences() {}
    public static JSONObject clean(JSONObject source, boolean imported) {
        JSONObject result=J.obj();
        for(String key:new String[]{"job","city","education","experience","type","keywords","companyKeywords","blackCompanies","blackJobs"}) {
            String value=source.optString(key,"");
            if(value.length()>4000) throw new IllegalArgumentException("求职条件文字过长");
            J.put(result,key,value);
        }
        number(source,result,"minSalary",0,0,1000000,true);
        number(source,result,"maxSalary",100000,0,1000000,true);
        number(source,result,"threshold",70,0,100,true);
        number(source,result,"dailyApplications",10,1,100,true);
        number(source,result,"dailyCalls",50,1,500,true);
        number(source,result,"dailyTokens",100000,1000,10000000,true);
        number(source,result,"dailyCost",0,0,1000000,false);
        number(source,result,"intervalSeconds",20,5,3600,true);
        number(source,result,"dedupDays",30,1,3650,true);
        if(result.optInt("minSalary")>result.optInt("maxSalary"))throw new IllegalArgumentException("最低工资不能高于最高工资");
        // A backup cannot silently authorize sending from a new installation.
        J.put(result,"allowBossGreeting",!imported&&source.optBoolean("allowBossGreeting"));
        return result;
    }
    private static void number(JSONObject source,JSONObject result,String key,double fallback,double min,double max,boolean integer) {
        double value=source.has(key)?source.optDouble(key,Double.NaN):fallback;
        if(!Double.isFinite(value)||value<min||value>max||(integer&&value!=Math.floor(value)))throw new IllegalArgumentException("求职限额格式或范围不合法："+key);
        J.put(result,key,value);
    }
}
