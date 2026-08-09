package org.example.zkteco.adapter.pull;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

final class ZkPullTransport implements AutoCloseable {
    enum Mode { TCP, UDP }
    private final Mode mode;
    private final String host;
    private final int port;
    private final int timeoutMs;
    private Socket tcp;
    private DatagramSocket udp;

    ZkPullTransport(Mode mode, String host, int port, int timeoutMs) {
        this.mode=mode; this.host=host; this.port=port; this.timeoutMs=timeoutMs;
    }
    void open() throws IOException {
        if (mode == Mode.TCP) {
            tcp = new Socket();
            tcp.connect(new InetSocketAddress(host,port), timeoutMs);
            tcp.setSoTimeout(timeoutMs);
        } else {
            udp = new DatagramSocket();
            udp.connect(InetAddress.getByName(host), port);
            udp.setSoTimeout(timeoutMs);
        }
    }
    byte[] exchange(byte[] rawUdpPacket) throws IOException {
        if (mode == Mode.TCP) {
            byte[] wrapped=ZkPacketCodec.wrapTcp(rawUdpPacket);
            OutputStream out=tcp.getOutputStream(); out.write(wrapped); out.flush();
            InputStream in=tcp.getInputStream();
            byte[] h=in.readNBytes(8);
            if (h.length != 8) throw new EOFException("short TCP wrapper");
            for (int i=0;i<4;i++) if (h[i] != ZkPacketCodec.TCP_MAGIC[i]) throw new IOException("invalid ZK TCP magic");
            int len=ByteBuffer.wrap(h,4,4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (len < 8 || len > 16_777_216) throw new IOException("invalid ZK TCP payload length: "+len);
            byte[] body=in.readNBytes(len);
            if (body.length != len) throw new EOFException("short ZK TCP payload");
            return body;
        }
        DatagramPacket req=new DatagramPacket(rawUdpPacket,rawUdpPacket.length);
        udp.send(req);
        byte[] buf=new byte[65535]; DatagramPacket res=new DatagramPacket(buf,buf.length); udp.receive(res);
        return Arrays.copyOf(res.getData(),res.getLength());
    }
    @Override public void close() {
        if (tcp != null) try { tcp.close(); } catch(IOException ignored) {}
        if (udp != null) udp.close();
    }
}
