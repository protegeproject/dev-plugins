package edu.stanford.smi.protegex.monitor;

import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protege.util.Log;

public class PluginProperties {
    public static final String ENABLE_SERVER_PING="monitor.load.ping.enable";
    public static final boolean DEFAULT_SERVER_PING_ENABLED=false;
    private static boolean server_ping_enabled;
    
    public static final String IDLE_TIMEOUT_PROPERTY="monitor.load.idle_timeout";
    public static final long DEFAULT_IDLE_TIMEOUT = 100;
    private static Long idle_timeout;
    
    public static final String SERVER_PING_INTERVAL_PROPERTY = "monitor.load.server_ping_interval";
    public static final long DEFAULT_PING_INTERVAL = 1000;
    private static Long ping_interval;
    
    public static final String LATE_PING_PROPERTY = "monitor.load.server_late_ping";
    public static final long DEFAULT_LATE_PING = 500;
    private static Long late_ping;
    
    public static long getIdleTimeout() {
        if (idle_timeout == null) {
            idle_timeout = getLong(IDLE_TIMEOUT_PROPERTY, DEFAULT_IDLE_TIMEOUT);
        }
        return idle_timeout;       
    }
    
    
    public static long getServerPingInterval() {
        if (ping_interval == null) {
            ping_interval = getLong(SERVER_PING_INTERVAL_PROPERTY, DEFAULT_PING_INTERVAL);
        }
        return ping_interval;
    }
    
    public static long getLatePingTimeout() {
        if (late_ping == null) {
            late_ping = getLong(LATE_PING_PROPERTY, DEFAULT_LATE_PING);
        }
        return late_ping;
    }
    
    public static boolean getServerPingEnabled() {
        String value = ApplicationProperties.getApplicationOrSystemProperty(ENABLE_SERVER_PING);
        if (value != null) {
            return new Boolean(value);
        }
        return DEFAULT_SERVER_PING_ENABLED;
    }

    private static Long getLong(String property, long defaultValue) {
        try {
            String value = 
                ApplicationProperties.getApplicationOrSystemProperty(IDLE_TIMEOUT_PROPERTY);
            if (!(value == null || value.equals(""))) {
                return new Long(value);
            }
        } catch (NumberFormatException nfe) {
            Log.emptyCatchBlock(nfe);
        }
        return defaultValue;
    }
}
