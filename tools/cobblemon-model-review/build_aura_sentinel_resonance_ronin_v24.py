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
v22.NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v27-derived-normal.json'
v22.SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v27-derived-shiny.json'
def shell(origin,size,uv,*,pivot,rotation,light=82,dark=89): return mcube(origin,size,uv,light=light,dark=dark,pivot=pivot,rotation=rotation)
def cosmetic_bones():
    # V27 abandons the rejected backpack/mantle cluster. Identity follows Lucario's
    # diagonal anatomical flow: collar -> rib -> hip -> opposite shin, leaving broad
    # negative space around the head, chest spike, back and tail.
    collar=v1.bone('ouros_v27_aura_collar','torso3',[0.0,30.4,-1.0],[
        shell((-4.8,29.4,-3.2),(4.7,0.45,1.15),84,pivot=(-1.2,30.5,-2.5),rotation=(4,-7,-31),light=88,dark=81),
        shell((0.2,29.7,-3.15),(3.7,0.38,1.05),84,pivot=(0.5,30.4,-2.5),rotation=(3,8,27),light=86,dark=80)
    ])
    rib=v1.bone('ouros_v27_resonance_rib','torso2',[-1.8,25.4,-1.5],[
        shell((-5.0,23.5,-3.0),(5.8,0.62,1.05),82,pivot=(-1.8,26.0,-2.2),rotation=(7,-5,-36),light=85,dark=90),
        shell((-4.0,21.8,-2.65),(4.7,0.52,0.92),83,pivot=(-1.7,24.0,-2.0),rotation=(4,-3,-26),light=84,dark=89)
    ])
    hip=v1.bone('ouros_v27_hip_sweep','torso',[2.0,20.7,0.0],[
        shell((0.4,18.6,-2.0),(5.2,0.72,1.25),80,pivot=(2.2,21.0,-1.1),rotation=(10,12,24),light=83,dark=90),
        shell((2.2,17.2,-1.4),(3.8,0.58,1.1),81,pivot=(2.8,19.4,-0.8),rotation=(8,17,34),light=84,dark=89)
    ])
    forearm=v1.bone('ouros_v27_forearm_trace','arm_left',[4.4,23.3,-0.4],[
        shell((3.7,20.5,-1.4),(0.7,4.2,1.3),85,pivot=(4.2,22.7,-0.5),rotation=(9,0,-11),light=87,dark=82)
    ])
    shin=v1.bone('ouros_v27_shin_trace','leg_right',[-2.4,13.2,0.1],[
        shell((-3.35,8.5,-1.2),(0.8,4.8,1.35),85,pivot=(-2.7,12.8,-0.4),rotation=(-7,4,9),light=86,dark=82)
    ])
    return [collar,rib,hip,forearm,shin]
def paint_pixel(r:int,g:int,b:int,a:int,x:int,y:int,*,shiny:bool):
    if a==0: return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    cream=r>170 and g>135 and b<205; white=r>205 and g>205 and b>205; red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red: return r,g,b,a
    blue=b>r*1.20 and b>g*1.08 and sat>25
    # V27 paint carries most of the identity: three deterministic value bands plus
    # sparse directional edge glints. Normal/shiny are processed independently.
    if blue:
        band=((x//6)+(y//8))%3
        facing=12 if ((2*x+y)%23 in (0,1)) else 0
        if shiny:
            factors=((.72,.80,.90),(.78,.84,.94),(.66,.75,.86))[band]
        else:
            factors=((.54,.66,.82),(.62,.72,.88),(.48,.60,.77))[band]
        nr=int(r*factors[0])+5+facing//4; ng=int(g*factors[1])+7+facing//3; nb=int(b*factors[2])+12+facing
        if y>30: nr-=5; ng-=4; nb-=2
        return *(max(0,min(255,v)) for v in (nr,ng,nb)),a
    if lum<135 and sat<105:
        facing=10 if ((3*x+y)%29 in (0,1)) else 0
        nr=int(r*.68)+5+facing//5; ng=int(g*.72)+7+facing//4; nb=int(b*.90)+14+facing
        if y>28: nr-=4; ng-=4; nb-=2
        return *(max(0,min(255,v)) for v in (nr,ng,nb)),a
    return r,g,b,a
def post_patch():
    data=json.loads(v1.MANIFEST.read_text(encoding='utf-8'))
    data['concept']='Aura Sentinel — Resonance Ronin V27'; data['artStatus']='ARTISTIC FAIL'; data['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=data['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+5; p['cosmeticBoneCount']=5; p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=data['builder']; b['scriptPath']='tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v24.py'; b['command']=['python',b['scriptPath']]
    b['outputs']=list(dict.fromkeys([x.replace('v26-derived-normal.json','v27-derived-normal.json').replace('v26-derived-shiny.json','v27-derived-shiny.json') for x in b['outputs']]))
    q=data['qualityIntent']
    q['signaturePieces']=['Open diagonal aura collar framing the chest spike','Two-stage torso resonance rib following the waist taper','Opposed forearm and shin traces that continue the diagonal rhythm through the full body']
    q['macroFormPlan']='V27 deletes the rejected shoulder/back mantle architecture entirely. Five anatomy-parented systems trace a diagonal collar-rib-hip line and answer it on the opposite forearm/shin. The head, dorsal outline and tail stay deliberately open; silhouette change comes from distributed contour gestures rather than one backpack-like mass.'
    q['paintPlan']='Normal and shiny remain independently derived from exact 1.7.3 baselines. Cobalt biology uses three deterministic local value bands, stronger indigo occlusion and sparse directional edge glints; dark biology receives separate indigo shaping. Cream spikes, whites, eyes, dimensions, UV layout and alpha semantics remain protected.'
    q['gameplayReadGoal']='At 160 px read a coherent diagonal resonance path across collar, torso, hip and opposite limbs. No shoulder backpack, mantle rectangle, cage, repeated fins or large slabs may appear.'
    q['iterationNote']='V26 passed technical floors but direct Blockbench QA still read the compact shoulder/back shell as a backpack-like cuboid mass. V27 abandons that architecture instead of shrinking or thickening it again, distributing eight thin rotated contour volumes across the anatomy and asking paint to carry more of the transformation.'
    data['variantCoverage']['variants'][0]['coverage']='Default preserves the exact official 87-bone Lucario geometry and uses a validated V27 normal texture independently derived from the exact official 1.7.3 baseline.'
    data['variantCoverage']['variants'][1]['coverage']='Shiny uses the same V27 cosmetic geometry and overlay plus an independently derived V27 texture from the exact official shiny 1.7.3 baseline.'
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=paint_pixel; v22.main(); post_patch()
if __name__=='__main__': main()
