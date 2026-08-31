#!/usr/bin/env python3
"""Build one authored Cobblemon candidate from a professional manifest plus geometry spec.

The manifest pins the official release, source hashes and outputs. The geometry spec
contains only appended ouros_* cosmetic bones. This builder never changes official
bones and never grants artistic approval.
"""
from __future__ import annotations
import argparse, hashlib, json, struct, urllib.request, zipfile, zlib
from io import BytesIO
from pathlib import Path

PALETTE=[(230,238,250,255),(91,130,207,255),(186,204,235,255),(247,248,250,255),(59,86,143,255),(211,221,240,255),(119,151,211,255),(239,243,250,255)]

def h256(b:bytes)->str:return hashlib.sha256(b).hexdigest()
def h512(b:bytes)->str:return hashlib.sha512(b).hexdigest()
def chunk(t:bytes,d:bytes)->bytes:return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
def read_rgba(data:bytes):
    if data[:8]!=b'\x89PNG\r\n\x1a\n': raise SystemExit('official texture is not PNG')
    pos=8; idat=b''; w=h=None
    while pos<len(data):
        n=struct.unpack('>I',data[pos:pos+4])[0]; t=data[pos+4:pos+8]; d=data[pos+8:pos+8+n]; pos+=12+n
        if t==b'IHDR':
            w,h,bd,ct,_,_,_=struct.unpack('>IIBBBBB',d)
            if (bd,ct)!=(8,6): raise SystemExit(f'expected RGBA8 PNG, got bitDepth={bd} colorType={ct}')
        elif t==b'IDAT': idat+=d
        elif t==b'IEND': break
    raw=zlib.decompress(idat); stride=w*4; rows=[]; prev=bytearray(stride); i=0
    for _y in range(h):
        f=raw[i]; i+=1; scan=bytearray(raw[i:i+stride]); i+=stride; out=bytearray(stride)
        for x in range(stride):
            a=out[x-4] if x>=4 else 0; b=prev[x]; c=prev[x-4] if x>=4 else 0
            if f==0: val=scan[x]
            elif f==1: val=(scan[x]+a)&255
            elif f==2: val=(scan[x]+b)&255
            elif f==3: val=(scan[x]+((a+b)//2))&255
            elif f==4:
                p=a+b-c; pa=abs(p-a); pb=abs(p-b); pc=abs(p-c); pr=a if pa<=pb and pa<=pc else (b if pb<=pc else c); val=(scan[x]+pr)&255
            else: raise SystemExit(f'unsupported PNG filter {f}')
            out[x]=val
        rows.append(out); prev=out
    return w,h,rows

def encode_rgba(w:int,h:int,rows:list[bytearray])->bytes:
    raw=b''.join(b'\x00'+bytes(r) for r in rows)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')

def derived_body(official:bytes)->bytes:
    w,h,rows=read_rgba(official)
    if (w,h)!=(128,64): raise SystemExit(f'unexpected Lucario texture dimensions {(w,h)}')
    for y,row in enumerate(rows):
        for x in range(w):
            i=x*4; r,g,b,a=row[i:i+4]
            if a==0: continue
            lum=(r*3+g*6+b)//10; var=((x*7+y*11)%13)-6
            if lum>175: nr=min(255,int(r*.88+28+var)); ng=min(255,int(g*.92+34+var)); nb=min(255,int(b*.98+42+var))
            elif lum<70: nr=max(0,int(r*.58+4+var//2)); ng=max(0,int(g*.66+9+var//2)); nb=max(0,int(b*.82+18+var//2))
            else: nr=max(0,min(255,int(r*.72+8+var))); ng=max(0,min(255,int(g*.82+18+var))); nb=max(0,min(255,int(b*.98+34+var)))
            row[i:i+4]=bytes((nr,ng,nb,a))
    for j,rgba in enumerate(PALETTE):
        x,y=80+j,63
        if rows[y][x*4+3]!=0: raise SystemExit(f'palette slot {x},{y} is not transparent in official baseline')
        rows[y][x*4:(x+1)*4]=bytes(rgba)
    return encode_rgba(w,h,rows)

def solid_uv(uv):
    face={'uv':uv,'uv_size':[1,1]}
    return {k:dict(face) for k in ('north','east','south','west','up','down')}
def safe(root:Path,raw:str)->Path:
    p=(root/raw).resolve()
    if root.resolve()!=p and root.resolve() not in p.parents: raise SystemExit(f'unsafe output path {raw}')
    p.parent.mkdir(parents=True,exist_ok=True); return p

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('--manifest',required=True,type=Path); ap.add_argument('--geometry-spec',required=True,type=Path); args=ap.parse_args()
    root=Path('.').resolve(); manifest=json.loads(args.manifest.read_text()); spec=json.loads(args.geometry_spec.read_text()); official=manifest['officialSource']; prod=manifest['production']
    if spec.get('species')!=manifest.get('species'): raise SystemExit('species mismatch between manifest and geometry spec')
    with urllib.request.urlopen(f"https://api.modrinth.com/v2/version/{official['modrinthVersionId']}",timeout=30) as r: version=json.load(r)
    f=next((x for x in version['files'] if x.get('primary')),version['files'][0])
    with urllib.request.urlopen(f['url'],timeout=90) as r: jar=r.read()
    if h256(jar)!=official['jarSha256'] or h512(jar)!=official['jarSha512']: raise SystemExit('official JAR hash mismatch')
    with zipfile.ZipFile(BytesIO(jar)) as z:
        model_bytes=z.read(official['modelPath']); texture_bytes=z.read(official['referenceTexture']['path'])
    if h256(model_bytes)!=official['modelSha256']: raise SystemExit('official model hash mismatch')
    if h256(texture_bytes)!=official['referenceTexture']['sha256']: raise SystemExit('official texture hash mismatch')
    model=json.loads(model_bytes); geos=model.get('minecraft:geometry')
    if not isinstance(geos,list) or len(geos)!=1: raise SystemExit('expected one official geometry')
    bones=geos[0].get('bones'); n=official['officialBoneCount']
    if not isinstance(bones,list) or len(bones)!=n: raise SystemExit(f'official bone count mismatch expected={n} actual={len(bones) if isinstance(bones,list) else None}')
    geos[0]['description']['identifier']='geometry.ouros_aura_sentinel_lucario'
    official_names={b.get('name') for b in bones}; appended=set()
    for b in spec['bones']:
        if not b['name'].startswith('ouros_'): raise SystemExit('cosmetic bone namespace violation')
        if b['parent'] not in official_names and b['parent'] not in appended: raise SystemExit(f"unknown cosmetic parent {b['parent']}")
        nb={'name':b['name'],'parent':b['parent'],'pivot':b['pivot'],'cubes':[]}
        for c in b['cubes']:
            nc={'origin':c['origin'],'size':c['size'],'uv':solid_uv(c['uv'])}
            if 'pivot' in c: nc['pivot']=c['pivot']
            if 'rotation' in c: nc['rotation']=c['rotation']
            nb['cubes'].append(nc)
        bones.append(nb); appended.add(nb['name'])
    model_out=(json.dumps(model,separators=(',',':'))+'\n').encode(); body_out=derived_body(texture_bytes)
    config=manifest['builderConfig']; resolver_obj={'species':config['speciesId'],'order':config['resolverOrder'],'variations':config['resolverVariations']}; resolver_out=(json.dumps(resolver_obj,separators=(',',':'))+'\n').encode()
    outs={prod['modelPath']:model_out,prod['textures'][0]['path']:body_out,prod['runtimeAssets'][0]['path']:resolver_out}
    for raw,data in outs.items(): safe(root,raw).write_bytes(data)
    print(json.dumps({'status':'BUILT_NOT_ART_APPROVED','modelSha256':h256(model_out),'bodySha256':h256(body_out),'resolverSha256':h256(resolver_out),'officialBoneCount':n,'derivedBoneCount':len(bones),'cosmeticBoneCount':len(appended)},indent=2))
if __name__=='__main__': main()
