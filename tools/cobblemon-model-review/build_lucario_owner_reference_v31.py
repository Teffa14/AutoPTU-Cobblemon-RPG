#!/usr/bin/env python3
"""Lucario V37: owner-reference primary-mass reconstruction.

V36b proved that adding more small panels made the replica noisier and less faithful.
The supplied owner reference is built from a few decisive voxel masses: a tall folded
white cap that encloses the ear bases, a large left blue ribbon, a narrow white torso
with long sleeves, oversized charcoal gloves, and one deep continuous white apron over
a blue rear/side under-skirt. V37 reconstructs those reads directly.

The exact official Cobblemon 1.7.3 Lucario anatomy remains JSON-equivalent. Cosmetic
bones only add presentation geometry. AutoPTU/Ouros remains authoritative for every
battle fact and outcome.
"""
from __future__ import annotations
import importlib.util,json,struct,zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
V22=ROOT/'tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py'
spec=importlib.util.spec_from_file_location('owner_reference_v37_v22',V22)
if spec is None or spec.loader is None: raise SystemExit('cannot load validated V22 professional pipeline')
v22=importlib.util.module_from_spec(spec); spec.loader.exec_module(v22); v1=v22.v1
NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v37-reference-derived-normal.json'
SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v37-reference-derived-shiny.json'
v22.NORMAL_META=NORMAL_META; v22.SHINY_META=SHINY_META

PALETTE={
  80:(27,29,36,255),81:(43,45,54,255),82:(5,73,112,255),83:(8,104,153,255),
  84:(62,76,187,255),85:(124,139,201,255),86:(176,183,202,255),87:(210,215,226,255),
  88:(235,238,243,255),89:(195,201,213,255),90:(137,145,155,255),91:(22,26,34,255),
  92:(10,88,136,255),93:(155,166,211,255)
}
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
    # CAP: tall enough to enclose the official ear bases, but each fold has a
    # different offset/rotation so the silhouette reads as folded cloth, not a tower.
    hat=v1.bone('ouros_v37_reference_cap','head_angle',[0,39,-.2],[
      C((-5.15,37.70,-3.02),(10.30,1.42,6.10),82,pivot=(0,38.38,.02),rotation=(0,0,-2)),
      C((-4.72,38.72,-2.78),(9.42,2.10,5.65),88,pivot=(-.48,39.72,.12),rotation=(0,0,5)),
      C((-4.30,40.40,-2.48),(8.80,2.35,5.18),87,pivot=(.36,41.52,.25),rotation=(0,0,-4)),
      C((-3.92,42.32,-2.20),(8.12,2.30,4.72),89,pivot=(-.22,43.42,.35),rotation=(0,0,4)),
      C((-3.40,44.10,-1.88),(7.05,2.18,4.18),88,pivot=(.18,45.15,.45),rotation=(0,0,-3)),
    ])
    # Dominant ribbon is moved onto the camera-facing/front half of the left cap.
    # It breaks the vertical crown silhouette exactly where the supplied image does.
    ribbons=v1.bone('ouros_v37_reference_cap_ribbons','head_angle',[0,40,-1],[
      C((-6.20,38.65,-2.35),(2.55,3.35,2.65),83,pivot=(-4.95,40.15,-1.10),rotation=(0,-8,18)),
      C((-6.42,41.05,-2.05),(2.48,3.40,2.55),82,pivot=(-5.12,42.48,-.82),rotation=(0,-10,27)),
      C((-6.02,43.62,-1.72),(2.22,3.00,2.35),83,pivot=(-4.82,44.85,-.58),rotation=(0,-12,32)),
      C((3.82,39.70,-1.78),(1.15,2.45,1.90),83,pivot=(4.34,40.80,-.88),rotation=(0,8,-15)),
    ])
    collar=v1.bone('ouros_v37_reference_collar','neck',[0,32.0,-.2],[
      C((-3.18,30.98,-2.62),(6.36,1.00,4.92),82),
      C((-2.62,31.55,-2.72),(5.24,.55,5.12),83),
    ])
    # Compact bow/clasp. The center plate is large enough to visually replace the
    # biological cream chest-spike read in the supplied reference.
    clasp=v1.bone('ouros_v37_reference_clasp','torso3',[0,29.7,-3.3],[
      C((-3.50,28.82,-4.18),(3.05,1.35,.58),84,pivot=(-1.72,29.45,-3.82),rotation=(0,0,-19)),
      C((.45,28.82,-4.18),(3.05,1.35,.58),84,pivot=(1.72,29.45,-3.82),rotation=(0,0,19)),
      C((-1.20,27.82,-4.42),(2.40,2.55,.95),90,pivot=(0,29.10,-3.95),rotation=(0,0,45)),
      C((-.55,26.70,-4.34),(1.10,2.20,.82),87),
    ])
    # One fitted torso shell. Front is deliberately uninterrupted white; three small
    # periwinkle bars provide the exact target's restrained vertical accents.
    bodice=v1.bone('ouros_v37_reference_bodice','torso3',[0,26.2,-.5],[
      C((-4.00,22.70,-3.82),(8.00,6.35,1.02),88),
      C((-4.22,22.92,-2.88),(.72,5.92,5.42),87,pivot=(-3.86,25.88,-.25),rotation=(0,-4,0)),
      C((3.50,22.92,-2.88),(.72,5.92,5.42),87,pivot=(3.86,25.88,-.25),rotation=(0,4,0)),
      C((-3.10,23.20,1.58),(6.20,5.45,.65),89),
      C((-2.48,23.30,-4.12),(.52,2.55,.28),84),
      C((1.96,23.30,-4.12),(.52,2.55,.28),84),
      C((-.26,23.00,-4.14),(.52,3.05,.28),93),
    ])
    # Long two-stage sleeves. The target does not have shoulder armor or ballooning.
    sl=v1.bone('ouros_v37_reference_sleeve_left','arm_left',[4.5,29.7,-.4],[
      C((2.15,28.42,-1.70),(5.20,2.35,2.58),88,pivot=(4.70,29.57,-.40),rotation=(0,0,-4)),
      C((6.92,28.28,-1.90),(4.55,2.55,2.96),87,pivot=(9.00,29.52,-.40),rotation=(0,0,-2)),
    ])
    sr=v1.bone('ouros_v37_reference_sleeve_right','arm_right',[-4.5,29.7,-.4],[
      C((-7.35,28.42,-1.70),(5.20,2.35,2.58),88,pivot=(-4.70,29.57,-.40),rotation=(0,0,4)),
      C((-11.47,28.28,-1.90),(4.55,2.55,2.96),87,pivot=(-9.00,29.52,-.40),rotation=(0,0,2)),
    ])
    gl=v1.bone('ouros_v37_reference_glove_left','arm_left2',[12,29.4,-.4],[
      C((11.05,28.00,-2.32),(2.35,3.05,3.84),84),
      C((12.70,27.95,-2.72),(5.65,3.05,4.55),80,pivot=(15.35,29.45,-.35),rotation=(0,0,-2)),
    ])
    gr=v1.bone('ouros_v37_reference_glove_right','arm_right2',[-12,29.4,-.4],[
      C((-13.40,28.00,-2.32),(2.35,3.05,3.84),84),
      C((-18.35,27.95,-2.72),(5.65,3.05,4.55),80,pivot=(-15.35,29.45,-.35),rotation=(0,0,2)),
      C((-19.20,27.55,-.70),(.62,2.15,.62),90),
    ])
    # White waist flare is a single broad bridge, matching the owner reference's
    # continuous horizontal lip above the apron.
    waist=v1.bone('ouros_v37_reference_waist','torso',[0,19.7,-.3],[
      C((-6.25,17.48,-4.18),(12.50,2.05,7.95),88),
      C((-5.72,17.12,-4.42),(11.44,.58,.42),85),
    ])
    # PRIMARY SKIRT MASSES. Blue is only rear/side under-skirt. The apron is one deep
    # white volume joined to two white side wraps. No fragmented tiles or central gap.
    skirt=v1.bone('ouros_v37_reference_apron_skirt','torso',[0,17.8,-.3],[
      # blue under-skirt: three pieces
      C((-5.70,9.20,2.18),(11.40,8.35,1.38),82,pivot=(0,17.00,2.82),rotation=(-3,0,0)),
      C((-6.45,9.38,-1.78),(2.45,8.10,4.75),83,pivot=(-5.18,16.90,.58),rotation=(0,-7,-4)),
      C((4.00,9.38,-1.78),(2.45,8.10,4.75),83,pivot=(5.18,16.90,.58),rotation=(0,7,4)),
      # white apron: one substantial front body, then two wraps around hips
      C((-5.20,10.05,-5.18),(10.40,7.35,3.30),88,pivot=(0,17.12,-3.45),rotation=(2,0,0)),
      C((-6.18,10.42,-3.75),(1.82,6.95,4.62),87,pivot=(-5.20,16.82,-1.62),rotation=(0,-13,-3)),
      C((4.36,10.42,-3.75),(1.82,6.95,4.62),87,pivot=(5.20,16.82,-1.62),rotation=(0,13,3)),
      # broad cool-white upper apron/yoke integrates with waist instead of hanging
      C((-5.62,15.42,-4.80),(11.24,2.00,3.15),89,pivot=(0,17.05,-3.20),rotation=(3,0,0)),
      # restrained lavender hem from supplied target
      C((-5.08,9.72,-5.42),(10.16,.58,.34),85),
      C((-4.72,9.16,-5.43),(9.44,.46,.30),84),
    ])
    return [hat,ribbons,collar,clasp,bodice,sl,sr,gl,gr,waist,skirt]

# UV rectangles of the exact official 1.7.3 Lucario ear cubes. The target reference
# has no bright-blue upright ear posts above the cap; where the cap does not occlude
# them, dark biology must read as charcoal. Cream biology is likewise neutralized to
# cool grey/white because the target's chest/waist landmarks are silver/white.
def in_rect(x,y,x0,y0,x1,y1): return x0 <= x < x1 and y0 <= y < y1
def ear_texel(x,y):
    return (in_rect(x,y,15,21,41,31) or in_rect(x,y,0,34,9,46))
def paint_pixel(r,g,b,a,x,y,*,shiny):
    if a==0:return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    red=r>105 and r>g*1.35 and r>b*1.35
    white=r>210 and g>210 and b>210
    cream=r>170 and g>135 and b<205
    if red:return r,g,b,a
    if ear_texel(x,y):
      base=((37,39,47),(48,50,59),(28,30,38))[((x//4)+(y//4))%3]
      return (*base,a)
    if cream:
      base=((188,194,205),(210,215,224),(169,176,190))[((x//5)+(y//5))%3]
      return (*base,a)
    if white:return r,g,b,a
    if b>r*1.15 and b>g*1.04 and sat>20:
      t=((7,77,116),(10,101,148),(5,63,98)) if not shiny else ((18,88,126),(24,108,149),(12,71,105))
      tr,tg,tb=t[((x//8)+(y//10))%3]; m=.80
      return int(r*(1-m)+tr*m),int(g*(1-m)+tg*m),int(b*(1-m)+tb*m),a
    if lum<155:
      tr,tg,tb=((33,35,43),(46,48,57),(25,27,35))[((x//9)+(y//8))%3]; m=.70
      return int(r*(1-m)+tr*m),int(g*(1-m)+tg*m),int(b*(1-m)+tb*m),a
    return r,g,b,a

def post_patch():
    d=json.loads(v1.MANIFEST.read_text(encoding='utf-8'))
    d['concept']='Owner Reference Replica — Blue/White Maid Lucario V37'
    d['artStatus']='ARTISTIC FAIL'
    d['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=d['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+len(cosmetic_bones()); p['cosmeticBoneCount']=len(cosmetic_bones()); p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=d['builder']; b['scriptPath']='tools/cobblemon-model-review/build_lucario_owner_reference_v31.py'; b['command']=['python',b['scriptPath']]
    out=[]
    for x in b['outputs']:
      for old in ('v22-derived-normal.json','v29-reference-derived-normal.json','v30-reference-derived-normal.json','v31-reference-derived-normal.json','v32-reference-derived-normal.json','v33-reference-derived-normal.json','v34-reference-derived-normal.json','v35-reference-derived-normal.json','v36-reference-derived-normal.json'):
        x=x.replace(old,'v37-reference-derived-normal.json')
      for old in ('v22-derived-shiny.json','v29-reference-derived-shiny.json','v30-reference-derived-shiny.json','v31-reference-derived-shiny.json','v32-reference-derived-shiny.json','v33-reference-derived-shiny.json','v34-reference-derived-shiny.json','v35-reference-derived-shiny.json','v36-reference-derived-shiny.json'):
        x=x.replace(old,'v37-reference-derived-shiny.json')
      out.append(x)
    b['outputs']=list(dict.fromkeys(out))
    q=d['qualityIntent']
    q['signaturePieces']=['Tall irregular folded cool-white cap enclosing official ear bases, with dominant camera-facing left blue ribbon stack','Narrow uninterrupted white bodice and long white sleeves with periwinkle cuffs, oversized charcoal gloves and silver clasp','One deep continuous white apron volume wrapping the hips over a three-piece blue rear/side under-skirt']
    q['macroFormPlan']='V37 rejects V36b panel fragmentation. It uses a few large primary masses matching the supplied target: five cap folds, one torso shell, two-stage sleeves, two-piece gloves, one waist bridge, one deep apron front, two white hip wraps and three blue under-skirt masses.'
    q['paintPlan']='Normal/shiny derive independently from exact official 1.7.3 baselines. Exact official ear UV regions are repainted charcoal so uncovered ear tips cannot read as blue posts; cream biological landmarks are neutralized to cool grey/white; remaining blue biology stays saturated teal/navy like the target.'
    q['gameplayReadGoal']='At 160 px the first read must match the owner image: tall folded white cap with obvious left blue ribbon, black/red face, narrow white upper body with long sleeves, giant dark gloves, broad white hip volume and only secondary blue side/rear skirt.'
    q['antiPatternsToReject']=['Blue upright ear posts above the costume cap','Fragmented apron tiles','Detached rectangular apron placard','Stacked symmetric hat tower','Short block-armor sleeves','Any change to official biological bones or battle-state authority']
    q['iterationNote']='V36b exact Blockbench evidence was opened and rejected before this pass. V37 is based on the recovered original owner image, not on technical metrics. Owner approval remains absent.'
    d['variantCoverage']['variants'][0]['coverage']='Default preserves exact official 87-bone anatomy plus V37 owner-reference costume and independent normal paint.'
    d['variantCoverage']['variants'][1]['coverage']='Shiny uses identical V37 costume geometry and independent official-shiny derivation.'
    v1.MANIFEST.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def main():
    v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=paint_pixel; v22.write_overlay=write_overlay
    v22.main(); post_patch()
if __name__=='__main__': main()
