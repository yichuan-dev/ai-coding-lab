#!/usr/bin/env python3
"""Reject accidental credential/data artifacts in the public source package."""
from pathlib import Path
import re,sys
root=Path(__file__).resolve().parents[1]
blocked={'.apk','.aab','.db','.sqlite','.jks','.keystore','.p12','.hprof','.docx','.pdf','.jab'}
errors=[]
for p in root.rglob('*'):
    if not p.is_file() or any(x in p.parts for x in ('build','.gradle','.git')):continue
    if p.suffix.lower() in blocked or p.name.startswith(('.env','cookies','credentials')):errors.append(str(p.relative_to(root)));continue
    if p.suffix=='.jar':continue
    text=p.read_text(errors='replace')
    if re.search(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|sk-[a-zA-Z0-9_-]{28,}|__zp_stoken__\s*[:=]\s*["\'][^"\']{16,}',text):errors.append(str(p.relative_to(root)))
if errors:
    print('PRIVACY CHECK FAILED: '+', '.join(errors));sys.exit(1)
print('PASS: public source contains no blocked data artifacts or recognizable real credentials')
