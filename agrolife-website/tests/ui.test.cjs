const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const {randomUUID}=require('node:crypto');
const domino=require('domino');
const root=path.join(__dirname,'..');
// DOM tests do not emulate browser layout. Only missing browser primitives are shimmed.
function setup(saved=new Map()) {
  const w=domino.createWindow(fs.readFileSync(path.join(root,'demo.html'),'utf8'),'http://localhost:4173/demo.html');
  w.localStorage={getItem:k=>saved.get(k)||null,setItem:(k,v)=>saved.set(k,v)};
  w.crypto={randomUUID};w.CSS={escape:s=>String(s)};
  w.history.pushState=()=>{};w.scrollTo=()=>{};w.setTimeout=()=>1;w.clearTimeout=()=>{};
  if(!Object.getOwnPropertyDescriptor(w.Element.prototype,'dataset'))Object.defineProperty(w.Element.prototype,'dataset',{get(){return Object.fromEntries(Array.from(this.attributes).filter(a=>a.name.startsWith('data-')).map(a=>[a.name.slice(5).replace(/-([a-z])/g,(_,c)=>c.toUpperCase()),a.value]));}});
  const modal=w.document.querySelector('#modal');
  Object.defineProperty(modal,'showModal',{value:function(){this.setAttribute('open','')}});
  Object.defineProperty(modal,'close',{value:function(){this.removeAttribute('open')}});
  w.FormData=class {constructor(form){this.values=Array.from(form.querySelectorAll('[name]')).filter(el=>el.type!=='checkbox'||el.checked).map(el=>[el.getAttribute('name'),el.value||el.getAttribute('value')||el.querySelector('option[selected]')?.getAttribute('value')||el.querySelector('option')?.getAttribute('value')||'']);}[Symbol.iterator](){return this.values[Symbol.iterator]();}};
  const context=vm.createContext(w);
  for(const file of ['core.js','app.js'])vm.runInContext(fs.readFileSync(path.join(root,file),'utf8'),context,{filename:file});
  const run=code=>vm.runInContext(code,context),q=s=>w.document.querySelector(s);
  function fill(name,value){const el=q(`[name="${name}"]`);assert.ok(el,`Missing input ${name}`);Object.defineProperty(el,'value',{value,configurable:true,writable:true});}
  function fire(selector,type){const ev=w.document.createEvent('Event');ev.initEvent(type,true,true);const el=typeof selector==='string'?q(selector):selector;assert.ok(el,`Missing target ${selector}`);el.dispatchEvent(ev);}
  return {w,saved,run,q,fill,fire};
}
test('all seven screens render without exceptions',()=>{
  const {run,q}=setup();
  for(const view of ['overview','fields','cultures','calendar','tools','assistant','about']){run(`navigate('${view}')`);assert.ok(q('h1'));assert.ok(q('main').textContent.length>100);}
});
test('new field creates its crop plan, observation updates status, task completion persists',()=>{
  const h=setup(),today=h.run('today');
  h.run('openFieldForm()');h.fill('name','Северное поле <b>');h.fill('crop','raspberry');h.fill('area','0.75');h.fill('planted',today);h.fire('#field-form','submit');
  const id=h.run('state.fields.at(-1).id');
  assert.equal(h.run('state.fields.length'),4);
  assert.equal(h.run(`C.fieldSummary(state,'${id}',today).tasks.length`),8);
  assert.ok(!h.q('#modal h2 b'));
  h.run(`openObservation('${id}')`);h.fill('field',id);h.fill('date',today);h.fill('stage','Активный рост');h.fill('condition','attention');h.fill('note','Нужно проверить состояние листьев.');h.fire('#observation-form','submit');
  assert.ok(h.q('.detail-stage.attention'));
  assert.equal(h.run('state.observations.length'),1);
  const task=h.q('#modal [data-task]');task.checked=true;h.fire(task,'change');
  assert.equal(h.run(`C.fieldSummary(state,'${id}',today).done`),1);
  assert.equal(h.run(`C.fieldSummary(state,'${id}',today).attention`),true); // Completing a task does not erase an observed issue.
  const restored=setup(h.saved);
  assert.equal(restored.run('state.fields.length'),4);
  assert.equal(restored.run('state.observations.length'),1);
  assert.equal(restored.run(`C.fieldSummary(state,'${id}',today).done`),1);
});
test('task edit moves the calendar selection and preserves task identity',()=>{
  const h=setup(),count=h.run('state.tasks.length');h.run("openTaskForm('','t1')");h.fill('title','Перенесённый осмотр');h.fill('field','f1');h.fill('date','2027-02-03');h.fill('time','14:30');h.fill('type','inspect');h.fill('priority','high');h.fire('#task-form','submit');
  assert.equal(h.run('state.tasks.length'),count);
  assert.equal(h.run('selectedDate'),'2027-02-03');
  assert.ok(h.q('.calendar-header h2').textContent.includes('февраль'));
  assert.ok(h.q('.day-plan').textContent.includes('Перенесённый осмотр'));
});
test('water uses field data, creates a prefilled task, and invalidates stale calculations',()=>{
  const h=setup();h.run("waterFieldId='f1';navigate('tools')");
  assert.equal(h.q('#water-area').value,'2.5');
  for(const [key,value] of Object.entries({field:'f1',crop:'raspberry',area:'2.5',unit:'ha',rate:'15'}))h.fill(key,value);
  h.fire('#water-form','submit');assert.ok(h.q('.result-number').textContent.includes('375'));
  h.fire('[data-action="plan-water"]','click');assert.ok(h.q('#task-form [name="title"]').value.includes('375'));
  h.run("closeModal();navigate('tools')");
  for(const [key,value] of Object.entries({field:'f1',crop:'raspberry',area:'2.5',unit:'ha',rate:'15'}))h.fill(key,value);
  h.fire('#water-form','submit');h.fill('area','3');h.fire('#water-area','input');
  assert.equal(h.run('calculatedWater'),null);assert.ok(!h.q('[data-action="plan-water"]'));assert.ok(h.q('.recalculate-note'));
});
test('corrupt storage is not overwritten by new edits',()=>{
  const saved=new Map([['agrolife-web-demo-v1','broken data']]),h=setup(saved);
  assert.ok(h.q('.storage-banner'));
  h.run("commit({...state,profile:{name:'Тест',farm:'Ферма',region:'Регион'}})");
  assert.equal(saved.get('agrolife-web-demo-v1'),'broken data');
});
test('changing area units preserves physical field area',()=>{
  const h=setup();h.run("waterFieldId='f1';navigate('tools')");
  h.fill('area','2.5');h.fill('unit','m2');h.fire('#water-unit','change');
  assert.equal(h.q('#water-area').value,'25000');
  h.fill('unit','ha');h.fire('#water-unit','change');
  assert.equal(h.q('#water-area').value,'2.5');
});
test('calendar field filter applies to day indicators and list; assistant uses current observations',()=>{
  const h=setup();h.run("taskFilter='f2';navigate('calendar')");
  assert.equal(h.w.document.querySelectorAll('.day-plan [data-task]').length,1);
  assert.equal(h.q('.calendar-day.selected').textContent.includes('1 задача'),true);
  h.run("navigate('assistant');askAssistant('Последние осмотры')");
  assert.ok(h.q('.chat-bubble.assistant').textContent.includes('нет осмотров'));
  h.run("navigate('fields');fieldFilter='Ягодные';render()");assert.equal(h.w.document.querySelectorAll('.field-card').length,3);
});
