package org.example.zkteco.adapter.adms;
public final class AdmsParserSelfTest { public static void main(String[]a){ var r=AdmsPayloadParser.parse("ATTLOG","1	2026-08-08 08:15:00	0	1	0	0"); if(r.size()!=1||!"1".equals(r.get(0).pin()))throw new AssertionError(); System.out.println("ADMS parser self-test OK"); } }
