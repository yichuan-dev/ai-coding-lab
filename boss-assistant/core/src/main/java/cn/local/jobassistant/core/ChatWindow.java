package cn.local.jobassistant.core;

import java.util.*;
import org.json.*;

/** Derives a turn only from explicitly classified, ordered visible message bubbles. */
public final class ChatWindow {
    public record Bubble(boolean incoming,String text) {}
    public static List<String> incomingTurn(List<Bubble> ordered) {
        List<String> turn=new ArrayList<>();
        for(Bubble bubble:ordered) {
            if(bubble.text()==null||bubble.text().isBlank())continue;
            if(bubble.incoming())turn.add(bubble.text().trim());else turn.clear();
        }
        return turn;
    }
    public static boolean seen(JSONObject chat,String fingerprint,String question) {
        if(fingerprint.equals(chat.optString("messageId")))return true;
        JSONArray seen=J.array(chat,"seenMessageIds");for(int i=0;i<seen.length();i++)if(fingerprint.equals(seen.optString(i)))return true;
        // Scrolling can change the window hash. Previously answered identical text needs manual review.
        for(JSONObject audit:J.list(J.array(chat,"audit")))if(question.equals(audit.optString("question")))return true;
        return false;
    }
    public static void remember(JSONObject chat,String fingerprint) {
        JSONArray ids=J.array(chat,"seenMessageIds");ids.put(fingerprint);J.put(chat,"seenMessageIds",ids);
    }
}
