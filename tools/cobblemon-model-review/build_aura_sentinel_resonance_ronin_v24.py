#!/usr/bin/env python3
from __future__ import annotations
import importlib.util, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
V23_PATH=ROOT/'tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v23.py'
spec=importlib.util.spec_from_file_location('resonance_v23',V23_PATH)
if spec is None or spec.loader is None: raise SystemExit('cannot load V23 builder')
v23=importlib.util.module_from_spec(spec); spec.loader.exec_module(v23)
v22=v23.v22; v1=v23.v1; mcube=v23.mcube
v22.NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v24-derived-normal.json'
v22.SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v24-derived-shiny.json'
def shell(origin,size,uv,*,pivot,rotation,light=82,dark=89): return mcube(origin,size,uv,light=light,dark=dark,pivot=pivot,rotation=rotation)
def cosmetic_bones():
    root=v1.bone('ouros_v24_mantle_root','shoulder_right',[-3.8,30.0,0.0],[shell((-8.9,27.0,-2.2),(6.0,1.35,5.2),80,pivot=(-4.9,29.7,0.0),rotation=(18,-20,31),light=82,dark=90),shell((-8.0,27.8,-0.8),(5.2,1.15,4.4),81,pivot=(-4.7,30.0,0.4),rotation=(-12,-33,18),light=81,dark=89)])
    sweep=v1.bone('ouros_v24_mantle_sweep','torso3',[-4.0,29.0,1.1],[shell((-11.3,28.6,0.3),(7.4,1.25,4.0),81,pivot=(-6.0,30.0,0.8),rotation=(9,-25,44),light=81,dark=90),shell((-12.9,32.2,0.9),(6.6,1.05,3.3),80,pivot=(-8.0,33.0,1.2),rotation=(5,-17,58),light=83,dark=90),shell((-11.45,34.35,1.15),(5.6,0.92,2.9),81,pivot=(-8.35,35.0,1.35),rotation=(-1,-11,64),light=84,dark=89)])
    fall=v1.bone('ouros_v24_mantle_fall','torso2',[-3.0,23.0,1.0],[shell((-7.6,21.0,0.9),(5.4,0.95,4.4),80,pivot=(-4.4,25.0,1.2),rotation=(11,-22,23),light=81,dark=90),shell((-6.2,16.5,0.8),(4.2,0.72,4.1),81,pivot=(-3.7,21.0,1.1),rotation=(4,-15,10),light=82,dark=90),shell((-5.0,12.2,0.4),(2.8,0.52,4.3),80,pivot=(-3.1,17.0,0.8),rotation=(-5,-8,-7),light=82,dark=89),shell((-1.9,13.3,0.8),(1.65,0.40,3.5),81,pivot=(-1.2,17.2,1.0),rotation=(-7,8,13),light=81,dark=89)])
    bridge=v1.bone('ouros_v24_chest_edge','torso3',[0.0,28.3,-2.4],[shell((-3.8,26.8,-3.3),(5.7,0.24,0.95),84,pivot=(-0.3,28.5,-3.0),rotation=(3,0,-28),light=86,dark=80)])
    return [root,sweep,fall,bridge]
def post_patch():
    data=json.loads(v1.MANIFEST.read_text(encoding='utf-8')); data['concept']='Aura Sentinel — Resonance Ronin V24'; data['artStatus']='ARTISTIC FAIL'; data['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=data['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+4; p['cosmeticBoneCount']=4; p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=data['builder']; b['scriptPath']='tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v24.py'; b['command']=['python',b['scriptPath']]; b['outputs']=list(dict.fromkeys([x.replace('v23-derived-normal.json','v24-derived-normal.json').replace('v23-derived-shiny.json','v24-derived-shiny.json') for x in b['outputs']]))
    q=data['qualityIntent']; q['signaturePieces']=['Single rising diagonal aura-mantle crescent rooted at the right shoulder','Continuous descending back-to-hip fall with unequal forked finish','Restrained diagonal chest edge integrated into the same gesture']; q['macroFormPlan']='V24 replaces V23 body-hugging geometry after its exact Blockbench silhouette delta measured 0.0146. Two intersecting root shells launch three heavily overlapping progressively narrower rising facets; four descending facets continue the same gesture toward the hip and split around tail and legs.'; q['gameplayReadGoal']='At 160 px the right-shoulder crescent must remain one unmistakable diagonal silhouette while the face, ears, chest spike, hands, legs and tail stay readable.'; q['iterationNote']='V23 attachment and paint passed, but exact Blockbench evidence failed the unchanged silhouette floor at 0.0146 and direct review showed the form disappearing behind the body. V24 preserves paint and pushes one contiguous sweep outward and upward. The upper tip is deliberately pulled inward to overlap its predecessor and satisfy the real attachment contract without changing thresholds.'
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=v23.paint_pixel; v22.main(); post_patch()
if __name__=='__main__': main()
