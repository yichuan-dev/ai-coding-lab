package cn.local.jobassistant.core;

public final class RunGate {
    public enum Mode { TEST, CONFIRM, AUTO }
    public enum State { STOPPED, RUNNING, PAUSED, VERIFY, LOGIN, KEY_REQUIRED, UI_CHANGED, WAITING, SCREEN_HIDDEN }
    private Mode mode=Mode.TEST;
    private State state=State.STOPPED;
    private long epoch,lastAction=Long.MIN_VALUE;
    public synchronized void mode(Mode m) { state=State.STOPPED; mode=m; epoch++; }
    public synchronized Mode mode() { return mode; }
    public synchronized State state() { return state; }
    public synchronized long epoch() { return epoch; }
    public synchronized void start(boolean key) { state=key?State.RUNNING:State.KEY_REQUIRED; epoch++; }
    public synchronized void halt(State s) { state=s; epoch++; }
    public synchronized boolean valid(long e) { return state==State.RUNNING && e==epoch; }
    public synchronized boolean maySend(long e,boolean confirmed,boolean takeover) { return valid(e) && mode!=Mode.TEST && (mode==Mode.AUTO||confirmed) && !takeover; }
    public synchronized boolean rate(long now,long interval) { if(lastAction!=Long.MIN_VALUE && now-lastAction<Math.max(5000,interval)) return false; lastAction=now; return true; }
    public static State obstruction(String text) {
        if(text.matches("(?s).*(验证码|滑块|安全验证|人机验证|异常设备|请完成验证|操作频繁|账号异常|禁止自动化).*")) return State.VERIFY;
        if(text.matches("(?s).*(登录失效|重新登录|登录已过期|短信登录|扫码登录).*")) return State.LOGIN;
        return null;
    }
}
