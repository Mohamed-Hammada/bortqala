package org.example.devicehub; public interface Adapter { IntegrationRoute route(); ProbeResult probe(Endpoint e) throws Exception; }
