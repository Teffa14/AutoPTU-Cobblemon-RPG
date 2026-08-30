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
v22.NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v25-derived-normal.json'
v22.SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v25-derived-shiny.json'
def shell(origin,size,uv,*,pivot,rotation,light=82,dark=89): return mcube(origin,size,uv,light=light,dark=dark,pivot=pivot,rotation=rotation)
def cosmetic_bones():
    # V25: one compact shoulder/back mantle with real depth. The large forms overlap
    # as a continuous diagonal envelope instead of V24's thin rising fins.
    root=v1.bone('ouros_v25_mantle_root','shoulder_right',[-3.9,29.7,0.0],[
        shell((-9.0,26.8,-2.5),(6.8,2.1,5.7),80,pivot=(-4.9,29.6,0.0),rotation=(16,-18,28),light=82,dark=90),
        shell((-8.2,28.1,-1.7),(6.0,1.8,5.4),81,pivot=(-4.8,30.0,0.2),rotation=(-9,-29,20),light=83,dark=90)
    ])
    crest=v1.bone('ouros_v25_mantle_crest','torso3',[-4.4,29.8,1.0],[
        shell((-11.2,28.8,-0.9),(7.8,2.0,5.0),80,pivot=(-6.1,30.2,0.8),rotation=(8,-23,39),light=82,dark=90),
        shell((-11.6,32.1,-0.2),(7.0,1.7,4.5),81,pivot=(-7.0,32.7,1.0),rotation=(3,-17,49),light=84,dark=89)
    ])
    fall=v1.bone('ouros_v25_mantle_fall','torso2',[-3.2,23.0,1.0],[
        shell((-7.9,21.3,-0.9),(5.7,1.8,5.2),80,pivot=(-4.6,25.4,0.8),rotation=(9,-18,20),light=81,dark=90),
        shell((-6.4,16.8,-0.7),(4.7,1.45,4.8),81,pivot=(-3.9,21.2,0.8),rotation=(2,-12,8),light=82,dark=90),
        shell((-4.8,12.8,-0.4),(3.2,1.0,4.3),80,pivot=(-3.1,17.0,0.6),rotation=(-5,-6,-6),light=83,dark=89)
    ])
    edge=v1.bone('ouros_v25_chest_edge','torso3',[0.0,28.2,-2.4],[
        shell((-3.7,26.8,-3.35),(5.6,0.34,1.05),84,pivot=(-0.3,28.5,-3.0),rotation=(3,0,-27),light=86,dark=80)
    ])
    return [root,crest,fall,edge]
def post_patch():
    data=json.loads(v1.MANIFEST.read_text(encoding='utf-8'))
    data['concept']='Aura Sentinel — Resonance Ronin V25'
    data['artStatus']='ARTISTIC FAIL'
    data['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=data['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+4; p['cosmeticBoneCount']=4; p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=data['builder']; b['scriptPath']='tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v24.py'; b['command']=['python',b['scriptPath']]
    b['outputs']=list(dict.fromkeys([x.replace('v24-derived-normal.json','v25-derived-normal.json').replace('v24-derived-shiny.json','v25-derived-shiny.json') for x in b['outputs']]))
    q=data['qualityIntent']
    q['signaturePieces']=['One deep diagonal shoulder-to-back mantle envelope','Single descending back-to-hip taper with no forked bar tips','Small chest edge that visually hands off into the mantle rather than competing with it']
    q['macroFormPlan']='V25 keeps V24 silhouette ambition but removes the thin fin vocabulary. Four thick overlapping upper shells form one compact shoulder/back crest; three progressively smaller deep lower shells continue the same contour toward the hip. All primary surfaces overlap in depth and scale down rather than terminating as isolated bars.'
    q['gameplayReadGoal']='At 160 px the right shoulder/back mass must read as one broad tapered diagonal gesture with a quiet outer contour, not separate fins, slabs, or a backpack.'
    q['iterationNote']='V24 passed the unchanged technical floors but direct Blockbench QA showed its thin rising facets collapsing into bars/fins, especially from the rear. V25 reduces the cosmetic system to eight cubes with greater depth, stronger overlap, a shorter crest, and a continuous shoulder-back-hip taper. Thresholds and attachment limits remain unchanged.'
    data['variantCoverage']['variants'][0]['coverage']='Default preserves the exact official 87-bone Lucario geometry and uses a validated V25 normal texture derived independently from the exact official 1.7.3 baseline.'
    data['variantCoverage']['variants'][1]['coverage']='Shiny uses the same V25 cosmetic geometry and overlay plus an independently derived V25 texture from the exact official shiny 1.7.3 baseline.'
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    v22.cosmetic_bones=cosmetic_bones
    v22.paint_pixel=v23.paint_pixel
    v22.main()
    post_patch()
if __name__=='__main__': main()
