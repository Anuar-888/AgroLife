const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');

const context=vm.createContext({});
vm.runInContext(fs.readFileSync(path.join(__dirname,'..','core.js'),'utf8'),context);
const C=context.AgroCore;
const today='2026-09-29';
const seed=()=>C.initialState(today);

test('the catalog contains the three supported berry crops',()=>{
  assert.deepEqual(Object.keys(C.crops),['raspberry','strawberry','currant']);
  assert.deepEqual(Array.from(seed().profile.selectedCrops),['raspberry','strawberry','currant']);
});

test('each field receives a dated plan from planting to harvest',()=>{
  const state=seed();
  for(const field of state.fields){
    const planned=state.tasks.filter(task=>task.field===field.id&&task.source==='plan');
    assert.equal(planned.length,C.cropPlans[field.crop].length);
    assert.equal(planned[0].date,field.planted);
    assert.ok(planned.some(task=>task.type==='water'));
    assert.ok(planned.some(task=>task.type==='fertilize'));
    assert.ok(planned.some(task=>task.type==='harvest'));
  }
  assert.ok(C.validState(state));
});

test('changing the planting date rebuilds planned dates without duplicates',()=>{
  const state=seed();
  const field=state.fields.find(item=>item.id==='f1');
  const completed=state.tasks.find(task=>task.field==='f1'&&task.source==='plan');
  const marked=C.setTaskDone(state,completed.id,true,'2026-09-29T08:00:00Z');
  const next=C.upsertField(marked,{...field,planted:'2026-10-10'},{editId:'f1'});
  const planned=next.tasks.filter(task=>task.field==='f1'&&task.source==='plan');
  assert.equal(planned.length,C.cropPlans.raspberry.length);
  assert.equal(planned.find(task=>task.planKey==='raspberry:0:inspect').date,'2026-10-10');
  assert.equal(planned.find(task=>task.id===completed.id).done,true);
});

test('manual tasks remain editable and keep their identity',()=>{
  const state=seed();
  const next=C.upsertTask(state,{field:'f1',title:'Дополнительный полив',type:'water',date:'2026-10-01',time:'08:30',priority:'high'},{id:'manual-1'});
  const edited=C.upsertTask(next,{field:'f1',title:'Перенесённый полив',type:'water',date:'2026-10-02',time:'09:00',priority:'normal'},{editId:'manual-1'});
  assert.equal(edited.tasks.filter(task=>task.id==='manual-1').length,1);
  assert.equal(edited.tasks.find(task=>task.id==='manual-1').date,'2026-10-02');
});

test('watering calculation supports hectares and square metres',()=>{
  assert.equal(C.calculateWater(2.5,'ha',15).liters,375000);
  assert.equal(C.calculateWater(25000,'m2',15).cubicMeters,375);
  assert.throws(()=>C.calculateWater(0,'ha',15));
  assert.throws(()=>C.calculateWater(1,'acre',15));
});

test('dates, observations and stored state are validated',()=>{
  assert.equal(C.validDate('2028-02-29'),true);
  assert.equal(C.validDate('2026-02-29'),false);
  const state=seed();
  const observation={field:'f1',date:today,stage:'Активный рост',condition:'normal',note:'Кусты развиваются равномерно.'};
  const next=C.addObservation(state,observation,{id:'obs-1',today,at:'2026-09-29T10:00:00Z'});
  assert.equal(C.fieldSummary(next,'f1',today).latest.id,'obs-1');
  assert.ok(C.validState(next));
  assert.equal(C.validState({...next,version:99}),false);
});

test('legacy data migrates and receives generated crop plans',()=>{
  const state=seed();
  const legacy={version:1,fields:state.fields,tasks:state.tasks.filter(task=>!task.source)};
  const migrated=C.migrateState(legacy);
  assert.equal(migrated.version,2);
  assert.ok(migrated.tasks.some(task=>task.source==='plan'));
  assert.ok(C.validState(migrated));
});

test('Russian counters and HTML escaping remain correct',()=>{
  assert.equal(C.plural(21,'поле','поля','полей'),'поле');
  assert.equal(C.plural(12,'поле','поля','полей'),'полей');
  assert.equal(C.escape('<img src=x onerror="alert(1)">'),'&lt;img src=x onerror=&quot;alert(1)&quot;&gt;');
});
