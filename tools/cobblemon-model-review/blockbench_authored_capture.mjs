#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { chromium } from 'playwright-core';
function arg(n){const i=process.argv.indexOf(n);if(i<0||!process.argv[i+1])throw new Error(`missing ${n}`);return process.argv[i+1]}
function opt(n,d=null){const i=process.argv.indexOf(n);return i<0||!process.argv[i+1]?d:process.argv[i+1]}
const endpoint=arg('--endpoint'), texture=path.resolve(arg('--texture')), out=path.resolve(arg('--output-dir'));
const animPath=opt('--animation')?path.resolve(opt('--animation')):null, animName=opt('--animation-name'), animTime=Number(opt('--animation-time','0'));
const profileIn=opt('--camera-profile-in')?path.resolve(opt('--camera-profile-in')):null, profileOut=opt('--camera-profile-out')?path.resolve(opt('--camera-profile-out')):null;
const profile=profileIn?JSON.parse(fs.readFileSync(profileIn,'utf8')):null; fs.mkdirSync(out,{recursive:true});
let browser,last; for(let i=0;i<50;i++){try{browser=await chromium.connectOverCDP(endpoint);break}catch(e){last=e;await new Promise(r=>setTimeout(r,400))}} if(!browser)throw last;
const pages=browser.contexts().flatMap(c=>c.pages()); const page=pages.find(p=>p.url().includes('index.html'))??pages[0]; if(!page)throw new Error('no Blockbench page');
await page.waitForFunction(()=>typeof Project!=='undefined'&&Project&&typeof Cube!=='undefined'&&Cube.all.length>0&&typeof Preview!=='undefined'&&Preview.selected,{timeout:60000});
const info=await page.evaluate(async ({texture,animPath,animName,animTime,animContent})=>{
 const tex=new Texture().fromPath(texture).add(false,true); if(tex.setAsDefaultTexture)tex.setAsDefaultTexture(); else tex.use_as_default=true; tex.select();
 await new Promise((res,rej)=>{const t=Date.now();const id=setInterval(()=>{if(tex.error){clearInterval(id);rej(new Error('texture load error'))}else if(tex.width>0&&tex.height>0&&tex.img?.complete){clearInterval(id);res()}else if(Date.now()-t>15000){clearInterval(id);rej(new Error('texture timeout'))}},100)});
 Canvas.updateAllFaces();Canvas.updateAllBones();let applied=null;
 if(animContent&&animName){if(!Animator.open)Animator.join();const codec=AnimationCodec.getCodec();const imported=codec.loadFile({content:animContent,path:animPath,name:animPath?.split(/[\\/]/).pop()},[animName]);const a=imported?.find(x=>x.name===animName)??Animation.all.find(x=>x.name===animName);if(!a)throw new Error(`animation not found ${animName}`);a.select();a.playing=true;Timeline.setTime(animTime);Animator.preview();scene.updateMatrixWorld(true);await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));applied=a.name}
 return {format:Format.id,cubes:Cube.all.length,bones:Group.all.length,texture:[tex.width,tex.height],animation:applied};
},{texture,animPath,animName,animTime,animContent:animPath?fs.readFileSync(animPath,'utf8'):null});
if(!String(info.format).includes('bedrock'))throw new Error(`unexpected format ${info.format}`);
const views={front:{p:[0,0,-512],locked:'north',span:'x'},back:{p:[0,0,512],locked:'south',span:'x'},left:{p:[-512,0,0],locked:'west',span:'z'},right:{p:[512,0,0],locked:'east',span:'z'},three_quarter:{p:[-512,210,-512],locked:null,span:'max'}};
const meta={viewer:'Blockbench',blockbenchVersion:'5.1.6',model:info,views:{}};
for(const [name,v] of Object.entries(views)){
 const fixed=profile?.views?.[name]??null;
 const r=await page.evaluate(async ({name,v,fixed})=>{
  if(typeof Animator!=='undefined'&&Animator.open&&Animation?.selected)Animator.preview();scene.updateMatrixWorld(true);
  const b=new THREE.Box3();for(const c of Cube.all)if(c.mesh)b.expandByObject(c.mesh);if(b.isEmpty())throw new Error('empty bounds');const center=b.getCenter(new THREE.Vector3()),size=b.getSize(new THREE.Vector3());
  const h=v.span==='x'?size.x:v.span==='z'?size.z:Math.hypot(size.x,size.z)/Math.SQRT2;const vert=v.span==='max'?size.y+Math.min(size.x,size.z)*0.25:size.y;const zoom=Math.min(1024/40/Math.max(h*1.22,.001),1024/40/Math.max(vert*1.22,.001));
  const pre=Preview.selected;const preset={id:`ouros_${name}`,name:`Ouros ${name}`,projection:'orthographic',position:v.p,target:[0,0,0],zoom:fixed?.zoom??zoom};if(v.locked)preset.locked_angle=v.locked;pre.loadAnglePreset(preset);
  if(fixed){pre.controls.target.fromArray(fixed.target);pre.camera.position.fromArray(fixed.cameraPosition);pre.camOrtho.zoom=fixed.zoom}else{const off=pre.camera.position.clone().sub(pre.controls.target);pre.controls.target.copy(center);pre.camera.position.copy(center.clone().add(off));pre.camOrtho.zoom=zoom}
  pre.camOrtho.updateProjectionMatrix();pre.controls.update();pre.render();
  const shot=async res=>await new Promise((ok,bad)=>{const to=setTimeout(()=>bad(new Error('screenshot timeout')),15000);Screencam.advancedScreenshot(pre,{angle_preset:'view',resolution:[res,res],anti_aliasing:'off',show_gizmos:false,shading:true},d=>{clearTimeout(to);ok(d)})});
  const reviewZoom=pre.camOrtho.zoom;const data=await shot(1024);pre.camOrtho.zoom=reviewZoom*(160/1024);pre.camOrtho.updateProjectionMatrix();pre.controls.update();pre.render();const small=await shot(160);pre.camOrtho.zoom=reviewZoom;pre.camOrtho.updateProjectionMatrix();return {data,small,zoom:reviewZoom,target:pre.controls.target.toArray(),cameraPosition:pre.camera.position.toArray(),bounds:{min:b.min.toArray(),max:b.max.toArray(),size:size.toArray()}};
 },{name,v,fixed});
 for(const [suffix,data] of [['',r.data],['_gameplay_160',r.small]]){if(!data.startsWith('data:image/png;base64,'))throw new Error('bad png');fs.writeFileSync(path.join(out,`${name}${suffix}.png`),Buffer.from(data.split(',')[1],'base64'))}
 meta.views[name]={zoom:r.zoom,target:r.target,cameraPosition:r.cameraPosition,bounds:r.bounds};
}
fs.writeFileSync(path.join(out,'blockbench-metadata.json'),JSON.stringify(meta,null,2)+'\n');
if(profileOut)fs.writeFileSync(profileOut,JSON.stringify({viewer:'Blockbench',views:Object.fromEntries(Object.entries(meta.views).map(([k,v])=>[k,{zoom:v.zoom,target:v.target,cameraPosition:v.cameraPosition}]))},null,2)+'\n');
console.log(JSON.stringify(meta,null,2));await browser.close();
