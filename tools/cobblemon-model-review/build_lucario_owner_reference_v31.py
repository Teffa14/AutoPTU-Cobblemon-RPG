#!/usr/bin/env python3
"""Lucario V33: owner-reference fidelity pass.

V32 removed the incorrect square boot shells and darkened biological blue, but
matched-camera comparison still showed two dominant mismatches: the apron front
was visually interrupted by blue wedges and was too short/narrow, while the white
headpiece still read as a regular tower. V33 rebuilds those two masses directly.
The exact official 87-bone Lucario prefix remains untouched. Presentation only;
AutoPTU/Ouros remains authoritative for tactical battle facts.
"""
from __future__ import annotations
import importlib.util, json, struct, zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
V22_PATH=ROOT/'tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py'
spec=importlib.util.spec_from_file_location('owner_reference_v33_v22',V22_PATH)
if spec is None or spec.loader is None: raise SystemExit('cannot load validated V22 professional pipeline')
v22=importlib.util.module_from_spec(spec); spec.loader.exec_module(v22); v1=v22.v1
NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v33-reference-derived-normal.json'
SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v33-reference-derived-shiny.json'
v22.NORMAL_META=NORMAL_META; v22.SHINY_META=SHINY_META
PALETTE={80:(18,20,27,255),81:(34,37,47,255),82:(3,61,101,255),83:(6,102,153,255),84:(68,82,187,255),85:(132,146,202,255),86:(177,184,201,255),87:(216,221,230,255),88:(240,242,246,255),89:(198,203,214,255),90:(132,141,153,255),91:(27,32,43,255),92:(14,82,131,255)}
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
    # Fewer, larger offset folds copy the visible cap rhythm much more closely
    # than V32's evenly stepped tower. Blue remains a low band only.
    hat=v1.bone('ouros_v33_reference_headpiece','head_angle',[0,39,-.1],[
      C((-4.90,38.05,-2.55),(9.80,1.35,5.80),84),
      C((-4.55,39.18,-2.32),(9.10,1.15,5.35),88),
      C((-4.75,40.10,-2.08),(9.45,2.30,5.00),87,pivot=(-.10,41.20,.38),rotation=(0,0,4)),
      C((-3.95,42.10,-1.76),(8.95,2.35,4.55),89,pivot=(.35,43.20,.48),rotation=(0,0,-5)),
      C((-4.40,44.12,-1.48),(8.05,2.45,4.15),87,pivot=(-.25,45.25,.55),rotation=(0,0,5)),
      C((-3.05,46.28,-1.15),(7.10,1.72,3.55),88,pivot=(.40,47.05,.60),rotation=(0,0,-4)),
    ])
    ribbons=v1.bone('ouros_v33_reference_head_ribbons','head_angle',[0,40,1],[
      C((-5.15,37.90,.18),(1.62,4.10,2.40),83,pivot=(-4.28,39.70,1.35),rotation=(0,-7,18)),
      C((-5.45,40.60,.72),(1.50,3.60,2.08),82,pivot=(-4.62,42.05,1.66),rotation=(0,-10,27)),
      C((-5.04,43.20,1.02),(1.32,2.55,1.74),83,pivot=(-4.32,44.22,1.88),rotation=(0,-12,34)),
      C((3.58,38.80,.50),(1.00,2.78,1.65),83,pivot=(4.08,40.02,1.42),rotation=(0,8,-13)),
    ])
    collar=v1.bone('ouros_v33_reference_collar','neck',[0,32.1,-.1],[C((-3.15,31.08,-2.40),(6.30,1.12,4.60),82),C((-2.80,31.62,-2.55),(5.60,.66,4.90),83)])
    bow=v1.bone('ouros_v33_reference_bow','torso3',[0,30.1,-3],[C((-3.65,29,-4.10),(3.20,1.55,.68),84,pivot=(-1.75,29.75,-3.75),rotation=(0,0,-17)),C((.45,29,-4.10),(3.20,1.55,.68),84,pivot=(1.75,29.75,-3.75),rotation=(0,0,17)),C((-1.20,28.30,-4.28),(2.40,2.40,.86),90,pivot=(0,29.50,-3.85),rotation=(0,0,45)),C((-.55,27.20,-4.12),(1.10,1.55,.55),86)])
    bodice=v1.bone('ouros_v33_reference_bodice','torso3',[0,26.4,-.5],[C((-4.05,23,-3.70),(3.25,6.25,.72),88,pivot=(-2.10,26.10,-3.35),rotation=(0,0,-3)),C((.80,23,-3.70),(3.25,6.25,.72),88,pivot=(2.10,26.10,-3.35),rotation=(0,0,3)),C((-4.20,23,-2.95),(.72,6.20,5.55),87),C((3.48,23,-2.95),(.72,6.20,5.55),87),C((-3.20,23.25,1.55),(6.40,5.75,.72),89),C((-2.60,23.45,-4.08),(.62,2.30,.30),84),C((1.98,23.45,-4.08),(.62,2.30,.30),84),C((-.35,23.20,-4.14),(.70,3.10,.32),85)])
    sl=v1.bone('ouros_v33_reference_sleeve_left','arm_left',[4.5,29.7,-.4],[C((2.05,28.20,-1.78),(6.25,2.95,2.85),88,inflate=.04),C((7.80,28.15,-2.10),(4.15,3.00,3.45),87,inflate=.03)])
    sr=v1.bone('ouros_v33_reference_sleeve_right','arm_right',[-4.5,29.7,-.4],[C((-8.30,28.20,-1.78),(6.25,2.95,2.85),88,inflate=.04),C((-11.95,28.15,-2.10),(4.15,3.00,3.45),87,inflate=.03)])
    gl=v1.bone('ouros_v33_reference_glove_left','arm_left2',[12,29.4,-.4],[C((11.15,28,-2.30),(2.25,3.20,3.85),84),C((12.25,28.15,-2.60),(4.35,2.95,4.45),80),C((15.95,28.20,-2.80),(3.20,2.80,4.80),81)])
    gr=v1.bone('ouros_v33_reference_glove_right','arm_right2',[-12,29.4,-.4],[C((-13.40,28,-2.30),(2.25,3.20,3.85),84),C((-16.60,28.15,-2.60),(4.35,2.95,4.45),80),C((-19.15,28.20,-2.80),(3.20,2.80,4.80),81),C((-19.45,27.55,-.80),(.65,2.20,.65),90)])
    # Higher white waist removes the exposed black/cream gap seen in V32.
    waist=v1.bone('ouros_v33_reference_waist','torso',[0,20,-.2],[C((-6.55,18.70,-4.05),(13.10,2.05,8.00),88),C((-6.10,18.10,-3.75),(12.20,1.10,7.45),87),C((-5.60,18.05,-4.30),(11.20,.48,.35),85)])
    # Blue skirt is deliberately behind and at the sides. Three overlapping white
    # front shells form one continuous apron, growing wider toward the bottom.
    skirt=v1.bone('ouros_v33_reference_apron_skirt','torso',[0,18.2,-.2],[
      C((-7.20,9.20,-2.50),(2.05,9.25,5.90),83,pivot=(-5.95,17.70,.20),rotation=(0,0,-5)),
      C((5.15,9.20,-2.50),(2.05,9.25,5.90),83,pivot=(5.95,17.70,.20),rotation=(0,0,5)),
      C((-5.85,9.35,2.70),(11.70,9.00,.72),82,pivot=(0,17.60,3.05),rotation=(-3,0,0)),
      C((-4.95,14.15,-4.55),(9.90,4.25,1.00),87,pivot=(0,17.80,-4.05),rotation=(2,0,0)),
      C((-5.55,10.65,-4.62),(11.10,4.55,1.02),88,pivot=(0,14.95,-4.10),rotation=(2,0,0)),
      C((-6.05,8.55,-4.55),(12.10,3.05,.96),87,pivot=(0,11.25,-4.06),rotation=(1,0,0)),
      C((-5.95,8.45,-4.86),(11.90,.72,.36),84),
      C((-4.95,9.70,-4.88),(9.90,.48,.32),85),
    ])
    # Official lower legs and feet remain visible; no cosmetic boot cuboids.
    return [hat,ribbons,collar,bow,bodice,sl,sr,gl,gr,waist,skirt]
def paint_pixel(r,g,b,a,x,y,*,shiny):
    if a==0:return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    cream=r>170 and g>135 and b<205; white=r>210 and g>210 and b>210; red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red:return r,g,b,a
    if b>r*1.15 and b>g*1.04 and sat>20:
      targets=((6,50,82),(8,64,99),(3,38,68)) if not shiny else ((16,75,109),(22,89,126),(9,56,91)); tr,tg,tb=targets[((x//8)+(y//10))%3]; mix=.86
      nr=int(r*(1-mix)+tr*mix); ng=int(g*(1-mix)+tg*mix); nb=int(b*(1-mix)+tb*mix)
      if ((2*x+y)%31) in (0,1): nr+=2; ng+=3; nb+=5
      return max(0,min(255,nr)),max(0,min(255,ng)),max(0,min(255,nb)),a
    if lum<155:
      tr,tg,tb=((22,25,33),(31,34,43),(15,19,27))[((x//9)+(y//8))%3]; mix=.72
      return int(r*(1-mix)+tr*mix),int(g*(1-mix)+tg*mix),int(b*(1-mix)+tb*mix),a
    return r,g,b,a
def post_patch():
    data=json.loads(v1.MANIFEST.read_text(encoding='utf-8')); data['concept']='Owner Reference Replica — Blue/White Maid Lucario V33'; data['artStatus']='ARTISTIC FAIL'; data['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=data['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+len(cosmetic_bones()); p['cosmeticBoneCount']=len(cosmetic_bones()); p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=data['builder']; b['scriptPath']='tools/cobblemon-model-review/build_lucario_owner_reference_v31.py'; b['command']=['python',b['scriptPath']]
    out=[]
    for x in b['outputs']:
      for old in ('v22-derived-normal.json','v29-reference-derived-normal.json','v30-reference-derived-normal.json','v31-reference-derived-normal.json','v32-reference-derived-normal.json'): x=x.replace(old,'v33-reference-derived-normal.json')
      for old in ('v22-derived-shiny.json','v29-reference-derived-shiny.json','v30-reference-derived-shiny.json','v31-reference-derived-shiny.json','v32-reference-derived-shiny.json'): x=x.replace(old,'v33-reference-derived-shiny.json')
      out.append(x)
    b['outputs']=list(dict.fromkeys(out)); q=data['qualityIntent']; q['signaturePieces']=['Irregular folded cool-white cap with low blue band and dominant left ribbon stack','White fitted bodice/sleeves with blue cuffs, charcoal gloves and silver/blue clasp','Long solid white bell/trapezoid apron over subordinate blue side/back skirt with biological Lucario feet exposed']; q['macroFormPlan']='V33 concentrates fidelity work into the supplied reference silhouette: fewer larger offset cap folds, a higher white waist, and three overlapping white apron shells that create one uninterrupted long front mass. Blue skirt geometry moves behind/to the sides.'; q['paintPlan']='Normal/shiny derive independently from exact official 1.7.3 baselines. Biological blue is further reduced to deep navy/teal so the official tail remains anatomical but visually subordinate to the white costume.'; q['gameplayReadGoal']='At 160 px read the same first-order masses as the owner image: tall irregular white cap, black/red face, white upper body, long broad white apron, small blue side/back fields, dark gloves and Lucario-shaped dark feet.'; q['iterationNote']='V32 exact-head evidence (head 0e574162d5ceeebf79c7da38f60c05b25f104b4a) passed visual floors but direct image comparison still showed a short blue-cut apron and regular cap tower. V33 rebuilds those two dominant masses; owner approval remains absent.'
    data['variantCoverage']['variants'][0]['coverage']='Default preserves exact official 87-bone anatomy plus V33 costume and independent normal paint.'; data['variantCoverage']['variants'][1]['coverage']='Shiny uses identical V33 costume geometry and independent official-shiny derivation.'; v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=paint_pixel; v22.write_overlay=write_overlay; v22.main(); post_patch()
if __name__=='__main__':main()
