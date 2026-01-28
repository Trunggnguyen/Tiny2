package android.server.power.nextapp;

public final class NextAppContext {
    public final int timeBucket;     // 0..3/5
    public final boolean screenOn;
    public final int netType;        // 0 none, 1 cell, 2 wifi
    public final int launchType;     // 0 unk, 1 taskSwitch, 2 deepLink, 3 share, 4 notif...
    public final int batteryBucket;  // 0..3
    public final boolean maxPowerMode;

    public NextAppContext(int timeBucket, boolean screenOn, int netType, int launchType,
                          int batteryBucket, boolean maxPowerMode) {
        this.timeBucket = timeBucket;
        this.screenOn = screenOn;
        this.netType = netType;
        this.launchType = launchType;
        this.batteryBucket = batteryBucket;
        this.maxPowerMode = maxPowerMode;
    }
}
