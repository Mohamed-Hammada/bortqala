package org.example.zkteco.adapter.pull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class ZkPacketCodec {
    public static final byte[] TCP_MAGIC = {(byte)0x50,(byte)0x50,(byte)0x82,(byte)0x7d};
    private ZkPacketCodec() {}

    public static byte[] encodeUdp(int command, int sessionId, int replyId, byte[] payload) {
        byte[] data = payload == null ? new byte[0] : payload;
        ByteBuffer b = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short) command).putShort((short) 0).putShort((short) sessionId).putShort((short) replyId).put(data);
        byte[] packet = b.array();
        int checksum = checksum(packet);
        ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).putShort(2, (short) checksum);
        return packet;
    }

    public static byte[] wrapTcp(byte[] udpPacket) {
        ByteBuffer b = ByteBuffer.allocate(8 + udpPacket.length).order(ByteOrder.LITTLE_ENDIAN);
        b.put(TCP_MAGIC).putInt(udpPacket.length).put(udpPacket);
        return b.array();
    }

    public static ZkPacket decodeUdp(byte[] packet) {
        if (packet == null || packet.length < 8) throw new IllegalArgumentException("packet shorter than 8 bytes");
        ByteBuffer b = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        int command = Short.toUnsignedInt(b.getShort());
        int checksum = Short.toUnsignedInt(b.getShort());
        int sessionId = Short.toUnsignedInt(b.getShort());
        int replyId = Short.toUnsignedInt(b.getShort());
        return new ZkPacket(command, checksum, sessionId, replyId, Arrays.copyOfRange(packet,8,packet.length));
    }

    public static byte[] unwrapTcp(byte[] packet) {
        if (packet.length < 8) throw new IllegalArgumentException("TCP packet shorter than wrapper");
        for (int i=0;i<4;i++) if (packet[i] != TCP_MAGIC[i]) throw new IllegalArgumentException("invalid TCP magic");
        ByteBuffer b = ByteBuffer.wrap(packet,4,4).order(ByteOrder.LITTLE_ENDIAN);
        int length = b.getInt();
        if (length < 8 || length > packet.length - 8) throw new IllegalArgumentException("invalid TCP payload length " + length);
        return Arrays.copyOfRange(packet,8,8+length);
    }

    /** 16-bit one's-complement checksum used by classic ZK packets. */
    public static int checksum(byte[] bytes) {
        long sum = 0;
        int i = 0;
        while (i + 1 < bytes.length) {
            int word = (bytes[i] & 0xff) | ((bytes[i+1] & 0xff) << 8);
            sum += word;
            if (sum > 0xffff) sum -= 0xffff;
            i += 2;
        }
        if (i < bytes.length) sum += bytes[i] & 0xff;
        while (sum > 0xffff) sum = (sum & 0xffff) + (sum >>> 16);
        return (int)(~sum) & 0xffff;
    }
}
