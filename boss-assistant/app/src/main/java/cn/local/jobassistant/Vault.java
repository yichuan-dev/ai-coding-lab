package cn.local.jobassistant;

import android.content.Context;
import android.security.keystore.*;
import android.util.AtomicFile;
import cn.local.jobassistant.core.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.Consumer;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Private, authenticated encryption; corruption never silently overwrites the original. */
public final class Vault {
    private static final String ALIAS="job-assistant-v1";
    private final AtomicFile file;
    private JSONObject state;
    public Vault(Context c) {
        file=new AtomicFile(new File(c.getNoBackupFilesDir(),"vault.enc"));
        if(file.getBaseFile().exists()) {
            try { state=J.parse(new String(decrypt(file.readFully(),key(false)),StandardCharsets.UTF_8)); }
            catch(Exception e) { throw new IllegalStateException("本地数据无法解密。请保留原文件，或明确选择清除全部数据。"); }
        } else state=Ledger.defaults();
    }
    public synchronized JSONObject read() { return J.copy(state); }
    public synchronized void update(Consumer<JSONObject> change) {
        JSONObject next=J.copy(state); change.accept(next);
        byte[] plain=next.toString().getBytes(StandardCharsets.UTF_8); FileOutputStream out=null;
        try { byte[] encrypted=encrypt(plain,key(true)); out=file.startWrite(); out.write(encrypted); file.finishWrite(out); state=next; }
        catch(Exception e) { if(out!=null) file.failWrite(out); throw new IllegalStateException("本地保存失败，任务已中止"); }
        finally { Arrays.fill(plain,(byte)0); }
    }
    public static void erase(Context c) {
        new AtomicFile(new File(c.getNoBackupFilesDir(),"vault.enc")).delete();
        try { KeyStore s=KeyStore.getInstance("AndroidKeyStore"); s.load(null); s.deleteEntry(ALIAS); } catch(Exception ignored) {}
    }
    private static SecretKey key(boolean create) throws Exception {
        KeyStore s=KeyStore.getInstance("AndroidKeyStore"); s.load(null);
        if(s.containsAlias(ALIAS)) return (SecretKey)s.getKey(ALIAS,null);
        if(!create) throw new GeneralSecurityException();
        KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");
        g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build()); return g.generateKey();
    }
    static byte[] encrypt(byte[] plain,SecretKey key) throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key);
        ByteArrayOutputStream b=new ByteArrayOutputStream(); b.write(c.getIV()); b.write(c.doFinal(plain)); return b.toByteArray();
    }
    static byte[] decrypt(byte[] bytes,SecretKey key) throws Exception {
        if(bytes.length<28) throw new GeneralSecurityException();
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,bytes,0,12)); return c.doFinal(bytes,12,bytes.length-12);
    }
    static SecretKey derive(char[] password,byte[] salt) throws Exception {
        if(password.length<10) throw new IllegalArgumentException("备份密码至少 10 个字符");
        PBEKeySpec spec=new PBEKeySpec(password,salt,150000,256);
        try { return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),"AES"); } finally { spec.clearPassword(); }
    }
    public synchronized byte[] backup(char[] password) throws Exception {
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        ByteArrayOutputStream b=new ByteArrayOutputStream();b.write(new byte[]{'J','A','B','1'}); b.write(salt);
        byte[] plain=Ledger.backup(state).toString().getBytes(StandardCharsets.UTF_8);
        try { b.write(encrypt(plain,derive(password,salt)));return b.toByteArray(); } finally { Arrays.fill(plain,(byte)0); }
    }
    public void restore(byte[] bytes,char[] password) throws Exception {
        if(bytes.length<48||bytes.length>10*1024*1024||!Arrays.equals(Arrays.copyOf(bytes,4),new byte[]{'J','A','B','1'})) throw new IllegalArgumentException("不是有效备份文件");
        byte[] plain=decrypt(Arrays.copyOfRange(bytes,20,bytes.length),derive(password,Arrays.copyOfRange(bytes,4,20)));
        try {
            JSONObject b=J.parse(new String(plain,StandardCharsets.UTF_8)); if(b.optInt("schema")!=1) throw new IllegalArgumentException("备份版本不兼容");
            JSONObject profile=J.object(b,"profile"),prefs=J.object(b,"prefs"); JSONArray faq=J.array(b,"faq");
            // Import only documented fields; arbitrary backup content cannot change AI host or control bindings.
            JSONObject cleanProfile=J.obj();
            for(String k:MainActivity.PROFILE_FIELDS) if(profile.has(k)) J.put(cleanProfile,k,profile.optString(k));
            if(prefs.optInt("dailyCalls",50)<1||prefs.optInt("dailyCalls",50)>500||prefs.optInt("dailyApplications",10)<1||prefs.optInt("dailyApplications",10)>100||prefs.optInt("intervalSeconds",20)<5) throw new IllegalArgumentException("备份限额不合法");
            JSONArray cleanFaq=new JSONArray();
            for(JSONObject f:J.list(faq)) if(f.optBoolean("approved")&&Arrays.asList(ReplyPolicy.CATEGORIES).contains(f.optString("category"))) cleanFaq.put(J.obj("id",UUID.randomUUID().toString(),"category",f.optString("category"),"question",f.optString("question"),"answer",f.optString("answer"),"approved",true));
            update(s->{ J.put(s,"profile",cleanProfile);J.put(s,"prefs",prefs);J.put(s,"faq",cleanFaq); });
        } finally { Arrays.fill(plain,(byte)0); }
    }
}
