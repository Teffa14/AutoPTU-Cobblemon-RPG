#!/usr/bin/env python3
"""Lucario V31: self-contained owner-reference fidelity build.

Uses the validated V22 official-source/derived-texture pipeline directly. This file
is intentionally independent of the historical V29 builder so the active manifest
can transition through a compatibility wrapper without circular imports.
"""
from __future__ import annotations
import importlib.util, json, struct, zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
V22_PATH=ROOT/'tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py'
spec=importlib.util.spec_from_file_location('owner_reference_v31_v22',V22_PATH)
if spec is None or spec.loader is None: raise SystemExit('cannot load validated V22 professional pipeline')
v22=importlib.util.module_from_spec(spec); spec.loader.exec_module(v22); v1=v22.v1
NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v31-reference-derived-normal.json'
SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v31-reference-derived-shiny.json'
v22.NORMAL_META=NORMAL_META; v22.SHINY_META=SHINY_META
PALETTE={80:(18,20,27,255),81:(34,37,47,255),82:(2,67,108,255),83:(7,105,157,255),84:(66,82,188,255),85:(135,149,205,255),86:(177,184,201,255),87:(218,222,231,255),88:(239,241,245,255),89:(198,203,215,255),90:(132,141,153,255),91:(29,35,49,255),92:(16,86,136,255)}
def png_chunk(kind,payload): return struct.pack('>I',len(payload))+kind+payload+struct.pack('>I',zlib.crc32(kind+payload)&0xffffffff)
def write_overlay(path):
    w,h=128,64; pixels=bytearray(w*h*4)
    for x,rgba in PALETTE.items():
        i=((63*w)+x)*4; pixels[i:i+4]=bytes(rgba)
    raw=bytearray(); stride=w*4
    for y in range(h): raw.append(0); raw.extend(pixels[y*stride:(y+1)*stride])
    data=b'\x89PNG\r\n\x1a\n'+png_chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+png_chunk(b'IDAT',zlib.compress(bytes(raw),9))+png_chunk(b'IEND',b'')
    path.parent.mkdir(parents=True,exist_ok=True); path.write_bytes(data)
def C(origin,size,color,**kw): return v1.cube(origin,size,color,**kw)
def cosmetic_bones():
    hat=v1.bone('ouros_v31_reference_headpiece','head_angle',[0,39,-.1],[C((-4.85,38.1,-2.55),(9.7,1.3,5.8),84),C((-4.55,39.2,-2.35),(9.1,1,5.4),88),C((-4.15,40.05,-2.1),(8.3,2.45,4.95),87),C((-4.45,42.2,-1.9),(8.9,2.2,4.65),89,pivot=(0,43.2,.35),rotation=(0,0,3)),C((-4.05,44.1,-1.65),(8.1,2.3,4.2),87,pivot=(0,45.1,.45),rotation=(0,0,-3)),C((-3.55,46.05,-1.3),(7.1,1.7,3.65),88),C((-2.9,47.45,-1.05),(5.8,1.05,3.1),86)])
    ribbons=v1.bone('ouros_v31_reference_head_ribbons','head_angle',[0,40,1],[C((-5.05,37.95,.25),(1.55,4,2.35),83,pivot=(-4.2,39.7,1.35),rotation=(0,-7,18)),C((-5.35,40.55,.75),(1.45,3.55,2.05),82,pivot=(-4.55,42,1.65),rotation=(0,-10,27)),C((-4.95,43.15,1.05),(1.3,2.55,1.7),83,pivot=(-4.25,44.2,1.85),rotation=(0,-12,34)),C((3.55,38.8,.55),(1,2.85,1.7),83,pivot=(4.05,40.05,1.45),rotation=(0,8,-13))])
    collar=v1.bone('ouros_v31_reference_collar','neck',[0,32.1,-.1],[C((-3.15,31.1,-2.4),(6.3,1.15,4.6),82),C((-2.8,31.65,-2.55),(5.6,.65,4.9),83)])
    bow=v1.bone('ouros_v31_reference_bow','torso3',[0,30.1,-3],[C((-3.65,29,-4.1),(3.2,1.55,.68),84,pivot=(-1.75,29.75,-3.75),rotation=(0,0,-17)),C((.45,29,-4.1),(3.2,1.55,.68),84,pivot=(1.75,29.75,-3.75),rotation=(0,0,17)),C((-1.2,28.3,-4.28),(2.4,2.4,.86),90,pivot=(0,29.5,-3.85),rotation=(0,0,45)),C((-.55,27.2,-4.12),(1.1,1.55,.55),86)])
    bodice=v1.bone('ouros_v31_reference_bodice','torso3',[0,26.4,-.5],[C((-4.05,23,-3.7),(3.25,6.25,.72),88,pivot=(-2.1,26.1,-3.35),rotation=(0,0,-3)),C((.8,23,-3.7),(3.25,6.25,.72),88,pivot=(2.1,26.1,-3.35),rotation=(0,0,3)),C((-4.2,23,-2.95),(.72,6.2,5.55),87),C((3.48,23,-2.95),(.72,6.2,5.55),87),C((-3.2,23.25,1.55),(6.4,5.75,.72),89),C((-2.6,23.45,-4.08),(.62,2.3,.3),84),C((1.98,23.45,-4.08),(.62,2.3,.3),84),C((-.35,23.2,-4.14),(.7,3.1,.32),85)])
    sl=v1.bone('ouros_v31_reference_sleeve_left','arm_left',[4.5,29.7,-.4],[C((2.05,28.2,-1.78),(6.25,2.95,2.85),88,inflate=.04),C((7.8,28.15,-2.1),(4.15,3,3.45),87,inflate=.03)])
    sr=v1.bone('ouros_v31_reference_sleeve_right','arm_right',[-4.5,29.7,-.4],[C((-8.3,28.2,-1.78),(6.25,2.95,2.85),88,inflate=.04),C((-11.95,28.15,-2.1),(4.15,3,3.45),87,inflate=.03)])
    gl=v1.bone('ouros_v31_reference_glove_left','arm_left2',[12,29.4,-.4],[C((11.15,28,-2.3),(2.25,3.2,3.85),84),C((12.25,28.15,-2.6),(4.35,2.95,4.45),80),C((15.95,28.2,-2.8),(3.2,2.8,4.8),81)])
    gr=v1.bone('ouros_v31_reference_glove_right','arm_right2',[-12,29.4,-.4],[C((-13.4,28,-2.3),(2.25,3.2,3.85),84),C((-16.6,28.15,-2.6),(4.35,2.95,4.45),80),C((-19.15,28.2,-2.8),(3.2,2.8,4.8),81),C((-19.45,27.55,-.8),(.65,2.2,.65),90)])
    waist=v1.bone('ouros_v31_reference_waist','torso',[0,20,-.2],[C((-6.55,19.05,-4.05),(13.1,1.55,8),88),C((-6.1,18.35,-3.75),(12.2,1.05,7.45),87)])
    skirt=v1.bone('ouros_v31_reference_apron_skirt','torso',[0,18.5,-.2],[C((-6.95,11.35,-3),(2.1,7.1,6.1),83,pivot=(-5.75,17.9,0),rotation=(0,0,-5)),C((4.85,11.35,-3),(2.1,7.1,6.1),83,pivot=(5.75,17.9,0),rotation=(0,0,5)),C((-5.65,11.3,2.8),(11.3,7,.7),82,pivot=(0,17.85,3.15),rotation=(-3,0,0)),C((-5.35,11.15,-4.25),(10.7,7.2,.66),87,pivot=(0,17.85,-3.9),rotation=(2,0,0)),C((-4.65,10.55,-4.6),(9.3,7.35,.58),88,pivot=(0,17.4,-4.25),rotation=(4,0,0)),C((-5.15,10.65,-4.8),(10.3,.78,.34),84),C((-3.95,12,-4.76),(7.9,.55,.3),85)])
    ll=v1.bone('ouros_v31_reference_leg_left','leg_left4',[3.5,4.5,-.1],[C((1.25,-2.95,-1.85),(4.5,9.45,3.7),81,inflate=.03)])
    lr=v1.bone('ouros_v31_reference_leg_right','leg_right4',[-3.5,4.5,-.1],[C((-5.75,-2.95,-1.85),(4.5,9.45,3.7),81,inflate=.03)])
    bl=v1.bone('ouros_v31_reference_boot_left','foot_left',[3.5,-1.7,-1.2],[C((.7,-4.05,-4.35),(5.6,3.65,5.9),80,inflate=.03),C((1,-3.85,-4.6),(5,.7,1.1),81)])
    br=v1.bone('ouros_v31_reference_boot_right','foot_right',[-3.5,-1.7,-1.2],[C((-6.3,-4.05,-4.35),(5.6,3.65,5.9),80,inflate=.03),C((-6,-3.85,-4.6),(5,.7,1.1),81)])
    return [hat,ribbons,collar,bow,bodice,sl,sr,gl,gr,waist,skirt,ll,lr,bl,br]
def paint_pixel(r,g,b,a,x,y,*,shiny):
    if a==0:return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    cream=r>170 and g>135 and b<205; white=r>210 and g>210 and b>210; red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red:return r,g,b,a
    if b>r*1.15 and b>g*1.04 and sat>20:
        targets=((15,82,126),(22,101,148),(7,64,105)) if not shiny else ((27,101,145),(36,116,159),(18,82,126)); tr,tg,tb=targets[((x//8)+(y//10))%3]; mix=.72
        nr=int(r*(1-mix)+tr*mix); ng=int(g*(1-mix)+tg*mix); nb=int(b*(1-mix)+tb*mix); return nr,ng,nb,a
    if lum<155:
        tr,tg,tb=((25,28,36),(36,39,48),(18,22,31))[((x//9)+(y//8))%3]; mix=.68; return int(r*(1-mix)+tr*mix),int(g*(1-mix)+tg*mix),int(b*(1-mix)+tb*mix),a
    return r,g,b,a
def post_patch():
    data=json.loads(v1.MANIFEST.read_text(encoding='utf-8')); data['concept']='Owner Reference Replica — Blue/White Maid Lucario V31'; data['artStatus']='ARTISTIC FAIL'; data['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=data['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+len(cosmetic_bones()); p['cosmeticBoneCount']=len(cosmetic_bones()); p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=data['builder']; b['scriptPath']='tools/cobblemon-model-review/build_lucario_owner_reference_v31.py'; b['command']=['python',b['scriptPath']]
    out=[]
    for x in b['outputs']:
        x=x.replace('v22-derived-normal.json','v31-reference-derived-normal.json').replace('v29-reference-derived-normal.json','v31-reference-derived-normal.json').replace('v30-reference-derived-normal.json','v31-reference-derived-normal.json'); x=x.replace('v22-derived-shiny.json','v31-reference-derived-shiny.json').replace('v29-reference-derived-shiny.json','v31-reference-derived-shiny.json').replace('v30-reference-derived-shiny.json','v31-reference-derived-shiny.json'); out.append(x)
    b['outputs']=list(dict.fromkeys(out)); q=data['qualityIntent']; q['signaturePieces']=['Tall cool-white stepped headpiece with low blue band and asymmetric blue side ribbon','White fitted bodice and animated sleeves with blue cuffs, oversized charcoal gloves and silver/blue clasp','Broad layered white apron over blue side/back skirt with dark lower body']; q['macroFormPlan']='V31 is the owner-reference fidelity architecture and does not reuse Resonance Ronin macroforms.'; q['paintPlan']='Normal/shiny derive independently from exact official 1.7.3 baselines; accessory overlay uses dedicated cool-white/grey, royal-blue and charcoal swatches.'; q['gameplayReadGoal']='At 160 px read the supplied white-and-blue owner target immediately.'; q['iterationNote']='Owner image SHA-256 28da2aa76025e2c2e625eb8df60153656d6ea17289cfb2a56da62f9159e3e419. V31 is self-contained so active materialization can transition safely.'
    data['variantCoverage']['variants'][0]['coverage']='Default preserves exact official 87-bone anatomy plus V31 costume and independent normal paint.'; data['variantCoverage']['variants'][1]['coverage']='Shiny uses identical V31 costume geometry and independent official-shiny derivation.'; v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=paint_pixel; v22.write_overlay=write_overlay; v22.main(); post_patch()
if __name__=='__main__':main()
