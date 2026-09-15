package cn.local.jobassistant.core;

import org.junit.Test;
import org.json.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

public class SafetyTest {
    @Test public void keyIsMissingInNewProcessObject(){SessionKey k=new SessionKey(()->0L);k.set("synthetic-placeholder".toCharArray());assertFalse(new SessionKey(()->0L).present());}
    @Test public void keyClonesBothDirections(){SessionKey k=new SessionKey(()->0L);char[] source="synthetic-placeholder".toCharArray();k.set(source);source[0]='X';char[] copy=k.copy();copy[0]='Y';assertEquals('s',k.copy()[0]);}
    @Test public void backgroundExpiryIsMonotonic(){AtomicLong t=new AtomicLong(10);SessionKey k=new SessionKey(t::get);k.set("synthetic-placeholder".toCharArray());k.background();t.addAndGet(SessionKey.TTL);assertFalse(k.present());}
    @Test public void shortBackgroundCanContinue(){AtomicLong t=new AtomicLong(10);SessionKey k=new SessionKey(t::get);k.set("synthetic-placeholder".toCharArray());k.background();t.addAndGet(1000);k.foreground();t.addAndGet(SessionKey.TTL);assertTrue(k.present());}
    @Test public void expiryCannotBeResetByForeground(){AtomicLong t=new AtomicLong(10);SessionKey k=new SessionKey(t::get);k.set("synthetic-placeholder".toCharArray());k.background();t.addAndGet(SessionKey.TTL);k.foreground();assertFalse(k.present());}
    @Test public void clearingInvalidatesInflightRequests(){SessionKey k=new SessionKey(()->0L);k.set("synthetic-placeholder".toCharArray());long before=k.generation();k.clear();assertNotEquals(before,k.generation());assertThrows(IllegalStateException.class,k::copy);}
    @Test public void defaultModeNeverSends(){RunGate g=new RunGate();g.start(true);assertFalse(g.maySend(g.epoch(),true,false));}
    @Test public void confirmationAndTakeoverAreEnforced(){RunGate g=new RunGate();g.mode(RunGate.Mode.CONFIRM);g.start(true);assertFalse(g.maySend(g.epoch(),false,false));assertTrue(g.maySend(g.epoch(),true,false));assertFalse(g.maySend(g.epoch(),true,true));}
    @Test public void stopInvalidatesPreviouslyQueuedSend(){RunGate g=new RunGate();g.mode(RunGate.Mode.AUTO);g.start(true);long e=g.epoch();g.halt(RunGate.State.STOPPED);g.start(true);assertFalse(g.maySend(e,true,false));}
    @Test public void noKeyCannotStart(){RunGate g=new RunGate();g.start(false);assertEquals(RunGate.State.KEY_REQUIRED,g.state());}
    @Test public void safetyVerificationAndLoginAreDifferentStates(){assertEquals(RunGate.State.VERIFY,RunGate.obstruction("请完成安全验证"));assertEquals(RunGate.State.VERIFY,RunGate.obstruction("操作频繁"));assertEquals(RunGate.State.LOGIN,RunGate.obstruction("登录已过期"));assertNull(RunGate.obstruction("Java开发岗位"));}
    @Test public void rateLimitsAllRepeatedOperations(){RunGate g=new RunGate();assertTrue(g.rate(0,5000));assertFalse(g.rate(4999,5000));assertTrue(g.rate(5000,5000));}
    @Test public void jobIdentityIgnoresSalary(){JSONObject j=J.obj("company","测试公司","title","Java 开发","city","测试城市","hr","测试HR","salary","5-8K");String a=Ids.job(j);J.put(j,"salary","6-9K");assertEquals(a,Ids.job(j));J.put(j,"hr","另一个HR");assertNotEquals(a,Ids.job(j));}
    @Test public void hashDelimitersCannotCollide(){assertNotEquals(Ids.hash("ab","c"),Ids.hash("a","bc"));assertNotEquals(Ids.hash("a|b","c"),Ids.hash("a","b|c"));}
    @Test public void incompleteJobIdentityDoesNotAutomate(){assertEquals("",Ids.job(J.obj("company","测试公司","title","开发")));}
    @Test public void unknownApplySurvivesCrashAndNeverRetries(){JSONObject s=Ledger.defaults();long now=1700000000000L;assertTrue(Ledger.reserve(s,"a","apply","job",now,10,30));JSONObject restarted=J.parse(s.toString());assertFalse(Ledger.reserve(restarted,"b","apply","job",now+100*86400000L,10,30));}
    @Test public void successDedupWindowExpires(){JSONObject s=Ledger.defaults();long now=1700000000000L;assertTrue(Ledger.reserve(s,"a","apply","job",now,10,30));Ledger.sent(s,"a",now);assertFalse(Ledger.reserve(s,"b","apply","job",now+86400000L,10,30));assertTrue(Ledger.reserve(s,"c","apply","job",now+31*86400000L,10,30));}
    @Test public void uncertainApplyConsumesDailyLimit(){JSONObject s=Ledger.defaults();assertTrue(Ledger.reserve(s,"a","apply","one",1000,1,30));assertFalse(Ledger.reserve(s,"b","apply","two",2000,1,30));}
    @Test public void messageTaskIsUniqueAcrossRestart(){JSONObject s=Ledger.defaults();assertTrue(Ledger.reserve(s,"m","message","hr",1000,0,30));assertFalse(Ledger.reserve(J.parse(s.toString()),"m","message","hr",2000,0,30));}
    @Test public void failedAiCallStillConsumesBudget(){JSONObject s=Ledger.defaults();J.put(J.object(s,"prefs"),"dailyCalls",1);Ledger.reserveAi(s,1000);assertThrows(IllegalStateException.class,()->Ledger.reserveAi(s,2000));}
    @Test public void tokenBudgetStopsAdditionalRequests(){JSONObject s=Ledger.defaults();J.put(J.object(s,"prefs"),"dailyTokens",100);Ledger.account(s,1000,70,31);assertThrows(IllegalStateException.class,()->Ledger.reserveAi(s,1001));}
    @Test public void backupIsAnExplicitAllowlist(){JSONObject s=Ledger.defaults();J.put(s,"apiKey","synthetic");J.put(s,"cookie","synthetic");J.put(s,"session","synthetic");JSONObject b=Ledger.backup(s);assertEquals(4,b.length());for(String key:new String[]{"apiKey","cookie","session","chats","resumes","outbox","ai","bindings"})assertFalse(b.has(key));}
    @Test public void endpointRejectsCredentialAndCleartextTraps(){for(String s:new String[]{"http://example.com","https://a:password@example.com","https://example.com?key=secret","https://127.0.0.1","https://localhost","https://example.com#secret"})assertThrows(IllegalArgumentException.class,()->Privacy.endpoint(s));assertEquals("api.deepseek.com",Privacy.endpoint("https://api.deepseek.com/v1").getHost());}
    @Test public void secretsAreRemovedBeforeContextUse(){String marker="sk-"+"synthetic_test_value";assertFalse(Privacy.redact("Authorization: Bearer "+marker).contains(marker));assertFalse(Privacy.redact("cookie=synthetic_session").contains("synthetic_session"));assertFalse(Privacy.redact("110101199001011234").contains("110101199001011234"));}
    @Test public void aiCannotInventAProfileAnswer(){ReplyPolicy.Decision d=ReplyPolicy.decide("最快什么时候到岗？",J.obj(),"next-monday",false,false);assertTrue(d.holding());assertFalse(d.text().contains("周一"));}
    @Test public void followupDoesNotLoopHoldingMessages(){ReplyPolicy.Decision d=ReplyPolicy.decide("还没确认好吗？",J.obj(),"",true,false);assertFalse(d.allowed());assertEquals("",d.text());assertTrue(d.notifyUser());}
    @Test public void sensitiveRequestsOverrideAvailableFacts(){JSONObject facts=J.obj("x",J.obj("category","敏感资料","answer","不可发送"));ReplyPolicy.Decision d=ReplyPolicy.decide("可以提供身份证信息吗？",facts,"x",false,false);assertTrue(d.holding());assertTrue(d.notifyUser());assertFalse(d.text().contains("不可发送"));}
    @Test public void takeoverPreventsEveryReply(){assertFalse(ReplyPolicy.decide("方便聊聊吗？",J.obj(),"",false,true).allowed());}
    @Test public void compoundQuestionCannotGetPartialAutomaticAnswer(){JSONObject facts=J.obj("salary",J.obj("category","薪资","answer","期望月薪八千元"));assertTrue(ReplyPolicy.decide("期望工资多少？什么时候面试？",facts,"salary",false,false).holding());}
    @Test public void upstreamActivityRulesRemainCompatible(){String[] text={"刚刚活跃","今日活跃","3日内活跃","本周活跃","本月活跃","半年前活跃","活跃",""};int[] expected={100,80,60,40,30,20,10,0};for(int i=0;i<text.length;i++)assertEquals(expected[i],JobRules.hrActivity(text[i]));}
    @Test public void salaryRangeAndBlacklistFilterBeforeAi(){JSONObject j=J.obj("title","开发","company","测试公司","jd","Java","salary","5-8K");assertFalse(JobRules.reject(j,J.obj("blackCompanies","测试公司")).isBlank());assertFalse(JobRules.reject(j,J.obj("minSalary",9000)).isBlank());assertEquals("",JobRules.reject(j,J.obj("minSalary",6000)));J.put(j,"salary","200元/天");assertFalse(JobRules.reject(j,J.obj("minSalary",6000)).isBlank());}
}
