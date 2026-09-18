package cn.local.jobassistant.core;

import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class RecoveryTest {
    @Test public void burstCollectsOnlyIncomingAfterLastSelf(){List<ChatWindow.Bubble> messages=List.of(new ChatWindow.Bubble(true,"旧问题"),new ChatWindow.Bubble(false,"旧回答"),new ChatWindow.Bubble(true,"新问题一"),new ChatWindow.Bubble(true,"新问题二"));assertEquals(List.of("新问题一","新问题二"),ChatWindow.incomingTurn(messages));}
    @Test public void outgoingAcknowledgementIsNotANewIncomingTurn(){assertTrue(ChatWindow.incomingTurn(List.of(new ChatWindow.Bubble(true,"什么时候到岗"),new ChatWindow.Bubble(false,"我确认一下"))).isEmpty());}
    @Test public void scrollingCannotReplayAPreviousWindow(){JSONObject c=J.obj();ChatWindow.remember(c,"fingerprint-one");J.put(c,"messageId","fingerprint-two");assertTrue(ChatWindow.seen(J.parse(c.toString()),"fingerprint-one","旧问题"));}
    @Test public void answeredIdenticalTextRequiresManualReviewEvenIfWindowChanges(){JSONObject c=J.obj("audit",new JSONArray().put(J.obj("question","在吗")));assertTrue(ChatWindow.seen(c,"different-window","在吗"));}
    @Test public void backupPreferencesDiscardUnknownFieldsAndSendConsent(){JSONObject input=J.obj("apiKey","synthetic","cookie","synthetic","allowBossGreeting",true);JSONObject c=Preferences.clean(input,true);assertFalse(c.has("apiKey"));assertFalse(c.has("cookie"));assertFalse(c.optBoolean("allowBossGreeting"));assertEquals(20,c.optInt("intervalSeconds"));}
    @Test public void invalidBackupCannotRemoveRateAndDedupLimits(){for(JSONObject invalid:List.of(J.obj("intervalSeconds",0),J.obj("dedupDays",0),J.obj("threshold",101),J.obj("dailyCalls",500.5),J.obj("minSalary",10000,"maxSalary",5000),J.obj("dailyCost","NaN")))assertThrows(IllegalArgumentException.class,()->Preferences.clean(invalid,true));}
}
