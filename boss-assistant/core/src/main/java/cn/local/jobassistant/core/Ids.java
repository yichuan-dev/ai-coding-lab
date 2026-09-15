package cn.local.jobassistant.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import org.json.JSONObject;

public final class Ids {
    public static String hash(String... parts) {
        try {
            MessageDigest d=MessageDigest.getInstance("SHA-256");
            for(String p:parts) { byte[] b=(p==null?"":p).getBytes(StandardCharsets.UTF_8); d.update((b.length+":").getBytes(StandardCharsets.US_ASCII)); d.update(b); }
            StringBuilder r=new StringBuilder(); for(byte b:d.digest()) r.append(String.format(Locale.ROOT,"%02x",b&255)); return r.toString();
        } catch(Exception e) { throw new IllegalStateException("无法生成任务标识"); }
    }
    public static String normalize(String s) { return s.toLowerCase(Locale.ROOT).replaceAll("\\s+",""); }
    /** Adapted from upstream company/title/city dedup; salary removed, HR added. */
    public static String job(JSONObject j) {
        if(!j.optString("bossId").isBlank()) return hash("boss",j.optString("bossId"));
        for(String k:new String[]{"company","title","city","hr"}) if(j.optString(k).isBlank()) return "";
        return hash(normalize(j.optString("company")),normalize(j.optString("title")),normalize(j.optString("city")),normalize(j.optString("hr")));
    }
}
