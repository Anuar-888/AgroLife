'use strict';
globalThis.AgroCore = (() => {
  const crops = {
    raspberry: { name:'Малина', category:'Ягодные', image:'berries.jpg', rate:15 },
    strawberry: { name:'Клубника', category:'Ягодные', image:'berries.jpg', rate:10 },
    currant: { name:'Смородина', category:'Ягодные', image:'berries.jpg', rate:12 },
  };
  const cropPlans = {
    raspberry: [
      [0,'inspect','Первичный осмотр после посадки'],[7,'water','Полив после укоренения'],[14,'fertilize','Первая подкормка'],[30,'inspect','Осмотр побегов и влажности'],[45,'water','Полив перед цветением'],[55,'fertilize','Подкормка в начале цветения'],[75,'inspect','Проверить завязи'],[90,'harvest','Проверить готовность к сбору']
    ],
    strawberry: [
      [0,'inspect','Первичный осмотр клубники'],[5,'water','Полив после посадки'],[14,'fertilize','Подкормка после укоренения'],[28,'inspect','Осмотр цветения'],[40,'water','Полив в период налива ягод'],[55,'harvest','Проверить готовность ягод к сбору']
    ],
    currant: [
      [0,'inspect','Первичный осмотр смородины'],[7,'water','Полив после посадки'],[18,'fertilize','Весенняя подкормка'],[35,'inspect','Осмотр цветения и завязей'],[60,'water','Полив в период налива ягод'],[80,'harvest','Проверить готовность к сбору']
    ],
  };
  const stages = ['Не определена','Посадка','Всходы / укоренение','Активный рост','Цветение','Формирование урожая','Сбор урожая','Покой'];
  const taskTypes = {inspect:'Осмотр и уход',water:'Полив',fertilize:'Удобрение',harvest:'Сбор урожая',other:'Другая работа'};
  const conditions = {normal:'Без замечаний',attention:'Есть проблема',unknown:'Нужен осмотр'};
  const todayISO = (d=new Date()) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  function validDate(v) {
    if(typeof v!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(v)||v<'2000-01-01'||v>'2100-12-31')return false;
    return Number.isFinite(Date.parse(v))&&new Date(v).toISOString().slice(0,10)===v;
  }
  function addDays(date,count) {const d=new Date(`${date}T12:00:00Z`);d.setUTCDate(d.getUTCDate()+count);return d.toISOString().slice(0,10);}
  const textValid=(v,max)=>typeof v==='string'&&v.trim().length>0&&v.trim().length<=max;
  const validId=id=>typeof id==='string'&&/^[a-zA-Z0-9_-]{1,100}$/.test(id);
  const validTime=t=>typeof t==='string'&&/^([01]\d|2[0-3]):[0-5]\d$/.test(t);
  const timestamp=v=>typeof v==='string'&&Number.isFinite(Date.parse(v));
  const escape=v=>String(v).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const plural=(n,one,few,many)=>n%100>=11&&n%100<=14?many:n%10===1?one:n%10>=2&&n%10<=4?few:many;
  function initialState(today=todayISO()) {
    const state = {version:2,profile:{name:'Ануар',farm:'Моё хозяйство',region:'Алматинская область',selectedCrops:['raspberry','strawberry','currant']},fields:[
      {id:'f1',name:'Малиновый участок',crop:'raspberry',area:2.5,planted:addDays(today,-90)},
      {id:'f2',name:'Клубничная грядка',crop:'strawberry',area:1.2,planted:addDays(today,-75)},
      {id:'f3',name:'Смородиновый ряд',crop:'currant',area:1.8,planted:addDays(today,-180)}
    ],tasks:[
      {id:'t1',field:'f1',title:'Проверить влажность почвы',type:'inspect',time:'08:00',date:today,done:false,priority:'high',completedAt:null},
      {id:'t2',field:'f2',title:'Осмотреть посадки клубники',type:'inspect',time:'10:00',date:today,done:false,priority:'normal',completedAt:null},
      {id:'t3',field:'f3',title:'Проверить систему полива',type:'inspect',time:'17:00',date:today,done:false,priority:'normal',completedAt:null},
      {id:'t4',field:'f1',title:'Осмотреть побеги малины',type:'inspect',time:'09:00',date:addDays(today,1),done:false,priority:'normal',completedAt:null},
      {id:'t5',field:'f3',title:'Записать состояние смородины',type:'inspect',time:'10:00',date:addDays(today,4),done:false,priority:'normal',completedAt:null}
    ],observations:[]};
    return state.fields.reduce((current, field) => rebuildPlan(current, field.id), state);
  }
  function calculateWater(area,unit,rate) {
    if(!['number','string'].includes(typeof area)||!['number','string'].includes(typeof rate))throw new Error('Укажите площадь и норму числами.');
    area=Number(area);rate=Number(rate);
    if(!Number.isFinite(area)||area<=0||!Number.isFinite(rate)||rate<=0||rate>1000||!['ha','m2'].includes(unit))throw new Error('Укажите положительную площадь и норму до 1 000 л/м².');
    const squareMeters=area*(unit==='ha'?10000:1);
    if(squareMeters>1e9)throw new Error('Площадь не должна превышать 100 000 га.');
    const liters=squareMeters*rate;
    return {squareMeters,liters,cubicMeters:liters/1000};
  }
  function validateField(d) {
    if(!d||!textValid(d.name,60))return 'Введите название участка: от 1 до 60 символов.';
    if(typeof d.crop!=='string'||!Object.hasOwn(crops,d.crop))return 'Выберите культуру из списка.';
    if(!['number','string'].includes(typeof d.area)||!Number.isFinite(Number(d.area))||Number(d.area)<=0||Number(d.area)>100000)return 'Площадь должна быть больше 0 и не больше 100 000 га.';
    if(!validDate(d.planted))return 'Укажите существующую дату посадки с 2000 по 2100 год.';
    return '';
  }
  function validateTask(d,fields) {
    if(!d||!textValid(d.title,120))return 'Укажите задачу: от 1 до 120 символов.';
    if(!fields.some(f=>f.id===d.field))return 'Выберите существующий участок.';
    if(!validDate(d.date)||!validTime(d.time))return 'Проверьте дату и время работы.';
    if(typeof d.type!=='string'||!Object.hasOwn(taskTypes,d.type)&&!['fertilize'].includes(d.type))return 'Выберите тип работы.';
    if(!['normal','high'].includes(d.priority))return 'Выберите приоритет.';
    return '';
  }
  function validateObservation(d,fields,today=todayISO()) {
    if(!d||!fields.some(f=>f.id===d.field))return 'Выберите существующий участок.';
    if(!validDate(d.date)||d.date>today)return 'Осмотр можно записать только на сегодня или прошедший день.';
    if(d.date<fields.find(f=>f.id===d.field).planted)return 'Дата осмотра не может быть раньше посадки.';
    if(!stages.includes(d.stage)||!Object.hasOwn(conditions,d.condition))return 'Выберите стадию и состояние посадок.';
    if(!textValid(d.note,1200))return 'Опишите наблюдение: от 1 до 1 200 символов.';
    return '';
  }
  function validState(s) {
    if(!s||s.version!==2||!s.profile||!['name','farm','region'].every(k=>textValid(s.profile[k],80))||!Array.isArray(s.profile.selectedCrops)||s.profile.selectedCrops.length<1||s.profile.selectedCrops.some(id=>!Object.hasOwn(crops,id)))return false;
    if(!Array.isArray(s.fields)||s.fields.length>500||!s.fields.every(f=>f&&validId(f.id)&&!validateField(f)))return false;
    if(!Array.isArray(s.tasks)||s.tasks.length>2000||!s.tasks.every(t=>t&&validId(t.id)&&!validateTask(t,s.fields)&&typeof t.done==='boolean'&&(t.completedAt===null||timestamp(t.completedAt))))return false;
    if(!Array.isArray(s.observations)||s.observations.length>3000||!s.observations.every(o=>o&&validId(o.id)&&!validateObservation(o,s.fields,'2100-12-31')&&timestamp(o.createdAt)))return false;
    return [s.fields,s.tasks,s.observations].every(list=>new Set(list.map(x=>x.id)).size===list.length);
  }
  // Legacy task dates and completion are preserved; a demo growth percentage is not an observation.
  function migrateState(s) {
    if(s?.version===2){
      if(validState(s))return s;
      const base=initialState(), keptFields=Array.isArray(s.fields)?s.fields.filter(f=>f&&crops[f.crop]&&!validateField(f)):[];
      if(keptFields.length)base.fields=keptFields.map(f=>({id:f.id,name:f.name,crop:f.crop,area:Number(f.area),planted:f.planted}));
      const keptIds=new Set(base.fields.map(f=>f.id));
      const keptTasks=Array.isArray(s.tasks)?s.tasks.filter(t=>t&&keptIds.has(t.field)&&!validateTask({...t,priority:t.priority||'normal'},base.fields)).map(t=>({...t,priority:t.priority||'normal',completedAt:t.completedAt||null})):[];
      base.tasks=keptTasks;
      base.observations=Array.isArray(s.observations)?s.observations.filter(o=>o&&keptIds.has(o.field)&&!validateObservation(o,base.fields,'2100-12-31')&&timestamp(o.createdAt)):[];
      base.profile={...base.profile,...(s.profile||{}),selectedCrops:Array.isArray(s.profile?.selectedCrops)?s.profile.selectedCrops.filter(id=>crops[id]):base.profile.selectedCrops};
      if(!base.profile.selectedCrops.length)base.profile.selectedCrops=Object.keys(crops);
      return base.fields.reduce((current,field)=>rebuildPlan(current,field.id),base);
    }
    if(!s||s.version!==1||!Array.isArray(s.fields)||!Array.isArray(s.tasks))throw new Error('Неизвестный формат данных.');
    const base=initialState(), keptFields=s.fields.filter(f=>f&&crops[f.crop]&&!validateField(f)).map(f=>({id:f.id,name:f.name,crop:f.crop,area:Number(f.area),planted:f.planted}));
    if(keptFields.length)base.fields=keptFields;
    const keptIds=new Set(base.fields.map(f=>f.id));
    base.tasks=s.tasks.filter(t=>t&&keptIds.has(t.field)&&!validateTask({...t,priority:'normal'},base.fields)).map(t=>({...t,priority:'normal',completedAt:null}));
    return base.fields.reduce((current,field)=>rebuildPlan(current,field.id),base);
  }
  const taskStatus=(t,today=todayISO())=>t.done?'done':t.date<today?'overdue':t.date===today?'today':'upcoming';
  function sortTasks(tasks) {return [...tasks].sort((a,b)=>Number(a.done)-Number(b.done)||a.date.localeCompare(b.date)||(a.priority===b.priority?0:a.priority==='high'?-1:1)||a.time.localeCompare(b.time)||a.id.localeCompare(b.id));}
  function fieldSummary(s,id,today=todayISO()) {
    const tasks=sortTasks(s.tasks.filter(t=>t.field===id)),pending=tasks.filter(t=>!t.done),overdue=pending.filter(t=>t.date<today);
    const observations=s.observations.filter(o=>o.field===id).sort((a,b)=>b.date.localeCompare(a.date)||b.createdAt.localeCompare(a.createdAt)||b.id.localeCompare(a.id));
    const latest=observations[0]||null,done=tasks.filter(t=>t.done).length;
    return {tasks,pending,overdue,observations,latest,done,percent:tasks.length?Math.round(done/tasks.length*100):0,next:pending[0]||null,
      attention:overdue.length>0||latest?.condition==='attention',
      label:overdue.length?'Есть просрочки':latest?.condition==='attention'?'Есть проблема':pending.some(t=>t.date===today)?'Работы сегодня':latest?.condition==='normal'?'Осмотрено':'Нужен осмотр'};
  }
  function generatePlan(field, existingTasks = []) {
    const plan = cropPlans[field.crop] || [];
    return plan.map(([offset,type,title]) => ({
      id:`${field.id}-plan-${offset}-${type}`,
      field:field.id,
      title,
      type,
      time:type==='water'?'08:00':type==='harvest'?'09:00':'10:00',
      date:addDays(field.planted,offset),
      done:false,
      priority:type==='harvest'?'high':'normal',
      completedAt:null,
      source:'plan',
      planKey:`${field.crop}:${offset}:${type}`,
    })).filter(t => !existingTasks.some(old => old.id===t.id || old.planKey===t.planKey && old.field===field.id));
  }
  function rebuildPlan(s, fieldId) {
    const field=s.fields.find(f=>f.id===fieldId); if(!field) throw new Error('Участок не найден.');
    const previous=s.tasks.filter(t=>t.field===fieldId&&t.source==='plan');
    const manual=s.tasks.filter(t=>t.field!==fieldId||t.source!=='plan');
    const generated=generatePlan(field,[]).map(task=>{
      const old=previous.find(item=>item.planKey===task.planKey);
      return old?{...task,id:old.id,done:old.done,completedAt:old.completedAt}:task;
    });
    return {...s,tasks:sortTasks([...manual,...generated])};
  }
  function farmSummary(s,today=todayISO()) {
    const todays=sortTasks(s.tasks.filter(t=>t.date===today));
    return {area:s.fields.reduce((n,f)=>n+Number(f.area),0),fields:s.fields.length,crops:new Set(s.fields.map(f=>f.crop)).size,todays,
      overdue:sortTasks(s.tasks.filter(t=>taskStatus(t,today)==='overdue')),upcoming:sortTasks(s.tasks.filter(t=>taskStatus(t,today)==='upcoming')),
      doneToday:todays.filter(t=>t.done).length,pendingToday:todays.filter(t=>!t.done).length,attention:s.fields.filter(f=>fieldSummary(s,f.id,today).attention).length};
  }
  function upsertField(s,d,o) {
    const error=validateField(d);if(error)throw new Error(error);
    const old=o.editId&&s.fields.find(f=>f.id===o.editId);
    if(o.editId&&!old)throw new Error('Участок не найден.');
    if(!old&&s.fields.length>=500)throw new Error('Достигнут лимит 500 участков.');
    if(s.fields.some(f=>f.id!==o.editId&&f.name.trim().toLocaleLowerCase('ru')===d.name.trim().toLocaleLowerCase('ru')))throw new Error('Участок с таким названием уже есть.');
    if(old&&s.observations.some(obs=>obs.field===old.id&&obs.date<d.planted))throw new Error('Дата посадки позже уже записанного осмотра.');
    const field={id:old?.id||o.id,name:d.name.trim(),crop:d.crop,area:Number(d.area),planted:d.planted};
    if(!validId(field.id)||(!old&&s.fields.some(f=>f.id===field.id)))throw new Error('Некорректный идентификатор участка.');
    const next={...s,fields:old?s.fields.map(f=>f.id===field.id?field:f):[...s.fields,field]};
    return rebuildPlan(next,field.id);
  }
  function upsertTask(s,d,o) {
    const error=validateTask(d,s.fields);if(error)throw new Error(error);
    const old=o.editId&&s.tasks.find(t=>t.id===o.editId);
    if(o.editId&&!old)throw new Error('Задача не найдена.');
    if(!old&&s.tasks.length>=2000)throw new Error('Достигнут лимит 2 000 задач.');
    const task={id:old?.id||o.id,field:d.field,title:d.title.trim(),type:d.type,date:d.date,time:d.time,priority:d.priority,done:old?.done||false,completedAt:old?.completedAt||null};
    if(!validId(task.id)||(!old&&s.tasks.some(t=>t.id===task.id)))throw new Error('Некорректный идентификатор задачи.');
    return {...s,tasks:sortTasks(old?s.tasks.map(t=>t.id===task.id?task:t):[...s.tasks,task])};
  }
  function setTaskDone(s,id,done,at=new Date().toISOString()) {
    if(!s.tasks.some(t=>t.id===id))throw new Error('Задача не найдена.');
    if(typeof done!=='boolean'||!timestamp(at))throw new Error('Некорректное завершение задачи.');
    return {...s,tasks:s.tasks.map(t=>t.id===id?{...t,done,completedAt:done?at:null}:t)};
  }
  function addObservation(s,d,o) {
    const error=validateObservation(d,s.fields,o.today);if(error)throw new Error(error);
    if(s.observations.length>=3000)throw new Error('Достигнут лимит 3 000 наблюдений.');
    if(!validId(o.id)||s.observations.some(x=>x.id===o.id)||!timestamp(o.at))throw new Error('Некорректная запись наблюдения.');
    return {...s,observations:[...s.observations,{id:o.id,field:d.field,date:d.date,stage:d.stage,condition:d.condition,note:d.note.trim(),createdAt:o.at}]};
  }
  return {crops,cropPlans,stages,taskTypes,conditions,todayISO,validDate,addDays,escape,plural,initialState,calculateWater,validateField,validateTask,validateObservation,validState,migrateState,taskStatus,sortTasks,fieldSummary,farmSummary,generatePlan,rebuildPlan,upsertField,upsertTask,setTaskDone,addObservation};
})();
