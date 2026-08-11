package org.example.zkteco.adapter.adms;
import java.time.LocalDateTime;
import java.util.Map;
public record AdmsParsedRecord(String kind,String pin,LocalDateTime dateTime,Integer status,Integer verify,Map<String,String> fields,String raw){
    public AdmsParsedRecord { fields=fields==null?Map.of():Map.copyOf(fields); raw=raw==null?"":raw; }
}
