#!/usr/bin/env python3
"""Lucario V38: owner-reference integrated costume reconstruction.

V37 cleaned the silhouette but exact Blockbench evidence still exposed a black/blue
waist gap and a front apron that read as a separate placard. V38 makes the supplied
owner image the only visual target: one continuous deep white apron begins directly
at the waist bridge, blue exists only at the outer/rear skirt, the cap fully encloses
the official ear tips, and the official tail is painted into a subdued dark teal so it
cannot dominate the costume silhouette.

Official Cobblemon 1.7.3 Lucario anatomy remains JSON-equivalent. Presentation only;
AutoPTU/Ouros owns combatants, state, legality, RNG, damage and tactical outcomes.
"""
from __future__ import annotations
import importlib.util,json,struct,zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
V22=ROOT/'tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py'
spec=importlib.util.spec_from_file_location('owner_reference_v38_v22',V22)
if spec is None or spec.loader is None: raise SystemExit('cannot load validated V22 professional pipeline')
v22=importlib.util.module_from_spec(spec); spec.loader.exec_module(v22); v1=v22.v1
NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v38-reference-derived-normal.json'
SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v38-reference-derived-shiny.json'
v22.NORMAL_META=NORMAL_META; v22.SHINY_META=SHINY_META

PALETTE={80:(27,29,36,255),81:(43,45,54,255),82:(5,73,112,255),83:(8,104,153,255),84:(62,76,187,255),85:(124,139,201,255),86:(176,183,202,255),87:(210,215,226,255),88:(235,238,243,255),89:(195,201,213,255),90:(137,145,155,255),91:(22,26,34,255),92:(10,88,136,255),93:(155,166,211,255)}
def chunk(k,p): return struct.pack('>I',len(p))+k+p+struct.pack('>I',zlib.crc32(k+p)&0xffffffff)
def write_overlay(path):
    w,h=128,64; px=bytearray(w*h*4)
    for x,c in PALETTE.items():
        i=((63*w)+x)*4; px[i:i+4]=bytes(c)
    raw=bytearray(); s=w*4
    for y in range(h): raw.append(0); raw.extend(px[y*s:(y+1)*s])
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(raw),9))+chunk(b'IEND',b''))
def C(o,s,c,**kw): return v1.cube(o,s,c,**kw)

def cosmetic_bones():
    # Five broad, offset folds + top lid. The lid exceeds official ear tip height so
    # no dark biological ear plane can puncture the crown in any review angle.
    cap=v1.bone('ouros_v38_reference_cap','head_angle',[0,39,-.2],[
      C((-5.18,37.62,-3.08),(10.36,1.55,6.20),82,pivot=(0,38.38,.02),rotation=(0,0,-2)),
      C((-4.78,38.72,-2.82),(9.55,2.18,5.72),88,pivot=(-.42,39.76,.12),rotation=(0,0,5)),
      C((-4.36,40.45,-2.52),(8.90,2.42,5.24),87,pivot=(.34,41.60,.24),rotation=(0,0,-4)),
      C((-3.98,42.40,-2.24),(8.25,2.42,4.80),89,pivot=(-.18,43.55,.34),rotation=(0,0,4)),
      C((-3.58,44.30,-1.98),(7.42,2.28,4.45),88,pivot=(.16,45.40,.45),rotation=(0,0,-3)),
      C((-3.72,46.18,-2.05),(7.72,.95,4.55),87,pivot=(.12,46.62,.42),rotation=(0,0,2)),
    ])
    ribbons=v1.bone('ouros_v38_reference_cap_ribbons','head_angle',[0,40,-1.2],[
      C((-6.48,38.70,-3.00),(2.78,3.45,2.82),83,pivot=(-5.06,40.20,-1.62),rotation=(0,-8,18)),
      C((-6.68,41.25,-2.72),(2.68,3.45,2.72),82,pivot=(-5.28,42.72,-1.35),rotation=(0,-10,27)),
      C((-6.28,43.82,-2.42),(2.42,3.02,2.52),83,pivot=(-4.98,45.08,-1.18),rotation=(0,-12,32)),
      C((3.88,39.78,-2.40),(1.20,2.52,2.05),83,pivot=(4.44,40.90,-1.38),rotation=(0,8,-15)),
    ])
    collar=v1.bone('ouros_v38_reference_collar','neck',[0,32.0,-.2],[C((-3.18,30.98,-2.62),(6.36,1.00,4.92),82),C((-2.62,31.55,-2.72),(5.24,.55,5.12),83)])
    clasp=v1.bone('ouros_v38_reference_clasp','torso3',[0,29.7,-3.3],[C((-3.50,28.82,-4.18),(3.05,1.35,.58),84,pivot=(-1.72,29.45,-3.82),rotation=(0,0,-19)),C((.45,28.82,-4.18),(3.05,1.35,.58),84,pivot=(1.72,29.45,-3.82),rotation=(0,0,19)),C((-1.20,27.82,-4.42),(2.40,2.55,.95),90,pivot=(0,29.10,-3.95),rotation=(0,0,45)),C((-.58,26.55,-4.34),(1.16,2.42,.84),87)])
    bodice=v1.bone('ouros_v38_reference_bodice','torso3',[0,26.2,-.5],[C((-4.00,22.55,-3.82),(8.00,6.55,1.02),88),C((-4.22,22.80,-2.88),(.72,6.12,5.42),87,pivot=(-3.86,25.88,-.25),rotation=(0,-4,0)),C((3.50,22.80,-2.88),(.72,6.12,5.42),87,pivot=(3.86,25.88,-.25),rotation=(0,4,0)),C((-3.10,23.05,1.58),(6.20,5.72,.65),89),C((-2.48,23.18,-4.12),(.52,2.70,.28),84),C((1.96,23.18,-4.12),(.52,2.70,.28),84),C((-.26,22.88,-4.14),(.52,3.22,.28),93)])
    sl=v1.bone('ouros_v38_reference_sleeve_left','arm_left',[4.5,29.7,-.4],[C((2.15,28.42,-1.70),(5.20,2.35,2.58),88,pivot=(4.70,29.57,-.40),rotation=(0,0,-4)),C((6.92,28.28,-1.90),(4.55,2.55,2.96),87,pivot=(9.00,29.52,-.40),rotation=(0,0,-2))])
    sr=v1.bone('ouros_v38_reference_sleeve_right','arm_right',[-4.5,29.7,-.4],[C((-7.35,28.42,-1.70),(5.20,2.35,2.58),88,pivot=(-4.70,29.57,-.40),rotation=(0,0,4)),C((-11.47,28.28,-1.90),(4.55,2.55,2.96),87,pivot=(-9.00,29.52,-.40),rotation=(0,0,2))])
    gl=v1.bone('ouros_v38_reference_glove_left','arm_left2',[12,29.4,-.4],[C((11.05,28.00,-2.32),(2.35,3.05,3.84),84),C((12.70,27.95,-2.72),(5.65,3.05,4.55),80,pivot=(15.35,29.45,-.35),rotation=(0,0,-2))])
    gr=v1.bone('ouros_v38_reference_glove_right','arm_right2',[-12,29.4,-.4],[C((-13.40,28.00,-2.32),(2.35,3.05,3.84),84),C((-18.35,27.95,-2.72),(5.65,3.05,4.55),80,pivot=(-15.35,29.45,-.35),rotation=(0,0,2)),C((-19.20,27.55,-.70),(.62,2.15,.62),90)])
    # Extend the white waist bridge downward and forward so there is no exposed black
    # belt/gap between torso and apron in neutral or battle poses.
    waist=v1.bone('ouros_v38_reference_waist','torso',[0,19.5,-.3],[
      C((-6.30,16.55,-4.82),(12.60,3.20,8.65),88,pivot=(0,18.20,-.45),rotation=(1,0,0)),
      C((-5.78,16.35,-5.08),(11.56,.52,.40),85),
    ])
    # Under-skirt stays physically outside/behind the apron. Its inner edges start at
    # +/-5.45 so blue cannot intrude into the center front read.
    skirt=v1.bone('ouros_v38_reference_apron_skirt','torso',[0,17.2,-.3],[
      C((-5.85,8.72,2.15),(11.70,8.40,1.42),82,pivot=(0,16.70,2.82),rotation=(-3,0,0)),
      C((-7.05,8.95,-1.25),(1.72,8.15,4.15),83,pivot=(-6.10,16.70,.70),rotation=(0,-7,-4)),
      C((5.33,8.95,-1.25),(1.72,8.15,4.15),83,pivot=(6.10,16.70,.70),rotation=(0,7,4)),
      # Single deep apron body; depth gives the 3/4 image its real skirt volume.
      C((-5.72,9.15,-5.52),(11.44,8.15,4.62),88,pivot=(0,16.95,-3.05),rotation=(2,0,0)),
      # One broad upper facing piece merges apron into the white waist bridge.
      C((-6.00,15.72,-5.20),(12.00,1.72,4.08),89,pivot=(0,17.02,-3.05),rotation=(2,0,0)),
      C((-5.48,8.92,-5.78),(10.96,.64,.34),85),
      C((-5.05,8.35,-5.80),(10.10,.48,.30),84),
    ])
    return [cap,ribbons,collar,clasp,bodice,sl,sr,gl,gr,waist,skirt]

def in_rect(x,y,x0,y0,x1,y1): return x0 <= x < x1 and y0 <= y < y1
# Exact official ear UV neighborhoods from the preserved model.
def ear_texel(x,y): return in_rect(x,y,15,21,41,31) or in_rect(x,y,0,34,9,46)
# Exact official tail UV neighborhoods. A subdued dark teal tail is still biologically
# Lucario but no longer competes with the supplied costume's white/blue hierarchy.
def tail_texel(x,y):
    return (in_rect(x,y,36,14,58,28) or in_rect(x,y,23,37,47,52) or in_rect(x,y,0,35,25,52) or in_rect(x,y,117,10,128,26))
def paint_pixel(r,g,b,a,x,y,*,shiny):
    if a==0:return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    red=r>105 and r>g*1.35 and r>b*1.35; white=r>210 and g>210 and b>210; cream=r>170 and g>135 and b<205
    if red:return r,g,b,a
    if ear_texel(x,y):
      base=((34,36,44),(45,47,56),(25,28,36))[((x//4)+(y//4))%3]; return (*base,a)
    if tail_texel(x,y):
      base=((6,55,79),(7,66,94),(9,47,69))[((x//6)+(y//6))%3]; return (*base,a)
    if cream:
      base=((188,194,205),(210,215,224),(169,176,190))[((x//5)+(y//5))%3]; return (*base,a)
    if white:return r,g,b,a
    if b>r*1.15 and b>g*1.04 and sat>20:
      t=((7,77,116),(10,101,148),(5,63,98)) if not shiny else ((18,88,126),(24,108,149),(12,71,105)); tr,tg,tb=t[((x//8)+(y//10))%3]; m=.80
      return int(r*(1-m)+tr*m),int(g*(1-m)+tg*m),int(b*(1-m)+tb*m),a
    if lum<155:
      tr,tg,tb=((33,35,43),(46,48,57),(25,27,35))[((x//9)+(y//8))%3]; m=.70
      return int(r*(1-m)+tr*m),int(g*(1-m)+tg*m),int(b*(1-m)+tb*m),a
    return r,g,b,a

def post_patch():
    d=json.loads(v1.MANIFEST.read_text(encoding='utf-8'))
    d['concept']='Owner Reference Replica — Blue/White Maid Lucario V38'; d['artStatus']='ARTISTIC FAIL'; d['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=d['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+len(cosmetic_bones()); p['cosmeticBoneCount']=len(cosmetic_bones()); p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=d['builder']; b['scriptPath']='tools/cobblemon-model-review/build_lucario_owner_reference_v31.py'; b['command']=['python',b['scriptPath']]
    out=[]
    for x in b['outputs']:
      for old in ('v22-derived-normal.json','v29-reference-derived-normal.json','v30-reference-derived-normal.json','v31-reference-derived-normal.json','v32-reference-derived-normal.json','v33-reference-derived-normal.json','v34-reference-derived-normal.json','v35-reference-derived-normal.json','v36-reference-derived-normal.json','v37-reference-derived-normal.json'): x=x.replace(old,'v38-reference-derived-normal.json')
      for old in ('v22-derived-shiny.json','v29-reference-derived-shiny.json','v30-reference-derived-shiny.json','v31-reference-derived-shiny.json','v32-reference-derived-shiny.json','v33-reference-derived-shiny.json','v34-reference-derived-shiny.json','v35-reference-derived-shiny.json','v36-reference-derived-shiny.json','v37-reference-derived-shiny.json'): x=x.replace(old,'v38-reference-derived-shiny.json')
      out.append(x)
    b['outputs']=list(dict.fromkeys(out))
    q=d['qualityIntent']; q['signaturePieces']=['Fully enclosed irregular folded white cap with dominant left blue ribbon','Narrow uninterrupted white torso and long sleeves with periwinkle cuffs, oversized charcoal gloves and silver clasp','Continuous deep white apron beginning at the waist, with blue visible only outside/rear and a restrained lavender hem']
    q['macroFormPlan']='V38 removes the remaining waist discontinuity and fragmented skirt logic. One white waist bridge overlaps one deep apron body; blue under-skirt is limited to one rear and two outer side masses.'
    q['paintPlan']='Normal/shiny derive independently from exact official 1.7.3 baselines. Ear UVs are charcoal, cream landmarks cool grey/white, and tail UVs dark teal to prevent the base tail from becoming the dominant costume read.'
    q['gameplayReadGoal']='At 160 px the costume must read as the supplied maid/chef Lucario before the base species: folded white cap, obvious left blue ribbon, white torso/arms, dark gloves and a broad continuous white hip/apron mass.'
    q['antiPatternsToReject']=['Exposed ear holes through cap','Black waist gap','Blue triangles breaking the apron center','Detached apron placard','Bright tail dominating 3/4 silhouette','Any change to official biological bones or battle-state authority']
    q['iterationNote']='V37 exact Blockbench hero/front/battle were opened and rejected because the waist and apron still separated visually. V38 rebuilds that junction and subdues the tail. Owner approval remains absent.'
    d['variantCoverage']['variants'][0]['coverage']='Default preserves exact official 87-bone anatomy plus V38 owner-reference costume and independent normal paint.'; d['variantCoverage']['variants'][1]['coverage']='Shiny uses identical V38 costume geometry and independent official-shiny derivation.'
    v1.MANIFEST.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def main():
    v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=paint_pixel; v22.write_overlay=write_overlay; v22.main(); post_patch()
if __name__=='__main__': main()
