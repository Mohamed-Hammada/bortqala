package org.example.zkteco.adapter.pull;

public record ZkPacket(int command, int checksum, int sessionId, int replyId, byte[] data) {
    public ZkPacket {
        data = data == null ? new byte[0] : data.clone();
    }
    @Override public byte[] data() { return data.clone(); }
}
