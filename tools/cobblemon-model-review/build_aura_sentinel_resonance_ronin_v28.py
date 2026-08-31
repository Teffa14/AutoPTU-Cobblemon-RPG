#!/usr/bin/env python3
from __future__ import annotations
import importlib.util, json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
V24_PATH=ROOT/'tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v24.py'
spec=importlib.util.spec_from_file_location('resonance_v24',V24_PATH)
if spec is None or spec.loader is None: raise SystemExit('cannot load V24 builder')
v24=importlib.util.module_from_spec(spec); spec.loader.exec_module(v24)
v22=v24.v22; v1=v24.v1; mcube=v24.mcube
v22.NORMAL_META=ROOT/'docs/cobblemon-skins/0448_lucario/v28-derived-normal.json'
v22.SHINY_META=ROOT/'docs/cobblemon-skins/0448_lucario/v28-derived-shiny.json'

def shell(origin,size,uv,*,pivot,rotation,light=82,dark=89):
    return mcube(origin,size,uv,light=light,dark=dark,pivot=pivot,rotation=rotation)

def cosmetic_bones():
    # V28 keeps V27's clean rear/head and replaces scattered strap-like traces with one
    # authored shoulder-to-upper-torso arc. Three overlapping, differently rotated
    # facets form a tapered crescent that breaks silhouette on only one side; an inner
    # counter-facet roots the arc into the torso while preserving negative space around
    # the chest spike. A small opposite hip echo carries the diagonal without becoming
    # a second dominant system.
    arc=v1.bone('ouros_v28_resonance_arc','arm_left',[4.0,29.0,-0.8],[
        shell((3.6,28.7,-3.0),(4.8,0.72,1.18),84,pivot=(4.2,29.5,-1.8),rotation=(9,-16,-34),light=89,dark=79),
        shell((5.8,27.0,-2.7),(5.3,0.62,1.08),84,pivot=(5.4,28.6,-1.7),rotation=(13,-23,-48),light=87,dark=78),
        shell((7.6,24.9,-2.3),(4.6,0.52,0.96),83,pivot=(6.7,26.9,-1.5),rotation=(17,-29,-58),light=85,dark=77)
    ])
    root=v1.bone('ouros_v28_arc_root','torso3',[1.2,29.2,-1.2],[
        shell((0.1,27.5,-2.7),(4.7,0.54,1.04),82,pivot=(1.6,29.0,-1.8),rotation=(6,-8,22),light=84,dark=90),
        shell((1.0,26.0,-2.45),(3.9,0.46,0.90),83,pivot=(1.8,27.8,-1.6),rotation=(4,-5,33),light=83,dark=91)
    ])
    hip=v1.bone('ouros_v28_hip_echo','torso',[-1.7,20.2,-0.3],[
        shell((-4.1,18.4,-1.9),(3.8,0.48,0.92),85,pivot=(-2.2,20.1,-1.0),rotation=(9,8,-24),light=86,dark=81)
    ])
    return [arc,root,hip]

def paint_pixel(r:int,g:int,b:int,a:int,x:int,y:int,*,shiny:bool):
    if a==0: return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    cream=r>170 and g>135 and b<205
    white=r>205 and g>205 and b>205
    red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red: return r,g,b,a
    blue=b>r*1.18 and b>g*1.06 and sat>22
    # V28 keeps the body-wide material identity from V27 but uses broader local value
    # fields and fewer checker-like transitions. A narrow directional glint repeats the
    # shoulder arc direction without painting a literal motif.
    if blue:
        field=((x//12)+(y//14))%3
        glint=11 if ((x+2*y)%31 in (0,1)) else 0
        if shiny:
            factors=((.73,.81,.91),(.80,.86,.95),(.68,.77,.88))[field]
        else:
            factors=((.55,.67,.83),(.64,.74,.89),(.49,.61,.78))[field]
        nr=int(r*factors[0])+5+glint//4
        ng=int(g*factors[1])+7+glint//3
        nb=int(b*factors[2])+12+glint
        if y>32: nr-=4; ng-=3; nb-=1
        return *(max(0,min(255,v)) for v in (nr,ng,nb)),a
    if lum<138 and sat<108:
        glint=8 if ((2*x+y)%37 in (0,1)) else 0
        nr=int(r*.69)+5+glint//5
        ng=int(g*.73)+7+glint//4
        nb=int(b*.91)+14+glint
        if y>30: nr-=3; ng-=3; nb-=1
        return *(max(0,min(255,v)) for v in (nr,ng,nb)),a
    return r,g,b,a

def post_patch():
    data=json.loads(v1.MANIFEST.read_text(encoding='utf-8'))
    data['concept']='Aura Sentinel — Resonance Ronin V28'
    data['artStatus']='ARTISTIC FAIL'
    data['ownerApproval']={'required':True,'approved':False,'approvedHeadSha':None,'evidenceSetSha256':None,'approvalRecord':None}
    p=data['production']
    p['productionBoneCount']=v1.OFFICIAL_BONES+3
    p['cosmeticBoneCount']=3
    p['cosmeticCubeCount']=sum(len(b.get('cubes',[])) for b in cosmetic_bones())
    b=data['builder']
    b['scriptPath']='tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v28.py'
    b['command']=['python',b['scriptPath']]
    outputs=[]
    for x in b['outputs']:
        x=x.replace('v27-derived-normal.json','v28-derived-normal.json').replace('v27-derived-shiny.json','v28-derived-shiny.json')
        outputs.append(x)
    b['outputs']=list(dict.fromkeys(outputs))
    q=data['qualityIntent']
    q['signaturePieces']=[
        'Single asymmetric resonance arc sweeping from left shoulder past the upper torso',
        'Torso-root counter-facet that preserves chest-spike negative space',
        'Small opposite hip echo carrying the diagonal through the body without a second bulky system'
    ]
    q['macroFormPlan']='V28 preserves V27 clean head/back/tail and deletes the scattered collar-rib-limb traces. One three-facet tapered shoulder arc provides the only dominant silhouette break, rooted by two smaller torso facets around the chest spike. A single hip echo continues the diagonal rhythm. No mantle, backpack, cage, repeated fins or orthogonal armor stack is allowed.'
    q['paintPlan']='Normal and shiny remain independently derived from exact 1.7.3 baselines. Broader cobalt value fields, indigo occlusion and sparse directional glints support the shoulder arc without drawing literal costume markings. Cream spikes, whites, eyes, dimensions, UV layout and alpha semantics stay protected.'
    q['gameplayReadGoal']='At 160 px read one premium asymmetric shoulder crescent plus a coherent cobalt/indigo material treatment. The silhouette break must exceed the unchanged 0.04 floor through useful contour, not oversized slabs.'
    q['iterationNote']='V27 solved rear clutter but failed silhouette at 0.0036 because all added forms hugged the official outline and read as straps. V28 removes those traces and spends the geometry budget on one anatomy-connected, overlapping, tapered shoulder/upper-torso signature system with deliberate negative space.'
    data['variantCoverage']['variants'][0]['coverage']='Default preserves the exact official 87-bone Lucario geometry and uses a validated V28 normal texture independently derived from the exact official 1.7.3 baseline.'
    data['variantCoverage']['variants'][1]['coverage']='Shiny uses the same V28 cosmetic geometry and overlay plus an independently derived V28 texture from the exact official shiny 1.7.3 baseline.'
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def main():
    v22.cosmetic_bones=cosmetic_bones
    v22.paint_pixel=paint_pixel
    v22.main()
    post_patch()

if __name__=='__main__': main()
