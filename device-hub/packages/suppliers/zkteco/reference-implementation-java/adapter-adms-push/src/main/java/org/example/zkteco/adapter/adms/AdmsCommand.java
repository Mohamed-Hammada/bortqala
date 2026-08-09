package org.example.zkteco.adapter.adms;
import java.time.Instant;
public record AdmsCommand(long id,String serialNumber,String payload,Instant createdAt){
  public AdmsCommand { createdAt=createdAt==null?Instant.now():createdAt; }
  public String wireValue(){ return "C:"+id+":"+payload; }
}
