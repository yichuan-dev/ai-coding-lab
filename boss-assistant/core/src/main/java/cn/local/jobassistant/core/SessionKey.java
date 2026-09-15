package cn.local.jobassistant.core;

import java.util.Arrays;
import java.util.function.LongSupplier;

/** Process memory only. This type deliberately has no serialization or storage API. */
public final class SessionKey {
    public static final long TTL = 30*60*1000L;
    private final LongSupplier clock;
    private char[] key;
    private long backgroundAt=-1, generation;
    public SessionKey(LongSupplier clock) { this.clock=clock; }
    public synchronized void set(char[] value) { clear(); if(value.length<8||value.length>512) throw new IllegalArgumentException("请检查 API Key"); key=value.clone(); }
    public synchronized void clear() { if(key!=null) Arrays.fill(key,'\0'); key=null; backgroundAt=-1; generation++; }
    private void expire() { if(backgroundAt>=0 && clock.getAsLong()-backgroundAt>=TTL) clear(); }
    public synchronized boolean present() { expire(); return key!=null; }
    public synchronized char[] copy() { expire(); if(key==null) throw new IllegalStateException("请重新输入 API Key"); return key.clone(); }
    public synchronized long generation() { expire(); return generation; }
    public synchronized void background() { if(backgroundAt<0) backgroundAt=clock.getAsLong(); }
    public synchronized void foreground() { expire(); backgroundAt=-1; }
}
