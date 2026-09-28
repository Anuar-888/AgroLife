 'use strict';
const C = AgroCore;
const { crops, cropPlans, stages, taskTypes, conditions, escape: esc, calculateWater, todayISO, addDays, plural } = C;
const STORAGE_KEY = 'agrolife-web-demo-v1'; // Retained to migrate existing users without losing their data.
let today = todayISO();
let state = C.initialState(today);
let storageError = '';
let corruptStorage = false;
try {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (raw) {
    try {
      const previous = JSON.parse(raw);
      state = C.migrateState(previous);
      if (previous.version === 1 && !localStorage.getItem(STORAGE_KEY + '-backup')) localStorage.setItem(STORAGE_KEY + '-backup', raw);
    } catch (error) {
      corruptStorage = true;
      storageError = 'Сохранённые данные не удалось открыть. Исходная запись сохранена; текущие изменения можно выгрузить в файл.';
    }
  }
} catch { storageError = 'Локальное сохранение недоступно. Перед закрытием страницы скачайте данные.'; }
let currentView = 'overview', fieldFilter = 'all', search = '', taskFilter = 'all', taskScope = 'day';
let selectedDate = today, calendarYear = Number(today.slice(0,4)), calendarMonth = Number(today.slice(5,7))-1;
let chatMessages = [], lastFocus, modalFieldId = null, calculatedWater = null, waterFieldId = '';
const $ = s => document.querySelector(s);
const format = n => new Intl.NumberFormat('ru-RU',{maximumFractionDigits:2}).format(n);
const dateLabel = (d,year=false) => new Date(d+'T12:00:00').toLocaleDateString('ru-RU',{day:'numeric',month:'long',...(year?{year:'numeric'}:{})});
const uid = prefix => `${prefix}-${crypto.randomUUID()}`;
const paths = {
  leaf: '<path d="M12 21V10M12 14C4 14 3 8 4 4c7 0 10 4 8 10Zm0-4C12 4 17 2 21 3c0 6-3 9-9 7Z"/>',
  grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  fields: '<path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3V6Zm6-3v15m6-12v15"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M7 3v4m10-4v4M3 11h18m-14 4h2m4 0h2m-8 3h2"/>',
  calculator: '<rect x="5" y="2" width="14" height="20" rx="3"/><path d="M8 6h8M8 11h1m6 0h1m-8 4h1m6 0h1m-8 4h1m6 0h1"/>',
  sparkle: '<path d="m12 3 2.7 6.3L21 12l-6.3 2.7L12 21l-2.7-6.3L3 12l6.3-2.7L12 3Zm7-1v4m-2-2h4"/>',
  arrow: '<path d="M4 12h16m-6-6 6 6-6 6"/>',
  up: '<path d="M6 18 18 6M6 6h12v12"/>',
  chevron: '<path d="m9 5 7 7-7 7"/>',
  down: '<path d="m6 9 6 6 6-6"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9m-9 12h6"/>',
  pin: '<path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 1v2m0 18v2M1 12h2m18 0h2M4.2 4.2l1.5 1.5m12.6 12.6 1.5 1.5M4.2 19.8l1.5-1.5M18.3 5.7l1.5-1.5"/>',
  cloud: '<path d="M7 18a5 5 0 1 1 1-9 7 7 0 0 1 13 3 3 3 0 0 1-1 6H7Z"/>',
  drop: '<path d="M12 2S5 10 5 15a7 7 0 0 0 14 0C19 10 12 2 12 2Z"/><path d="M8 15a4 4 0 0 0 4 4"/>',
  wind: '<path d="M3 8h12a3 3 0 1 0-3-3M2 12h17a3 3 0 1 1-3 3M4 16h5a3 3 0 1 1-3 3"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  circleCheck: '<circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6m0-10v.1"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  book: '<path d="M12 5C9 2 4 3 2 4v16c4-2 7-1 10 1 3-2 6-3 10-1V4c-2-1-7-2-10 1Zm0 0v16"/>',
  download: '<path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5"/>',
  send: '<path d="m22 2-7 20-4-9L2 9 22 2Zm0 0L11 13"/>',
};
const icon = (name, cls = '') => `<svg class="icon ${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.leaf}</svg>`;

const navItems = [['overview','grid','Обзор хозяйства'],['fields','fields','Мои поля'],['cultures','leaf','Культуры'],['calendar','calendar','Задачи и календарь'],['tools','calculator','Расчёт полива'],['assistant','sparkle','Помощник Асыл']];
const titles = {overview:'Обзор хозяйства',fields:'Мои поля',cultures:'Культуры',calendar:'Задачи и календарь',tools:'Расчёт полива',assistant:'Помощник Асыл',about:'О проекте'};
function toast(message) { const el=$('#toast');el.textContent=message;el.classList.add('visible');clearTimeout(toast.timer);toast.timer=setTimeout(()=>el.classList.remove('visible'),4500); }
function commit(next) {
  if (!C.validState(next)) throw new Error('Не удалось сохранить: проверьте данные.');
  state=next;
  if(corruptStorage)return;
  try {localStorage.setItem(STORAGE_KEY,JSON.stringify(state));storageError='';}
  catch {storageError='Не удалось сохранить в браузере. Скачайте данные перед закрытием страницы.';}
}
function navigate(view) {
  currentView=Object.hasOwn(titles,view)?view:'overview';document.body.classList.remove('menu-open');
  if(location.hash!==`#${currentView}`)history.pushState(null,'',`#${currentView}`);
  render();window.scrollTo({top:0,behavior:'instant'});
}
function fieldName(id) {return state.fields.find(f=>f.id===id)?.name||'Участок';}
function moveToDate(date) {selectedDate=date;calendarYear=Number(date.slice(0,4));calendarMonth=Number(date.slice(5,7))-1;}
function pageHeading(eyebrow,title,subtitle,action='') {return `<div class="page-heading"><div><div class="eyebrow">${eyebrow}</div><h1>${title}</h1><p>${subtitle}</p></div>${action}</div>`;}
const addButton=()=>`<button class="button primary" data-action="add-field">${icon('plus')}Добавить поле</button>`;
function render() {
  document.title=`${titles[currentView]} — AgroLife`;
  const summary=C.farmSummary(state,today);
  $('#app').innerHTML=`<aside class="sidebar" id="sidebar"><a class="brand" href="index.html" aria-label="AgroLife — на сайт"><span class="brand-mark">${icon('leaf')}</span>Agro<span>Life</span><i></i></a>
    <button class="workspace" data-action="profile"><span class="workspace-icon">${icon('fields')}</span><span><strong>${esc(state.profile.farm)}</strong><small>${esc(state.profile.region)}</small></span>${icon('down')}</button>
    <div class="nav-label">МОЁ ХОЗЯЙСТВО</div><nav aria-label="Основная навигация">${navItems.map(([id,i,label])=>`<a href="#${id}" class="nav-item ${currentView===id?'active':''}" ${currentView===id?'aria-current="page"':''}>${icon(i)}<span>${label}</span>${id==='fields'?`<span class="nav-count">${state.fields.length}</span>`:''}</a>`).join('')}</nav>
    <div class="sidebar-bottom"><div class="grow-card">${icon('leaf')}<strong>Каждый день —<br>шаг к урожаю.</strong><p>Начните с одного участка<br>и понятного плана.</p><button class="text-button" data-action="add-field">Добавить своё поле ${icon('arrow')}</button></div><a class="nav-item ${currentView==='about'?'active':''}" href="#about">${icon('info')}О проекте и данные</a><div class="sidebar-footer"><span class="live-dot"></span>Локальное рабочее пространство</div></div></aside>
    <button class="sidebar-backdrop" data-action="menu-close" aria-label="Закрыть меню"></button>
    <div class="workspace-main"><header class="topbar"><div class="breadcrumb"><button class="icon-button mobile-menu" data-action="menu" aria-label="Открыть меню">${icon('menu')}</button><span class="breadcrumb-home">Моё хозяйство</span><span class="breadcrumb-divider">/</span><strong>${titles[currentView]}</strong></div><div class="header-actions"><span class="demo-label"><span></span>Прототип</span><button class="icon-button notification" data-action="notifications" aria-label="Задачи, требующие внимания">${icon('bell')}${summary.overdue.length?'<i></i>':''}</button><button class="profile" data-action="profile" aria-label="Настройки хозяйства"><span class="avatar">${esc(state.profile.name[0].toUpperCase())}</span><span class="profile-name">${esc(state.profile.name)}</span>${icon('down')}</button></div></header>
    ${storageError?`<div class="storage-banner" role="alert">${icon('info')}<span>${esc(storageError)}</span><button class="text-button" data-action="export">Скачать данные</button></div>`:''}
    <main id="main" tabindex="-1">${({overview:overviewView,fields:fieldsView,cultures:culturesView,calendar:calendarView,tools:toolsView,assistant:assistantView,about:aboutView})[currentView]()}</main>
    <footer class="main-footer"><a href="index.html">AgroLife © ${today.slice(0,4)}</a><span>Забота о земле начинается с внимания ${icon('leaf')}</span><span>Ваши данные — в этом браузере</span></footer></div>`;
  bindViewEvents();
}
function overviewView() {
  const s=C.farmSummary(state,today);
  const plan=s.overdue.length?s.overdue.slice(0,3):s.todays;
  return `${pageHeading('ВАШ СЕЗОН В ОДНОМ МЕСТЕ','Всё важное — перед вами<span class="heading-dot">.</span>',`${esc(state.profile.name)}, начните день с понятного плана для вашего хозяйства.`,`<span class="today-date">${icon('calendar')}${dateLabel(today,true)}</span>`)}
    ${s.overdue.length?`<button class="attention-banner" data-action="show-overdue">${icon('clock')}<span><strong>${s.overdue.length} ${plural(s.overdue.length,'просроченная задача','просроченные задачи','просроченных задач')}</strong>Перенесите работу или отметьте выполнение.</span>${icon('arrow')}</button>`:''}
    <div class="overview-layout"><div class="overview-left"><section class="hero"><img src="assets/farm-landscape.jpg" alt="Зелёные поля в свете солнца" fetchpriority="high"><div class="hero-shade"></div><div class="hero-content"><span class="hero-kicker"><span></span>СЕЗОН ${today.slice(0,4)}</span><h2>Больше заботы.<br>Больше жизни.</h2><p>Понимайте, что происходит на полях.<br>Знайте, что делать дальше.</p><button class="button hero-button" data-nav="fields">Мои поля ${icon('arrow')}</button></div><div class="hero-location">${icon('pin')}${esc(state.profile.region)}</div></section>
    <section class="stats" aria-label="Показатели хозяйства"><div class="stat"><span class="stat-label">Общая площадь ${icon('fields')}</span><strong>${format(s.area)}<small>га</small></strong><span class="stat-foot">${s.fields} ${plural(s.fields,'участок','участка','участков')}</span></div><div class="stat"><span class="stat-label">Работы сегодня ${icon('circleCheck')}</span><strong>${s.pendingToday}<small>осталось</small></strong><span class="stat-foot">Выполнено ${s.doneToday} из ${s.todays.length}</span></div><div class="stat ${s.attention?'stat-attention':''}"><span class="stat-label">Нужно внимание ${icon('clock')}</span><strong>${s.attention}<small>${plural(s.attention,'поле','поля','полей')}</small></strong><span class="stat-foot">${s.overdue.length?'Есть незавершённые работы':'Просрочек нет'}</span></div></section>
    <section class="fields-section"><div class="section-heading"><div><h2>Ваши поля <span class="count-badge">${s.fields}</span></h2><p>Состояние и следующий шаг по каждому участку</p></div><button class="text-button" data-nav="fields">Все поля ${icon('arrow')}</button></div><div class="field-grid">${state.fields.slice(0,3).map(fieldCard).join('')||empty('Добавьте первый участок','Начните с культуры, площади и даты посадки.')}</div></section>
    <section class="assistant-teaser"><span class="assistant-mark">${icon('sparkle')}</span><div><div class="teaser-tag">ПОМОЩНИК В ЕЖЕДНЕВНЫХ ДЕЛАХ</div><h3>Начните с вопроса Асыл.</h3><p>Соберёт задачи и покажет, каким полям нужно внимание.</p></div><button class="button secondary" data-nav="assistant">Спросить ${icon('arrow')}</button></section></div>
    <aside class="overview-right"><section class="panel tasks-panel"><div class="section-heading"><h2>${s.overdue.length?'Сначала завершить':'План на сегодня'}</h2><span class="count-badge">${s.overdue.length||s.pendingToday}</span></div><div class="task-progress" aria-label="Выполнение сегодняшнего плана"><span style="width:${s.todays.length?s.doneToday/s.todays.length*100:0}%"></span></div><div class="task-list">${plan.length?plan.map(taskRow).join(''):empty('На сегодня всё','Добавьте работу или загляните в календарь.')}</div><button class="text-button full-width" data-nav="calendar">Весь календарь ${icon('arrow')}</button></section>${weatherCard()}<div class="daily-note">${icon('book')}<div><strong>Не забывайте про осмотр</strong><p>Запишите наблюдение в карточке поля. Оно станет основой следующего решения.</p></div></div></aside></div>
    <div class="demo-footnote">${icon('info')}Первые три участка — примеры. Статусы и показатели рассчитываются по вашим задачам и наблюдениям.</div>`;
}
function empty(title,text) {return `<div class="empty-state compact">${icon('leaf')}<h3>${title}</h3><p>${text}</p></div>`;}
function fieldCard(f) {
  const c=crops[f.crop],s=C.fieldSummary(state,f.id,today);
  return `<button class="field-card" data-field="${esc(f.id)}"><div class="field-image"><img src="assets/${c.image}" alt="Иллюстрация: ${c.name}" loading="lazy"><span class="field-tag ${s.attention?'attention':''}"><span></span>${s.label}</span><span class="field-open">${icon('up')}</span></div><div class="field-card-body"><div class="field-crop">${c.name}<span>${format(f.area)} га</span></div><h3>${esc(f.name)}</h3><p class="field-observation">${s.latest?`${esc(s.latest.stage)} · ${dateLabel(s.latest.date)}`:'Стадия ещё не подтверждена'}</p><div class="field-stage"><span>Выполнено работ</span><span>${s.done} / ${s.tasks.length}</span></div><div class="field-progress"><span style="width:${s.percent}%"></span></div><div class="field-bottom">${icon(s.next?'clock':'check')}<span>${s.next?esc(s.next.title):'Добавьте следующий шаг'}</span>${icon('chevron')}</div></div></button>`;
}
function taskRow(t) {
  const status=C.taskStatus(t,today);
  return `<div class="task-row ${t.done?'done':''} ${status==='overdue'?'overdue':''}"><label class="task-toggle"><input type="checkbox" data-task="${esc(t.id)}" ${t.done?'checked':''} aria-label="${t.done?'Вернуть в план':'Выполнить'}: ${esc(t.title)}"><span class="custom-check">${icon('check')}</span><span class="task-copy"><strong>${esc(t.title)}</strong><small>${esc(fieldName(t.field))}</small><span class="task-time ${t.type}">${icon(t.type==='water'?'drop':t.type==='fertilize'?'leaf':'clock')}${taskTypes[t.type]||'Работа'} · ${dateLabel(t.date)} · ${t.time}${t.priority==='high'?' · Важно':''}</span>${status==='overdue'?'<span class="overdue-label">Просрочено</span>':''}${t.done?'<span class="done-label">Выполнено</span>':''}</span></label><button class="icon-button task-edit" data-edit-task="${esc(t.id)}" aria-label="Изменить задачу: ${esc(t.title)}" title="Изменить или перенести">${icon('calendar')}</button></div>`;
}
function weatherCard() {return `<section class="weather-card"><div class="weather-top"><span>${icon('pin')}${esc(state.profile.region)}</span></div><div class="weather-placeholder">${icon('sun')}<h3>Погода для вашего поля</h3><p>Прогноз пока не подключён. Задачи и расчёты не используют вымышленные погодные данные.</p><span class="pill">Скоро в AgroLife</span></div></section>`;}
function fieldsView() {
  const fields=state.fields.filter(f=>(fieldFilter==='all'||crops[f.crop].category===fieldFilter)&&`${f.name} ${crops[f.crop].name}`.toLocaleLowerCase('ru').includes(search.trim().toLocaleLowerCase('ru')));
  return `${pageHeading('КАЖДОМУ УЧАСТКУ — ВНИМАНИЕ','Ваши поля. Ваш порядок.','Откройте поле, чтобы записать осмотр или запланировать работу.',addButton())}<div class="filter-toolbar"><div class="tabs" role="group" aria-label="Фильтр культур">${['all','Ягодные','Овощные','Плодовые'].map(c=>`<button class="tab ${fieldFilter===c?'active':''}" data-filter="${c}" aria-pressed="${fieldFilter===c}">${c==='all'?'Все культуры':c}</button>`).join('')}</div><label class="search-input">${icon('search')}<input id="field-search" type="search" placeholder="Название или культура" value="${esc(search)}" aria-label="Найти поле"></label></div><div class="field-grid all-fields">${fields.map(fieldCard).join('')}${!search&&fieldFilter==='all'?`<button class="add-field-card" data-action="add-field"><span>${icon('plus')}</span><strong>Место для нового урожая</strong><small>Добавьте участок и первую задачу</small></button>`:''}</div>${!fields.length?empty('Полей не найдено','Измените запрос или добавьте участок.'):''}<div class="info-strip">${icon('info')}Статус поля учитывает просроченные работы и последний осмотр. Полоса показывает выполненные задачи, а не рост растения.</div>`;
}
function culturesView() {
  const selected = state.profile.selectedCrops || Object.keys(crops);
  return `${pageHeading('КАТАЛОГ КУЛЬТУР','Три культуры — понятный сезон.','Выберите культуры, с которыми работает ваше хозяйство. Этот выбор сохраняется в профиле и используется при добавлении полей.',`<button class="button primary" data-action="profile">${icon('check')}Сохранить в профиле</button>`)}
  <div class="culture-grid">${Object.entries(crops).map(([id,c])=>{const plan=cropPlans[id]||[];return `<article class="culture-card ${selected.includes(id)?'selected':''}"><div class="culture-image"><img src="assets/${c.image}" alt="${c.name}" loading="lazy"><span class="culture-select">${selected.includes(id)?icon('check'):'+'}</span></div><div class="culture-body"><div class="culture-meta"><span>${c.category}</span><span>${plan.length} этапов плана</span></div><h2>${c.name}</h2><p>${id==='raspberry'?'Цветение, полив, подкормка и сбор ягод.':id==='strawberry'?'Укоренение, цветение, налив и регулярный сбор.':'Цветение, завязи, полив и созревание ягод.'}</p><div class="culture-plan">${plan.slice(0,4).map(([day,type,title])=>`<span><b>Д${day}</b>${type==='water'?'Полив':type==='fertilize'?'Удобрение':type==='harvest'?'Сбор':'Осмотр'}</span>`).join('')}</div><button class="button ${selected.includes(id)?'secondary':'primary'} culture-toggle" data-culture="${id}" aria-pressed="${selected.includes(id)}">${selected.includes(id)?'Выбрана':'Выбрать культуру'}</button></div></article>`;}).join('')}</div><div class="info-strip">${icon('info')}После выбора культура появится в профиле и в форме нового поля. Дата посадки поля запускает индивидуальный календарный план: осмотр → полив → удобрение → сбор.</div>`;
}
function calendarView() {
  const monthName=new Date(calendarYear,calendarMonth,1).toLocaleDateString('ru-RU',{month:'long',year:'numeric'});
  const offset=(new Date(calendarYear,calendarMonth,1).getDay()+6)%7,days=new Date(calendarYear,calendarMonth+1,0).getDate();
  const filtered=state.tasks.filter(t=>taskFilter==='all'||t.field===taskFilter);
  const tasks=C.sortTasks(filtered.filter(t=>taskScope==='overdue'?C.taskStatus(t,today)==='overdue':taskScope==='done'?t.done:t.date===selectedDate));
  return `${pageHeading('ПОНЯТНЫЙ ПЛАН НА КАЖДЫЙ ДЕНЬ','Время заботиться.','Назначайте работы, переносите сроки и сохраняйте результат.',`<button class="button primary" data-action="add-task">${icon('plus')}Добавить задачу</button>`)}
    <div class="calendar-toolbar"><div class="tabs" role="group" aria-label="Режим списка задач">${[['day','Выбранный день'],['overdue','Просрочено'],['done','Выполнено']].map(([id,label])=>`<button class="tab ${taskScope===id?'active':''}" data-scope="${id}" aria-pressed="${taskScope===id}">${label}</button>`).join('')}</div><label class="form-label calendar-filter">Поле<select id="calendar-field"><option value="all">Все участки</option>${state.fields.map(f=>`<option value="${esc(f.id)}" ${taskFilter===f.id?'selected':''}>${esc(f.name)}</option>`).join('')}</select></label></div>
    <div class="calendar-layout"><section class="panel calendar-panel"><div class="calendar-header"><h2>${monthName}</h2><div><button class="icon-button" data-action="prev-month" aria-label="Предыдущий месяц">${icon('chevron','rotate')}</button><button class="text-button" data-action="today">Сегодня</button><button class="icon-button" data-action="next-month" aria-label="Следующий месяц">${icon('chevron')}</button></div></div><div class="calendar-grid">${['Пн','Вт','Ср','Чт','Пт','Сб','Вс'].map(d=>`<span class="weekday">${d}</span>`).join('')}${'<span></span>'.repeat(offset)}${Array.from({length:days},(_,i)=>{const d=`${calendarYear}-${String(calendarMonth+1).padStart(2,'0')}-${String(i+1).padStart(2,'0')}`;const ts=filtered.filter(t=>t.date===d);return `<button class="calendar-day ${d===selectedDate?'selected':''} ${d===today?'today':''}" data-date="${d}" aria-pressed="${d===selectedDate}" aria-label="${dateLabel(d,true)}: задач ${ts.length}"><strong>${i+1}</strong><span class="day-dots">${ts.slice(0,3).map(t=>`<i class="${t.done?'completed':t.type}"></i>`).join('')}</span>${ts.length?`<small>${ts.length} ${plural(ts.length,'задача','задачи','задач')}</small>`:''}</button>`;}).join('')}</div><div class="calendar-legend"><span><i class="water"></i>Полив</span><span><i></i>Другие работы</span><span><i class="completed"></i>Выполнено</span></div></section>
    <section class="panel day-plan"><div class="eyebrow">${taskScope==='day'?'ПЛАН РАБОТ':'ВСЕ ДАТЫ'}</div><h2>${taskScope==='overdue'?'Нужно завершить':taskScope==='done'?'История работ':dateLabel(selectedDate)}</h2><div class="task-list">${tasks.length?tasks.map(taskRow).join(''):empty(taskScope==='overdue'?'Нет просроченных работ':taskScope==='done'?'Здесь появится история':'День свободен',taskScope==='done'?'Отмечайте задачи, которые уже выполнены.':'Можно запланировать следующий осмотр.')}</div><button class="button secondary full-width" data-action="add-task">${icon('plus')}Запланировать работу</button></section></div>`;
}
function toolsView() {
  calculatedWater=null;
  const f=state.fields.find(f=>f.id===waterFieldId)||state.fields[0];waterFieldId=f?.id||'';
  return `${pageHeading('ПРОЗРАЧНЫЙ РАСЧЁТ ДЛЯ ВАШЕГО ПОЛЯ','Каждая капля имеет значение.','Выберите участок, задайте норму и перенесите расчёт в план работ.')}<div class="calculator-layout"><section class="panel calculator-panel"><div class="panel-title-icon">${icon('drop')}<h2>Объём на один полив</h2></div><form id="water-form"><label class="form-label">Ваш участок<select id="water-field" name="field"><option value="">Расчёт без участка</option>${state.fields.map(field=>`<option value="${esc(field.id)}" ${field.id===waterFieldId?'selected':''}>${esc(field.name)} · ${crops[field.crop].name}</option>`).join('')}</select></label><label class="form-label">Культура<select id="water-crop" name="crop">${Object.entries(crops).map(([id,c])=>`<option value="${id}" ${id===f?.crop?'selected':''}>${c.name}</option>`).join('')}</select></label><div class="form-row"><label class="form-label">Площадь<input type="number" name="area" id="water-area" min="0.0001" step="any" value="${f?.area||1}" required></label><label class="form-label unit-select">Единица<select name="unit" id="water-unit"><option value="ha">Гектары (га)</option><option value="m2">Метры² (м²)</option></select></label></div><label class="form-label">Норма на один полив, л/м²<input type="number" name="rate" id="water-rate" min="0.01" max="1000" step="any" value="${crops[f?.crop||'raspberry'].rate}" required><small>Начальное значение — пример. Укажите норму, подтверждённую для вашего участка.</small></label><p class="form-error" id="water-error" role="alert"></p><button class="button primary full-width">${icon('calculator')}Рассчитать объём</button></form></section><section class="water-result" id="water-result" aria-live="polite">${waterPlaceholder()}</section></div><div class="calculation-note">${icon('info')}<div><strong>Прозрачная формула, а не автоматическое назначение.</strong><p>Площадь в м² × норма в л/м² = литры. Делим на 1 000, получаем м³. Почва, осадки, состояние растений и потери системы в эту формулу не входят. Задача появится в календаре только после вашего подтверждения.</p></div></div>`;
}
function waterPlaceholder() {return `<div class="eyebrow">ПЛАНИРУЙТЕ ОСОЗНАННО</div><span class="water-result-icon">${icon('drop')}</span><h2>Площадь.<br>Норма.<br>Понятный объём.</h2><p>Результат покажет, сколько воды соответствует введённой норме.</p><div class="formula-tag">1 га = 10 000 м² · 1 м³ = 1 000 л</div>`;}
function assistantView() {return `${pageHeading('ВАШИ ДАННЫЕ — В ПОНЯТНОМ ВИДЕ','Асыл, с чего начнём?','Помощник читает ваш текущий план, участки и журнал осмотров.','<span class="pill">Локальные сценарии · без AI</span>')}<section class="chat-panel panel"><div class="chat-header"><span class="assistant-mark">${icon('sparkle')}</span><div><strong>Асыл</strong><small>Работает с данными этого хозяйства</small></div>${icon('leaf')}</div><div class="chat-messages" id="chat-messages">${!chatMessages.length?`<div class="chat-welcome"><div class="assistant-orbit">${icon('leaf')}</div><h2>Весь план —<br>в одном разговоре.</h2><p>Спросите о задачах, полях или последних наблюдениях.</p></div>`:''}${chatMessages.map(m=>`<div class="chat-bubble ${m.role}">${esc(m.text).replace(/\n/g,'<br>')}</div>`).join('')}</div><div class="chat-suggestions">${['Мой план на сегодня','Какие поля требуют внимания?','Последние осмотры'].map(q=>`<button data-question="${q}">${q}${icon('up')}</button>`).join('')}</div><form id="chat-form"><label class="sr-only" for="chat-input">Вопрос помощнику</label><input id="chat-input" maxlength="1500" placeholder="Что запланировано на сегодня?" required autocomplete="off"><button class="button primary" aria-label="Отправить вопрос">${icon('send')}</button></form><p class="chat-disclaimer">Асыл показывает факты из вашего журнала. Он не назначает обработки или нормы полива.</p></section>`;}
function aboutView() {return `${pageHeading('РАБОЧЕЕ ПРОСТРАНСТВО ФЕРМЕРА','Меньше догадок. Больше ясности.','Поля, наблюдения и выполненные работы связаны в одну историю.')}<section class="about-hero"><div><div class="hero-kicker">ОТ НАБЛЮДЕНИЯ К ДЕЙСТВИЮ</div><h2>Посмотрите на поле.<br>Запишите. Запланируйте.</h2><p>AgroLife помогает сохранить контекст каждого участка и понять, какая работа нужна дальше.</p><button class="button hero-button" data-nav="fields">Открыть мои поля ${icon('arrow')}</button></div>${icon('leaf')}</section><div class="about-grid">${[['fields','Ваши участки','Меняйте площадь и данные поля. При добавлении можно сразу запланировать первый осмотр.'],['book','Факты с поля','Записывайте стадию, состояние и заметку. Последний осмотр определяет статус участка.'],['calendar','Работы и история','Переносите задачи, отмечайте выполнение. Просрочки видны и в календаре, и на главной.']].map(([i,h,p])=>`<section class="panel">${icon(i)}<h3>${h}</h3><p>${p}</p></section>`).join('')}</div><section class="panel prototype-scope"><h2>Ваше хозяйство сохраняется здесь</h2><p>Все изменения хранятся в этом браузере. Выгрузка включает поля, задачи, осмотры и настройки. Аккаунты, облачная синхронизация, прогноз погоды и AI пока не подключены.</p><div class="data-actions"><button class="button primary" data-action="export">${icon('download')}Скачать резервную копию</button><button class="button secondary" data-action="profile">Настройки хозяйства</button></div><div class="info-strip">${icon('info')}Очистка данных браузера удалит локальные записи. Скачанный JSON — резервная копия; импорт пока не реализован.</div></section>`;}
function showModal(html,fieldId=null) {
  const m=$('#modal');if(!m.open)lastFocus=document.activeElement;modalFieldId=fieldId;
  m.innerHTML=`<button class="icon-button modal-close" data-action="close-modal" aria-label="Закрыть">${icon('close')}</button>${html}`;
  if(!m.open)m.showModal();
}
function closeModal() {modalFieldId=null;$('#modal').close();lastFocus?.isConnected&&lastFocus.focus();}
function openFieldForm(id='') {
  const f=state.fields.find(f=>f.id===id);
  const available=(state.profile.selectedCrops||Object.keys(crops)).filter(id=>crops[id]);
  showModal(`<div class="modal-icon">${icon('leaf')}</div><div class="eyebrow">${f?'НАСТРОЙКИ УЧАСТКА':'НОВОЕ ПОЛЕ'}</div><h2 id="modal-title">${f?'Данные вашего поля':'Здесь начинается урожай.'}</h2><p class="modal-description">Площадь пригодится для расчётов, а дата посадки запускает план ухода.</p><form id="field-form" data-edit-id="${f?.id||''}"><label class="form-label">Название<input name="name" maxlength="60" value="${esc(f?.name||'')}" placeholder="Например, Северный участок" required autofocus></label><div class="form-row"><label class="form-label">Культура<select name="crop">${available.map(id=>{const c=crops[id];return `<option value="${id}" ${id===f?.crop?'selected':''}>${c.name}</option>`}).join('')}</select></label><label class="form-label">Площадь, га<input type="number" name="area" min="0.0001" max="100000" step="any" value="${f?.area||''}" placeholder="2,5" required></label></div><label class="form-label">Дата посадки<input name="planted" type="date" min="2000-01-01" max="2100-12-31" value="${f?.planted||today}" required></label><p class="form-hint">После сохранения AgroLife сразу рассчитает даты полива, подкормок, осмотров и подготовки к сбору.</p><p id="field-error" class="form-error" role="alert"></p><button class="button primary full-width">${icon(f?'check':'plus')}${f?'Сохранить изменения':'Добавить поле и план'}</button></form>`);
}
function openField(id) {
  const f=state.fields.find(f=>f.id===id);if(!f)return;const s=C.fieldSummary(state,id,today);
  showModal(`<div class="detail-cover"><img src="assets/${crops[f.crop].image}" alt="Иллюстрация культуры"></div><div class="eyebrow">${crops[f.crop].name.toUpperCase()} · ${format(f.area)} ГА</div><h2 id="modal-title">${esc(f.name)}</h2><p class="modal-description">Посадка ${dateLabel(f.planted,true)} <button class="inline-button" data-action="edit-field" data-id="${esc(id)}">Изменить</button></p><div class="detail-stage ${s.attention?'attention':''}">${icon('leaf')}<span>${s.label}</span><span class="pill">${s.latest?dateLabel(s.latest.date):'Нет осмотров'}</span></div><div class="detail-actions"><button class="button primary" data-action="observe" data-id="${esc(id)}">${icon('book')}Записать осмотр</button><button class="button secondary" data-action="field-task" data-id="${esc(id)}">${icon('plus')}Задача</button><button class="button secondary" data-action="field-calc" data-id="${esc(id)}">${icon('drop')}Полив</button></div><h3 class="detail-task-title">План и выполненные работы</h3><div class="task-list">${s.tasks.length?s.tasks.map(taskRow).join(''):empty('План пока пуст','Запланируйте первый осмотр участка.')}</div><h3 class="detail-task-title">Журнал осмотров <span class="count-badge">${s.observations.length}</span></h3><div class="observation-list">${s.observations.length?s.observations.slice(0,20).map(o=>`<article class="observation"><div><strong>${esc(o.stage)}</strong><span>${dateLabel(o.date,true)}</span></div><small class="${o.condition==='attention'?'overdue-label':''}">${conditions[o.condition]}</small><p>${esc(o.note).replace(/\n/g,'<br>')}</p></article>`).join(''):empty('Сначала — наблюдение','Запишите фактическую стадию и состояние растений.')}</div>${s.observations.length>20?'<p class="muted">Показаны последние 20 осмотров. Полная история есть в выгрузке.</p>':''}`,id);
}
function openTaskForm(fieldId='',editId='',prefill={}) {
  if(!state.fields.length){openFieldForm();return;}
  const task=state.tasks.find(t=>t.id===editId);
  const v=task||{field:fieldId||(taskFilter!=='all'?taskFilter:state.fields[0].id),title:'',date:selectedDate,time:'09:00',type:'inspect',priority:'normal',...prefill};
  showModal(`<div class="modal-icon">${icon('calendar')}</div><div class="eyebrow">${task?'ИЗМЕНЕНИЕ ПЛАНА':'СЛЕДУЮЩИЙ ШАГ'}</div><h2 id="modal-title">${task?'Уточним задачу.':'Что нужно сделать?'}</h2><p class="modal-description">${task?.done?'Задача останется выполненной. Вернуть её в план можно снятием отметки.':'Задайте дату и приоритет. Работа появится в календаре и на карточке участка.'}</p><form id="task-form" data-edit-id="${task?.id||''}"><label class="form-label">Задача<input name="title" value="${esc(v.title)}" maxlength="120" placeholder="Например, проверить влажность почвы" required autofocus></label><label class="form-label">Поле<select name="field">${state.fields.map(f=>`<option value="${esc(f.id)}" ${f.id===v.field?'selected':''}>${esc(f.name)}</option>`).join('')}</select></label><div class="form-row"><label class="form-label">Дата<input name="date" type="date" min="2000-01-01" max="2100-12-31" value="${v.date}" required></label><label class="form-label">Время<input name="time" type="time" value="${v.time}" required></label></div><div class="form-row"><label class="form-label">Тип<select name="type">${Object.entries(taskTypes).map(([id,label])=>`<option value="${id}" ${id===v.type?'selected':''}>${label}</option>`).join('')}</select></label><label class="form-label">Приоритет<select name="priority"><option value="normal" ${v.priority==='normal'?'selected':''}>Обычный</option><option value="high" ${v.priority==='high'?'selected':''}>Важный</option></select></label></div><p id="task-error" class="form-error" role="alert"></p><button class="button primary full-width">${icon('check')}${task?'Сохранить изменения':'Добавить в план'}</button></form>`);
}
function openObservation(fieldId) {
  const f=state.fields.find(f=>f.id===fieldId);if(!f)return;
  if(f.planted>today){toast('Посадка запланирована на будущее. Осмотр можно записать после её даты.');return;}
  const last=C.fieldSummary(state,fieldId,today).latest;
  showModal(`<div class="modal-icon">${icon('book')}</div><div class="eyebrow">${esc(f.name)}</div><h2 id="modal-title">Что вы увидели на поле?</h2><p class="modal-description">Записывайте наблюдаемое состояние. Новый осмотр обновит карточку участка.</p><form id="observation-form"><input type="hidden" name="field" value="${esc(fieldId)}"><label class="form-label">Дата осмотра<input name="date" type="date" min="${f.planted}" max="${today}" value="${today}" required></label><div class="form-row"><label class="form-label">Стадия<select name="stage">${stages.map(s=>`<option ${s===last?.stage?'selected':''}>${s}</option>`).join('')}</select></label><label class="form-label">Состояние<select name="condition">${Object.entries(conditions).map(([id,label])=>`<option value="${id}" ${id==='unknown'?'selected':''}>${label}</option>`).join('')}</select></label></div><label class="form-label">Наблюдение<textarea name="note" rows="4" maxlength="1200" placeholder="Состояние листьев, почвы, замеченные изменения…" required></textarea></label><p id="observation-error" class="form-error" role="alert"></p><button class="button primary full-width">${icon('check')}Сохранить осмотр</button></form>`);
}
function openProfile() {const selected=state.profile.selectedCrops||Object.keys(crops);showModal(`<div class="modal-icon">${icon('fields')}</div><div class="eyebrow">ВАШЕ РАБОЧЕЕ ПРОСТРАНСТВО</div><h2 id="modal-title">Профиль и культуры</h2><p class="modal-description">Выберите культуры, которые доступны при создании новых полей. Уже созданные поля не изменятся.</p><form id="profile-form">${[['name','Ваше имя'],['farm','Название хозяйства'],['region','Регион']].map(([key,label])=>`<label class="form-label">${label}<input name="${key}" value="${esc(state.profile[key])}" maxlength="80" required></label>`).join('')}<div class="profile-crops"><strong>Культуры в хозяйстве</strong>${Object.entries(crops).map(([id,c])=>`<label class="check-label"><input type="checkbox" name="selectedCrops" value="${id}" ${selected.includes(id)?'checked':''}>${c.name}<small>${(cropPlans[id]||[]).length} шагов от посадки до сбора</small></label>`).join('')}</div><p id="profile-error" class="form-error" role="alert"></p><button class="button primary full-width">Сохранить профиль</button></form>`);}
function askAssistant(question) {
  const q=question.trim().slice(0,1500);if(!q)return;const normalized=q.toLocaleLowerCase('ru'),s=C.farmSummary(state,today);let answer;
  if(/осмотр|наблюд|журнал/.test(normalized)) {const list=[...state.observations].sort((a,b)=>b.date.localeCompare(a.date)||b.createdAt.localeCompare(a.createdAt)).slice(0,3);answer=list.length?`Последние наблюдения:\n${list.map(o=>`${dateLabel(o.date)} · ${fieldName(o.field)}: ${o.stage}. ${conditions[o.condition]}. ${o.note}`).join('\n\n')}`:'В журнале ещё нет осмотров. Откройте карточку поля и нажмите «Записать осмотр»: укажите стадию, состояние и ваши наблюдения.';}
  else if(/внимани|проблем|пол[ея]|просроч/.test(normalized)) {const fields=state.fields.filter(f=>C.fieldSummary(state,f.id,today).attention);answer=fields.length?`Нужно внимание:\n${fields.map(f=>{const sum=C.fieldSummary(state,f.id,today);return `${f.name}: ${sum.overdue.length?`${sum.overdue.length} просроченных работ`:''}${sum.overdue.length&&sum.latest?.condition==='attention'?'; ':''}${sum.latest?.condition==='attention'?'в последнем осмотре отмечена проблема':''}.`;}).join('\n')}\nОткройте карточку участка для уточнения плана.`:'Просроченных работ и отмеченных проблем нет. Это не оценка здоровья растений: фактическое состояние нужно подтвердить осмотром.';}
  else if(/полив|вод/.test(normalized)) answer='Откройте «Расчёт полива» и выберите участок. Площадь подставится из его карточки. Введите подтверждённую норму: м² × л/м² = литры. Результат можно добавить в календарь как задачу. Я не могу самостоятельно назначить норму без проверенных данных о почве и культуре.';
  else {const plan=[...s.overdue,...s.todays.filter(t=>!t.done)];answer=plan.length?`План на ${dateLabel(today)}:\n${plan.slice(0,10).map(t=>`${t.date<today?'Просрочено · ':''}${dateLabel(t.date)}, ${t.time} — ${t.title} (${fieldName(t.field)}).`).join('\n')}${plan.length>10?'\nОстальные задачи видны в календаре.':''}`:`На сегодня незавершённых задач нет.${s.upcoming[0]?` Ближайшая работа: ${dateLabel(s.upcoming[0].date)} — ${s.upcoming[0].title} (${fieldName(s.upcoming[0].field)}).`:' Добавьте следующий шаг в календаре.'}`;}
  chatMessages.push({role:'user',text:q},{role:'assistant',text:answer});render();$('#chat-messages').scrollTop=$('#chat-messages').scrollHeight;$('#chat-input').focus();
}
function invalidateWater() {calculatedWater=null;const el=$('#water-result');if(el)el.innerHTML=waterPlaceholder()+'<p class="recalculate-note">Параметры изменены. Рассчитайте объём заново.</p>';}
function bindViewEvents() {
  let previousWaterUnit = $('#water-unit')?.value || 'ha';
  $('#field-search')?.addEventListener('input',e=>{search=e.target.value;const pos=e.target.selectionStart;render();const input=$('#field-search');input.focus();input.setSelectionRange(pos,pos);});
  $('#calendar-field')?.addEventListener('change',e=>{taskFilter=e.target.value;render();});
  $('#water-field')?.addEventListener('change',e=>{waterFieldId=e.target.value;const f=state.fields.find(f=>f.id===waterFieldId);if(f){$('#water-area').value=f.area;$('#water-unit').value='ha';previousWaterUnit='ha';$('#water-crop').value=f.crop;$('#water-rate').value=crops[f.crop].rate;}invalidateWater();});
  $('#water-crop')?.addEventListener('change',e=>{const selected=state.fields.find(f=>f.id===$('#water-field').value);if(selected&&selected.crop!==e.target.value){$('#water-field').value='';waterFieldId='';}$('#water-rate').value=crops[e.target.value].rate;invalidateWater();});
  $('#water-unit')?.addEventListener('change',e=>{const area=$('#water-area'),value=Number(area.value),next=e.target.value;if(previousWaterUnit!==next&&Number.isFinite(value)&&value>0)area.value=String(previousWaterUnit==='ha'?value*10000:value/10000);previousWaterUnit=next;invalidateWater();});
  ['water-area','water-rate'].forEach(id=>$('#'+id)?.addEventListener('input',invalidateWater));
}
function downloadData() {const blob=new Blob([JSON.stringify({...state,exportedAt:new Date().toISOString()},null,2)],{type:'application/json'});const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=`agrolife-${today}.json`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);toast('Резервная копия подготовлена.');}
document.addEventListener('click',e=>{
  const link=e.target.closest('a[href^="#"]');if(link&&Object.hasOwn(titles,link.hash.slice(1))){e.preventDefault();navigate(link.hash.slice(1));return;}
  const nav=e.target.closest('[data-nav]');if(nav){navigate(nav.dataset.nav);return;}
  const field=e.target.closest('[data-field]');if(field){openField(field.dataset.field);return;}
  const filter=e.target.closest('[data-filter]');if(filter){fieldFilter=filter.dataset.filter;render();return;}
  const scope=e.target.closest('[data-scope]');if(scope){taskScope=scope.dataset.scope;render();return;}
  const date=e.target.closest('[data-date]');if(date){selectedDate=date.dataset.date;taskScope='day';render();return;}
  const question=e.target.closest('[data-question]');if(question){askAssistant(question.dataset.question);return;}
  const culture=e.target.closest('[data-culture]');if(culture){
    const id=culture.dataset.culture, selected=new Set(state.profile.selectedCrops||Object.keys(crops));
    if(selected.has(id)){if(selected.size===1){toast('Оставьте хотя бы одну культуру в профиле.');return;}selected.delete(id);}else selected.add(id);
    try{commit({...state,profile:{...state.profile,selectedCrops:[...selected]}});render();toast(selected.has(id)?`${crops[id].name} добавлена в профиль.`:`${crops[id].name} убрана из новых полей.`);}catch(error){toast(error.message);}return;
  }
  const edit=e.target.closest('[data-edit-task]');if(edit){openTaskForm('',edit.dataset.editTask);return;}
  const a=e.target.closest('[data-action]');if(!a)return;
  switch(a.dataset.action){
    case 'add-field':openFieldForm();break;
    case 'edit-field':openFieldForm(a.dataset.id);break;
    case 'close-modal':closeModal();break;
    case 'add-task':openTaskForm();break;
    case 'observe':openObservation(a.dataset.id);break;
    case 'menu':document.body.classList.add('menu-open');break;
    case 'menu-close':document.body.classList.remove('menu-open');break;
    case 'profile':openProfile();break;
    case 'export':downloadData();break;
    case 'notifications':case 'show-overdue':moveToDate(today);taskScope=C.farmSummary(state,today).overdue.length?'overdue':'day';taskFilter='all';navigate('calendar');break;
    case 'prev-month':case 'next-month':{const next=new Date(calendarYear,calendarMonth+(a.dataset.action==='next-month'?1:-1),1);if(next.getFullYear()<2000||next.getFullYear()>2100)return;calendarYear=next.getFullYear();calendarMonth=next.getMonth();selectedDate=todayISO(next);taskScope='day';render();break;}
    case 'today':moveToDate(today);taskScope='day';render();break;
    case 'field-task':openTaskForm(a.dataset.id);break;
    case 'field-calc':waterFieldId=a.dataset.id;closeModal();navigate('tools');break;
    case 'plan-water':if(calculatedWater){const w=calculatedWater;openTaskForm(w.field,'',{title:`Полив: ${format(w.result.cubicMeters)} м³ · ${format(w.result.squareMeters)} м² × ${format(w.rate)} л/м²`,date:today,type:'water'});}break;
  }
});
document.addEventListener('change',e=>{if(!e.target.matches('[data-task]'))return;const id=e.target.dataset.task,done=e.target.checked;try{commit(C.setTaskDone(state,id,done));const field=modalFieldId;render();if(field)openField(field);else $(`[data-task="${CSS.escape(id)}"]`)?.focus();toast(done?'Работа выполнена. План и статус поля обновлены.':'Задача возвращена в план.');}catch(error){e.target.checked=!done;toast(error.message);}});
document.addEventListener('submit',e=>{
  const forms=['field-form','task-form','observation-form','profile-form','water-form','chat-form'];if(!forms.includes(e.target.id))return;e.preventDefault();
  const form=e.target,data=Object.fromEntries(new FormData(form)),editId=form.dataset.editId||'';
  try {
    if(form.id==='field-form') {const id=editId||uid('f');commit(C.upsertField(state,data,{id,editId}));closeModal();fieldFilter='all';search='';navigate('fields');openField(id);toast(editId?'Данные участка и план обновлены.':'Поле и план работ добавлены.');}
    else if(form.id==='task-form') {commit(C.upsertTask(state,data,{id:uid('t'),editId}));closeModal();moveToDate(data.date);taskScope='day';taskFilter='all';navigate('calendar');toast(editId?'План обновлён.':'Работа добавлена в календарь.');}
    else if(form.id==='observation-form') {commit(C.addObservation(state,data,{id:uid('o'),today,at:new Date().toISOString()}));render();openField(data.field);toast('Осмотр записан. Состояние поля обновлено.');}
    else if(form.id==='profile-form') {const profile=Object.fromEntries(['name','farm','region'].map(k=>[k,(data[k]||'').trim()]));const selected=[...form.querySelectorAll('input[name="selectedCrops"]:checked')].map(input=>input.value);if(Object.values(profile).some(v=>!v||v.length>80))throw new Error('Заполните все поля: до 80 символов.');if(!selected.length)throw new Error('Выберите хотя бы одну культуру.');commit({...state,profile:{...state.profile,...profile,selectedCrops:selected}});closeModal();render();toast('Профиль и список культур сохранены.');}
    else if(form.id==='chat-form')askAssistant($('#chat-input').value);
    else if(form.id==='water-form') {const result=calculateWater(data.area,data.unit,data.rate);calculatedWater={result,rate:Number(data.rate),field:data.field};$('#water-error').textContent='';$('#water-result').innerHTML=`<div class="eyebrow">ПО ВАШИМ ПАРАМЕТРАМ</div><span class="water-result-icon">${icon('drop')}</span><div class="result-number">${format(result.cubicMeters)}<span>м³</span></div><div class="result-liters">${format(result.liters)} литров на один полив</div><div class="result-formula"><span>Площадь<strong>${format(result.squareMeters)} м²</strong></span><span>Ваша норма<strong>${format(Number(data.rate))} л/м²</strong></span><div>${format(result.squareMeters)} × ${format(Number(data.rate))} = ${format(result.liters)} л</div></div>${data.field?`<button class="button primary full-width" data-action="plan-water">${icon('calendar')}Запланировать этот полив</button>`:'<p>Выберите участок в форме, чтобы добавить полив в календарь.</p>'}<small id="calculation-status">Проверьте норму перед планированием работы.</small>`;}
  } catch(error) {if(form.id==='water-form')invalidateWater();const el=$('#'+form.id.replace('-form','-error'));if(el)el.textContent=error.message;else toast(error.message);}
});
$('#modal').addEventListener('click',e=>{if(e.target!==$('#modal'))return;const r=$('#modal').getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)closeModal();});
$('#modal').addEventListener('close',()=>{modalFieldId=null;lastFocus?.isConnected&&lastFocus.focus();});
document.addEventListener('keydown',e=>{if(e.key==='Escape')document.body.classList.remove('menu-open');});
window.addEventListener('popstate',()=>{currentView=Object.hasOwn(titles,location.hash.slice(1))?location.hash.slice(1):'overview';render();});
window.addEventListener('hashchange',()=>{currentView=Object.hasOwn(titles,location.hash.slice(1))?location.hash.slice(1):'overview';render();});
// Refresh the current date after a night away, without discarding an open form.
 document.addEventListener('visibilitychange',()=>{if(!document.hidden&&!$('#modal').open&&today!==todayISO()){today=todayISO();moveToDate(today);render();}});
currentView=Object.hasOwn(titles,location.hash.slice(1))?location.hash.slice(1):'overview';
render();
