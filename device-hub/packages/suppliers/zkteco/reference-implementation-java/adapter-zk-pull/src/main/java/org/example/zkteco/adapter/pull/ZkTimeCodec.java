package org.example.zkteco.adapter.pull;

import java.time.LocalDateTime;

public final class ZkTimeCodec {
    private ZkTimeCodec() {}

    public static LocalDateTime decode(long value) {
        long second = value % 60; value /= 60;
        long minute = value % 60; value /= 60;
        long hour = value % 24; value /= 24;
        long day = value % 31 + 1; value /= 31;
        long month = value % 12 + 1; value /= 12;
        long year = value + 2000;
        return LocalDateTime.of((int) year, (int) month, (int) day, (int) hour, (int) minute, (int) second);
    }

    public static long encode(LocalDateTime dt) {
        long value = dt.getYear() - 2000L;
        value = value * 12 + (dt.getMonthValue() - 1);
        value = value * 31 + (dt.getDayOfMonth() - 1);
        value = value * 24 + dt.getHour();
        value = value * 60 + dt.getMinute();
        value = value * 60 + dt.getSecond();
        return value;
    }
}
