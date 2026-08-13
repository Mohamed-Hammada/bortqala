from __future__ import annotations
import re
from typing import Iterable

def normalize_version(value: str):
    if not value: return ()
    nums = re.findall(r"\d+", str(value))
    return tuple(int(x) for x in nums[:6])

def _pad(a, b):
    n=max(len(a),len(b)); return a+(0,)*(n-len(a)), b+(0,)*(n-len(b))

def compare_versions(left: str, right: str) -> int:
    a,b=_pad(normalize_version(left), normalize_version(right))
    return (a>b)-(a<b)

def version_satisfies(value: str, spec: str | None) -> bool:
    if not spec or spec.strip() in ("*","any"): return True
    if not value: return False
    for raw in spec.split(','):
        term=raw.strip()
        if not term: continue
        if term.endswith('.*'):
            prefix=normalize_version(term[:-2]); v=normalize_version(value)
            if v[:len(prefix)] != prefix: return False
            continue
        m=re.match(r'^(>=|<=|==|=|>|<)?\s*([vV]?\d+(?:\.\d+)*)$', term)
        if not m:
            # Unknown vendor notation must not be treated as compatible.
            return False
        op=m.group(1) or '==' ; target=m.group(2)
        c=compare_versions(value,target)
        ok={'==':c==0,'=':c==0,'>=':c>=0,'<=':c<=0,'>':c>0,'<':c<0}[op]
        if not ok: return False
    return True
