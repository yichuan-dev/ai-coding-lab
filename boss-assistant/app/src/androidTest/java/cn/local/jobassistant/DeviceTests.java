package cn.local.jobassistant;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import cn.local.jobassistant.core.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

/** Runs against a separate preview package and synthetic fixtures, never a user's live BOSS account. */
public final class DeviceTests extends Instrumentation {
    private AssistantApp app;
    private static volatile int responseStatus=200;
    private static volatile String responseBody="";
    private static volatile String auth="";
    private Bundle arguments;
    private static final String FIXTURE_KEY="synthetic-api-credential-for-instrumentation";
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);this.arguments=arguments;start();}
    @Override public void onStart(){
        app=(AssistantApp)getTargetContext().getApplicationContext();
        if(arguments!=null && "true".equals(arguments.getString("seedMemory"))){
            MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(()->{app.key.set(FIXTURE_KEY.toCharArray());activity.show("home");});waitForIdleSync();
            Bundle result=new Bundle();result.putString("stream","SYNTHETIC_KEY_PRESENT_IN_PROCESS="+app.key.present()+"\n");
            finish(Activity.RESULT_OK,result);return;
        }
        URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){return new FakeConnection(u);}}:null);
        List<Method> tests=new ArrayList<>();for(Method m:getClass().getDeclaredMethods())if(m.getName().startsWith("test"))tests.add(m);tests.sort(Comparator.comparing(Method::getName));
        StringBuilder report=new StringBuilder();int failed=0,index=0;
        for(Method test:tests){
            Bundle status=new Bundle();status.putString("class",getClass().getName());status.putString("test",test.getName());status.putInt("numtests",tests.size());status.putInt("current",++index);status.putString("id","InstrumentationTestRunner");sendStatus(1,status);
            try{reset();test.invoke(this);report.append("PASS ").append(test.getName()).append('\n');status.putString("stream","PASS "+test.getName()+"\n");sendStatus(0,status);}
            catch(Throwable e){failed++;Throwable cause=e instanceof InvocationTargetException?e.getCause():e;report.append("FAIL ").append(test.getName()).append(" ").append(cause.getClass().getSimpleName()).append(" ").append(Privacy.redact(String.valueOf(cause.getMessage()))).append('\n');status.putString("stack",cause.getClass().getSimpleName()+": "+Privacy.redact(String.valueOf(cause.getMessage())));sendStatus(-2,status);}
        }
        report.append("TOTAL ").append(tests.size()).append(" FAILED ").append(failed).append('\n');
        try{Files.write(new File(app.getFilesDir(),"device-test-results.txt").toPath(),report.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}
        app.key.clear();Bundle result=new Bundle();result.putString("stream",report.toString());result.putInt("failed",failed);finish(failed==0?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
    private void reset(){app.key.clear();app.gate.halt(RunGate.State.STOPPED);Vault.erase(app);app.vault=new Vault(app);responseStatus=200;responseBody=response("{\"ok\":true}");auth="";}
    private static String response(String text){return J.obj("choices",new JSONArray().put(J.obj("message",J.obj("content",text))),"usage",J.obj("prompt_tokens",10,"completion_tokens",5)).toString();}
    private static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
    public void testEncryptedVaultRoundTripAndTamper()throws Exception{
        String personal="synthetic-resume-private-marker";app.vault.update(s->J.put(J.object(s,"profile"),"技能",personal));
        byte[] file=Files.readAllBytes(new File(app.getNoBackupFilesDir(),"vault.enc").toPath());check(!new String(file,StandardCharsets.ISO_8859_1).contains(personal),"plaintext in vault");check(new Vault(app).read().getJSONObject("profile").getString("技能").equals(personal),"round trip");
        file[file.length-1]^=1;Files.write(new File(app.getNoBackupFilesDir(),"vault.enc").toPath(),file);boolean rejected=false;try{new Vault(app);}catch(IllegalStateException e){rejected=true;}check(rejected,"tamper accepted");check(Arrays.equals(file,Files.readAllBytes(new File(app.getNoBackupFilesDir(),"vault.enc").toPath())),"corrupt original overwritten");
    }
    public void testKeyRequestAndNoPersistence()throws Exception{
        app.key.set(FIXTURE_KEY.toCharArray());JSONObject result=app.ai.json("返回 JSON",J.obj("test","synthetic"));check(result.optBoolean("ok"),"HTTP response");check(auth.equals("Bearer "+FIXTURE_KEY),"memory key not transmitted correctly");
        check(!app.vault.read().toString().contains(FIXTURE_KEY),"key in database");check(!new String(app.vault.backup("test-password-only".toCharArray()),StandardCharsets.ISO_8859_1).contains(FIXTURE_KEY),"key in backup");
        for(File directory:new File[]{app.getFilesDir(),app.getNoBackupFilesDir()})scan(directory);
        check(Ledger.usage(app.vault.read(),System.currentTimeMillis()).optInt("calls")==1,"AI count");check(Ledger.usage(app.vault.read(),System.currentTimeMillis()).optInt("tokens")==15,"tokens");app.key.clear();check(!app.key.present(),"key survived clearing");
    }
    private void scan(File file)throws Exception{if(file.isDirectory()){File[] children=file.listFiles();if(children!=null)for(File f:children)scan(f);}else if(file.length()<10*1024*1024)check(!new String(Files.readAllBytes(file.toPath()),StandardCharsets.ISO_8859_1).contains(FIXTURE_KEY),"credential persisted to file");}
    public void testFreshActivityRejectsAnOldProcessKey()throws Exception{
        app.key.set(FIXTURE_KEY.toCharArray());app.gate.start(true);
        MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        check(!app.key.present(),"fresh launch reused old process key");check(app.gate.state()==RunGate.State.STOPPED,"fresh launch retained send authority");
        runOnMainSync(()->{app.key.set(FIXTURE_KEY.toCharArray());activity.finishAndRemoveTask();});waitForIdleSync();
        // Task removal is asynchronous; an idle main queue does not mean onDestroy ran yet.
        long deadline=SystemClock.uptimeMillis()+15000;
        while(!activity.isDestroyed()&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(50);
        check(activity.isDestroyed(),"task removal did not destroy activity");runOnMainSync(()->{});
        check(!app.key.present(),"finishing task retained key without running service");
    }
    public void testHttpFailuresAndTimeoutAreHandled()throws Exception{
        app.key.set(FIXTURE_KEY.toCharArray());for(int status:new int[]{401,403,402,429,500,0}){responseStatus=status;responseBody=FIXTURE_KEY;boolean failed=false;try{app.ai.json("返回 JSON",J.obj());}catch(Exception e){failed=true;check(!String.valueOf(e.getMessage()).contains(FIXTURE_KEY),"error revealed key");}check(failed,"HTTP error accepted");}
    }
    public void testMalformedAiCannotBecomeAnAnswer()throws Exception{
        app.key.set(FIXTURE_KEY.toCharArray());responseBody=response("not JSON; fabricated candidate history");boolean rejected=false;try{app.ai.json("返回 JSON",J.obj());}catch(IOException e){rejected=true;}check(rejected,"invalid AI JSON accepted");
    }
    public void testMessageHistoryAndImportPolicyOnAndroid()throws Exception{
        JSONObject c=J.obj();ChatWindow.remember(c,"seen-on-device");check(ChatWindow.seen(J.parse(c.toString()),"seen-on-device","synthetic question"),"Android JSONArray compatibility");
        JSONObject p=Preferences.clean(J.obj("allowBossGreeting",true,"apiKey","synthetic"),true);check(!p.optBoolean("allowBossGreeting")&&!p.has("apiKey"),"backup import policy");
    }
    public void testClearingDataRejectsAnOlderImport()throws Exception{
        long version=app.dataVersion;app.clearLocalData();boolean cancelled=false;
        try{app.withData(version,v->v.update(s->J.put(J.object(s,"profile"),"技能","old-import-must-not-return")));}catch(IOException e){cancelled=true;}
        check(cancelled,"old data operation was not cancelled");check(J.object(app.vault.read(),"profile").length()==0,"old private data restored after erase");
    }
    public void testEncryptedBackupAllowlistAndWrongPassword()throws Exception{
        app.vault.update(s->{J.put(J.object(s,"profile"),"技能","synthetic Java basics");J.array(s,"resumes").put(J.obj("file","private-file-marker"));J.array(s,"chats").put(J.obj("text","private-chat-marker"));});byte[] backup=app.vault.backup("test-password-only".toCharArray());
        boolean wrong=false;try{app.vault.restore(backup,"wrong-password-only".toCharArray());}catch(Exception e){wrong=true;}check(wrong,"wrong password accepted");Vault.erase(app);app.vault=new Vault(app);app.vault.restore(backup,"test-password-only".toCharArray());check(J.object(app.vault.read(),"profile").optString("技能").equals("synthetic Java basics"),"profile not restored");check(J.array(app.vault.read(),"resumes").length()==0,"resume in backup");check(J.array(app.vault.read(),"chats").length()==0,"chat in backup");
    }
    private static byte[] docx(String xml)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream out=new ZipOutputStream(bytes)){out.putNextEntry(new ZipEntry("word/document.xml"));out.write(xml.getBytes(StandardCharsets.UTF_8));out.closeEntry();}return bytes.toByteArray();}
    public void testDocxParsingAndExternalEntityRejection()throws Exception{
        byte[] document=docx("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>合成简历 Java 课程项目</w:t></w:r></w:p></w:body></w:document>");check(ResumeReader.parse(app,"synthetic.docx",document).contains("Java 课程项目"),"DOCX text");boolean rejected=false;try{ResumeReader.parse(app,"bad.docx",docx("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///not-a-real-secret'>]><x>&e;</x>"));}catch(IOException e){rejected=true;}check(rejected,"external entity accepted");
    }
    public void testPdfTextExtraction()throws Exception{
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(app);byte[] bytes;
        try(com.tom_roush.pdfbox.pdmodel.PDDocument d=new com.tom_roush.pdfbox.pdmodel.PDDocument()){
            com.tom_roush.pdfbox.pdmodel.PDPage page=new com.tom_roush.pdfbox.pdmodel.PDPage();d.addPage(page);
            try(com.tom_roush.pdfbox.pdmodel.PDPageContentStream c=new com.tom_roush.pdfbox.pdmodel.PDPageContentStream(d,page)){c.beginText();c.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA,12);c.newLineAtOffset(30,700);c.showText("Synthetic resume Java basics");c.endText();}
            ByteArrayOutputStream out=new ByteArrayOutputStream();d.save(out);bytes=out.toByteArray();
        }check(ResumeReader.parse(app,"synthetic.pdf",bytes).contains("Java basics"),"PDF extraction");
    }
    public void testAndroidScreensAndSecureWindow()throws Exception{
        MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        for(String page:new String[]{"key","home","jobs","chats","resumes","profile","faq","settings","bindings","backup","privacy"}){
            runOnMainSync(()->activity.show(page));waitForIdleSync();check((activity.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"secure flag absent");
            runOnMainSync(()->{try{View view=activity.getWindow().getDecorView();check(view.getWidth()>0&&view.getHeight()>0,"empty page");Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));File dir=new File(app.getFilesDir(),"test-screens");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,page+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}catch(Exception e){throw new RuntimeException(e);}});
        }
        runOnMainSync(activity::finish);waitForIdleSync();
    }
    private static final class FakeConnection extends HttpURLConnection{
        FakeConnection(URL url){super(url);}
        @Override public void disconnect(){}@Override public boolean usingProxy(){return false;}@Override public void connect(){}
        @Override public void setRequestProperty(String key,String value){if(key.equals("Authorization"))auth=value;}
        @Override public OutputStream getOutputStream(){return new ByteArrayOutputStream();}
        @Override public int getResponseCode()throws IOException{if(responseStatus==0)throw new SocketTimeoutException();return responseStatus;}
        @Override public InputStream getInputStream(){return new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));}
    }
}
