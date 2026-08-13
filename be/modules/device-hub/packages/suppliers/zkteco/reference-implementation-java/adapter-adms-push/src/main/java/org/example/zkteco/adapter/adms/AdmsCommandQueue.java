package org.example.zkteco.adapter.adms;
import java.util.*; import java.util.concurrent.*; import java.util.concurrent.atomic.AtomicLong;
public final class AdmsCommandQueue {
  private final AtomicLong ids=new AtomicLong(); private final ConcurrentMap<String,ConcurrentLinkedQueue<AdmsCommand>> queues=new ConcurrentHashMap<>();
  public AdmsCommand enqueue(String serial,String payload){ if(serial==null||serial.isBlank())throw new IllegalArgumentException("serial required"); if(payload==null||payload.isBlank())throw new IllegalArgumentException("payload required"); var c=new AdmsCommand(ids.incrementAndGet(),serial,payload,null); queues.computeIfAbsent(serial,k->new ConcurrentLinkedQueue<>()).add(c); return c; }
  public AdmsCommand poll(String serial){var q=queues.get(serial);return q==null?null:q.poll();}
  public int size(String serial){var q=queues.get(serial);return q==null?0:q.size();}
}
