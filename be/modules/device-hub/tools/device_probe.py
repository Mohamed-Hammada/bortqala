#!/usr/bin/env python3
import argparse, socket, ssl, urllib.request, urllib.error, json

COMMON_PORTS=[80,443,4370,8000,8080,8443,5000,51211,51212]

def tcp(host,port,timeout=1.5):
    try:
        with socket.create_connection((host,port),timeout): return True
    except OSError:return False

def http(host,port,https=False,timeout=2):
    scheme='https' if https else 'http'
    ctx=ssl._create_unverified_context() if https else None
    try:
        with urllib.request.urlopen(f'{scheme}://{host}:{port}/',timeout=timeout,context=ctx) as r:
            return {'status':r.status,'server':r.headers.get('Server')}
    except urllib.error.HTTPError as e:return {'status':e.code,'server':e.headers.get('Server')}
    except Exception as e:return {'error':str(e)}

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('host'); ap.add_argument('--ports',default=','.join(map(str,COMMON_PORTS)))
    a=ap.parse_args(); out={'host':a.host,'open_ports':[],'http':{}}
    for p in [int(x) for x in a.ports.split(',') if x.strip()]:
        if tcp(a.host,p):
            out['open_ports'].append(p)
            if p in (80,8000,8080):out['http'][str(p)]=http(a.host,p)
            if p in (443,8443):out['http'][str(p)]=http(a.host,p,True)
    print(json.dumps(out,indent=2))
if __name__=='__main__': main()
