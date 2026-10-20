(() => {
'use strict';
const $ = (s, scope = document) => scope.querySelector(s);
const $$ = (s, scope = document) => [...scope.querySelectorAll(s)];
const jobs = [
{id:'northline',company:'Northline',title:'Java Backend Developer',city:'Madrid',mode:'Remote',salary:'€38,000–46,000',role:'Backend',tags:['Java','Spring Boot','PostgreSQL'],experience:'2+ years',days:'None',description:'Build and maintain backend services for a small product team. Work with Java, Spring Boot and PostgreSQL.',requirements:['Experience building REST APIs','Automated testing and code review','Residence in Spain'],pay:'€38,000–46,000 gross / year'},
{id:'forma',company:'Forma Labs',title:'Full Stack Developer',city:'Valencia',mode:'Hybrid',salary:'€34,000–42,000',role:'Full stack',tags:['React','TypeScript','Java'],experience:'2+ years',days:'2 per week',description:'Develop product features across a React interface and Java services, working with design and engineering.',requirements:['React and TypeScript experience','Java service development','Two office days per week in Valencia'],pay:'€34,000–42,000 gross / year'},
{id:'cedar',company:'Cedar Systems',title:'SAP Developer',city:'Barcelona',mode:'Hybrid',salary:'Not disclosed',role:'SAP',tags:['SAP','ABAP','SQL'],experience:'1+ year',days:'1 per week',description:'Extend business workflows and maintain integrations for internal teams.',requirements:['SAP development experience','SQL fundamentals','One office day per week in Barcelona'],pay:'Not disclosed'},
{id:'meridian',company:'Meridian',title:'Frontend Developer',city:'Madrid',mode:'Remote',salary:'€32,000–40,000',role:'Frontend',tags:['React','TypeScript','CSS'],experience:'1+ year',days:'None',description:'Build accessible interfaces with a small engineering and design team.',requirements:['React and TypeScript experience','Responsive CSS','Residence in Spain'],pay:'€32,000–40,000 gross / year'},
{id:'stillwater',company:'Stillwater',title:'Backend Engineer',city:'Bilbao',mode:'On-site',salary:'€36,000–44,000',role:'Backend',tags:['Java','Docker','SQL'],experience:'3+ years',days:'5 per week',description:'Develop reliable internal services and support their deployment.',requirements:['Java and SQL experience','Container fundamentals','Office-based work in Bilbao'],pay:'€36,000–44,000 gross / year'},
{id:'common',company:'Common Works',title:'Junior Java Developer',city:'Seville',mode:'Remote',salary:'€24,000–29,000',role:'Backend',tags:['Java','Spring Boot','JUnit'],experience:'0–1 years',days:'None',description:'Join a small backend team with structured mentoring and regular code reviews.',requirements:['Java fundamentals','Personal or academic projects welcome','Residence in Spain'],pay:'€24,000–29,000 gross / year'}
];
const state={view:'discover',saved:new Set(),profile:null,technologies:[],activeJob:null};
const current=document.body.dataset.variant==='00-current';
const esc=v=>String(v).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const status=$('#live-status');
function announce(message){status.textContent=message;}
function renderJobs(){
 const q=$('#search').value.trim().toLowerCase(),mode=$('#filter-mode').value,role=$('#filter-role').value;
 const filtered=jobs.filter(j=>(state.view!=='saved'||state.saved.has(j.id))&&(!q||[j.title,j.company,j.city,...j.tags].join(' ').toLowerCase().includes(q))&&(!mode||j.mode===mode)&&(!role||j.role===role));
 $('#result-count').textContent=filtered.length+' '+(filtered.length===1?'opportunity':'opportunities');
 $('#job-list').innerHTML=filtered.length?filtered.map((j,i)=>'<article class="job"><div class="job-index" aria-hidden="true">'+String(i+1).padStart(2,'0')+'</div><div class="job-main"><p class="company">'+esc(j.company)+'</p><h2><button class="title-button" data-open="'+j.id+'">'+esc(j.title)+'</button></h2><p class="job-meta">'+esc(j.city)+' · '+j.mode+' · Spain</p><ul class="tags" aria-label="Technologies">'+j.tags.map(t=>'<li>'+t+'</li>').join('')+'</ul></div><div class="job-end"><p class="salary">'+j.salary+'</p><span class="salary-unit">'+(j.pay==='Not disclosed'?'Salary':'gross / year')+'</span><button class="save-button" data-save="'+j.id+'" aria-label="'+(state.saved.has(j.id)?'Unsave ':'Save ')+esc(j.title)+'" aria-pressed="'+state.saved.has(j.id)+'">'+(state.saved.has(j.id)?'Saved ✓':'Save')+'</button></div></article>').join(''):'<div class="empty"><h2>'+(state.view==='saved'&&state.saved.size===0?'Nothing saved yet.':'No opportunities found.')+'</h2><p>'+(state.view==='saved'&&state.saved.size===0?'Save an opportunity from Discover.':'Try a different search or clear the filters.')+'</p><button type="button" id="empty-reset">'+(state.view==='saved'&&state.saved.size===0?'Explore opportunities':'Clear filters')+'</button></div>';
 $$('.saved-count').forEach(e=>e.textContent=String(state.saved.size));
 $$('#job-list [data-open]').forEach(b=>b.addEventListener('click',()=>openJob(b.dataset.open)));
 $$('#job-list [data-save]').forEach(b=>b.addEventListener('click',()=>toggleSave(b.dataset.save)));
 $('#empty-reset')?.addEventListener('click',()=>{resetFilters();if(state.view==='saved'&&state.saved.size===0) location.hash='discover';else renderJobs();});
}
function resetFilters(){$('#search').value='';$('#filter-mode').value='';$('#filter-role').value='';}
function toggleSave(id){
 const was=state.saved.has(id);if(was)state.saved.delete(id);else state.saved.add(id);
 renderJobs();syncDetail();
 const replacement=$('#job-list [data-save="'+id+'"]');
 if(replacement)replacement.focus();else if(!$('#detail').open)$('#view-heading').focus();
 announce(was?'Opportunity removed from saved.':'Opportunity saved for this preview session.');
}
function syncDetail(){
 if(!state.activeJob)return;
 const b=$('#detail-save'),saved=state.saved.has(state.activeJob);
 b.textContent=saved?'Saved ✓':'Save opportunity';b.setAttribute('aria-pressed',String(saved));
}
function openJob(id){
 const j=jobs.find(j=>j.id===id);state.activeJob=id;
 $('#detail-company').textContent=j.company;
 $('#detail-title').textContent=j.title;
 $('#detail-meta').textContent=j.city+' · '+j.mode+' · Spain';
 $('#detail-description').textContent=j.description;
 $('#detail-facts').innerHTML='<div><dt>Salary</dt><dd>'+j.pay+'</dd></div><div><dt>Experience</dt><dd>'+j.experience+'</dd></div><div><dt>Office days</dt><dd>'+j.days+'</dd></div><div><dt>Contract</dt><dd>Full-time</dd></div>';
 $('#detail-requirements').innerHTML=j.requirements.map(x=>'<li>'+esc(x)+'</li>').join('');
 syncDetail();$('#detail').showModal();$('#detail-close').focus();
}
$('#detail-save').addEventListener('click',()=>{toggleSave(state.activeJob);$('#detail-save').focus();});
$('#detail-close').addEventListener('click',()=>$('#detail').close());
$('#detail').addEventListener('close',()=>{$('#job-list [data-open="'+state.activeJob+'"]')?.focus();});
function setView(){
 const target=location.hash.slice(1);
 state.view=['discover','saved','profile'].includes(target)?target:(current?'profile':'discover');
 $('#catalog-view').hidden=state.view==='profile';$('#profile-view').hidden=state.view!=='profile';
 const hero=$('#current-hero');if(hero)hero.hidden=state.view!=='profile';
 const direction=$('#direction');if(direction)direction.hidden=state.view!=='profile';
 $$('.view-link').forEach(a=>{if(a.dataset.view===state.view)a.setAttribute('aria-current','page');else a.removeAttribute('aria-current');});
 $('#view-heading').textContent=state.view==='saved'?'Saved opportunities':'Discover opportunities';
 $('#catalog-kicker').textContent=state.view==='saved'?'Your shortlist':'The next chapter';
 $('#catalog-caption').textContent=state.view==='saved'?'Keep the opportunities worth returning to.':'A considered next step.';
 resetFilters();renderJobs();
}
window.addEventListener('hashchange',setView);
['#search','#filter-mode','#filter-role'].forEach(s=>$(s).addEventListener(s==='#search'?'input':'change',()=>{renderJobs();announce($('#result-count').textContent);}));
$('#clear-filters').addEventListener('click',()=>{resetFilters();renderJobs();$('#search').focus();announce('Filters cleared.');});
const form=$('#profile-form'),tech=$('#technology');
function drawTech(){
 $('#technology-list').innerHTML=state.technologies.map((t,i)=>'<li><span>'+esc(t)+'</span><button type="button" data-remove="'+i+'" aria-label="Remove '+esc(t)+'">×</button></li>').join('');
 $$('#technology-list button').forEach(b=>b.addEventListener('click',()=>{state.technologies.splice(Number(b.dataset.remove),1);drawTech();dirty();tech.focus();}));
}
function dirty(){$('#draft-state').textContent='Unsaved changes';$('#profile-message').textContent='';}
function addTech(){
 const name=tech.value.trim().replace(/\s+/g,' ');
 const error=!name?'Enter a technology.':name.length>40?'Use 40 characters or fewer.':state.technologies.length>=20?'Add up to 20 technologies.':state.technologies.some(t=>t.toLowerCase()===name.toLowerCase())?'This technology is already added.':'';
 $('#technology-error').textContent=error;tech.setAttribute('aria-invalid',String(Boolean(error)));
 if(error){tech.focus();return false;}state.technologies.push(name);tech.value='';drawTech();dirty();tech.focus();return true;
}
$('#add-technology').addEventListener('click',addTech);
tech.addEventListener('keydown',e=>{if(e.key==='Enter'&&!e.isComposing){e.preventDefault();addTech();}});
const work=$('#profile-workmode'),days=$('#profile-days');
function showDays(){const show=['Hybrid','On-site'].includes(work.value);$('#days-field').hidden=!show;days.required=show;if(!show)days.value='';}
work.addEventListener('change',()=>{days.value='';showDays();});
form.addEventListener('input',dirty);form.addEventListener('change',dirty);
function updateSummary(){
 const p=state.profile;
 $('#profile-summary-content').innerHTML=p?'<dl><dt>Target role</dt><dd>'+esc(p.role)+'</dd><dt>Technologies</dt><dd>'+p.technologies.map(esc).join(' · ')+'</dd><dt>Relevant experience</dt><dd>'+p.years+' years</dd><dt>Residence</dt><dd>'+esc(p.city)+', '+esc(p.country)+'</dd><dt>Work mode</dt><dd>'+p.mode+'</dd><dt>Maximum on-site days</dt><dd>'+(p.days?p.days+' per week':'None — remote only')+'</dd></dl>':'<p class="summary-empty">No saved profile yet.</p>';
 $('#profile-badge').textContent=p?'Profile saved':'No saved profile';
}
form.addEventListener('submit',e=>{
 e.preventDefault();$('#form-error').textContent='';
 if(tech.value.trim()&&!addTech())return;
 const invalid=$$('input,select',form).find(el=>!el.checkValidity()||(el.required&&el.type==='text'&&!el.value.trim()));
 if(invalid){$('#form-error').textContent='Complete the required fields.';invalid.setAttribute('aria-invalid','true');invalid.focus();invalid.reportValidity();return;}
 if(!state.technologies.length){$('#technology-error').textContent='Add at least one technology.';tech.setAttribute('aria-invalid','true');tech.focus();return;}
 $$('[aria-invalid]',form).forEach(el=>el.removeAttribute('aria-invalid'));
 state.profile={role:$('#profile-role').value,years:Number($('#profile-years').value),technologies:[...state.technologies],country:$('#profile-country').value.trim(),city:$('#profile-city').value.trim(),mode:work.value,days:days.value};
 updateSummary();$('#draft-state').textContent='Saved';$('#profile-message').textContent='Profile saved for this preview session.';
});
$('#clear-profile').addEventListener('click',()=>{
 if(!confirm('Clear the profile and edits in this preview?'))return;
 form.reset();state.profile=null;state.technologies=[];tech.value='';drawTech();showDays();updateSummary();$('#draft-state').textContent='No unsaved changes';$('#profile-message').textContent='Profile cleared.';$('#technology-error').textContent='';$('#form-error').textContent='';$$('[aria-invalid]',form).forEach(el=>el.removeAttribute('aria-invalid'));$('#profile-role').focus();
});
$('#fill-example').addEventListener('click',()=>{
 $('#profile-role').value='Backend developer';$('#profile-years').value='2';$('#profile-country').value='Spain';$('#profile-city').value='Madrid';work.value='Remote';showDays();state.technologies=['Java','Spring Boot','PostgreSQL'];tech.value='';drawTech();dirty();$('#technology-error').textContent='';$('#form-error').textContent='';announce('Example profile loaded. Select Save profile to update the summary.');
});
$('#simulate-connection')?.addEventListener('click',()=>{const b=$('#simulate-connection');b.disabled=true;b.textContent='Checking…';$('#connection-text').textContent='Checking the preview…';setTimeout(()=>{b.disabled=false;b.textContent='Check connection again';$('#connection-text').textContent='Preview ready — simulated connection';},400);});
showDays();updateSummary();setView();
})();