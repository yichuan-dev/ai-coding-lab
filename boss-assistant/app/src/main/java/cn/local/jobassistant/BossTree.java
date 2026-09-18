package cn.local.jobassistant;

import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Bundle;
import java.util.*;
import cn.local.jobassistant.core.ChatWindow;

/** Only stable resource IDs (optionally constrained by an ancestor ID); no coordinates. */
public final class BossTree implements AutoCloseable {
    private final List<AccessibilityNodeInfo> nodes=new ArrayList<>();
    BossTree(AccessibilityNodeInfo root){walk(root,0);}
    private void walk(AccessibilityNodeInfo n,int depth){if(n==null)return;if(nodes.size()>=1500||depth>40){n.recycle();return;}nodes.add(n);for(int i=0;i<n.getChildCount();i++)walk(n.getChild(i),depth+1);}
    static String text(AccessibilityNodeInfo n){return n==null||n.getText()==null?"":n.getText().toString().trim();}
    String allText(){StringBuilder s=new StringBuilder();for(AccessibilityNodeInfo n:nodes)if(n.isVisibleToUser())s.append(text(n)).append('\n');return s.toString();}
    List<AccessibilityNodeInfo> find(String selector){
        List<AccessibilityNodeInfo> r=new ArrayList<>();if(selector==null||selector.isBlank())return r;String[] parts=selector.split("\\|",2);
        for(AccessibilityNodeInfo n:nodes){if(!n.isVisibleToUser()||!parts[0].equals(n.getViewIdResourceName()))continue;
            if(parts.length==1){r.add(n);continue;}
            AccessibilityNodeInfo p=n.getParent();int depth=0;boolean matched=false;
            while(p!=null && depth++<12){if(parts[1].equals(p.getViewIdResourceName()))matched=true;AccessibilityNodeInfo next=p.getParent();p.recycle();p=next;if(matched)break;}if(p!=null)p.recycle();if(matched)r.add(n);
        }return r;
    }
    AccessibilityNodeInfo one(String id){List<AccessibilityNodeInfo> a=find(id);return a.size()==1?a.get(0):null;}
    String value(String id){return text(one(id));}
    List<String> values(String id){List<String>a=new ArrayList<>();for(AccessibilityNodeInfo n:find(id)){String t=text(n);if(!t.isBlank())a.add(t);}return a;}
    List<ChatWindow.Bubble> messages(String incoming,String outgoing){
        List<AccessibilityNodeInfo> hr=find(incoming),self=find(outgoing);
        List<ChatWindow.Bubble> result=new ArrayList<>();
        for(AccessibilityNodeInfo n:nodes){boolean a=hr.contains(n),b=self.contains(n);if(a&&b)throw new IllegalArgumentException("消息气泡方向不明确");if(a||b)result.add(new ChatWindow.Bubble(a,text(n)));}
        return result;
    }
    static boolean click(AccessibilityNodeInfo n){if(n==null||!n.isEnabled())return false;if(n.isClickable())return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);AccessibilityNodeInfo p=n.getParent();try{return p!=null&&p.isClickable()&&p.isEnabled()&&p.performAction(AccessibilityNodeInfo.ACTION_CLICK);}finally{if(p!=null)p.recycle();}}
    static boolean enter(AccessibilityNodeInfo n,String text){if(n==null||!n.isEditable()||!n.isEnabled())return false;Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b);}
    List<String> inventory(){TreeSet<String> s=new TreeSet<>();for(AccessibilityNodeInfo n:nodes){String id=n.getViewIdResourceName();if(id==null||!n.isVisibleToUser())continue;s.add(id);AccessibilityNodeInfo p=n.getParent();try{if(p!=null&&p.getViewIdResourceName()!=null)s.add(id+"|"+p.getViewIdResourceName());}finally{if(p!=null)p.recycle();}}return new ArrayList<>(s);}
    @Override public void close(){for(AccessibilityNodeInfo n:nodes)n.recycle();}
}
