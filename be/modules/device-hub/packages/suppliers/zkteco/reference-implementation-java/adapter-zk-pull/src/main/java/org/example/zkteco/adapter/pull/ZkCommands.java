package org.example.zkteco.adapter.pull;

/** Stable command identifiers used by the classic ZK communication protocol. */
public final class ZkCommands {
    private ZkCommands() {}
    public static final int CONNECT = 1000;
    public static final int EXIT = 1001;
    public static final int ENABLE_DEVICE = 1002;
    public static final int DISABLE_DEVICE = 1003;
    public static final int RESTART = 1004;
    public static final int POWER_OFF = 1005;
    public static final int AUTH = 1102;
    public static final int GET_VERSION = 1100;
    public static final int OPTIONS_READ = 11;
    public static final int OPTIONS_WRITE = 12;
    public static final int ATTENDANCE_LOG_READ = 13;
    public static final int CLEAR_ATTENDANCE_LOG = 15;
    public static final int UNLOCK = 31;
    public static final int GET_FREE_SIZES = 50;
    public static final int GET_TIME = 201;
    public static final int SET_TIME = 202;
    public static final int REGISTER_EVENT = 500;
    public static final int ACK_OK = 2000;
    public static final int ACK_ERROR = 2001;
    public static final int ACK_DATA = 2002;
    public static final int ACK_UNAUTH = 2005;
}
