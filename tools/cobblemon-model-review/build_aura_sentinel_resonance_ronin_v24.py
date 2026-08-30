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
v22.NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v26-derived-normal.json'
v22.SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v26-derived-shiny.json'
def shell(origin,size,uv,*,pivot,rotation,light=82,dark=89): return mcube(origin,size,uv,light=light,dark=dark,pivot=pivot,rotation=rotation)
def cosmetic_bones():
    # V26 deletes V25's descending/finned chain. One compact shoulder/back shell owns
    # the silhouette; each piece is thick enough to read as volume rather than a bar.
    root=v1.bone('ouros_v26_mantle_root','shoulder_right',[-3.8,29.7,0.0],[
        shell((-9.0,26.7,-2.7),(6.9,2.5,6.0),80,pivot=(-4.8,29.6,0.0),rotation=(14,-18,27),light=82,dark=90),
        shell((-8.3,28.1,-1.9),(6.2,2.1,5.7),81,pivot=(-4.7,30.0,0.2),rotation=(-8,-28,19),light=83,dark=90)
    ])
    crown=v1.bone('ouros_v26_mantle_crown','torso3',[-4.7,29.8,0.9],[
        shell((-11.1,29.0,-1.4),(7.5,2.4,5.6),80,pivot=(-6.2,30.5,0.5),rotation=(7,-20,37),light=82,dark=90),
        shell((-10.9,32.2,-0.8),(6.2,2.0,5.0),81,pivot=(-6.9,32.8,0.7),rotation=(1,-14,46),light=84,dark=89)
    ])
    back=v1.bone('ouros_v26_back_shell','torso2',[-2.8,24.0,1.0],[
        shell((-6.2,20.6,-1.2),(5.0,2.2,5.6),80,pivot=(-3.8,25.1,0.7),rotation=(8,-15,15),light=81,dark=90)
    ])
    edge=v1.bone('ouros_v26_chest_edge','torso3',[0.0,28.2,-2.4],[
        shell((-3.7,26.8,-3.35),(5.6,0.34,1.05),84,pivot=(-0.3,28.5,-3.0),rotation=(3,0,-27),light=86,dark=80)
    ])
    return [root,crown,back,edge]
def paint_pixel(r:int,g:int,b:int,a:int,x:int,y:int,*,shiny:bool):
    if a==0: return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    cream=r>170 and g>135 and b<205; white=r>205 and g>205 and b>205; red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red: return r,g,b,a
    blue=b>r*1.20 and b>g*1.08 and sat>25
    if blue:
        # Deep lacquered cobalt: stronger value hierarchy than V25, with sparse facing
        # highlights and coordinate-stable microvariation; not a flat hue rotation.
        facing=10 if ((x+2*y)%29 in (0,1,2)) else (4 if ((2*x+y)%17==0) else 0)
        occlusion=max(0,(y-24)//7)
        if shiny:
            nr=int(r*.70)+5; ng=int(g*.78)+7+facing//3; nb=int(b*.90)+9+facing
        else:
            nr=int(r*.55)+5; ng=int(g*.66)+7+facing//3; nb=int(b*.82)+12+facing
        nr-=occlusion//3; ng-=occlusion//4; nb-=occlusion//6
        return *(max(0,min(255,v)) for v in (nr,ng,nb)),a
    if lum<125 and sat<90:
        edge=8 if ((3*x+y)%31 in (0,1)) else 0
        occlusion=max(0,(y-20)//7)
        nr=int(r*.70)+5+edge//4-occlusion//4
        ng=int(g*.73)+6+edge//3-occlusion//4
        nb=int(b*.88)+13+edge-occlusion//5
        return *(max(0,min(255,v)) for v in (nr,ng,nb)),a
    return r,g,b,a
def post_patch():
    data=json.loads(v1.MANIFEST.read_text(encoding='utf-8'))
    data['concept']='Aura Sentinel — Resonance Ronin V26'; data['artStatus']='ARTISTIC FAIL'; data['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=data['production']; p['productionBoneCount']=v1.OFFICIAL_BONES+4; p['cosmeticBoneCount']=4; p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=data['builder']; b['scriptPath']='tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v24.py'; b['command']=['python',b['scriptPath']]
    b['outputs']=list(dict.fromkeys([x.replace('v25-derived-normal.json','v26-derived-normal.json').replace('v25-derived-shiny.json','v26-derived-shiny.json') for x in b['outputs']]))
    q=data['qualityIntent']
    q['signaturePieces']=['Compact deep shoulder/back mantle shell','Short rising crown built from overlapping volumes rather than fins','Single quiet chest edge that connects the shell to the torso']
    q['macroFormPlan']='V26 removes the entire V25 descending chain after direct rear-view QA showed thin fins. Two deep shoulder roots overlap two broad crown volumes and one short back shell, producing a single compact asymmetric mass with no lower bar tips or forked panels.'
    q['paintPlan']='Normal and shiny remain independently derived from exact 1.7.3 baselines. Blue biology is repainted as deep lacquered cobalt using multi-level value shaping, sparse facing highlights and deterministic microvariation; dark biology receives stronger indigo occlusion and edge accents. Cream spikes, white landmarks, red eyes, UV layout, dimensions and alpha semantics remain intact.'
    q['gameplayReadGoal']='At 160 px read one dark cobalt shoulder/back crest with a broad diagonal outer contour. No fins, hanging slabs, backpack rectangle or detached bars may survive.'
    q['iterationNote']='V25 passed technical floors but direct Blockbench QA still showed stacked head-side slabs and multiple rear fins. V26 deletes the descending chain, reduces the cosmetic system to six cubes, deepens the remaining shells, and increases authored paint contrast instead of adding geometry.'
    data['variantCoverage']['variants'][0]['coverage']='Default preserves the exact official 87-bone Lucario geometry and uses a validated V26 normal texture independently derived from the exact official 1.7.3 baseline.'
    data['variantCoverage']['variants'][1]['coverage']='Shiny uses the same V26 cosmetic geometry and overlay plus an independently derived V26 texture from the exact official shiny 1.7.3 baseline.'
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=paint_pixel; v22.main(); post_patch()
if __name__=='__main__': main()
