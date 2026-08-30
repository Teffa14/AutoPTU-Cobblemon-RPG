#!/usr/bin/env python3
from __future__ import annotations
import argparse, copy, hashlib, json
from pathlib import Path
from PIL import Image

PALETTE={
 'dojo_black':(16,18,16,255),'charcoal':(43,45,39,255),'gold':(232,184,42,255),
 'gold_dark':(145,99,22,255),'wrap':(116,106,86,255),'cream':(226,214,170,255),
 'cobra_green':(70,105,43,255),'lacquer':(82,43,26,255),'shadow':(8,9,8,255)}
PIXELS={k:(i,63) for i,k in enumerate(PALETTE)}
FACES=('north','east','south','west','up','down')

def sha256(p:Path)->str:
 h=hashlib.sha256(); h.update(p.read_bytes()); return h.hexdigest()

def solid_uv(mat:str)->dict:
 x,y=PIXELS[mat]; return {f:{'uv':[x,y],'uv_size':[1,1]} for f in FACES}

def cube(origin,size,mat,**extra):
 d={'origin':origin,'size':size,'uv':solid_uv(mat)}
 for k,v in extra.items():
  if v is not None:d[k]=v
 return d

def cobra_fan_mantle():
 return {'name':'ouros_cobra_mantle','parent':'torso2','pivot':[0,20.0,3.1],'cubes':[
  # low bridge, always behind the biological head
  cube([-4.4,16.35,3.22],[8.8,0.95,0.48],'shadow'),
  cube([-3.8,17.10,3.38],[7.6,0.32,0.24],'gold_dark'),
  # left fan: three narrow stepped blades instead of one square plate
  cube([-5.75,16.85,2.90],[1.35,7.65,0.72],'dojo_black',pivot=[-5.05,20.70,3.25],rotation=[0,0,-8]),
  cube([-7.30,17.65,2.94],[1.55,7.00,0.70],'charcoal',pivot=[-6.45,21.05,3.27],rotation=[0,0,-17]),
  cube([-8.95,18.85,2.98],[1.70,5.75,0.68],'dojo_black',pivot=[-8.00,21.55,3.29],rotation=[0,0,-28]),
  cube([-5.75,24.05,3.00],[1.32,0.36,0.50],'gold'),
  cube([-7.28,24.15,3.04],[1.52,0.34,0.48],'gold_dark',pivot=[-6.45,24.31,3.28],rotation=[0,0,-17]),
  cube([-8.72,23.95,3.08],[1.60,0.34,0.46],'gold',pivot=[-7.92,24.12,3.31],rotation=[0,0,-28]),
  cube([-6.96,20.00,3.67],[1.10,2.35,0.24],'cobra_green',pivot=[-6.40,21.10,3.79],rotation=[0,0,-17]),
  # right fan: slightly smaller, intentional asymmetry
  cube([4.40,17.10,2.90],[1.30,7.15,0.72],'dojo_black',pivot=[5.05,20.65,3.25],rotation=[0,0,7]),
  cube([5.70,18.00,2.94],[1.48,6.40,0.70],'charcoal',pivot=[6.40,21.05,3.27],rotation=[0,0,15]),
  cube([7.05,19.15,2.98],[1.55,5.15,0.68],'dojo_black',pivot=[7.75,21.55,3.29],rotation=[0,0,24]),
  cube([4.42,23.85,3.00],[1.28,0.34,0.50],'gold_dark'),
  cube([5.72,23.95,3.04],[1.46,0.32,0.48],'gold',pivot=[6.40,24.11,3.28],rotation=[0,0,15]),
  cube([7.12,23.65,3.08],[1.42,0.32,0.46],'gold_dark',pivot=[7.83,23.81,3.31],rotation=[0,0,24]),
  cube([5.92,20.15,3.67],[1.05,2.20,0.24],'cobra_green',pivot=[6.45,21.15,3.79],rotation=[0,0,15]),
  # central rear serpent glyph made only from Ouros geometry
  cube([-0.34,17.70,3.82],[0.68,4.55,0.22],'gold'),
  cube([-1.12,19.35,3.84],[2.24,0.46,0.20],'gold_dark',pivot=[0,19.58,3.94],rotation=[0,0,45]),
  cube([-0.72,22.15,3.86],[1.44,0.52,0.20],'cobra_green',pivot=[0,22.41,3.96],rotation=[0,0,45]),
 ]}

def champion_gi():
 return {'name':'ouros_cobra_gi','parent':'torso2','pivot':[0,15.0,0],'cubes':[
  # stronger open-V chest masses, still exposing the central biological torso
  cube([-4.45,12.05,-3.76],[3.25,5.80,0.40],'dojo_black',pivot=[-2.82,14.95,-3.56],rotation=[0,0,-7]),
  cube([1.20,12.05,-3.76],[3.25,5.80,0.40],'charcoal',pivot=[2.82,14.95,-3.56],rotation=[0,0,7]),
  cube([-4.00,17.15,-4.02],[3.42,0.40,0.22],'gold',pivot=[-2.30,17.35,-3.91],rotation=[0,0,-29]),
  cube([0.58,17.15,-4.02],[3.42,0.40,0.22],'gold_dark',pivot=[2.30,17.35,-3.91],rotation=[0,0,29]),
  # side cloth and a split rear coat create garment depth
  cube([-4.72,11.95,-2.75],[0.42,6.05,5.70],'dojo_black'),
  cube([4.30,11.95,-2.75],[0.42,6.05,5.70],'charcoal'),
  cube([-4.25,12.00,3.30],[3.85,5.95,0.34],'dojo_black'),
  cube([0.40,12.35,3.30],[3.85,5.60,0.34],'charcoal'),
  cube([-4.22,11.70,3.62],[3.82,0.30,0.18],'gold'),
  cube([0.42,12.08,3.62],[3.80,0.28,0.18],'gold_dark'),
  # pronounced sleeveless shoulder yoke connecting to the mantle
  cube([-6.55,16.95,-2.35],[2.30,1.05,4.95],'dojo_black',pivot=[-5.40,17.47,0.12],rotation=[0,0,-9]),
  cube([4.25,17.15,-2.20],[2.00,0.88,4.65],'charcoal',pivot=[5.25,17.59,0.12],rotation=[0,0,8]),
  cube([-6.58,17.83,-2.24],[2.18,0.28,4.72],'gold',pivot=[-5.49,17.97,0.12],rotation=[0,0,-9]),
  cube([4.32,17.87,-2.10],[1.84,0.24,4.42],'gold_dark',pivot=[5.24,17.99,0.11],rotation=[0,0,8]),
 ]}

def belt_sash():
 return {'name':'ouros_cobra_belt_sash','parent':'torso','pivot':[0,12.1,0],'cubes':[
  cube([-4.55,11.50,-3.34],[9.10,1.10,0.38],'shadow'),
  cube([-4.55,11.50,2.96],[9.10,1.10,0.38],'shadow'),
  cube([-1.55,11.25,-3.78],[3.10,1.40,0.44],'gold'),
  cube([-0.42,10.35,-3.94],[0.84,1.00,0.26],'cobra_green'),
  cube([-3.30,5.55,-3.48],[1.90,6.10,0.44],'dojo_black',pivot=[-2.35,11.35,-3.26],rotation=[-7,0,9]),
  cube([-3.06,5.55,-3.66],[1.42,0.34,0.18],'gold',pivot=[-2.35,5.72,-3.57],rotation=[-7,0,9]),
  cube([1.35,7.15,-3.44],[1.30,4.45,0.42],'charcoal',pivot=[2.00,11.35,-3.23],rotation=[-6,0,-6]),
  cube([1.48,7.15,-3.62],[1.04,0.28,0.18],'gold_dark',pivot=[2.00,7.29,-3.53],rotation=[-6,0,-6]),
 ]}

def forearm(name,parent,left):
 x0,x1=((9.25,13.65) if left else (-13.65,-9.25)); acc='gold' if left else 'gold_dark'
 return {'name':name,'parent':parent,'pivot':[(x0+x1)/2,20.45,0],'cubes':[
  cube([x0,19.45,-1.10],[x1-x0,0.32,2.20],'wrap'),
  cube([x0,21.08,-1.10],[x1-x0,0.32,2.20],'wrap'),
  cube([x0+0.70,20.10,-1.38],[x1-x0-1.40,0.30,0.18],acc),
 ]}

def leg_rail(name,parent,x0,stage,accent,left):
 y0={2:7.50,3:4.50,4:1.50}[stage]
 ox=x0+(2.78 if left else 0.02)
 return {'name':name,'parent':parent,'pivot':[x0+1.5,y0+1.5,0],'cubes':[
  cube([x0-0.06,y0+2.36,-1.56],[3.12,0.28,3.12],'wrap'),
  cube([ox,y0+0.35,-1.72],[0.24,1.95,0.20],'dojo_black'),
  cube([ox+0.02,y0+0.62,-1.96],[0.20,1.35,0.16],accent),
 ]}

def foot_guard(name,parent,x0,accent,left):
 c=[cube([x0+0.16,1.48,-2.75],[3.68,0.32,3.58],'dojo_black'),cube([x0+0.38,1.78,-2.82],[3.24,0.20,0.20],accent),cube([x0+0.28,0.42,0.62],[3.44,0.28,1.10],'wrap')]
 if left:c.append(cube([x0+3.44,1.44,-2.34],[0.28,0.38,1.85],'gold'))
 return {'name':name,'parent':parent,'pivot':[x0+2,1,-0.4],'cubes':c}

def cosmetic_bones():
 return [cobra_fan_mantle(),champion_gi(),belt_sash(),
  forearm('ouros_cobra_left_forearm','arm_left2',True),forearm('ouros_cobra_right_forearm','arm_right2',False),
  leg_rail('ouros_cobra_left_leg2','leg_left2',1.0,2,'gold',True),leg_rail('ouros_cobra_left_leg3','leg_left3',1.0,3,'gold_dark',True),leg_rail('ouros_cobra_left_leg4','leg_left4',1.0,4,'gold',True),
  leg_rail('ouros_cobra_right_leg2','leg_right2',-4.0,2,'gold_dark',False),leg_rail('ouros_cobra_right_leg3','leg_right3',-4.0,3,'gold',False),leg_rail('ouros_cobra_right_leg4','leg_right4',-4.0,4,'gold_dark',False),
  foot_guard('ouros_cobra_left_foot','foot_left',0.5,'gold',True),foot_guard('ouros_cobra_right_foot','foot_right',-4.5,'gold_dark',False)]

def derive_model(src,dst):
 data=json.loads(Path(src).read_text()); geo=data['minecraft:geometry'][0]; original=copy.deepcopy(geo['bones'])
 if len(original)!=30:raise SystemExit(f'expected 30 official Hitmonlee bones, got {len(original)}')
 geo['description']['identifier']='geometry.ouros_cobra_dojo_hitmonlee'; geo['bones']=original+cosmetic_bones(); Path(dst).parent.mkdir(parents=True,exist_ok=True); Path(dst).write_text(json.dumps(data,separators=(',',':'))+'\n')

def derive_texture(src,dst):
 im=Image.open(src).convert('RGBA')
 if im.size!=(64,64):raise SystemExit(f'unexpected texture size {im.size}')
 for _,(x,y) in PIXELS.items():
  if im.getpixel((x,y))[3]!=0:raise SystemExit(f'material texel {(x,y)} not free')
 for n,(x,y) in PIXELS.items():im.putpixel((x,y),PALETTE[n])
 Path(dst).parent.mkdir(parents=True,exist_ok=True); im.save(dst,optimize=True)

def main():
 ap=argparse.ArgumentParser(); ap.add_argument('--model',required=True,type=Path); ap.add_argument('--normal',required=True,type=Path); ap.add_argument('--shiny',required=True,type=Path); ap.add_argument('--output-root',required=True,type=Path); a=ap.parse_args()
 mo=a.output_root/'ouros_cobra_dojo_hitmonlee.geo.json'; no=a.output_root/'ouros_cobra_dojo_hitmonlee.png'; so=a.output_root/'ouros_cobra_dojo_hitmonlee_shiny.png'
 derive_model(a.model,mo); derive_texture(a.normal,no); derive_texture(a.shiny,so); cb=cosmetic_bones()
 report={'modelSha256':sha256(mo),'normalSha256':sha256(no),'shinySha256':sha256(so),'originalBones':30,'derivedBones':43,'cosmeticBones':13,'cosmeticCubes':sum(len(b.get('cubes',[])) for b in cb),'palettePixels':PIXELS,'bodyTexturePolicy':'official biological texels unchanged; only verified transparent y=63 swatches added','artDirection':'fan-shaped cobra mantle framing head + stronger open-V champion gi + longitudinal kick rails'}
 (a.output_root/'build-report.json').write_text(json.dumps(report,indent=2)+'\n'); print(json.dumps(report,indent=2))
if __name__=='__main__':main()
