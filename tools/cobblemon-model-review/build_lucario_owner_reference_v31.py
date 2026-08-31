#!/usr/bin/env python3
"""Lucario V36b: owner-reference macroform correction.

The owner rejected the current visual because the cap still read as a tower and the
apron still read as a rigid board. V36b keeps the exact official 87-bone Lucario
biology but replaces those failed reads with a low four-layer cap and a segmented,
close-to-body apron with angled side lobes and a stepped hem. Bodice and sleeves are
thinned/rotated so they wrap the body instead of reading as box armor.

Presentation only. AutoPTU/Ouros owns all tactical battle facts.
"""
from __future__ import annotations
import importlib.util,json,struct,zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
V22=ROOT/'tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py'
spec=importlib.util.spec_from_file_location('owner_reference_v36b_v22',V22)
if spec is None or spec.loader is None: raise SystemExit('cannot load validated V22 professional pipeline')
v22=importlib.util.module_from_spec(spec); spec.loader.exec_module(v22); v1=v22.v1
NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v36-reference-derived-normal.json'; SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v36-reference-derived-shiny.json'; v22.NORMAL_META=NORMAL_META; v22.SHINY_META=SHINY_META
PALETTE={80:(29,31,39,255),81:(43,45,54,255),82:(5,72,112,255),83:(7,105,153,255),84:(69,82,188,255),85:(129,143,202,255),86:(180,186,203,255),87:(211,216,226,255),88:(235,238,243,255),89:(197,202,214,255),90:(137,145,155,255),91:(24,29,39,255),92:(12,89,137,255)}
def chunk(k,p): return struct.pack('>I',len(p))+k+p+struct.pack('>I',zlib.crc32(k+p)&0xffffffff)
def write_overlay(path):
    w,h=128,64; px=bytearray(w*h*4)
    for x,c in PALETTE.items(): i=((63*w)+x)*4; px[i:i+4]=bytes(c)
    raw=bytearray(); s=w*4
    for y in range(h): raw.append(0); raw.extend(px[y*s:(y+1)*s])
    path.parent.mkdir(parents=True,exist_ok=True); path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(raw),9))+chunk(b'IEND',b''))
def C(o,s,c,**kw): return v1.cube(o,s,c,**kw)
def cosmetic_bones():
    # LOW CAP: broad overlapping folds. Total crown height ~3.6, not a tall stack.
    hat=v1.bone('ouros_v36_reference_headpiece','head_angle',[0,39,-.1],[
      C((-5.10,37.95,-2.62),(10.20,.72,5.72),82,pivot=(0,38.25,.20),rotation=(0,0,-2)),
      C((-4.72,38.48,-2.48),(9.44,1.12,5.25),88,pivot=(-.40,39.02,.32),rotation=(0,0,5)),
      C((-4.18,39.30,-2.22),(8.78,1.36,4.72),87,pivot=(.30,39.96,.42),rotation=(0,0,-7)),
      C((-3.62,40.28,-1.92),(7.46,1.18,4.10),89,pivot=(-.10,40.84,.52),rotation=(0,0,4))
    ])
    ribbons=v1.bone('ouros_v36_reference_head_ribbons','head_angle',[0,40,1],[C((-5.28,38.10,.08),(1.66,3.55,2.25),83,pivot=(-4.40,39.58,1.20),rotation=(0,-7,18)),C((-5.50,40.45,.60),(1.48,2.95,1.92),82,pivot=(-4.70,41.70,1.52),rotation=(0,-10,27)),C((-5.02,42.58,.92),(1.28,2.00,1.58),83,pivot=(-4.34,43.42,1.70),rotation=(0,-12,34)),C((3.72,38.72,.42),(.92,2.42,1.52),83,pivot=(4.18,39.84,1.25),rotation=(0,8,-13))])
    collar=v1.bone('ouros_v36_reference_collar','neck',[0,32.1,-.1],[C((-3.20,31.10,-2.42),(6.40,.82,4.58),82,pivot=(0,31.52,-.15),rotation=(0,0,-1)),C((-2.62,31.58,-2.55),(5.24,.48,4.88),83)])
    bow=v1.bone('ouros_v36_reference_bow','torso3',[0,30.1,-3],[C((-3.55,29.05,-4.12),(3.05,1.38,.58),84,pivot=(-1.70,29.72,-3.75),rotation=(0,0,-19)),C((.50,29.05,-4.12),(3.05,1.38,.58),84,pivot=(1.70,29.72,-3.75),rotation=(0,0,19)),C((-1.05,28.42,-4.22),(2.10,2.10,.72),90,pivot=(0,29.45,-3.86),rotation=(0,0,45))])
    # Fitted white panels wrap front/sides/back; depth reduced to avoid a box torso.
    bodice=v1.bone('ouros_v36_reference_bodice','torso3',[0,26.4,-.5],[C((-3.80,23.05,-3.72),(3.05,6.00,.54),88,pivot=(-2.00,26.15,-3.38),rotation=(0,0,-5)),C((.75,23.05,-3.72),(3.05,6.00,.54),88,pivot=(2.00,26.15,-3.38),rotation=(0,0,5)),C((-4.05,23.40,-2.82),(.54,5.55,5.15),87,pivot=(-3.72,26.15,-.35),rotation=(0,-5,0)),C((3.51,23.40,-2.82),(.54,5.55,5.15),87,pivot=(3.72,26.15,-.35),rotation=(0,5,0)),C((-2.90,23.65,1.52),(5.80,5.05,.54),89,pivot=(0,26.15,1.82),rotation=(-3,0,0)),C((-2.55,23.60,-4.04),(.50,2.12,.28),84),C((2.05,23.60,-4.04),(.50,2.12,.28),84),C((-.30,23.30,-4.10),(.60,2.85,.28),85)])
    # Three sleeve segments per side produce long narrow taper, not balloon slabs.
    sl=v1.bone('ouros_v36_reference_sleeve_left','arm_left',[4.5,29.7,-.4],[C((2.20,28.35,-1.70),(4.25,2.45,2.55),88,pivot=(4.30,29.55,-.35),rotation=(0,0,-5)),C((5.95,28.25,-1.86),(3.55,2.62,2.88),87,pivot=(7.50,29.55,-.35),rotation=(0,0,-3)),C((8.90,28.18,-2.02),(2.75,2.76,3.10),89,pivot=(10.05,29.52,-.35),rotation=(0,0,-2))])
    sr=v1.bone('ouros_v36_reference_sleeve_right','arm_right',[-4.5,29.7,-.4],[C((-6.45,28.35,-1.70),(4.25,2.45,2.55),88,pivot=(-4.30,29.55,-.35),rotation=(0,0,5)),C((-9.50,28.25,-1.86),(3.55,2.62,2.88),87,pivot=(-7.50,29.55,-.35),rotation=(0,0,3)),C((-11.65,28.18,-2.02),(2.75,2.76,3.10),89,pivot=(-10.05,29.52,-.35),rotation=(0,0,2))])
    gl=v1.bone('ouros_v36_reference_glove_left','arm_left2',[12,29.4,-.4],[C((11.15,28.05,-2.28),(2.05,3.05,3.70),84),C((12.20,28.18,-2.55),(4.05,2.78,4.22),80),C((15.60,28.22,-2.72),(2.95,2.60,4.48),81)])
    gr=v1.bone('ouros_v36_reference_glove_right','arm_right2',[-12,29.4,-.4],[C((-13.20,28.05,-2.28),(2.05,3.05,3.70),84),C((-16.25,28.18,-2.55),(4.05,2.78,4.22),80),C((-18.95,28.22,-2.72),(2.95,2.60,4.48),81),C((-19.18,27.62,-.82),(.58,2.00,.58),90)])
    waist=v1.bone('ouros_v36_reference_waist','torso',[0,20,-.2],[C((-5.75,18.88,-3.78),(11.50,1.42,7.28),88),C((-5.15,18.35,-3.45),(10.30,.72,6.72),87),C((-4.75,18.12,-4.08),(9.50,.34,.30),85)])
    # SEGMENTED APRON: yoke + angled upper side panels + staggered lower lobes.
    # This keeps cloth close to the hips and breaks the previous rectangular board.
    skirt=v1.bone('ouros_v36_reference_apron_skirt','torso',[0,18.2,-.2],[
      C((-5.70,10.05,.55),(1.25,7.95,3.15),83,pivot=(-5.05,17.45,1.90),rotation=(0,0,-7)),
      C((4.45,10.05,.55),(1.25,7.95,3.15),83,pivot=(5.05,17.45,1.90),rotation=(0,0,7)),
      C((-4.95,10.20,2.72),(9.90,7.70,.58),82,pivot=(0,17.30,3.02),rotation=(-4,0,0)),
      C((-3.30,14.05,-4.78),(6.60,3.35,.72),87,pivot=(0,16.45,-4.35),rotation=(5,0,0)),
      C((-5.25,12.85,-4.52),(3.35,3.70,.68),88,pivot=(-3.65,16.00,-4.10),rotation=(3,-7,-8)),
      C((1.90,12.85,-4.52),(3.35,3.70,.68),88,pivot=(3.65,16.00,-4.10),rotation=(3,7,8)),
      C((-3.55,9.80,-4.92),(7.10,3.75,.76),88,pivot=(0,12.85,-4.48),rotation=(2,0,0)),
      C((-5.55,8.85,-4.58),(3.20,3.85,.72),87,pivot=(-3.85,12.25,-4.18),rotation=(2,-8,-10)),
      C((2.35,8.85,-4.58),(3.20,3.85,.72),87,pivot=(3.85,12.25,-4.18),rotation=(2,8,10)),
      C((-6.02,10.25,-3.72),(1.05,7.30,2.82),89,pivot=(-5.35,16.80,-2.20),rotation=(0,-10,-5)),
      C((4.97,10.25,-3.72),(1.05,7.30,2.82),89,pivot=(5.35,16.80,-2.20),rotation=(0,10,5)),
      C((-3.60,8.78,-5.12),(7.20,.46,.24),84),
      C((-4.92,8.05,-4.78),(2.45,.40,.24),85,pivot=(-3.55,9.20,-4.60),rotation=(0,0,-8)),
      C((2.47,8.05,-4.78),(2.45,.40,.24),85,pivot=(3.55,9.20,-4.60),rotation=(0,0,8))
    ])
    return [hat,ribbons,collar,bow,bodice,sl,sr,gl,gr,waist,skirt]
def paint_pixel(r,g,b,a,x,y,*,shiny):
    if a==0:return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100; cream=r>170 and g>135 and b<205; white=r>210 and g>210 and b>210; red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red:return r,g,b,a
    if b>r*1.15 and b>g*1.04 and sat>20:
      t=((9,73,109),(11,94,137),(5,57,92)) if not shiny else ((19,86,121),(25,103,142),(12,67,101)); tr,tg,tb=t[((x//8)+(y//10))%3]; m=.78; return int(r*(1-m)+tr*m),int(g*(1-m)+tg*m),int(b*(1-m)+tb*m),a
    if lum<155:
      tr,tg,tb=((34,35,43),(47,48,57),(25,27,35))[((x//9)+(y//8))%3]; m=.68; return int(r*(1-m)+tr*m),int(g*(1-m)+tg*m),int(b*(1-m)+tb*m),a
    return r,g,b,a
def post_patch():
    d=json.loads(v1.MANIFEST.read_text(encoding='utf-8')); d['concept']='Owner Reference Replica — Blue/White Maid Lucario V36b'; d['artStatus']='ARTISTIC FAIL'; d['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}; p=d['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+len(cosmetic_bones()); p['cosmeticBoneCount']=len(cosmetic_bones()); p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones()); b=d['builder']; b['scriptPath']='tools/cobblemon-model-review/build_lucario_owner_reference_v31.py'; b['command']=['python',b['scriptPath']]
    out=[]
    for x in b['outputs']:
      for old in ('v22-derived-normal.json','v29-reference-derived-normal.json','v30-reference-derived-normal.json','v31-reference-derived-normal.json','v32-reference-derived-normal.json','v33-reference-derived-normal.json','v34-reference-derived-normal.json','v35-reference-derived-normal.json'): x=x.replace(old,'v36-reference-derived-normal.json')
      for old in ('v22-derived-shiny.json','v29-reference-derived-shiny.json','v30-reference-derived-shiny.json','v31-reference-derived-shiny.json','v32-reference-derived-shiny.json','v33-reference-derived-shiny.json','v34-reference-derived-shiny.json','v35-reference-derived-shiny.json'): x=x.replace(old,'v36-reference-derived-shiny.json')
      out.append(x)
    b['outputs']=list(dict.fromkeys(out)); q=d['qualityIntent']; q['signaturePieces']=['Low folded cool-white cap with teal band and asymmetric blue ribbon stack','Fitted white bodice and tapered sleeves with blue cuffs, charcoal gloves and silver clasp','Segmented hip-wrapping white apron with angled lobes, stepped hem and blue rear underskirt']; q['macroFormPlan']='V36b replaces the rejected tower-cap and placard-apron read. Cap height is compressed into four broad overlapping folds. Apron is built from a shallow yoke, angled upper side panels, staggered lower lobes and narrow hip wraps so the contour follows the body instead of hanging as one board.'; q['paintPlan']='Normal/shiny remain independent exact-official derivations with blue/teal biological value ramps plus dedicated cool-white/grey, periwinkle-blue and charcoal accessory materials.'; q['gameplayReadGoal']='At 160 px read a low cap, fitted upper garment and soft segmented apron/skirt. No tower, box corset or detached placard should dominate.'; q['iterationNote']='Owner rejected V35/current V36 visual. V36b materially rebuilds the two failed macroforms: cap crown height reduced by roughly half; apron front split into overlapping angled lobes with staggered hem heights; side wraps narrowed; bodice and sleeves thinned/rotated.'; d['variantCoverage']['variants'][0]['coverage']='Default preserves exact official 87-bone anatomy plus V36b costume and independent normal paint.'; d['variantCoverage']['variants'][1]['coverage']='Shiny uses identical V36b costume geometry and independent official-shiny derivation.'; v1.MANIFEST.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main(): v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=paint_pixel; v22.write_overlay=write_overlay; v22.main(); post_patch()
if __name__=='__main__':main()
