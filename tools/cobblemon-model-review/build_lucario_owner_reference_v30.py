#!/usr/bin/env python3
"""Lucario owner-reference replica V30.

Material rework of the active owner-supplied blue/white maid-style Lucario target.
V29b fixed the costume palette, but exact Blockbench evidence still read too much
as a blue skirted uniform rather than the supplied broad white apron silhouette.
V30 keeps the current official 87-bone Lucario prefix and rebuilds the costume
proportions around the visible target: taller white headpiece, white dominant
bodice/sleeves, broad white apron front, blue side skirt panels, dark gloves and
lower body, plus the blue bow/silver clasp.

Presentation only. AutoPTU/Ouros remains authoritative for battle facts.
"""
from __future__ import annotations
import importlib.util, json
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
V29_PATH=ROOT/'tools/cobblemon-model-review/build_lucario_owner_reference_v29.py'
spec=importlib.util.spec_from_file_location('owner_v29',V29_PATH)
if spec is None or spec.loader is None: raise SystemExit('cannot load V29b builder')
v29=importlib.util.module_from_spec(spec); spec.loader.exec_module(v29)
v22=v29.v22; v1=v29.v1

NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v30-reference-derived-normal.json'
SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v30-reference-derived-shiny.json'
v22.NORMAL_META=NORMAL_META; v22.SHINY_META=SHINY_META
REFERENCE_SHA256=v29.REFERENCE_SHA256

PALETTE={
 80:(18,20,27,255), 81:(38,41,50,255), 82:(3,55,96,255),
 83:(7,88,137,255), 84:(67,82,184,255), 85:(150,160,174,255),
 86:(206,211,221,255), 87:(242,244,247,255),
}

def C(origin,size,color,*,pivot=None,rotation=None,inflate=None,mirror=None):
    return v1.cube(origin,size,color,pivot=pivot,rotation=rotation,inflate=inflate,mirror=mirror)

def write_overlay(path:Path)->None:
    im=Image.new('RGBA',(128,64),(0,0,0,0))
    for x,rgba in PALETTE.items(): im.putpixel((x,63),rgba)
    path.parent.mkdir(parents=True,exist_ok=True)
    im.save(path,format='PNG',optimize=True,compress_level=9)

def cosmetic_bones()->list[dict]:
    # Reference-dominant stepped white headpiece. Broader at the bottom, narrower
    # at the crown, with a single royal/periwinkle band at the base.
    hat=v1.bone('ouros_v30_reference_headpiece','head_angle',[0,39.2,-0.1],[
      C((-4.9,38.25,-2.55),(9.8,1.0,5.55),84),
      C((-4.75,39.05,-2.45),(9.5,1.2,5.4),87),
      C((-4.35,40.05,-2.2),(8.7,2.0,5.0),86),
      C((-4.0,41.75,-1.95),(8.0,2.0,4.6),87),
      C((-3.55,43.45,-1.65),(7.1,2.0,4.1),86),
      C((-3.05,45.15,-1.35),(6.1,1.8,3.55),87),
      C((-2.55,46.65,-1.05),(5.1,1.25,3.0),86),
    ])
    ribbons=v1.bone('ouros_v30_reference_head_ribbons','head_angle',[0,39.0,1.0],[
      C((-4.55,37.8,0.25),(1.35,4.3,2.25),83,pivot=(-3.85,39.8,1.35),rotation=(0,-8,14)),
      C((-4.25,40.9,0.7),(1.15,3.15,1.8),82,pivot=(-3.65,42.0,1.55),rotation=(0,-10,22)),
      C((3.2,38.15,0.35),(1.3,3.7,2.1),83,pivot=(3.85,39.9,1.4),rotation=(0,8,-13)),
    ])
    bow=v1.bone('ouros_v30_reference_bow','torso3',[0,30.2,-3.0],[
      C((-3.9,29.2,-4.05),(3.5,1.65,.72),84,pivot=(-1.9,30.0,-3.7),rotation=(0,0,-20)),
      C((.4,29.2,-4.05),(3.5,1.65,.72),84,pivot=(1.9,30.0,-3.7),rotation=(0,0,20)),
      C((-1.15,28.45,-4.2),(2.3,2.3,.85),86,pivot=(0,29.6,-3.78),rotation=(0,0,45)),
      C((-.5,27.35,-4.05),(1.0,1.6,.55),85),
    ])
    # White shirt shell with a slimmer waist than V29b and blue vertical trim.
    bodice=v1.bone('ouros_v30_reference_bodice','torso3',[0,26.7,-.5],[
      C((-4.3,22.9,-3.7),(3.55,6.6,.72),87,pivot=(-2.3,26.2,-3.34),rotation=(0,0,-3)),
      C((.75,22.9,-3.7),(3.55,6.6,.72),87,pivot=(2.3,26.2,-3.34),rotation=(0,0,3)),
      C((-4.35,23.0,-2.95),(.7,6.3,5.55),86), C((3.65,23.0,-2.95),(.7,6.3,5.55),86),
      C((-3.25,23.1,1.6),(6.5,6.1,.72),87),
      C((-2.55,23.0,-4.05),(.65,2.5,.3),84), C((1.9,23.0,-4.05),(.65,2.5,.3),84),
    ])
    sleeve_l=v1.bone('ouros_v30_reference_sleeve_left','arm_left',[4.8,29.7,-.4],[
      C((2.0,28.15,-1.85),(6.6,3.0,3.0),87,inflate=.05),
      C((8.0,28.1,-2.2),(4.25,3.05,3.6),86,inflate=.04),
    ])
    sleeve_r=v1.bone('ouros_v30_reference_sleeve_right','arm_right',[-4.8,29.7,-.4],[
      C((-8.6,28.15,-1.85),(6.6,3.0,3.0),87,inflate=.05),
      C((-12.25,28.1,-2.2),(4.25,3.05,3.6),86,inflate=.04),
    ])
    glove_l=v1.bone('ouros_v30_reference_glove_left','arm_left2',[12.0,29.6,-.4],[
      C((11.3,28.0,-2.35),(2.1,3.2,3.9),84),
      C((12.45,28.15,-2.65),(4.35,3.0,4.45),80),
      C((16.05,28.3,-2.8),(2.95,2.7,4.75),81),
    ])
    glove_r=v1.bone('ouros_v30_reference_glove_right','arm_right2',[-12.0,29.6,-.4],[
      C((-13.4,28.0,-2.35),(2.1,3.2,3.9),84),
      C((-16.8,28.15,-2.65),(4.35,3.0,4.45),80),
      C((-19.0,28.3,-2.8),(2.95,2.7,4.75),81),
    ])
    waist=v1.bone('ouros_v30_reference_waist','torso',[0,20.1,-.2],[
      C((-6.8,19.2,-4.15),(13.6,1.4,8.3),87),
      C((-6.35,18.45,-3.9),(12.7,1.0,7.8),86),
    ])
    # The decisive V30 change: white apron becomes the dominant front silhouette.
    # Blue is restricted to side/back cloth, like the supplied target.
    apron=v1.bone('ouros_v30_reference_apron','torso',[0,18.7,-.3],[
      C((-6.15,10.7,-4.0),(12.3,8.1,.8),86,pivot=(0,18.0,-3.6),rotation=(3,0,0)),
      C((-5.6,11.2,-4.5),(11.2,7.25,.82),87,pivot=(0,17.85,-4.05),rotation=(2,0,0)),
      C((-5.0,10.8,-4.72),(10.0,1.0,.35),87),
      C((-4.55,11.55,-4.75),(9.1,.55,.3),86),
      C((-4.7,10.75,-4.8),(2.0,1.5,.34),84), C((2.7,10.75,-4.8),(2.0,1.5,.34),84),
    ])
    skirt=v1.bone('ouros_v30_reference_blue_skirt','torso',[0,18.3,0],[
      C((-7.2,11.2,-2.9),(2.0,7.3,6.0),83,pivot=(-5.9,17.9,0),rotation=(0,0,-6)),
      C((5.2,11.2,-2.9),(2.0,7.3,6.0),83,pivot=(5.9,17.9,0),rotation=(0,0,6)),
      C((-5.8,11.1,2.9),(11.6,7.35,.7),83,pivot=(0,17.9,3.2),rotation=(-3,0,0)),
    ])
    stocking_l=v1.bone('ouros_v30_reference_stocking_left','leg_left4',[3.5,4.5,-.1],[C((1.2,-3.0,-1.9),(4.6,9.55,3.8),81,inflate=.03)])
    stocking_r=v1.bone('ouros_v30_reference_stocking_right','leg_right4',[-3.5,4.5,-.1],[C((-5.8,-3.0,-1.9),(4.6,9.55,3.8),81,inflate=.03)])
    boot_l=v1.bone('ouros_v30_reference_boot_left','foot_left',[3.5,-1.7,-1.2],[C((.65,-4.1,-4.4),(5.7,3.7,6.0),80,inflate=.03),C((.95,-3.9,-4.65),(5.1,.72,1.15),81)])
    boot_r=v1.bone('ouros_v30_reference_boot_right','foot_right',[-3.5,-1.7,-1.2],[C((-6.35,-4.1,-4.4),(5.7,3.7,6.0),80,inflate=.03),C((-6.05,-3.9,-4.65),(5.1,.72,1.15),81)])
    return [hat,ribbons,bow,bodice,sleeve_l,sleeve_r,glove_l,glove_r,waist,apron,skirt,stocking_l,stocking_r,boot_l,boot_r]

def patch_metadata()->None:
    v29.patch_metadata()
    for old,new in ((v29.NORMAL_META,NORMAL_META),(v29.SHINY_META,SHINY_META)):
        if old.is_file() and not new.is_file():
            new.write_text(old.read_text(encoding='utf-8'),encoding='utf-8')
    for path in (NORMAL_META,SHINY_META):
        if not path.is_file(): continue
        d=json.loads(path.read_text(encoding='utf-8'))
        d['sourceReferenceSha256']=REFERENCE_SHA256
        d['bodyTexelRework']='OWNER_REFERENCE_MATERIAL_MATCH_V30'
        d['paletteIntent']='Preserve reference cobalt/teal biology while costume geometry supplies dominant cool white, royal blue and charcoal distribution.'
        d['repaintRegions']=['existing blue biological texels','existing dark biological texels']
        path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def post_patch()->None:
    data=json.loads(v1.MANIFEST.read_text(encoding='utf-8'))
    data['concept']='Owner Reference Replica — Blue/White Maid Lucario V30'
    data['artStatus']='ARTISTIC FAIL'
    data['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=data['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+len(cosmetic_bones()); p['cosmeticBoneCount']=len(cosmetic_bones()); p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=data['builder']; b['scriptPath']='tools/cobblemon-model-review/build_lucario_owner_reference_v30.py'; b['command']=['python',b['scriptPath']]
    converted=[]
    for item in b['outputs']:
        item=item.replace('v29-reference-derived-normal.json','v30-reference-derived-normal.json').replace('v29-reference-derived-shiny.json','v30-reference-derived-shiny.json')
        converted.append(item)
    for path in (NORMAL_META,SHINY_META):
        rel=str(path.relative_to(ROOT)).replace('\\','/')
        if rel not in converted: converted.append(rel)
    b['outputs']=list(dict.fromkeys(converted))
    q=data['qualityIntent']
    q['signaturePieces']=['Tall stepped white headpiece with blue base band','White fitted bodice and full white sleeves ending in blue cuffs and oversized dark gloves','Broad white apron as the dominant lower-front mass with blue side/back skirt and dark stocking/boot lower body']
    q['macroFormPlan']='V30 keeps the owner-reference clean-sheet direction and materially corrects V29b proportions. The apron is now the dominant lower-front silhouette, while blue cloth is pushed to side/back panels. Headpiece, white sleeves, dark gloves and lower body remain distributed across official animated parents. No Resonance Ronin geometry returns.'
    q['paintPlan']='Normal and shiny remain independently derived from exact Cobblemon 1.7.3 baselines. Biology stays teal/cobalt plus charcoal; costume uses explicit cool-white/light-gray, royal/deep blue and black overlay slots.'
    q['gameplayReadGoal']='At 160 px first read must match the supplied existing skin: tall white headpiece, white torso/sleeves, broad white apron, blue side panels and dark legs/gloves.'
    q['iterationNote']='V29b exact Blockbench evidence corrected the previous cyan/gold palette but still let blue skirt mass dominate the lower body. V30 rebalances geometry toward the supplied render: larger white apron front, narrower blue side panels, slightly broader headpiece and stronger white sleeve coverage. Owner approval remains absent. Source reference SHA-256: '+REFERENCE_SHA256+'.'
    data['variantCoverage']['variants'][0]['coverage']='Default preserves the exact official 87-bone Lucario geometry and adds V30 owner-reference costume geometry plus independently derived normal body paint.'
    data['variantCoverage']['variants'][1]['coverage']='Shiny uses the same V30 costume geometry and independently derives body paint from the official 1.7.3 shiny baseline.'
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def main()->None:
    v22.cosmetic_bones=cosmetic_bones
    v22.paint_pixel=v29.paint_pixel
    v22.write_overlay=write_overlay
    v22.main()
    patch_metadata(); post_patch()

if __name__=='__main__': main()
