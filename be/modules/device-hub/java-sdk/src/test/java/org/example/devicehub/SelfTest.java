package org.example.devicehub;
public class SelfTest { public static void main(String[] a){ if(Vendor.values().length!=7) throw new AssertionError(); if(IntegrationRoute.values().length<16) throw new AssertionError(); System.out.println("java-sdk self-test OK"); } }
