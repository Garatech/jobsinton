import { useEffect, useId, useRef, useState } from 'react';
import { compareAdzuna, ExpiredListingReference } from '../api/adzuna';
import { ComparisonError, type Comparison } from '../api/matching';
import { useLanguage } from '../i18n/Language';
import type { ProfessionalProfile } from '../profile/model';
import { ComparisonResult } from './ComparisonResult';

type State = { kind: 'idle' | 'loading' } | { kind: 'ready'; result: Comparison }
  | { kind: 'error'; message: string; canRetry: boolean };

export function LiveComparison({ id, reference, profile }: {
  id: string; reference: string; profile: ProfessionalProfile | null;
}) {
  const { t } = useLanguage();
  const panelId = useId();
  const active = useRef<AbortController | null>(null);
  const [open, setOpen] = useState(false);
  const [state, setState] = useState<State>({ kind: 'idle' });
  useEffect(() => () => { active.current?.abort(); active.current = null; }, []);

  async function compare() {
    if (!profile || active.current) return;
    const controller = new AbortController();
    active.current = controller;
    setState({ kind: 'loading' });
    const timer = window.setTimeout(() => controller.abort(), 8000);
    try {
      const result = await compareAdzuna(id, reference, profile, controller.signal);
      if (active.current === controller) setState({ kind: 'ready', result });
    } catch (error) {
      if (active.current === controller) setState({
        kind: 'error', canRetry: !(error instanceof ExpiredListingReference),
        message: controller.signal.aborted ? 'The comparison took too long. Please try again.'
          : error instanceof ComparisonError ? error.message : 'The comparison is unavailable. Please try again.',
      });
    } finally {
      window.clearTimeout(timer);
      if (active.current === controller) active.current = null;
    }
  }

  if (!profile) return <a className="text-link" href="#profile">{t('Set up a profile to compare')}</a>;
  function toggle() {
    setOpen(!open);
    if (!open && state.kind === 'idle') void compare();
  }
  return <div className="live-comparison">
    <button type="button" className="text-button" aria-expanded={open} aria-controls={panelId} onClick={toggle}>
      {t(open ? 'Hide comparison' : 'Compare with my profile')}
    </button>
    <div id={panelId} hidden={!open}>
      {state.kind === 'loading' && <p role="status">{t('Comparing your saved profile…')}</p>}
      {state.kind === 'error' && <><p role="alert">{t(state.message)}</p>
        {state.canRetry && <button type="button" className="secondary-button" onClick={() => void compare()}>{t('Retry comparison')}</button>}
      </>}
      {state.kind === 'ready' && <ComparisonResult result={state.result} />}
    </div>
  </div>;
}
