package org.example.zkteco.adapter.pull;
import java.time.LocalDateTime;
import java.util.Arrays;
public final class ZkProtocolSelfTest {
  public static void main(String[] args) {
    byte[] p=ZkPacketCodec.encodeUdp(ZkCommands.CONNECT,0,0,new byte[0]);
    if(p.length!=8) throw new AssertionError();
    ZkPacket d=ZkPacketCodec.decodeUdp(p); if(d.command()!=1000) throw new AssertionError();
    byte[] wrapped=ZkPacketCodec.wrapTcp(p); if(!Arrays.equals(p,ZkPacketCodec.unwrapTcp(wrapped))) throw new AssertionError();
    LocalDateTime t=LocalDateTime.of(2026,8,8,21,30,12); if(!t.equals(ZkTimeCodec.decode(ZkTimeCodec.encode(t)))) throw new AssertionError();
    System.out.println("ZK protocol codec self-test OK");
  }
}
