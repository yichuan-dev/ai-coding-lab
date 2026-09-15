package cn.local.jobassistant.core;

import org.json.*;
import java.util.*;

public final class J {
    private J() {}
    public static JSONObject obj(Object... kv) {
        JSONObject o = new JSONObject();
        try { for (int i=0;i<kv.length;i+=2) o.put((String)kv[i], kv[i+1]); }
        catch (JSONException e) { throw new IllegalArgumentException("数据格式错误"); }
        return o;
    }
    public static void put(JSONObject o,String k,Object v) { try { o.put(k,v); } catch(JSONException e) { throw new IllegalArgumentException("数据格式错误"); } }
    public static JSONObject parse(String s) { try { return new JSONObject(s); } catch(JSONException e) { throw new IllegalArgumentException("数据格式错误"); } }
    public static JSONObject copy(JSONObject o) { return parse(o.toString()); }
    public static JSONObject object(JSONObject o,String k) { JSONObject x=o.optJSONObject(k); return x==null?obj():x; }
    public static JSONArray array(JSONObject o,String k) { JSONArray a=o.optJSONArray(k); return a==null?new JSONArray():a; }
    public static List<JSONObject> list(JSONArray a) { List<JSONObject> r=new ArrayList<>(); for(int i=0;i<a.length();i++) if(a.optJSONObject(i)!=null) r.add(a.optJSONObject(i)); return r; }
    public static String string(JSONObject o,String k) { return o.optString(k,""); }
}
