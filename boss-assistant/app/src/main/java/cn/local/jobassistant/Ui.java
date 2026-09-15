package cn.local.jobassistant;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.widget.*;

final class Ui {
    static final int INK=Color.rgb(24,47,49),TEAL=Color.rgb(8,127,118),BG=Color.rgb(244,248,247),MUTED=Color.rgb(83,109,109);
    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static TextView text(Context c,String s,int size,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(c,5),0,dp(c,5));return t;}
    static TextView title(Context c,String s){TextView t=text(c,s,23,INK);t.setTypeface(null,Typeface.BOLD);return t;}
    static LinearLayout card(Context c,LinearLayout parent,String title){LinearLayout l=column(c);l.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,14));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(c,16));l.setBackground(bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,8),0,dp(c,8));parent.addView(l,p);TextView t=text(c,title,18,INK);t.setTypeface(null,Typeface.BOLD);l.addView(t);return l;}
    static Button button(Context c,LinearLayout parent,String label,Runnable action){Button b=new Button(c);b.setText(label);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(TEAL);b.setMinHeight(dp(c,44));b.setOnClickListener(v->action.run());parent.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;}
    static EditText field(Context c,LinearLayout parent,String label,String value,boolean multiline){parent.addView(text(c,label,13,MUTED));EditText e=new EditText(c);e.setTextSize(16);e.setTextColor(INK);e.setText(value);e.setSingleLine(!multiline);e.setInputType(InputType.TYPE_CLASS_TEXT|(multiline?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0)|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);e.setSaveEnabled(false);e.setFreezesText(false);e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);e.setImeOptions(e.getImeOptions()|0x1000000);if(multiline)e.setMinLines(3);parent.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
}
