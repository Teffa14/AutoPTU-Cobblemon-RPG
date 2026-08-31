#!/usr/bin/env python3
"""Lucario V40: cloth-contour rework of the locked maid candidate.

V39 passed engineering gates but failed visual QA because the cap read as stacked
horizontal slabs and the apron read as a rigid front board. V40 keeps the exact
Cobblemon 1.7.3 biological baseline and replaces only those Ouros macro-forms with
angled, overlapping, animation-parented cloth masses. Presentation only; AutoPTU/
Ouros remains authoritative for battle state and outcomes.
"""
from __future__ import annotations
import importlib.util,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'tools/cobblemon-model-review/build_lucario_owner_reference_v31.py'
spec=importlib.util.spec_from_file_location('lucario_v40_base',BASE)
if spec is None or spec.loader is None: raise SystemExit('cannot load validated V39 builder')
v39=importlib.util.module_from_spec(spec); spec.loader.exec_module(v39)
v1=v39.v1
NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v40-reference-derived-normal.json'
SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v40-reference-derived-shiny.json'
C=v39.C

def cosmetic_bones():
    bones=v39.cosmetic_bones()
    out=[]
    for bone in bones:
        name=bone.get('name')
        if name=='ouros_v39_reference_cap':
            # Low cloth crown built from overlapping angled lobes around the ears.
            bone=v1.bone('ouros_v40_reference_cap','head_angle',[0,39,-.2],[
                C((-4.55,38.30,-2.86),(4.95,2.10,5.55),88,pivot=(-2.05,39.45,-.05),rotation=(0,-5,8)),
                C((-.40,38.25,-2.86),(4.95,2.15,5.55),87,pivot=(2.05,39.45,-.05),rotation=(0,5,-8)),
                C((-3.95,40.05,-2.55),(4.30,2.25,5.05),89,pivot=(-1.75,41.18,.02),rotation=(0,-7,13)),
                C((-.35,40.00,-2.55),(4.30,2.30,5.05),88,pivot=(1.75,41.18,.02),rotation=(0,7,-13)),
                C((-3.20,41.82,-2.20),(3.55,1.75,4.45),87,pivot=(-1.40,42.65,.10),rotation=(0,-8,18)),
                C((-.35,41.78,-2.20),(3.55,1.78,4.45),89,pivot=(1.40,42.65,.10),rotation=(0,8,-18)),
                C((-4.72,37.72,-3.18),(3.65,.62,6.00),88,pivot=(-2.85,38.05,-.15),rotation=(0,-4,5)),
                C((1.07,37.72,-3.18),(3.65,.62,6.00),87,pivot=(2.85,38.05,-.15),rotation=(0,4,-5)),
            ])
        elif name=='ouros_v39_reference_cap_ribbons':
            bone=v1.bone('ouros_v40_reference_cap_ribbons','head_angle',[0,40,-1.2],[
                C((-6.28,38.45,-3.05),(2.55,3.15,2.70),83,pivot=(-5.02,40.05,-1.65),rotation=(0,-11,22)),
                C((-6.55,40.88,-2.72),(2.35,3.35,2.52),82,pivot=(-5.30,42.40,-1.40),rotation=(0,-14,34)),
                C((-6.12,43.35,-2.48),(2.05,2.85,2.35),83,pivot=(-5.03,44.62,-1.28),rotation=(0,-16,40)),
                C((4.05,39.45,-2.34),(1.05,2.18,1.90),83,pivot=(4.50,40.42,-1.38),rotation=(0,9,-18)),
            ])
        elif name=='ouros_v39_reference_apron_skirt':
            # Continuous waist-to-hem read from nested diagonal panels. No dominant
            # rectangular front face: each tier overlaps the next and widens down.
            bone=v1.bone('ouros_v40_reference_apron_skirt','torso',[0,17.4,-.3],[
                C((-5.45,9.15,2.16),(10.90,7.90,1.10),82,pivot=(0,16.40,2.70),rotation=(-4,0,0)),
                C((-6.78,9.45,-.35),(1.35,7.35,3.30),83,pivot=(-5.95,15.95,1.10),rotation=(0,-10,-6)),
                C((5.43,9.45,-.35),(1.35,7.35,3.30),83,pivot=(5.95,15.95,1.10),rotation=(0,10,6)),
                C((-3.95,14.80,-5.02),(4.15,2.25,4.05),89,pivot=(-1.85,16.65,-3.00),rotation=(3,-3,-5)),
                C((-.20,14.80,-5.02),(4.15,2.25,4.05),88,pivot=(1.85,16.65,-3.00),rotation=(3,3,5)),
                C((-4.90,12.25,-5.25),(5.15,3.25,4.32),88,pivot=(-2.25,15.05,-3.08),rotation=(4,-5,-8)),
                C((-.25,12.25,-5.25),(5.15,3.25,4.32),89,pivot=(2.25,15.05,-3.08),rotation=(4,5,8)),
                C((-5.78,9.15,-5.52),(6.00,3.85,4.58),87,pivot=(-2.65,12.45,-3.15),rotation=(5,-7,-10)),
                C((-.22,9.15,-5.52),(6.00,3.85,4.58),88,pivot=(2.65,12.45,-3.15),rotation=(5,7,10)),
                C((-6.18,10.05,-3.78),(1.42,5.75,4.00),87,pivot=(-5.30,15.20,-1.85),rotation=(0,-14,-6)),
                C((4.76,10.05,-3.78),(1.42,5.75,4.00),87,pivot=(5.30,15.20,-1.85),rotation=(0,14,6)),
                C((-5.28,8.88,-5.82),(4.85,.50,.30),85,pivot=(-2.55,9.10,-5.65),rotation=(0,0,-3)),
                C((.43,8.88,-5.82),(4.85,.50,.30),84,pivot=(2.55,9.10,-5.65),rotation=(0,0,3)),
            ])
        out.append(bone)
    return out

_orig_post=v39.post_patch
def post_patch():
    _orig_post()
    d=json.loads(v1.MANIFEST.read_text(encoding='utf-8'))
    d['concept']='Owner Reference Replica — Blue/White Maid Lucario V40'
    d['artStatus']='ARTISTIC FAIL'
    d['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=d['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+len(cosmetic_bones()); p['cosmeticBoneCount']=len(cosmetic_bones()); p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=d['builder']; b['scriptPath']='tools/cobblemon-model-review/build_lucario_owner_reference_v40.py'; b['command']=['python',b['scriptPath']]
    b['outputs']=[x.replace('v39-reference-derived-normal.json','v40-reference-derived-normal.json').replace('v39-reference-derived-shiny.json','v40-reference-derived-shiny.json') for x in b['outputs']]
    q=d['qualityIntent']
    q['signaturePieces']=['Low folded white cap built from asymmetric angled cloth lobes around the ears','Long white bodice/sleeves with periwinkle cuffs, charcoal gloves and silver clasp','Apron composed from nested diagonal overlapping panels that widen continuously from waist to hem']
    q['macroFormPlan']='V40 removes V39 stacked cap levels and the single-board apron read. Cap volume is split into angled overlapping lobes; apron volume is split into three widening cloth tiers per side plus hip wraps, all overlapping along a continuous waist-to-hem path.'
    q['gameplayReadGoal']='At 160 px the costume must read as one widening cloth garment with a low folded cap, without a dominant rectangular plate or horizontal cap tower.'
    q['antiPatternsToReject']=['Stacked horizontal cap slabs','Single rectangular apron face','Visible blue/black center gap','Detached skirt panels','Bright tail dominance','Any change to official biological bones or battle-state authority']
    q['iterationNote']='V39 exact Blockbench evidence passed technical checks but failed internal art QA for board-like apron and stacked cap. V40 materially reworks both macro-forms; owner approval remains absent.'
    d['variantCoverage']['variants'][0]['coverage']='Default preserves exact official 87-bone anatomy plus V40 cloth-contour costume and independent normal paint.'
    d['variantCoverage']['variants'][1]['coverage']='Shiny uses identical V40 costume geometry and an independent official-shiny derivation.'
    v1.MANIFEST.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def main():
    v39.v22.NORMAL_META=NORMAL_META; v39.v22.SHINY_META=SHINY_META
    v39.cosmetic_bones=cosmetic_bones; v39.post_patch=post_patch
    v39.main()
if __name__=='__main__': main()
