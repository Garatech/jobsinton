/* Shared enhancement for the standalone HTML preview. No network or production storage access. */
(() => {
  'use strict';
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const key = 'jobsinton.design.current-preversion.v1';
  const ids = new Set(['backend-java', 'frontend-react', 'data-engineer', 'fullstack', 'platform-engineer', 'sap-developer']);
  const defaults = ['backend-java', 'data-engineer'];
  const query = new URLSearchParams(location.search);
  let storageAvailable = true;
  let stored = {};
  try { stored = JSON.parse(sessionStorage.getItem(key) || '{}') || {}; }
  catch { storageAvailable = false; }
  if (typeof stored !== 'object' || Array.isArray(stored)) stored = {};
  const initialSaved = Array.isArray(stored.saved) ? stored.saved : query.has('saved') ? query.get('saved').split(',') : defaults;
  let saved = new Set(initialSaved.filter(id => ids.has(id)));
  let profile = stored.profile || null;
  let toastTimer;

  function persist() {
    try { sessionStorage.setItem(key, JSON.stringify({ saved: [...saved], profile })); storageAvailable = true; }
    catch { storageAvailable = false; }
  }
  function announce(message) {
    clearTimeout(toastTimer);
    $('#status').textContent = message;
    toastTimer = setTimeout(() => { $('#status').textContent = ''; }, 4500);
  }
  function linkState() {
    $$('a[href]').forEach(a => {
      const raw = a.getAttribute('href');
      if (!raw || raw.startsWith('#')) return;
      const url = new URL(raw, location.href);
      if (url.origin !== location.origin || !/\/[^/]+\.html$/.test(url.pathname)) return;
      url.searchParams.set('saved', [...saved].join(','));
      a.setAttribute('href', url.pathname.split('/').pop() + url.search + url.hash);
    });
  }
  function refreshSaved(focusAfterRemoval = false) {
    $$('.saved-count').forEach(el => { el.textContent = String(saved.size); });
    $$('[data-save]').forEach(button => {
      const yes = saved.has(button.dataset.save);
      button.setAttribute('aria-pressed', String(yes));
      button.setAttribute('aria-label', (yes ? 'Remove ' : 'Save ') + button.dataset.title + (yes ? ' from saved' : ''));
      button.textContent = yes ? 'Saved ✓' : 'Save ＋';
    });
    if (document.body.dataset.page === 'saved') {
      let activeHidden = false;
      $$('.job').forEach(job => {
        job.hidden = !saved.has(job.dataset.job);
        if (job.hidden && job.contains(document.activeElement)) activeHidden = true;
      });
      $('#saved-result').textContent = saved.size + ' saved ' + (saved.size === 1 ? 'opportunity' : 'opportunities');
      $('#empty-state').hidden = saved.size > 0;
      if (focusAfterRemoval && activeHidden) {
        const next = $('.job:not([hidden]) [data-save]') || $('#empty-state a');
        next.focus();
      }
    }
    linkState();
  }
  $$('[data-save]').forEach(button => button.addEventListener('click', () => {
    const id = button.dataset.save;
    const wasSaved = saved.has(id);
    if (wasSaved) saved.delete(id); else saved.add(id);
    persist();
    refreshSaved(true);
    announce(wasSaved ? 'Removed from your shortlist.' : 'Saved to your shortlist.');
  }));
  persist();
  refreshSaved();
  window.addEventListener('pageshow', event => {
    if (!event.persisted) return;
    try {
      const latest = JSON.parse(sessionStorage.getItem(key) || '{}');
      if (Array.isArray(latest.saved)) saved = new Set(latest.saved.filter(id => ids.has(id)));
    } catch { /* Keep the current page state when storage is unavailable. */ }
    refreshSaved();
  });
  $$('dialog [data-close]').forEach(button => button.addEventListener('click', () => button.closest('dialog').close()));
  $('#reset-demo').addEventListener('click', () => $('#reset-dialog').showModal());
  $('#confirm-reset').addEventListener('click', () => {
    saved = new Set(defaults);
    profile = null;
    persist();
    location.href = 'index.html?saved=' + encodeURIComponent(defaults.join(','));
  });
  if ($('#apply-open')) $('#apply-open').addEventListener('click', () => $('#apply-dialog').showModal());

  if ($('#search-form')) {
    const input = $('#query');
    const role = $('#role-filter');
    const salary = $('#salary-filter');
    const sort = $('#sort');
    const modes = $$('input[name="mode"]');
    const jobs = $$('.jobs .job');
    const validModes = new Set(modes.map(el => el.value));
    input.value = (query.get('q') || '').slice(0, 160);
    if ([...role.options].some(o => o.value === query.get('role'))) role.value = query.get('role');
    const selectedModes = (query.get('mode') || '').split(',').filter(m => validModes.has(m));
    modes.forEach(el => { el.checked = selectedModes.includes(el.value); });
    if ([...salary.options].some(o => o.value === query.get('salary'))) salary.value = query.get('salary');
    if (query.get('sort') === 'salary') sort.value = 'salary';
    let layout = query.get('view') === 'cards' ? 'cards' : 'list';
    function updateLayout() {
      $('.jobs').classList.toggle('cards', layout === 'cards');
      $$('[data-layout]').forEach(b => b.setAttribute('aria-pressed', String(b.dataset.layout === layout)));
    }
    function updateQuery() {
      const url = new URL(location.href);
      const entries = { q: input.value.trim(), role: role.value, mode: modes.filter(m => m.checked).map(m => m.value).join(','), salary: salary.value === '0' ? '' : salary.value, sort: sort.value === 'newest' ? '' : sort.value, view: layout === 'list' ? '' : layout };
      for (const [k, v] of Object.entries(entries)) { if (v) url.searchParams.set(k, v); else url.searchParams.delete(k); }
      url.searchParams.set('saved', [...saved].join(','));
      try { history.replaceState(null, '', url.href); } catch { /* Some file URL policies do not allow history writes. */ }
    }
    function chip(text, action) {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'filter-chip';
      b.textContent = text + ' ×';
      b.setAttribute('aria-label', 'Remove filter: ' + text);
      b.addEventListener('click', () => { action(); filter(); input.focus(); });
      $('#active-filters').append(b);
    }
    function filter() {
      const terms = input.value.trim().toLowerCase().split(/\s+/).filter(Boolean);
      const workModes = modes.filter(m => m.checked).map(m => m.value);
      jobs.sort((a, b) => sort.value === 'salary' ? Number(b.dataset.salary) - Number(a.dataset.salary) : Number(a.dataset.days) - Number(b.dataset.days));
      let count = 0;
      for (const job of jobs) {
        job.hidden = !(terms.every(t => job.dataset.search.includes(t)) && (!role.value || role.value === job.dataset.role) && (!workModes.length || workModes.includes(job.dataset.mode)) && Number(job.dataset.salary) >= Number(salary.value));
        if (!job.hidden) count++;
        $('.jobs').append(job);
      }
      $('#result-count').textContent = count + ' ' + (count === 1 ? 'opportunity' : 'opportunities');
      $('#empty-state').hidden = count !== 0;
      $('.end-note').hidden = count === 0 || count < jobs.length;
      $('#active-filters').replaceChildren();
      if (input.value.trim()) chip('“' + input.value.trim() + '”', () => { input.value = ''; });
      if (role.value) chip(role.value, () => { role.value = ''; });
      modes.filter(m => m.checked).forEach(m => chip(m.value, () => { m.checked = false; }));
      if (salary.value !== '0') chip('From €' + Number(salary.value).toLocaleString('en-GB'), () => { salary.value = '0'; });
      updateQuery();
    }
    function reset() {
      input.value = ''; role.value = ''; salary.value = '0'; modes.forEach(m => { m.checked = false; });
      filter(); input.focus();
    }
    $('#search-form').addEventListener('submit', event => { event.preventDefault(); filter(); });
    input.addEventListener('input', filter);
    [role, salary, sort, ...modes].forEach(el => el.addEventListener('change', filter));
    $('#clear-filters').addEventListener('click', reset);
    $('#empty-reset').addEventListener('click', reset);
    $$('[data-layout]').forEach(button => button.addEventListener('click', () => { layout = button.dataset.layout; updateLayout(); updateQuery(); }));
    updateLayout(); filter();
  }

  if ($('#company-search-form')) {
    const input = $('#company-query');
    function filterCompanies() {
      const terms = input.value.trim().toLowerCase().split(/\s+/).filter(Boolean);
      let count = 0;
      $$('[data-company-search]').forEach(card => {
        card.hidden = !terms.every(t => card.dataset.companySearch.includes(t));
        if (!card.hidden) count++;
      });
      $('#company-count').textContent = count + (count === 1 ? ' company' : ' companies');
      $('#empty-state').hidden = count !== 0;
    }
    input.addEventListener('input', filterCompanies);
    $('#company-search-form').addEventListener('submit', event => { event.preventDefault(); filterCompanies(); });
    $('#clear-company-search').addEventListener('click', () => { input.value = ''; filterCompanies(); input.focus(); });
  }

  if ($('#profile-form')) {
    const form = $('#profile-form');
    const fields = { role: $('#profile-role'), years: $('#profile-years'), country: $('#profile-country'), city: $('#profile-city'), mode: $('#profile-mode'), days: $('#profile-days') };
    const example = { role: fields.role.value, years: fields.years.value, country: fields.country.value, city: fields.city.value, mode: fields.mode.value, days: '', technologies: $$('#technology-list li > span').map(el => el.textContent) };
    function validProfile(p) {
      return p && Object.keys(fields).every(k => typeof p[k] === 'string' && p[k].length <= 80) &&
        [...fields.role.options].some(o => o.value === p.role && p.role) &&
        ['Remote', 'Hybrid', 'On-site'].includes(p.mode) &&
        /^\d+$/.test(p.years) && Number(p.years) <= 60 &&
        p.country.trim() && p.city.trim() &&
        (p.mode !== 'Hybrid' || /^[1-5]$/.test(p.days)) &&
        Array.isArray(p.technologies) && p.technologies.length > 0 && p.technologies.length <= 12 &&
        p.technologies.every(t => typeof t === 'string' && t.trim() && t.length <= 40);
    }
    if (!validProfile(profile)) profile = structuredClone(example);
    let committed = structuredClone(profile);
    let technologies = [...profile.technologies];
    function draft() {
      return { ...Object.fromEntries(Object.entries(fields).map(([k, el]) => [k, el.value.trim()])), technologies: [...technologies] };
    }
    function changed() {
      $('#draft-state').textContent = JSON.stringify(draft()) === JSON.stringify(committed) ? 'No unsaved changes' : 'Unsaved changes';
      $('#profile-message').textContent = '';
    }
    function modeField(clear = false) {
      const hybrid = fields.mode.value === 'Hybrid';
      $('#days-field').hidden = !hybrid;
      fields.days.required = hybrid;
      fields.days.disabled = !hybrid;
      if (clear || !hybrid) fields.days.value = '';
    }
    function renderTechnologies() {
      $('#technology-list').replaceChildren();
      technologies.forEach(t => {
        const li = document.createElement('li');
        const span = document.createElement('span');
        span.textContent = t;
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = '×';
        button.setAttribute('aria-label', 'Remove ' + t);
        button.addEventListener('click', () => {
          technologies = technologies.filter(x => x !== t);
          renderTechnologies(); changed(); $('#technology').focus();
        });
        li.append(span, button); $('#technology-list').append(li);
      });
    }
    function fill(p) {
      for (const [k, el] of Object.entries(fields)) { el.value = p[k]; el.removeAttribute('aria-invalid'); }
      technologies = [...p.technologies]; renderTechnologies(); modeField();
      $('#profile-error').hidden = true; $('#technology-error').textContent = ''; $('#technology').value = '';
      changed();
    }
    function summary() {
      $('#summary-role').textContent = committed.role;
      $('#summary-years').textContent = committed.years + (committed.years === '1' ? ' year' : ' years');
      $('#summary-location').textContent = committed.city + ', ' + committed.country;
      $('#summary-mode').textContent = committed.mode + (committed.mode === 'Hybrid' ? ' · up to ' + committed.days + ' office days / week' : '');
      $('#summary-tech').replaceChildren();
      committed.technologies.forEach(t => { const li = document.createElement('li'); li.textContent = t; $('#summary-tech').append(li); });
      const role = { 'Backend developer': 'Backend', 'Frontend developer': 'Frontend', 'Full stack developer': 'Full stack', 'Data engineer': 'Data', 'Platform engineer': 'Platform', 'SAP developer': 'SAP' }[committed.role];
      const params = new URLSearchParams({ role, mode: committed.mode, saved: [...saved].join(',') });
      $('#profile-explore').setAttribute('href', 'opportunities.html?' + params.toString());
    }
    function addTechnology() {
      const input = $('#technology'), value = input.value.trim();
      const error = !value ? 'Enter a technology.' : technologies.some(t => t.toLowerCase() === value.toLowerCase()) ? 'This technology is already added.' : technologies.length >= 12 ? 'Keep up to 12 technologies.' : '';
      $('#technology-error').textContent = error;
      if (error) { input.focus(); return; }
      technologies.push(value); input.value = ''; renderTechnologies(); changed(); input.focus();
    }
    $('#add-technology').addEventListener('click', addTechnology);
    $('#technology').addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); addTechnology(); } });
    Object.values(fields).forEach(el => el.addEventListener('input', changed));
    fields.mode.addEventListener('change', () => { modeField(true); changed(); });
    $('#use-example').addEventListener('click', () => { fill(example); fields.role.focus(); });
    $('#discard-changes').addEventListener('click', () => { fill(committed); fields.role.focus(); announce('Changes discarded.'); });
    form.addEventListener('submit', event => {
      event.preventDefault();
      let firstInvalid = null;
      for (const el of Object.values(fields)) {
        const valid = el.disabled || (el.checkValidity() && (!el.required || el.value.trim().length > 0));
        el.setAttribute('aria-invalid', String(!valid));
        if (!valid && !firstInvalid) firstInvalid = el;
      }
      if (!technologies.length && !firstInvalid) firstInvalid = $('#technology');
      if (firstInvalid) {
        $('#profile-error').textContent = 'Check the required fields and add at least one technology.';
        $('#profile-error').hidden = false; firstInvalid.focus(); return;
      }
      committed = draft(); profile = structuredClone(committed); persist(); summary(); changed();
      $('#profile-error').hidden = true;
      $('#profile-message').textContent = storageAvailable ? 'Preferences saved for this preview tab.' : 'Preferences updated on this page. Browser storage is unavailable.';
      announce('Preferences updated.');
    });
    fill(committed); summary(); persist();
  }
})();
