package cn.local.jobassistant.core;

import java.net.URI;
import java.util.regex.Pattern;

public final class Privacy {
    public static final Pattern SENSITIVE = Pattern.compile("身份证|银行卡|验证码|密码|家庭住址|详细住址|护照|人脸|证件照|学历证明|背景调查");
    private static final Pattern SECRET = Pattern.compile("(?i)sk-[a-z0-9_-]{8,}|Bearer\\s+[^\\s\"',}]+|(?:api[_ -]?key|cookie|session|authorization|密码)\\s*[:=：]\\s*[^\\n,}]+|(?<![0-9])[0-9]{11,19}[xX]?(?![0-9])");
    public static String redact(String s) { return s==null?"":SECRET.matcher(s).replaceAll("[已隐藏]"); }
    public static boolean hasSecret(String s) { return s!=null && SECRET.matcher(s).find(); }
    public static boolean sensitive(String s) { return s!=null && SENSITIVE.matcher(s).find(); }
    public static URI endpoint(String raw) {
        try {
            URI u=new URI(raw.trim()); String h=u.getHost();
            if(!"https".equals(u.getScheme())||h==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||h.equals("localhost")||!h.contains(".")||h.matches("[0-9.]+")||h.contains(":")) throw new Exception();
            return u;
        } catch(Exception e) { throw new IllegalArgumentException("Base URL 必须是可信服务商的 HTTPS 地址，不含账号、参数或 IP"); }
    }
    public static String httpError(int status) { return switch(status) { case 401,403 -> "API Key 无效或无访问权限"; case 402 -> "AI 账户余额不足"; case 429 -> "AI 请求过于频繁，请稍后重试"; default -> "AI 服务暂不可用（"+status+"）"; }; }
}
