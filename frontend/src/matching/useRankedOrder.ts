import { useLayoutEffect, useState } from 'react';
import { fetchRankedOrder, type RankedOrder } from '../api/ranking';
import type { ProfessionalProfile } from '../profile/model';

interface Completed { profile: ProfessionalProfile; idsKey: string; order: RankedOrder }

/** Only committed changes to the saved profile and candidate set start a new batch. */
export function useRankedOrder(profile: ProfessionalProfile | null, idsKey: string | null, onFailure: () => void) {
  const [completed, setCompleted] = useState<Completed | null>(null);
  useLayoutEffect(() => {
    setCompleted(null);
    if (!profile || !idsKey) return;
    const controller = new AbortController();
    let active = true;
    let deadline: number | undefined;
    const fail = () => {
      if (!active) return;
      active = false;
      controller.abort();
      onFailure();
    };
    // Coalesce quick input changes. Layout cleanup invalidates the previous request before paint.
    const start = window.setTimeout(() => {
      deadline = window.setTimeout(fail, 8000);
      fetchRankedOrder(profile, JSON.parse(idsKey) as string[], controller.signal)
        .then(order => { if (active) setCompleted({ profile, idsKey, order }); })
        .catch(fail).finally(() => window.clearTimeout(deadline));
    }, 150);
    return () => {
      active = false;
      controller.abort();
      window.clearTimeout(start);
      window.clearTimeout(deadline);
    };
  }, [profile, idsKey, onFailure]);
  return completed && completed.idsKey === idsKey && completed.profile === profile ? completed.order : null;
}
