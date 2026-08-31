#!/usr/bin/env python3
"""Lucario V32: owner-reference fidelity pass.

V31 established the correct white/blue/charcoal material family but direct
Blockbench comparison still showed four visible mismatches against the supplied
render: blocky cosmetic boots, an angular/fragmented apron, an overly regular
stepped headpiece, and biological blue (especially the tail) reading too bright.

V32 keeps the exact official 87-bone Lucario prefix, removes all cosmetic leg/foot
shells, rebuilds the headpiece as irregular offset cool-white masses, makes the
front apron one broad white-dominant bell/trapezoid, and darkens biological blue.
Presentation only; AutoPTU/Ouros remains authoritative for tactical battle facts.
"""
from __future__ import annotations
import importlib.util, json, struct, zlib
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
V22_PATH=ROOT/'tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py'
spec=importlib.util.spec_from_file_location('owner_reference_v32_v22',V22_PATH)
if spec is None or spec.loader is None: raise SystemExit('cannot load validated V22 professional pipeline')
v22=importlib.util.module_from_spec(spec); spec.loader.exec_module(v22); v1=v22.v1
NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v32-reference-derived-normal.json'
SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v32-reference-derived-shiny.json'
v22.NORMAL_META=NORMAL_META; v22.SHINY_META=SHINY_META

PALETTE={
  80:(18,20,27,255),81:(34,37,47,255),
  82:(3,61,101,255),83:(6,102,153,255),84:(68,82,187,255),
  85:(132,146,202,255),86:(177,184,201,255),87:(216,221,230,255),
  88:(240,242,246,255),89:(198,203,214,255),90:(132,141,153,255),
  91:(27,32,43,255),92:(14,82,131,255)
}

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
    # The target cap is not a centered pyramid. Wide offset blocks create the
    # irregular folded white silhouette seen in the supplied three-quarter render.
    hat=v1.bone('ouros_v32_reference_headpiece','head_angle',[0,39,-.1],[
      C((-4.85,38.10,-2.55),(9.70,1.28,5.75),84),
      C((-4.55,39.18,-2.35),(9.10,1.02,5.35),88),
      C((-4.35,40.03,-2.15),(8.35,2.35,4.95),87,pivot=(-.2,41.15,.30),rotation=(0,0,2)),
      C((-4.75,42.02,-1.95),(8.65,2.05,4.60),89,pivot=(-.45,42.95,.35),rotation=(0,0,5)),
      C((-3.95,43.85,-1.65),(8.30,2.18,4.28),87,pivot=(.15,44.90,.45),rotation=(0,0,-4)),
      C((-4.20,45.72,-1.42),(7.25,1.72,3.82),88,pivot=(-.35,46.45,.50),rotation=(0,0,4)),
      C((-2.95,47.12,-1.10),(6.10,1.05,3.25),86,pivot=(.05,47.55,.52),rotation=(0,0,-2)),
    ])
    ribbons=v1.bone('ouros_v32_reference_head_ribbons','head_angle',[0,40,1],[
      C((-5.12,37.92,.20),(1.60,4.05,2.38),83,pivot=(-4.25,39.70,1.35),rotation=(0,-7,18)),
      C((-5.42,40.58,.72),(1.48,3.55,2.05),82,pivot=(-4.60,42.00,1.65),rotation=(0,-10,27)),
      C((-5.02,43.14,1.02),(1.30,2.52,1.72),83,pivot=(-4.30,44.18,1.86),rotation=(0,-12,34)),
      C((3.55,38.82,.52),(1.00,2.80,1.68),83,pivot=(4.05,40.03,1.44),rotation=(0,8,-13)),
    ])
    collar=v1.bone('ouros_v32_reference_collar','neck',[0,32.1,-.1],[
      C((-3.15,31.08,-2.40),(6.30,1.12,4.60),82),
      C((-2.80,31.62,-2.55),(5.60,.66,4.90),83),
    ])
    bow=v1.bone('ouros_v32_reference_bow','torso3',[0,30.1,-3],[
      C((-3.65,29,-4.10),(3.20,1.55,.68),84,pivot=(-1.75,29.75,-3.75),rotation=(0,0,-17)),
      C((.45,29,-4.10),(3.20,1.55,.68),84,pivot=(1.75,29.75,-3.75),rotation=(0,0,17)),
      C((-1.20,28.30,-4.28),(2.40,2.40,.86),90,pivot=(0,29.50,-3.85),rotation=(0,0,45)),
      C((-.55,27.20,-4.12),(1.10,1.55,.55),86),
    ])
    bodice=v1.bone('ouros_v32_reference_bodice','torso3',[0,26.4,-.5],[
      C((-4.05,23,-3.70),(3.25,6.25,.72),88,pivot=(-2.10,26.10,-3.35),rotation=(0,0,-3)),
      C((.80,23,-3.70),(3.25,6.25,.72),88,pivot=(2.10,26.10,-3.35),rotation=(0,0,3)),
      C((-4.20,23,-2.95),(.72,6.20,5.55),87),C((3.48,23,-2.95),(.72,6.20,5.55),87),
      C((-3.20,23.25,1.55),(6.40,5.75,.72),89),
      C((-2.60,23.45,-4.08),(.62,2.30,.30),84),C((1.98,23.45,-4.08),(.62,2.30,.30),84),
      C((-.35,23.20,-4.14),(.70,3.10,.32),85),
    ])
    sl=v1.bone('ouros_v32_reference_sleeve_left','arm_left',[4.5,29.7,-.4],[
      C((2.05,28.20,-1.78),(6.25,2.95,2.85),88,inflate=.04),C((7.80,28.15,-2.10),(4.15,3.00,3.45),87,inflate=.03),
    ])
    sr=v1.bone('ouros_v32_reference_sleeve_right','arm_right',[-4.5,29.7,-.4],[
      C((-8.30,28.20,-1.78),(6.25,2.95,2.85),88,inflate=.04),C((-11.95,28.15,-2.10),(4.15,3.00,3.45),87,inflate=.03),
    ])
    gl=v1.bone('ouros_v32_reference_glove_left','arm_left2',[12,29.4,-.4],[
      C((11.15,28,-2.30),(2.25,3.20,3.85),84),C((12.25,28.15,-2.60),(4.35,2.95,4.45),80),C((15.95,28.20,-2.80),(3.20,2.80,4.80),81),
    ])
    gr=v1.bone('ouros_v32_reference_glove_right','arm_right2',[-12,29.4,-.4],[
      C((-13.40,28,-2.30),(2.25,3.20,3.85),84),C((-16.60,28.15,-2.60),(4.35,2.95,4.45),80),C((-19.15,28.20,-2.80),(3.20,2.80,4.80),81),C((-19.45,27.55,-.80),(.65,2.20,.65),90),
    ])
    waist=v1.bone('ouros_v32_reference_waist','torso',[0,20,-.2],[
      C((-6.55,19.05,-4.05),(13.10,1.55,8.00),88),C((-6.10,18.35,-3.75),(12.20,1.05,7.45),87),
    ])
    # Blue is now mostly the under-skirt at side/back. The front white apron is
    # two broad overlapping shallow masses, not diagonal cut-out panels.
    skirt=v1.bone('ouros_v32_reference_apron_skirt','torso',[0,18.5,-.2],[
      C((-7.05,11.30,-2.90),(2.15,7.15,6.00),83,pivot=(-5.80,17.90,.05),rotation=(0,0,-5)),
      C((4.90,11.30,-2.90),(2.15,7.15,6.00),83,pivot=(5.80,17.90,.05),rotation=(0,0,5)),
      C((-5.70,11.25,2.75),(11.40,7.05,.72),82,pivot=(0,17.85,3.12),rotation=(-3,0,0)),
      C((-5.65,10.90,-4.25),(11.30,7.45,.70),87,pivot=(0,17.75,-3.88),rotation=(2,0,0)),
      C((-5.05,10.35,-4.58),(10.10,7.55,.60),88,pivot=(0,17.35,-4.28),rotation=(3,0,0)),
      C((-5.35,10.20,-4.82),(10.70,.80,.34),84),
      C((-4.55,11.55,-4.78),(9.10,.56,.30),85),
    ])
    # Deliberately no ouros leg/boot bones: the reference keeps Lucario-shaped
    # dark lower legs and toes rather than square costume boots.
    return [hat,ribbons,collar,bow,bodice,sl,sr,gl,gr,waist,skirt]

def paint_pixel(r,g,b,a,x,y,*,shiny):
    if a==0:return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    cream=r>170 and g>135 and b<205; white=r>210 and g>210 and b>210; red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red:return r,g,b,a
    if b>r*1.15 and b>g*1.04 and sat>20:
      # Stronger navy/teal suppression keeps the official tail/anatomy from
      # overpowering the costume while retaining Lucario's species identity.
      targets=((7,57,91),(10,72,108),(4,43,75)) if not shiny else ((18,82,117),(24,96,134),(11,62,98))
      tr,tg,tb=targets[((x//8)+(y//10))%3]; mix=.82
      nr=int(r*(1-mix)+tr*mix); ng=int(g*(1-mix)+tg*mix); nb=int(b*(1-mix)+tb*mix)
      if ((2*x+y)%31) in (0,1): nr+=2; ng+=4; nb+=6
      return max(0,min(255,nr)),max(0,min(255,ng)),max(0,min(255,nb)),a
    if lum<155:
      tr,tg,tb=((22,25,33),(31,34,43),(15,19,27))[((x//9)+(y//8))%3]; mix=.72
      return int(r*(1-mix)+tr*mix),int(g*(1-mix)+tg*mix),int(b*(1-mix)+tb*mix),a
    return r,g,b,a

def post_patch():
    data=json.loads(v1.MANIFEST.read_text(encoding='utf-8'))
    data['concept']='Owner Reference Replica — Blue/White Maid Lucario V32'; data['artStatus']='ARTISTIC FAIL'; data['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=data['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+len(cosmetic_bones()); p['cosmeticBoneCount']=len(cosmetic_bones()); p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=data['builder']; b['scriptPath']='tools/cobblemon-model-review/build_lucario_owner_reference_v31.py'; b['command']=['python',b['scriptPath']]
    out=[]
    for x in b['outputs']:
      for old in ('v22-derived-normal.json','v29-reference-derived-normal.json','v30-reference-derived-normal.json','v31-reference-derived-normal.json'): x=x.replace(old,'v32-reference-derived-normal.json')
      for old in ('v22-derived-shiny.json','v29-reference-derived-shiny.json','v30-reference-derived-shiny.json','v31-reference-derived-shiny.json'): x=x.replace(old,'v32-reference-derived-shiny.json')
      out.append(x)
    b['outputs']=list(dict.fromkeys(out))
    q=data['qualityIntent']; q['signaturePieces']=['Irregular stacked cool-white cap with low blue band and dominant left-side ribbon stack','White fitted bodice/sleeves with blue cuffs, charcoal oversized gloves and silver/blue clasp','Broad solid white apron front over blue side/back under-skirt; official Lucario-shaped dark legs and feet remain exposed']
    q['macroFormPlan']='V32 removes V31 cosmetic leg/boot shells, replaces the angular apron read with one broad white-dominant front bell/trapezoid, and offsets the cap layers so the headpiece no longer reads as a symmetric tower.'
    q['paintPlan']='Normal/shiny derive independently from exact official 1.7.3 baselines. Biological blue is pushed to deep navy/teal, especially reducing tail dominance; accessory overlay remains dedicated cool white/grey, periwinkle/royal blue and charcoal.'
    q['gameplayReadGoal']='At 160 px match the supplied target first read: irregular tall white headpiece, black/red face, white sleeves/bodice, broad white apron, blue side/back skirt, dark gloves and Lucario-shaped dark legs/feet.'
    q['iterationNote']='V31 exact-head evidence passed technical visual floors but direct comparison to owner image SHA-256 28da2aa76025e2c2e625eb8df60153656d6ea17289cfb2a56da62f9159e3e419 still showed square boot shells, too much blue under-skirt, fragmented apron shapes, regular cap stacking and a bright blue tail. V32 fixes those visible mismatches without changing the official 87-bone anatomy or thresholds.'
    data['variantCoverage']['variants'][0]['coverage']='Default preserves exact official 87-bone anatomy plus V32 costume and independent normal paint.'; data['variantCoverage']['variants'][1]['coverage']='Shiny uses identical V32 costume geometry and independent official-shiny derivation.'
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=paint_pixel; v22.write_overlay=write_overlay; v22.main(); post_patch()
if __name__=='__main__':main()
