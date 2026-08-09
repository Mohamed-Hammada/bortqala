#!/usr/bin/env python3
"""Dependency-free first-pass ZKTeco network probe. It does not change device state."""
import argparse, socket, struct, json
MAGIC=b'PP\x82}'
def checksum(data):
    s=0
    if len(data)%2:data+=b'\0'
    for i in range(0,len(data),2):
        s+=(data[i]|(data[i+1]<<8)); s=(s&0xffff)+(s>>16)
    return (~s)&0xffff
def packet(cmd,session=0,reply=0):
    raw=struct.pack('<4H',cmd,0,session,reply); c=checksum(raw); raw=struct.pack('<4H',cmd,c,session,reply)
    return MAGIC+struct.pack('<I',len(raw))+raw
def main():
    ap=argparse.ArgumentParser(); ap.add_argument('host'); ap.add_argument('--port',type=int,default=4370); ap.add_argument('--timeout',type=float,default=3); a=ap.parse_args()
    out={'host':a.host,'port':a.port,'tcp_reachable':False,'zk_connect_ack':False}
    try:
      with socket.create_connection((a.host,a.port),a.timeout) as s:
        out['tcp_reachable']=True; s.settimeout(a.timeout); s.sendall(packet(1000)); h=s.recv(8)
        if len(h)==8 and h[:4]==MAGIC:
          n=struct.unpack('<I',h[4:])[0]; b=b''
          while len(b)<n:
            x=s.recv(n-len(b));
            if not x:break
            b+=x
          if len(b)>=8:
            cmd,cs,sid,rid=struct.unpack('<4H',b[:8]); out.update({'response_command':cmd,'session_id':sid,'reply_id':rid,'zk_connect_ack':cmd==2000})
    except Exception as e: out['error']=str(e)
    print(json.dumps(out,indent=2))
if __name__=='__main__':main()
