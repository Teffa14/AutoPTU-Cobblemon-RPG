#!/usr/bin/env python3
"""Lucario V36: owner-reference macroform reset.

V35 passed technical review but direct comparison against the owner's supplied
reference exposed the wrong construction strategy: too many stacked head slabs,
short/bulky sleeve shells, and a front apron assembled like a hanging placard.
V36 rebuilds the costume from the reference's large reads: a short broad folded
chef/maid cap with a dominant left blue ribbon, long narrow white sleeves, a fitted
white bodice, and a hip-wrapping white apron with blue confined to the rear/side
under-skirt. Official 87-bone Lucario anatomy remains byte-equivalent. Presentation
only; AutoPTU/Ouros owns all tactical battle facts.
"""
from __future__ import annotations
import importlib.util,json,struct,zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
V22=ROOT/'tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py'
spec=importlib.util.spec_from_file_location('owner_reference_v36_v22',V22)
if spec is None or spec.loader is None: raise SystemExit('cannot load validated V22 professional pipeline')
v22=importlib.util.module_from_spec(spec); spec.loader.exec_module(v22); v1=v22.v1
NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v36-reference-derived-normal.json'; SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v36-reference-derived-shiny.json'; v22.NORMAL_META=NORMAL_META; v22.SHINY_META=SHINY_META

# Accessory palette sampled toward the owner reference: cool whites, periwinkle,
# saturated blue/teal and readable charcoal rather than near-black slabs.
PALETTE={
  80:(29,31,39,255),81:(43,45,54,255),82:(5,72,112,255),83:(7,105,153,255),
  84:(69,82,188,255),85:(129,143,202,255),86:(180,186,203,255),87:(211,216,226,255),
  88:(235,238,243,255),89:(197,202,214,255),90:(137,145,155,255),91:(24,29,39,255),
  92:(12,89,137,255)
}
def chunk(k,p): return struct.pack('>I',len(p))+k+p+struct.pack('>I',zlib.crc32(k+p)&0xffffffff)
def write_overlay(path):
    w,h=128,64; px=bytearray(w*h*4)
    for x,c in PALETTE.items(): i=((63*w)+x)*4; px[i:i+4]=bytes(c)
    raw=bytearray(); s=w*4
    for y in range(h): raw.append(0); raw.extend(px[y*s:(y+1)*s])
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(raw),9))+chunk(b'IEND',b''))
def C(o,s,c,**kw): return v1.cube(o,s,c,**kw)

def cosmetic_bones():
    # Reference read: one broad folded cap, not a six-storey tower. Four overlapping
    # white masses make the crown; the teal/blue base is a separate brim/band.
    hat=v1.bone('ouros_v36_reference_headpiece','head_angle',[0,39,-.1],[
      C((-5.15,37.80,-2.72),(10.30,1.35,6.05),82),
      C((-4.72,38.85,-2.48),(9.45,2.25,5.62),87,pivot=(-.10,39.90,.25),rotation=(0,0,2)),
      C((-4.35,40.70,-2.22),(8.90,2.55,5.20),88,pivot=(.20,41.90,.38),rotation=(0,0,-2)),
      C((-3.88,42.95,-1.95),(8.05,2.75,4.78),87,pivot=(-.10,44.10,.50),rotation=(0,0,2)),
      C((-3.38,45.25,-1.68),(7.15,1.55,4.36),88,pivot=(.15,45.95,.58),rotation=(0,0,-2)),
    ])
    # The owner's image has a dominant vertical blue stack on the viewer-left side.
    ribbons=v1.bone('ouros_v36_reference_head_ribbons','head_angle',[0,40,1],[
      C((-6.30,38.20,.05),(2.45,3.55,2.55),83,pivot=(-5.05,39.55,1.28),rotation=(0,-9,15)),
      C((-6.45,40.95,.35),(2.38,3.65,2.42),82,pivot=(-5.15,42.35,1.48),rotation=(0,-10,23)),
      C((-6.15,43.78,.66),(2.15,3.25,2.22),83,pivot=(-5.02,45.02,1.68),rotation=(0,-11,28)),
      C((4.08,41.05,.42),(1.05,2.35,1.70),83,pivot=(4.50,42.08,1.30),rotation=(0,8,-16)),
    ])
    collar=v1.bone('ouros_v36_reference_collar','neck',[0,32.1,-.1],[
      C((-3.30,31.00,-2.48),(6.60,1.18,4.78),82),
      C((-2.88,31.58,-2.58),(5.76,.72,4.98),83),
    ])
    bow=v1.bone('ouros_v36_reference_bow','torso3',[0,30.1,-3],[
      C((-3.55,29.05,-4.12),(3.10,1.42,.66),84,pivot=(-1.75,29.72,-3.78),rotation=(0,0,-16)),
      C((.45,29.05,-4.12),(3.10,1.42,.66),84,pivot=(1.75,29.72,-3.78),rotation=(0,0,16)),
      C((-1.12,28.45,-4.30),(2.24,2.24,.84),90,pivot=(0,29.48,-3.88),rotation=(0,0,45)),
    ])
    # Fitted torso: one coherent white front shell with side wrap and restrained blue
    # vertical accents. This removes the V35 collage of small plates.
    bodice=v1.bone('ouros_v36_reference_bodice','torso3',[0,26.4,-.5],[
      C((-4.05,22.85,-3.78),(8.10,6.55,.92),88),
      C((-4.28,23.02,-2.96),(.78,6.20,5.55),87),
      C((3.50,23.02,-2.96),(.78,6.20,5.55),87),
      C((-3.22,23.28,1.55),(6.44,5.78,.72),89),
      C((-2.72,23.40,-4.10),(.55,2.65,.30),84),
      C((2.17,23.40,-4.10),(.55,2.65,.30),84),
      C((-.30,23.18,-4.15),(.60,3.20,.32),85),
    ])
    # Long, narrow sleeve reads from the target. Two overlapping segments cover the
    # animated upper-arm route without balloon shoulders.
    sl=v1.bone('ouros_v36_reference_sleeve_left','arm_left',[4.5,29.7,-.4],[
      C((2.20,28.62,-1.58),(5.35,2.35,2.55),88,inflate=.03),
      C((7.18,28.52,-1.68),(4.55,2.32,2.75),87,inflate=.03),
    ])
    sr=v1.bone('ouros_v36_reference_sleeve_right','arm_right',[-4.5,29.7,-.4],[
      C((-7.55,28.62,-1.58),(5.35,2.35,2.55),88,inflate=.03),
      C((-11.73,28.52,-1.68),(4.55,2.32,2.75),87,inflate=.03),
    ])
    # Large charcoal gloves remain a signature piece, but use two masses rather than
    # V35's brick pile. Blue cuff lives at the biological wrist transition.
    gl=v1.bone('ouros_v36_reference_glove_left','arm_left2',[12,29.4,-.4],[
      C((10.92,28.20,-2.18),(2.15,2.92,3.72),84),
      C((12.25,28.10,-2.48),(4.65,3.02,4.22),80),
      C((16.15,28.18,-2.62),(2.65,2.88,4.42),81),
    ])
    gr=v1.bone('ouros_v36_reference_glove_right','arm_right2',[-12,29.4,-.4],[
      C((-13.07,28.20,-2.18),(2.15,2.92,3.72),84),
      C((-16.90,28.10,-2.48),(4.65,3.02,4.22),80),
      C((-19.55,28.18,-2.62),(2.65,2.88,4.42),81),
      C((-19.65,27.55,-.70),(.62,2.15,.62),90),
    ])
    # Wide, shallow white waist ring is the upper skirt flare visible in the target.
    waist=v1.bone('ouros_v36_reference_waist','torso',[0,20,-.2],[
      C((-6.30,18.78,-4.00),(12.60,1.70,7.88),88),
      C((-5.88,18.15,-3.78),(11.76,.92,7.46),87),
    ])
    # Apron is built as a curved three-part front plus white side-wraps. The center
    # stays visually continuous while the side panels rotate around the hips. Blue
    # exists only behind/at the rear quarter, matching the owner reference.
    skirt=v1.bone('ouros_v36_reference_apron_skirt','torso',[0,18.2,-.2],[
      # rear/side blue under-skirt
      C((-5.72,9.30,2.50),(11.44,8.45,1.05),82,pivot=(0,17.25,2.95),rotation=(-2,0,0)),
      C((-6.28,9.35,-.10),(1.85,8.45,4.30),83,pivot=(-5.30,17.15,1.65),rotation=(0,-7,-4)),
      C((4.43,9.35,-.10),(1.85,8.45,4.30),83,pivot=(5.30,17.15,1.65),rotation=(0,7,4)),
      # upper white apron bridge
      C((-4.35,13.40,-5.05),(8.70,4.15,1.22),87),
      # lower front is two broad, barely-rotated halves to create a wrapped bell
      C((-5.25,8.72,-5.08),(5.45,5.25,1.30),88,pivot=(-2.45,13.25,-4.55),rotation=(0,-5,0)),
      C((-.20,8.72,-5.08),(5.45,5.25,1.30),88,pivot=(2.45,13.25,-4.55),rotation=(0,5,0)),
      # white hip wraps continue the apron around both sides
      C((-6.22,9.28,-4.05),(2.05,8.10,3.55),87,pivot=(-5.18,16.95,-2.15),rotation=(0,-16,-3)),
      C((4.17,9.28,-4.05),(2.05,8.10,3.55),87,pivot=(5.18,16.95,-2.15),rotation=(0,16,3)),
      # soft lower trim from the supplied render
      C((-5.35,9.12,-5.38),(10.70,.68,.30),85),
      C((-5.55,8.54,-5.42),(11.10,.58,.30),84),
    ])
    return [hat,ribbons,collar,bow,bodice,sl,sr,gl,gr,waist,skirt]

def paint_pixel(r,g,b,a,x,y,*,shiny):
    if a==0:return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    cream=r>170 and g>135 and b<205; white=r>210 and g>210 and b>210; red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red:return r,g,b,a
    # Target biology is saturated blue/teal, not the nearly-black V35 blue.
    if b>r*1.15 and b>g*1.04 and sat>20:
      t=((9,73,109),(11,94,137),(5,57,92)) if not shiny else ((19,86,121),(25,103,142),(12,67,101))
      tr,tg,tb=t[((x//8)+(y//10))%3]; m=.78
      return int(r*(1-m)+tr*m),int(g*(1-m)+tg*m),int(b*(1-m)+tb*m),a
    if lum<155:
      tr,tg,tb=((34,35,43),(47,48,57),(25,27,35))[((x//9)+(y//8))%3]; m=.68
      return int(r*(1-m)+tr*m),int(g*(1-m)+tg*m),int(b*(1-m)+tb*m),a
    return r,g,b,a

def post_patch():
    d=json.loads(v1.MANIFEST.read_text(encoding='utf-8'))
    d['concept']='Owner Reference Replica — Blue/White Maid Lucario V36'
    d['artStatus']='ARTISTIC FAIL'
    d['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=d['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+len(cosmetic_bones()); p['cosmeticBoneCount']=len(cosmetic_bones()); p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=d['builder']; b['scriptPath']='tools/cobblemon-model-review/build_lucario_owner_reference_v31.py'; b['command']=['python',b['scriptPath']]
    out=[]
    for x in b['outputs']:
      for old in ('v22-derived-normal.json','v29-reference-derived-normal.json','v30-reference-derived-normal.json','v31-reference-derived-normal.json','v32-reference-derived-normal.json','v33-reference-derived-normal.json','v34-reference-derived-normal.json','v35-reference-derived-normal.json'): x=x.replace(old,'v36-reference-derived-normal.json')
      for old in ('v22-derived-shiny.json','v29-reference-derived-shiny.json','v30-reference-derived-shiny.json','v31-reference-derived-shiny.json','v32-reference-derived-shiny.json','v33-reference-derived-shiny.json','v34-reference-derived-shiny.json','v35-reference-derived-shiny.json'): x=x.replace(old,'v36-reference-derived-shiny.json')
      out.append(x)
    b['outputs']=list(dict.fromkeys(out))
    q=d['qualityIntent']
    q['signaturePieces']=['Short broad folded cool-white cap with thick teal base and dominant left blue ribbon stack','Slim white fitted bodice and long narrow sleeves with periwinkle cuffs, charcoal gloves and silver clasp','Broad bell-like white apron wrapping the hips; blue confined to rear/side under-skirt']
    q['macroFormPlan']='V36 is a macroform reset from the owner image: four broad cap masses instead of a stacked tower, long narrow arm coverage instead of balloon sleeves, and a three-part curved apron plus side wraps instead of a placard.'
    q['paintPlan']='Normal/shiny derive independently from exact official 1.7.3 baselines. Biological blue is restored toward the owner reference saturated teal/navy range; charcoal gains readable value steps; accessory overlay keeps cool-white, periwinkle and blue.'
    q['gameplayReadGoal']='At 160 px match the supplied reference first read: broad short white cap with large left ribbon, black/red face, narrow white upper body and arms, oversized dark gloves, and one wide hip-wrapping white apron over blue rear skirt.'
    q['iterationNote']='Owner reference was reopened directly before V36. V35 technical success is explicitly rejected as an artistic basis; V36 replaces the wrong macroform strategy rather than applying proportional micro-adjustments. Owner approval remains absent.'
    d['variantCoverage']['variants'][0]['coverage']='Default preserves exact official 87-bone anatomy plus V36 owner-reference costume and independent normal paint.'
    d['variantCoverage']['variants'][1]['coverage']='Shiny uses identical V36 costume geometry and independent official-shiny derivation.'
    v1.MANIFEST.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def main():
    v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=paint_pixel; v22.write_overlay=write_overlay
    v22.main(); post_patch()
if __name__=='__main__': main()
