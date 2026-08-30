#!/usr/bin/env python3
from __future__ import annotations
import argparse, copy, hashlib, json
from pathlib import Path
from PIL import Image

PALETTE = {
    'dojo_black': (18, 20, 18, 255),
    'charcoal': (42, 44, 38, 255),
    'charcoal_light': (65, 67, 56, 255),
    'gold': (232, 184, 42, 255),
    'gold_dark': (151, 104, 24, 255),
    'wrap': (107, 99, 82, 255),
    'cream': (226, 213, 165, 255),
    'cobra_green': (82, 112, 48, 255),
    'lacquer': (74, 39, 27, 255),
    'shadow': (9, 10, 9, 255),
}
PIXELS = {name: (i, 63) for i, name in enumerate(PALETTE)}
FACES = ('north','east','south','west','up','down')

NORMAL_MAP = {
    (189,145,117,255):(42,44,38,255),
    (204,164,130,255):(58,60,50,255),
    (174,126,105,255):(29,31,28,255),
    (159,108,93,255):(19,21,19,255),
    (225,210,163,255):(226,181,45,255),
    (219,178,143,255):(174,124,30,255),
    (240,230,179,255):(240,201,73,255),
    (210,190,147,255):(139,107,48,255),
    (179,152,117,255):(78,69,53,255),
    (195,170,131,255):(98,83,59,255),
    (164,135,105,255):(63,57,46,255),
    (206,186,79,255):(232,184,42,255),
    (221,209,89,255):(244,206,65,255),
}
SHINY_MAP = {
    (122,152,48,255):(31,38,24,255),
    (142,167,57,255):(47,55,31,255),
    (102,137,39,255):(21,29,18,255),
    (83,122,34,255):(14,20,13,255),
    (206,192,118,255):(219,176,48,255),
    (155,182,67,255):(147,125,35,255),
    (221,213,131,255):(234,196,72,255),
    (191,170,105,255):(126,103,48,255),
    (161,130,80,255):(72,65,44,255),
    (176,148,92,255):(92,77,52,255),
    (146,112,68,255):(57,53,37,255),
    (161,176,110,255):(203,169,48,255),
    (181,191,123,255):(225,194,71,255),
}

def sha256(path:Path)->str:
    h=hashlib.sha256(); h.update(path.read_bytes()); return h.hexdigest()

def solid_uv(mat:str)->dict:
    x,y=PIXELS[mat]
    return {f:{'uv':[x,y],'uv_size':[1,1]} for f in FACES}

def cube(origin,size,mat,**extra):
    d={'origin':origin,'size':size,'uv':solid_uv(mat)}
    for k,v in extra.items():
        if v is not None:d[k]=v
    return d

def cowl():
    return {'name':'ouros_cobra_cowl','parent':'torso2','pivot':[0,20,0],'cubes':[
        cube([-5.15,17.0,2.85],[10.3,7.5,1.0],'dojo_black'),
        cube([-4.65,23.9,2.35],[9.3,1.1,1.65],'charcoal'),
        cube([-5.55,16.5,1.4],[1.15,6.7,2.3],'charcoal'),
        cube([4.40,16.5,1.4],[1.15,6.7,2.3],'charcoal'),
        cube([-8.25,18.0,1.75],[3.6,5.3,1.55],'dojo_black',pivot=[-6.45,20.65,2.52],rotation=[0,0,-10]),
        cube([4.65,18.25,1.75],[3.1,5.05,1.55],'dojo_black',pivot=[6.20,20.77,2.52],rotation=[0,0,8]),
        cube([-8.55,22.75,1.65],[3.55,0.45,1.75],'gold',pivot=[-6.78,22.97,2.52],rotation=[0,0,-10]),
        cube([4.90,22.75,1.65],[2.95,0.42,1.75],'gold_dark',pivot=[6.38,22.96,2.52],rotation=[0,0,8]),
        cube([-5.15,17.4,-3.90],[0.85,3.0,1.05],'gold_dark'),
        cube([4.30,17.4,-3.90],[0.85,3.0,1.05],'gold_dark'),
        cube([-4.80,22.55,-3.82],[3.10,0.52,0.70],'gold',pivot=[-3.25,22.81,-3.47],rotation=[0,0,-13]),
        cube([1.70,22.55,-3.82],[3.10,0.52,0.70],'gold_dark',pivot=[3.25,22.81,-3.47],rotation=[0,0,13]),
        cube([-7.90,23.2,2.05],[0.62,3.20,0.85],'gold',pivot=[-7.59,23.4,2.48],rotation=[0,0,-23]),
        cube([-6.85,23.0,2.10],[0.55,2.35,0.72],'cream',pivot=[-6.57,23.2,2.46],rotation=[0,0,-12]),
        cube([-0.70,23.85,3.82],[1.40,1.15,0.28],'cobra_green'),
    ]}

def gi_shell():
    return {'name':'ouros_cobra_gi_shell','parent':'torso2','pivot':[0,16.5,0],'cubes':[
        cube([-4.55,12.25,-3.75],[9.10,6.45,0.48],'dojo_black'),
        cube([-4.75,12.25,3.30],[9.50,6.60,0.45],'charcoal'),
        cube([-4.78,12.15,-3.25],[0.62,6.75,6.60],'dojo_black'),
        cube([4.16,12.15,-3.25],[0.62,6.75,6.60],'dojo_black'),
        cube([-6.65,17.20,-2.85],[2.45,1.55,5.90],'charcoal',pivot=[-5.42,17.97,0.10],rotation=[0,0,-8]),
        cube([4.20,17.35,-2.75],[2.20,1.30,5.65],'charcoal_light',pivot=[5.30,18.00,0.08],rotation=[0,0,7]),
        cube([-6.70,18.50,-2.65],[2.55,0.34,5.40],'gold',pivot=[-5.42,18.67,0.05],rotation=[0,0,-8]),
        cube([4.18,18.45,-2.55],[2.28,0.30,5.22],'gold_dark',pivot=[5.32,18.60,0.06],rotation=[0,0,7]),
        cube([-3.80,17.45,-4.05],[5.30,0.66,0.36],'cream',pivot=[-1.15,17.78,-3.87],rotation=[0,0,-25]),
        cube([-1.45,15.25,-4.07],[5.35,0.66,0.38],'gold',pivot=[1.22,15.58,-3.88],rotation=[0,0,24]),
        cube([-0.45,12.35,-4.12],[0.90,4.20,0.34],'gold_dark'),
        cube([-4.30,11.45,-3.60],[3.78,1.10,7.10],'charcoal'),
        cube([0.52,11.45,-3.60],[3.78,1.10,7.10],'dojo_black'),
        cube([-4.10,10.70,3.52],[3.45,1.65,0.42],'dojo_black',pivot=[-2.38,11.53,3.73],rotation=[-5,0,6]),
        cube([0.70,11.10,3.52],[3.30,1.25,0.42],'charcoal',pivot=[2.35,11.73,3.73],rotation=[-5,0,-5]),
        cube([-2.80,13.25,-4.16],[2.25,0.34,0.24],'gold',pivot=[-1.68,13.42,-4.04],rotation=[0,0,-19]),
        cube([0.55,13.25,-4.16],[2.25,0.34,0.24],'gold',pivot=[1.68,13.42,-4.04],rotation=[0,0,19]),
    ]}

def belt_sash():
    return {'name':'ouros_cobra_belt_sash','parent':'torso','pivot':[0,12.5,0],'cubes':[
        cube([-4.65,11.65,-3.55],[9.30,1.15,7.10],'shadow'),
        cube([-4.80,12.55,-3.70],[9.60,0.36,7.40],'gold_dark'),
        cube([-1.50,11.45,-3.92],[3.00,1.55,0.42],'gold'),
        cube([-0.85,10.10,-4.05],[1.70,1.55,0.36],'cobra_green'),
        cube([-3.55,5.70,-3.05],[2.55,6.25,0.62],'dojo_black',pivot=[-2.27,11.45,-2.74],rotation=[-6,0,8]),
        cube([0.80,6.35,-3.02],[2.40,5.55,0.62],'charcoal',pivot=[2.00,11.45,-2.71],rotation=[-6,0,-7]),
        cube([-3.30,5.70,-3.22],[2.05,0.38,0.22],'gold',pivot=[-2.27,5.89,-3.11],rotation=[-6,0,8]),
        cube([1.00,6.35,-3.19],[1.98,0.36,0.22],'gold_dark',pivot=[1.99,6.53,-3.08],rotation=[-6,0,-7]),
        cube([-1.35,10.10,3.66],[2.70,2.15,0.35],'lacquer'),
        cube([-0.85,10.60,4.02],[1.70,0.42,0.20],'gold'),
    ]}

def forearm(name,parent,side):
    if side=='left':
        return {'name':name,'parent':parent,'pivot':[11.5,20.5,0],'cubes':[
            cube([9.35,19.15,-1.35],[4.35,0.42,2.70],'wrap'),
            cube([9.35,21.45,-1.35],[4.35,0.42,2.70],'wrap'),
            cube([10.20,19.25,-1.56],[2.45,2.40,0.36],'dojo_black'),
            cube([10.52,19.55,-1.82],[1.80,1.80,0.26],'gold'),
            cube([12.65,19.45,-1.50],[0.55,2.00,3.00],'gold_dark'),
        ]}
    return {'name':name,'parent':parent,'pivot':[-11.5,20.5,0],'cubes':[
        cube([-13.70,19.15,-1.35],[4.35,0.42,2.70],'wrap'),
        cube([-13.70,21.45,-1.35],[4.35,0.42,2.70],'wrap'),
        cube([-12.65,19.25,-1.56],[2.45,2.40,0.36],'dojo_black'),
        cube([-12.32,19.55,-1.82],[1.80,1.80,0.26],'gold_dark'),
        cube([-13.20,19.45,-1.50],[0.55,2.00,3.00],'gold'),
    ]}

def leg_guard(name,parent,x0,stage,accent):
    y_ranges={2:(7.55,3.0),3:(4.55,3.0),4:(1.55,3.0)}
    y,h=y_ranges[stage]
    return {'name':name,'parent':parent,'pivot':[x0+1.5,y+h/2,-1.5],'cubes':[
        cube([x0-0.18,y-0.08,-1.72],[3.36,h+0.16,0.46],'dojo_black'),
        cube([x0-0.12,y+0.18,1.28],[3.24,h-0.36,0.36],'charcoal'),
        cube([x0-0.28,y+0.12,-1.62],[0.40,h-0.24,3.24],'gold_dark'),
        cube([x0+2.88,y+0.12,-1.62],[0.40,h-0.24,3.24],accent),
        cube([x0+0.24,y+h-0.48,-1.95],[2.52,0.34,0.30],accent),
    ]}

def foot_guard(name,parent,x0,accent,asym=False):
    cubes=[
        cube([x0-0.18,0.18,-3.24],[4.36,1.72,0.48],'dojo_black'),
        cube([x0+0.08,1.55,-2.95],[3.84,0.34,4.78],accent),
        cube([x0+0.10,0.35,-5.18],[3.80,0.42,2.30],'gold_dark'),
        cube([x0+0.28,0.74,-5.30],[3.44,0.30,0.22],'cream'),
        cube([x0+1.60,0.10,-5.55],[0.80,1.95,0.42],'charcoal'),
    ]
    if asym:
        cubes += [
            cube([x0+3.55,1.25,-4.82],[2.00,0.38,0.60],'gold',pivot=[x0+3.75,1.44,-4.52],rotation=[0,-18,0]),
            cube([x0+4.95,1.28,-5.00],[1.55,0.26,0.42],'cream',pivot=[x0+5.05,1.41,-4.79],rotation=[0,-18,0]),
        ]
    return {'name':name,'parent':parent,'pivot':[x0+2.0,1.0,0],'cubes':cubes}

def cosmetic_bones():
    return [
      cowl(), gi_shell(), belt_sash(),
      forearm('ouros_cobra_left_forearm','arm_left2','left'),
      forearm('ouros_cobra_right_forearm','arm_right2','right'),
      leg_guard('ouros_cobra_left_leg2','leg_left2',1.0,2,'gold'),
      leg_guard('ouros_cobra_left_leg3','leg_left3',1.0,3,'gold_dark'),
      leg_guard('ouros_cobra_left_leg4','leg_left4',1.0,4,'gold'),
      leg_guard('ouros_cobra_right_leg2','leg_right2',-4.0,2,'gold_dark'),
      leg_guard('ouros_cobra_right_leg3','leg_right3',-4.0,3,'gold'),
      leg_guard('ouros_cobra_right_leg4','leg_right4',-4.0,4,'gold_dark'),
      foot_guard('ouros_cobra_left_foot','foot_left',0.5,'gold',True),
      foot_guard('ouros_cobra_right_foot','foot_right',-4.5,'gold_dark',False),
    ]

def derive_model(src:Path,dst:Path):
    data=json.loads(src.read_text(encoding='utf-8'))
    geo=data['minecraft:geometry'][0]
    original=copy.deepcopy(geo['bones'])
    if len(original)!=30: raise SystemExit(f'expected 30 Hitmonlee bones, got {len(original)}')
    geo['description']['identifier']='geometry.ouros_cobra_dojo_hitmonlee'
    geo['bones']=original+cosmetic_bones()
    dst.parent.mkdir(parents=True,exist_ok=True)
    dst.write_text(json.dumps(data,separators=(',',':'))+'\n',encoding='utf-8')

def derive_texture(src:Path,dst:Path,mapping:dict):
    im=Image.open(src).convert('RGBA')
    if im.size!=(64,64): raise SystemExit(f'unexpected texture size {im.size}')
    for name,(x,y) in PIXELS.items():
        if im.getpixel((x,y))[3]!=0: raise SystemExit(f'palette texel {(x,y)} not free in official texture')
    px=im.load()
    for y in range(im.height):
        for x in range(im.width):
            p=px[x,y]
            if p in mapping: px[x,y]=mapping[p]
    for name,(x,y) in PIXELS.items(): px[x,y]=PALETTE[name]
    dst.parent.mkdir(parents=True,exist_ok=True)
    im.save(dst,optimize=True)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--model',required=True,type=Path)
    ap.add_argument('--normal',required=True,type=Path)
    ap.add_argument('--shiny',required=True,type=Path)
    ap.add_argument('--output-root',required=True,type=Path)
    a=ap.parse_args()
    model_out=a.output_root/'ouros_cobra_dojo_hitmonlee.geo.json'
    normal_out=a.output_root/'ouros_cobra_dojo_hitmonlee.png'
    shiny_out=a.output_root/'ouros_cobra_dojo_hitmonlee_shiny.png'
    derive_model(a.model,model_out)
    derive_texture(a.normal,normal_out,NORMAL_MAP)
    derive_texture(a.shiny,shiny_out,SHINY_MAP)
    report={'modelSha256':sha256(model_out),'normalSha256':sha256(normal_out),'shinySha256':sha256(shiny_out),'originalBones':30,'derivedBones':43,'cosmeticBones':13,'cosmeticCubes':sum(len(b.get('cubes',[])) for b in cosmetic_bones()),'palettePixels':PIXELS}
    (a.output_root/'build-report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
if __name__=='__main__': main()
