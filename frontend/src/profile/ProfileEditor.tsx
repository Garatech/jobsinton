import { useRef, useState, type FormEvent } from 'react';
import {
  cleanText, emptyDraft, ROLES, WORK_MODES, technologyError, toDraft, validateProfile,
  type ProfileDraft, type ProfileErrors, type ProfessionalProfile,
} from './model';
import { loadProfile, removeProfile, saveProfile } from './storage';
import { profileSearch } from '../pages/Home';

const fieldOrder: (keyof ProfileDraft)[] = [
  'role', 'technologies', 'experienceYears', 'country', 'city', 'workMode', 'maxOnsiteDays',
];

/** Draft edits and the last successful save stay separate, including when storage fails. */
export function ProfileEditor({ onSaved }: { onSaved: (profile: ProfessionalProfile | null) => void }) {
  const [initial] = useState(loadProfile);
  const [saved, setSaved] = useState<ProfessionalProfile | null>(
    initial.kind === 'loaded' ? initial.profile : null,
  );
  const [draft, setDraft] = useState<ProfileDraft>(
    () => initial.kind === 'loaded' ? toDraft(initial.profile) : emptyDraft(),
  );
  const [hasStoredData, setHasStoredData] = useState(initial.kind === 'loaded' || initial.kind === 'invalid');
  const [technology, setTechnology] = useState('');
  const [errors, setErrors] = useState<ProfileErrors>({});
  const [message, setMessage] = useState('');
  const [storageError, setStorageError] = useState(
    initial.kind === 'invalid'
      ? 'Saved profile data could not be read. Save a new profile to replace it, or clear the saved profile.'
      : initial.kind === 'unavailable'
        ? 'Browser storage is unavailable. You can edit a profile, but saving may be blocked.'
        : '',
  );
  const technologyInput = useRef<HTMLInputElement>(null);
  const dirty = technology.length > 0
    || JSON.stringify(draft) !== JSON.stringify(saved ? toDraft(saved) : emptyDraft());

  function update(field: Exclude<keyof ProfileDraft, 'technologies'>, value: string) {
    setDraft((current) => ({
      ...current,
      [field]: value,
      // Switching modes discards a previous limit so hidden values cannot reappear unexpectedly.
      ...(field === 'workMode' ? { maxOnsiteDays: '' } : {}),
    }));
    setErrors((current) => ({
      ...current, [field]: undefined,
      ...(field === 'workMode' ? { maxOnsiteDays: undefined } : {}),
    }));
    setMessage('');
  }

  function addTechnology() {
    const error = technologyError(technology, draft.technologies);
    setErrors((current) => ({ ...current, technologies: error }));
    if (error) {
      technologyInput.current?.focus();
      return;
    }
    const name = cleanText(technology);
    setDraft((current) => ({ ...current, technologies: [...current.technologies, name] }));
    setTechnology('');
    setMessage(name + ' added.');
    technologyInput.current?.focus();
  }

  function deleteTechnology(index: number) {
    const name = draft.technologies[index];
    setDraft((current) => ({ ...current, technologies: current.technologies.filter((_, i) => i !== index) }));
    setErrors((current) => ({ ...current, technologies: undefined }));
    setMessage(name + ' removed.');
    technologyInput.current?.focus();
  }

  function focusError(nextErrors: ProfileErrors) {
    const field = fieldOrder.find((item) => nextErrors[item]);
    // Wait for React to attach the error descriptions before focusing their control.
    window.requestAnimationFrame(() => {
      if (field) document.getElementById('profile-' + field)?.focus();
    });
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage('');
    let candidate = draft;
    let pendingError: string | undefined;
    // Include an unsubmitted technology on Save rather than silently dropping typed input.
    if (technology.length) {
      pendingError = technologyError(technology, draft.technologies);
      if (!pendingError) {
        candidate = { ...draft, technologies: [...draft.technologies, cleanText(technology)] };
        setDraft(candidate);
        setTechnology('');
      }
    }
    const result = validateProfile(candidate);
    const nextErrors: ProfileErrors = result.ok ? {} : result.errors;
    if (pendingError) nextErrors.technologies = pendingError;
    setErrors(nextErrors);
    if (!result.ok || pendingError) {
      focusError(nextErrors);
      return;
    }
    if (!saveProfile(result.profile)) {
      setStorageError('Your profile could not be saved. Your edits are still here. Allow browser storage and try again.');
      return;
    }
    setSaved(result.profile);
    onSaved(result.profile);
    setDraft(toDraft(result.profile));
    setHasStoredData(true);
    setStorageError('');
    setMessage('Profile saved in this browser.');
  }

  function clear() {
    if (!window.confirm('Clear the saved profile and current edits from this browser?')) return;
    if (!removeProfile()) {
      setMessage('');
      setStorageError('The saved profile could not be cleared. Allow browser storage and try again.');
      return;
    }
    setDraft(emptyDraft());
    setSaved(null);
    onSaved(null);
    setTechnology('');
    setHasStoredData(false);
    setErrors({});
    setStorageError('');
    setMessage('Profile cleared from this browser.');
    document.getElementById('profile-role')?.focus();
  }

  function discard() {
    setDraft(saved ? toDraft(saved) : emptyDraft());
    setTechnology('');
    setErrors({});
    setMessage('Unsaved changes discarded.');
    document.getElementById('profile-role')?.focus();
  }

  function accessibility(field: keyof ProfileDraft, hint?: string) {
    return {
      'aria-invalid': Boolean(errors[field]),
      'aria-describedby': [hint, errors[field] ? 'profile-' + field + '-error' : ''].filter(Boolean).join(' ') || undefined,
    };
  }

  function fieldError(field: keyof ProfileDraft) {
    return errors[field]
      ? <p className="field-error" id={'profile-' + field + '-error'}>{errors[field]}</p>
      : null;
  }

  return (
    <section id="profile" className="profile-section" aria-labelledby="profile-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">01 / YOUR STARTING POINT</p>
          <h2 id="profile-title">Professional profile</h2>
        </div>
        <span className="planned-label">Saved on this device</span>
      </div>
      <p className="profile-intro" id="profile-storage-note">
        Set your direction and working preferences. Your profile stays in this browser;
        it is sent to the server for opportunity comparisons but is not stored there or synced between devices. Clearing browser data removes it.
      </p>
      <div className="profile-layout">
        <form className="profile-form" onSubmit={submit} noValidate aria-label="Professional profile" aria-describedby="profile-storage-note">
          <div className="form-heading"><h2>Your preferences</h2><button type="button" className="text-button" disabled={!dirty} onClick={discard}>Discard changes</button></div>
          <p className="form-note">All profile fields are required, including at least one technology. Changes are saved only when you select Save profile. Opportunity comparisons send the saved profile to the server without storing it there.</p>
          {storageError && <p className="notice notice-error" role="alert">{storageError}</p>}
          {Object.values(errors).some(Boolean) && <p className="notice notice-error" role="alert">Check the highlighted fields before saving.</p>}
          <fieldset>
            <legend>Your direction</legend>
            <div className="field-grid">
              <div className="field">
                <label htmlFor="profile-role">Target role</label>
                <select id="profile-role" value={draft.role} onChange={(event) => update('role', event.target.value)} required {...accessibility('role')}>
                  <option value="">Choose a role</option>
                  {Object.entries(ROLES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
                {fieldError('role')}
              </div>
              <div className="field">
                <label htmlFor="profile-experienceYears">Years of relevant experience</label>
                <input id="profile-experienceYears" type="number" min="0" max="60" step="1" inputMode="numeric" value={draft.experienceYears}
                  onChange={(event) => update('experienceYears', event.target.value)} required {...accessibility('experienceYears', 'experience-hint')} />
                <p id="experience-hint" className="field-hint">Whole years from 0 to 60. Starting out? Zero is valid.</p>
                {fieldError('experienceYears')}
              </div>
            </div>
          </fieldset>
          <fieldset>
            <legend>Technologies</legend>
            <label htmlFor="profile-technologies">Technology</label>
            <div className="technology-entry">
              <input ref={technologyInput} id="profile-technologies" type="text" maxLength={40} value={technology}
                placeholder="e.g. Java, React, SAP"
                onChange={(event) => {
                  setTechnology(event.target.value);
                  setErrors((current) => ({ ...current, technologies: undefined }));
                  setMessage('');
                }}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' && !event.nativeEvent.isComposing) {
                    event.preventDefault();
                    addTechnology();
                  }
                }}
                {...accessibility('technologies', 'technology-hint')} />
              <button className="secondary-button" type="button" onClick={addTechnology}>Add technology</button>
            </div>
            <p className="field-hint" id="technology-hint">Add 1–20 technologies, up to 40 characters each. Press Enter or select Add technology.</p>
            {fieldError('technologies')}
            {draft.technologies.length > 0 && (
              <ul className="technology-list" aria-label="Selected technologies">
                {draft.technologies.map((name, index) => (
                  <li key={name.toLowerCase()}>
                    <span>{name}</span>
                    <button type="button" aria-label={'Remove ' + name} onClick={() => deleteTechnology(index)}>×</button>
                  </li>
                ))}
              </ul>
            )}
          </fieldset>
          <fieldset>
            <legend>Location and working preferences</legend>
            <p className="field-hint">Use your place of residence. This does not establish eligibility to work in another country.</p>
            <div className="field-grid">
              <div className="field">
                <label htmlFor="profile-country">Country of residence</label>
                <input id="profile-country" autoComplete="country-name" maxLength={80} value={draft.country}
                  onChange={(event) => update('country', event.target.value)} required {...accessibility('country')} />
                {fieldError('country')}
              </div>
              <div className="field">
                <label htmlFor="profile-city">City of residence</label>
                <input id="profile-city" autoComplete="address-level2" maxLength={80} value={draft.city}
                  onChange={(event) => update('city', event.target.value)} required {...accessibility('city')} />
                {fieldError('city')}
              </div>
              <div className="field">
                <label htmlFor="profile-workMode">Preferred work mode</label>
                <select id="profile-workMode" value={draft.workMode} onChange={(event) => update('workMode', event.target.value)} required {...accessibility('workMode')}>
                  <option value="">Choose a work mode</option>
                  {Object.entries(WORK_MODES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
                {fieldError('workMode')}
              </div>
              {(draft.workMode === 'hybrid' || draft.workMode === 'onsite') && (
                <div className="field">
                  <label htmlFor="profile-maxOnsiteDays">Maximum on-site days per week</label>
                  <select id="profile-maxOnsiteDays" value={draft.maxOnsiteDays} onChange={(event) => update('maxOnsiteDays', event.target.value)} required {...accessibility('maxOnsiteDays')}>
                    <option value="">Choose a limit</option>
                    {[1, 2, 3, 4, 5].map((days) => <option key={days} value={days}>{days} {days === 1 ? 'day' : 'days'}</option>)}
                  </select>
                  {fieldError('maxOnsiteDays')}
                </div>
              )}
            </div>
          </fieldset>
          <p className="draft-state">{dirty ? 'You have unsaved changes.' : saved ? 'Your saved profile is up to date.' : 'Create your first profile.'}</p>
          <div className="profile-actions">
            <button type="submit" className="primary">Save profile</button>
            <button className="secondary-button" type="button" disabled={!hasStoredData && !dirty} onClick={clear}>Clear saved profile</button>
          </div>
          <p className="profile-message" role="status" aria-label="Profile updates" aria-live="polite" aria-atomic="true">{message}</p>
        </form>
        <aside className="profile-summary" aria-labelledby="profile-summary-title">
          <p className="eyebrow">YOUR PREFERENCES, AT A GLANCE</p>
          <h3 id="profile-summary-title">Saved profile</h3>
          {saved ? (
            <>
              <dl>
                <dt>Target role</dt><dd>{ROLES[saved.role]}</dd>
                <dt>Technologies</dt><dd><ul className="summary-technologies">{saved.technologies.map((name) => <li key={name.toLowerCase()}>{name}</li>)}</ul></dd>
                <dt>Relevant experience</dt><dd>{saved.experienceYears} {saved.experienceYears === 1 ? 'year' : 'years'}</dd>
                <dt>Residence</dt><dd>{saved.city}, {saved.country}</dd>
                <dt>Work mode</dt><dd>{WORK_MODES[saved.workMode]}</dd>
                <dt>Maximum on-site days</dt><dd>{saved.maxOnsiteDays === null ? 'None — remote only' : saved.maxOnsiteDays + ' per week'}</dd>
              </dl>
              <p className="field-hint">This summary shows your last successful save.</p>
            </>
          ) : <p className="summary-empty">No saved profile yet. Complete the form to bring your preferences together.</p>}
          <p className="summary-note">Your preferences stay in this browser and are used only after saving.</p>
          {saved && <a className="button primary" href={profileSearch(saved)}>Explore opportunities <span aria-hidden="true">↗</span></a>}
        </aside>
      </div>
    </section>
  );
}
