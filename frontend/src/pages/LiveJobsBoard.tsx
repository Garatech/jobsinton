import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  fetchAtsFeed,
  fetchSmartRecruitersDetail,
  getCachedAtsFeed,
  type AtsFeedResponse,
  type AtsJob,
  type AtsProviderId,
} from '../api/ats';
import {
  loadCustomTags,
  parsePresetId,
  saveCustomTags,
  STACK_PRESETS,
  type StackPresetId,
} from '../ats/presets';
import {
  evaluateAtsJob,
  formatFreshness,
  sortScoredJobs,
  type ScoredAtsJob,
} from '../ats/scoring';
import { useLanguage } from '../i18n/Language';
import type { ProfessionalProfile } from '../profile/model';
import { EmptyState } from '../ui';

const PROVIDERS: readonly { id: AtsProviderId; label: string }[] = [
  { id: 'personio', label: 'Personio' },
  { id: 'teamtailor', label: 'Teamtailor' },
  { id: 'recruitee', label: 'Recruitee' },
  { id: 'factorial', label: 'Factorial HR' },
  { id: 'greenhouse', label: 'Greenhouse' },
  { id: 'lever', label: 'Lever' },
  { id: 'smartrecruiters', label: 'SmartRecruiters' },
  { id: 'workable', label: 'Workable' },
  { id: 'ashby', label: 'Ashby' },
];

const WORKPLACE_LABELS: Record<'remote' | 'hybrid' | 'onsite', string> = {
  remote: 'Remote',
  hybrid: 'Hybrid',
  onsite: 'On-site',
};

const PAGE_BATCH = 40;

type FeedState =
  | { kind: 'loading'; previous: AtsFeedResponse | null }
  | { kind: 'ready'; feed: AtsFeedResponse }
  | { kind: 'error'; message: string; previous: AtsFeedResponse | null };

interface LiveJobsBoardProps {
  query: string;
  profile: ProfessionalProfile | null;
}

export function LiveJobsBoard({ query, profile }: LiveJobsBoardProps) {
  const { t, language } = useLanguage();
  const initialParams = useMemo(() => new URLSearchParams(query), [query]);

  const [state, setState] = useState<FeedState>(() => {
    const cached = getCachedAtsFeed();
    return cached ? { kind: 'ready', feed: cached } : { kind: 'loading', previous: null };
  });

  const [preset, setPreset] = useState<StackPresetId>(() => parsePresetId(initialParams.get('preset')));
  const [customTags, setCustomTags] = useState<string[]>(() => loadCustomTags(profile));
  const [tagInput, setTagInput] = useState('');
  const [tagError, setTagError] = useState('');

  const [onlyMatches, setOnlyMatches] = useState<boolean>(() => initialParams.get('filter') !== 'all');
  const [onlyFresh, setOnlyFresh] = useState<boolean>(() => initialParams.get('filter') === 'fresh');
  const [onlySpainRemote, setOnlySpainRemote] = useState<boolean>(() => initialParams.get('filter') === 'spain');
  const [providerFilter, setProviderFilter] = useState<AtsProviderId | 'all'>('all');
  const [searchText, setSearchText] = useState('');
  const [sortMode, setSortMode] = useState<'relevance' | 'newest'>('relevance');
  const [visibleLimit, setVisibleLimit] = useState(PAGE_BATCH);
  const [showBoards, setShowBoards] = useState(false);

  const loadFeed = useCallback((refresh: boolean, signal?: AbortSignal) => {
    setState((current) => ({
      kind: 'loading',
      previous: current.kind === 'ready' ? current.feed : current.previous,
    }));
    fetchAtsFeed({ refresh, signal })
      .then((feed) => {
        if (signal?.aborted) return;
        setState({ kind: 'ready', feed });
        setVisibleLimit(PAGE_BATCH);
      })
      .catch((error: unknown) => {
        if (signal?.aborted) return;
        const message = error instanceof Error ? error.message : 'Could not load live ATS jobs.';
        setState((current) => ({
          kind: 'error',
          message,
          previous: current.kind === 'ready' ? current.feed : current.previous,
        }));
      });
  }, []);

  useEffect(() => {
    if (state.kind === 'ready') return;
    const controller = new AbortController();
    loadFeed(false, controller.signal);
    return () => controller.abort();
    // Intentionally run once on mount if client cache is empty.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const activeFeed = state.kind === 'ready' ? state.feed : state.previous;
  const allJobs = activeFeed?.jobs ?? [];

  const handleJobHydrated = useCallback((updatedJob: AtsJob) => {
    setState((current) => {
      if (current.kind !== 'ready') return current;
      return {
        kind: 'ready',
        feed: {
          ...current.feed,
          jobs: current.feed.jobs.map((job) => (job.id === updatedJob.id ? updatedJob : job)),
        },
      };
    });
  }, []);

  const presetCounts = useMemo(() => {
    const counts: Record<StackPresetId, number> = {
      'all-tech': 0,
      sap: 0,
      fullstack: 0,
      custom: 0,
    };
    for (const job of allJobs) {
      if (evaluateAtsJob(job, 'all-tech', customTags).isMatch) counts['all-tech']++;
      if (evaluateAtsJob(job, 'sap', customTags).isMatch) counts.sap++;
      if (evaluateAtsJob(job, 'fullstack', customTags).isMatch) counts.fullstack++;
      if (evaluateAtsJob(job, 'custom', customTags).isMatch) counts.custom++;
    }
    return counts;
  }, [allJobs, customTags]);

  const evaluatedAll = useMemo(
    () => allJobs.map((job) => evaluateAtsJob(job, preset, customTags)),
    [allJobs, preset, customTags],
  );

  const quickCounts = useMemo(() => {
    let matches = 0;
    let fresh = 0;
    let spainRemote = 0;
    const byProvider: Record<AtsProviderId, number> = {
      greenhouse: 0,
      lever: 0,
      smartrecruiters: 0,
      workable: 0,
      ashby: 0,
      personio: 0,
      recruitee: 0,
      teamtailor: 0,
      factorial: 0,
    };
    for (const item of evaluatedAll) {
      if (item.isMatch) matches++;
      if (item.isFresh48h) fresh++;
      if (item.isSpainOrRemote) spainRemote++;
      byProvider[item.job.provider]++;
    }
    return { matches, fresh, spainRemote, byProvider };
  }, [evaluatedAll]);

  const filteredJobs = useMemo(() => {
    const terms = searchText.trim().toLowerCase().split(/\s+/).filter(Boolean);
    const matching = evaluatedAll.filter((item) => {
      if (onlyMatches && !item.isMatch) return false;
      if (onlyFresh && !item.isFresh48h) return false;
      if (onlySpainRemote && !item.isSpainOrRemote) return false;
      if (providerFilter !== 'all' && item.job.provider !== providerFilter) return false;
      if (terms.length > 0) {
        const haystack = [
          item.job.title,
          item.job.companyName,
          item.job.location ?? '',
          item.job.department ?? '',
          item.job.providerLabel,
          item.job.description,
          ...item.matchedKeywords,
        ].join(' ').toLowerCase();
        if (!terms.every((term) => haystack.includes(term))) return false;
      }
      return true;
    });
    return sortScoredJobs(matching, sortMode);
  }, [evaluatedAll, onlyMatches, onlyFresh, onlySpainRemote, providerFilter, searchText, sortMode]);

  const showingAllFetched = !onlyMatches && !onlyFresh && !onlySpainRemote
    && providerFilter === 'all' && searchText.trim() === '';

  function selectPreset(next: StackPresetId) {
    setPreset(next);
    setOnlyMatches(true);
    setVisibleLimit(PAGE_BATCH);
  }

  function showAllFetched() {
    setOnlyMatches(false);
    setOnlyFresh(false);
    setOnlySpainRemote(false);
    setProviderFilter('all');
    setSearchText('');
    setVisibleLimit(PAGE_BATCH);
  }

  function addCustomTag(event: FormEvent) {
    event.preventDefault();
    const cleaned = tagInput.trim().replace(/\s+/g, ' ');
    if (!cleaned) {
      setTagError('Enter a technology to add.');
      return;
    }
    if (cleaned.length > 40) {
      setTagError('Use 40 characters or fewer per technology.');
      return;
    }
    if (customTags.some((tag) => tag.toLowerCase() === cleaned.toLowerCase())) {
      setTagError('This technology is already in your stack.');
      return;
    }
    if (customTags.length >= 20) {
      setTagError('Add up to 20 technologies.');
      return;
    }
    const next = [...customTags, cleaned];
    setCustomTags(next);
    saveCustomTags(next);
    setTagInput('');
    setTagError('');
  }

  function removeCustomTag(tag: string) {
    const next = customTags.filter((item) => item !== tag);
    setCustomTags(next);
    saveCustomTags(next);
  }

  function syncFromSavedProfile() {
    if (!profile || profile.technologies.length === 0) return;
    const next = [...profile.technologies];
    setCustomTags(next);
    saveCustomTags(next);
    setTagError('');
  }

  const activePresetMeta = STACK_PRESETS.find((item) => item.id === preset) ?? STACK_PRESETS[0];
  const displayedJobs = filteredJobs.slice(0, visibleLimit);
  const remainingCount = Math.max(0, filteredJobs.length - displayedJobs.length);

  return (
    <section className="ats-workspace" aria-label={t('Live ATS job boards')}>
      {/* Board health & cache status bar */}
      <div className="ats-status-bar">
        <div className="ats-status-summary" role="status" aria-live="polite">
          {state.kind === 'loading' && !activeFeed && (
            <>
              <span className="ats-dot loading" aria-hidden="true" />
              <span>{t('Fetching live postings from public company ATS boards…')}</span>
            </>
          )}
          {activeFeed && (
            <>
              <span
                className={'ats-dot ' + (activeFeed.succeededBoards > 0 ? 'ok' : 'warn')}
                aria-hidden="true"
              />
              <span>
                {activeFeed.succeededBoards} / {activeFeed.totalBoards} {t('company boards live')}
                {' · '}
                {activeFeed.jobs.length} {t(activeFeed.jobs.length === 1 ? 'job' : 'jobs')}
                {' · '}
                {formatCacheLabel(activeFeed, language)}
              </span>
            </>
          )}
          {state.kind === 'error' && !activeFeed && (
            <>
              <span className="ats-dot error" aria-hidden="true" />
              <span>{t(state.message)}</span>
            </>
          )}
        </div>

        <div className="ats-status-actions">
          {activeFeed && (
            <button
              type="button"
              className="text-button"
              aria-expanded={showBoards}
              onClick={() => setShowBoards((open) => !open)}
            >
              {t(showBoards ? 'Hide board status' : 'Board status')}
              {activeFeed.failedBoards > 0 ? ` (${activeFeed.failedBoards})` : ''}
            </button>
          )}
          <button
            type="button"
            className="secondary-button ats-refresh-btn"
            disabled={state.kind === 'loading'}
            onClick={() => loadFeed(true)}
          >
            {t(state.kind === 'loading' ? 'Refreshing…' : 'Force Refresh')}
          </button>
        </div>
      </div>

      {state.kind === 'error' && (
        <p className="notice notice-error ats-error-banner" role="alert">
          {t(state.message)}
        </p>
      )}

      {showBoards && activeFeed && (
        <div className="ats-boards-drawer" aria-label={t('Company board diagnostics')}>
          <ul className="ats-boards-grid">
            {activeFeed.boards.map((board) => (
              <li key={`${board.provider}:${board.companySlug}`} className={board.ok ? 'board-ok' : 'board-fail'}>
                <div className="board-row-top">
                  <strong>{board.companyName}</strong>
                  <span className={'ats-provider-badge ats-' + board.provider}>{board.providerLabel}</span>
                </div>
                <p className="board-row-meta">
                  {board.ok
                    ? `${board.jobCount} ${t(board.jobCount === 1 ? 'job' : 'jobs')} · ${board.durationMs}ms`
                    : `${t('Unavailable')}: ${board.error ?? t('Request failed')}`}
                </p>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Developer Stack Presets */}
      <div className="ats-presets-panel">
        <div className="ats-presets-head">
          <p className="eyebrow">{t('DEVELOPER STACK PRESETS')}</p>
          <p className="ats-preset-desc">{t(activePresetMeta.description)}</p>
        </div>
        <div className="ats-preset-pills" role="group" aria-label={t('Developer stack presets')}>
          {STACK_PRESETS.map((item) => {
            const active = preset === item.id;
            return (
              <button
                key={item.id}
                type="button"
                className={'ats-preset-pill' + (active ? ' active' : '')}
                aria-pressed={active}
                onClick={() => selectPreset(item.id)}
              >
                <span>{t(item.label)}</span>
                {activeFeed && <span className="pill-count">{presetCounts[item.id]}</span>}
              </button>
            );
          })}
        </div>

        {(preset === 'sap' || preset === 'fullstack') && (
          <ul className="ats-active-keywords" aria-label={t('Active preset keywords')}>
            {activePresetMeta.keywords.map((kw) => (
              <li key={kw}>{kw}</li>
            ))}
          </ul>
        )}

        {preset === 'custom' && (
          <div className="ats-custom-editor">
            <form className="ats-custom-form" onSubmit={addCustomTag}>
              <label className="sr-only" htmlFor="ats-custom-tag-input">{t('Add stack keyword')}</label>
              <input
                id="ats-custom-tag-input"
                type="text"
                maxLength={40}
                placeholder={t('Add keyword (e.g. Kotlin, Vue, Kubernetes)')}
                value={tagInput}
                onChange={(event) => {
                  setTagInput(event.target.value);
                  if (tagError) setTagError('');
                }}
              />
              <button type="submit" className="secondary-button">{t('Add tag')}</button>
              {profile && profile.technologies.length > 0 && (
                <button type="button" className="text-button" onClick={syncFromSavedProfile}>
                  {t('Sync from saved profile')}
                </button>
              )}
            </form>
            {tagError && <p className="field-error" role="alert">{t(tagError)}</p>}
            <ul className="ats-custom-tags" aria-label={t('Custom stack keywords')}>
              {customTags.map((tag) => (
                <li key={tag}>
                  <span>{tag}</span>
                  <button
                    type="button"
                    aria-label={`${t('Remove')} ${tag}`}
                    onClick={() => removeCustomTag(tag)}
                  >
                    ×
                  </button>
                </li>
              ))}
              {customTags.length === 0 && (
                <li className="ats-custom-empty">{t('Add at least one keyword to match live jobs.')}</li>
              )}
            </ul>
          </div>
        )}
      </div>

      {/* Smart One-Click Quick Filters */}
      <div className="ats-quick-filters" role="group" aria-label={t('Quick filters')}>
        <button
          type="button"
          className={'ats-toggle-pill' + (onlyMatches ? ' active' : '')}
          aria-pressed={onlyMatches}
          onClick={() => { setOnlyMatches((v) => !v); setVisibleLimit(PAGE_BATCH); }}
        >
          <span>{t('My Profile Matches')}</span>
          {activeFeed && <span className="pill-count">{quickCounts.matches}</span>}
        </button>

        <button
          type="button"
          className={'ats-toggle-pill fresh-pill' + (onlyFresh ? ' active' : '')}
          aria-pressed={onlyFresh}
          onClick={() => { setOnlyFresh((v) => !v); setVisibleLimit(PAGE_BATCH); }}
        >
          <span>{t('Fresh (< 48h)')}</span>
          {activeFeed && <span className="pill-count">{quickCounts.fresh}</span>}
        </button>

        <button
          type="button"
          className={'ats-toggle-pill' + (onlySpainRemote ? ' active' : '')}
          aria-pressed={onlySpainRemote}
          onClick={() => { setOnlySpainRemote((v) => !v); setVisibleLimit(PAGE_BATCH); }}
        >
          <span>{t('Spain / Madrid / Remote')}</span>
          {activeFeed && <span className="pill-count">{quickCounts.spainRemote}</span>}
        </button>

        <button
          type="button"
          className={'ats-toggle-pill' + (showingAllFetched ? ' active' : '')}
          aria-pressed={showingAllFetched}
          onClick={showAllFetched}
        >
          <span>{t('All Fetched Jobs')}</span>
          {activeFeed && <span className="pill-count">{allJobs.length}</span>}
        </button>
      </div>

      {/* Search + Sort + ATS Provider Filter */}
      <div className="ats-controls-row">
        <div className="ats-search-input">
          <label className="sr-only" htmlFor="ats-search-box">{t('Filter live jobs')}</label>
          <span className="search-icon" aria-hidden="true">⌕</span>
          <input
            id="ats-search-box"
            type="search"
            maxLength={120}
            placeholder={t('Filter by title, company, stack or city…')}
            value={searchText}
            onChange={(event) => { setSearchText(event.target.value); setVisibleLimit(PAGE_BATCH); }}
          />
          {searchText && (
            <button
              type="button"
              className="text-button"
              onClick={() => setSearchText('')}
            >
              {t('Clear')}
            </button>
          )}
        </div>

        <div className="ats-sort-control">
          <label className="sort-label" htmlFor="ats-sort">{t('Sort by')}</label>
          <select
            id="ats-sort"
            value={sortMode}
            onChange={(event) => setSortMode(event.target.value as 'relevance' | 'newest')}
          >
            <option value="relevance">{t('Match & freshness')}</option>
            <option value="newest">{t('Newest first')}</option>
          </select>
        </div>
      </div>

      <div className="ats-provider-bar" role="group" aria-label={t('Filter by ATS provider')}>
        <button
          type="button"
          className={'ats-provider-filter' + (providerFilter === 'all' ? ' active' : '')}
          aria-pressed={providerFilter === 'all'}
          onClick={() => { setProviderFilter('all'); setVisibleLimit(PAGE_BATCH); }}
        >
          {t('All ATS')}
        </button>
        {PROVIDERS.map((provider) => (
          <button
            key={provider.id}
            type="button"
            className={'ats-provider-filter' + (providerFilter === provider.id ? ' active' : '')}
            aria-pressed={providerFilter === provider.id}
            onClick={() => {
              setProviderFilter((current) => (current === provider.id ? 'all' : provider.id));
              setVisibleLimit(PAGE_BATCH);
            }}
          >
            <span>{provider.label}</span>
            {activeFeed && <span className="provider-mini-count">{quickCounts.byProvider[provider.id]}</span>}
          </button>
        ))}
      </div>

      {/* Results count */}
      {activeFeed && (
        <div className="ats-results-meta">
          <p role="status">
            {filteredJobs.length} {t(filteredJobs.length === 1 ? 'live opportunity' : 'live opportunities')}
            {filteredJobs.length !== allJobs.length && (
              <span className="muted"> ({t('of')} {allJobs.length} {t('fetched')})</span>
            )}
          </p>
        </div>
      )}

      {/* Initial loading skeleton */}
      {state.kind === 'loading' && !activeFeed && (
        <div className="ats-skeletons" aria-hidden="true">
          <div className="ats-skeleton-card" />
          <div className="ats-skeleton-card" />
          <div className="ats-skeleton-card" />
        </div>
      )}

      {/* Job Feed Cards */}
      {activeFeed && displayedJobs.length > 0 && (
        <div className="jobs ats-job-list">
          {displayedJobs.map((scored) => (
            <LiveJobCard
              key={scored.job.id}
              scored={scored}
              onJobHydrated={handleJobHydrated}
            />
          ))}
        </div>
      )}

      {remainingCount > 0 && (
        <div className="ats-load-more">
          <button
            type="button"
            className="secondary-button"
            onClick={() => setVisibleLimit((current) => current + PAGE_BATCH)}
          >
            {t('Show more opportunities')} ({remainingCount} {t('remaining')})
          </button>
        </div>
      )}

      {/* Helpful empty-filter recovery state */}
      {activeFeed && displayedJobs.length === 0 && (
        <EmptyState
          title="No live jobs match the current filters."
          description={allJobs.length > 0
            ? 'Broaden your filters or switch presets to explore all fetched company openings.'
            : 'No jobs were returned from the configured boards right now. Try refreshing.'}
        >
          <div className="ats-empty-actions">
            {allJobs.length > 0 && !showingAllFetched && (
              <button type="button" className="primary" onClick={showAllFetched}>
                {t('Show All Fetched Jobs')} ({allJobs.length})
              </button>
            )}
            {preset !== 'all-tech' && (
              <button type="button" className="secondary-button" onClick={() => selectPreset('all-tech')}>
                {t('Switch to All Tech Roles')}
              </button>
            )}
            {allJobs.length === 0 && (
              <button type="button" className="primary" onClick={() => loadFeed(true)}>
                {t('Retry live fetch')}
              </button>
            )}
          </div>
        </EmptyState>
      )}
    </section>
  );
}

interface LiveJobCardProps {
  scored: ScoredAtsJob;
  onJobHydrated: (job: AtsJob) => void;
}

function LiveJobCard({ scored, onJobHydrated }: LiveJobCardProps) {
  const { t, language } = useLanguage();
  const { job, matchedKeywords } = scored;
  const [expanded, setExpanded] = useState(false);
  const [hydrating, setHydrating] = useState(false);
  const [hydrateError, setHydrateError] = useState('');

  const freshness = formatFreshness(job.hoursSincePublished, job.publishedAt, language);
  const canHydrate = job.provider === 'smartrecruiters' && !job.hasFullDescription;

  async function toggleDescription() {
    if (expanded) {
      setExpanded(false);
      return;
    }
    setExpanded(true);
    if (!canHydrate || hydrating) return;
    setHydrating(true);
    setHydrateError('');
    try {
      const detail = await fetchSmartRecruitersDetail(job.companySlug, job.sourceId);
      onJobHydrated({
        ...job,
        description: detail.description,
        descriptionSnippet: detail.descriptionSnippet || job.descriptionSnippet,
        applyUrl: detail.applyUrl || job.applyUrl,
        hasFullDescription: detail.hasFullDescription,
      });
    } catch (error) {
      setHydrateError(error instanceof Error ? error.message : 'Could not load full description.');
    } finally {
      setHydrating(false);
    }
  }

  return (
    <article className={'job ats-card' + (scored.isFresh24h ? ' ats-card-hot' : '')} data-ats-job={job.id}>
      <div className="ats-card-body">
        <div className="ats-card-header">
          <span className="ats-company">{job.companyName}</span>
          <span className={'ats-provider-badge ats-' + job.provider}>{job.providerLabel}</span>
          {freshness && (
            <span className={'ats-freshness ats-freshness-' + freshness.tier}>
              {freshness.text}
            </span>
          )}
        </div>

        <h3 className="ats-card-title">
          <a href={job.applyUrl} target="_blank" rel="noopener noreferrer">
            {job.title}
          </a>
        </h3>

        <p className="job-meta ats-card-meta">
          <span>{job.location ?? t('Location not specified')}</span>
          {job.workplaceType && (
            <>
              <span aria-hidden="true"> · </span>
              <span className="ats-workplace-pill">{t(WORKPLACE_LABELS[job.workplaceType])}</span>
            </>
          )}
          {job.department && (
            <>
              <span aria-hidden="true"> · </span>
              <span>{job.department}</span>
            </>
          )}
          {job.compensation && (
            <>
              <span aria-hidden="true"> · </span>
              <strong className="ats-comp">{job.compensation}</strong>
            </>
          )}
        </p>

        {matchedKeywords.length > 0 && (
          <ul className="tags ats-matched-tags" aria-label={t('Matched stack keywords')}>
            {matchedKeywords.map((keyword) => (
              <li key={keyword} className="ats-match-chip">{keyword}</li>
            ))}
          </ul>
        )}

        {!expanded && job.descriptionSnippet && (
          <p className="ats-snippet">{job.descriptionSnippet}</p>
        )}

        {(job.description || canHydrate) && (
          <div className="ats-desc-control">
            <button
              type="button"
              className="text-button ats-desc-toggle"
              aria-expanded={expanded}
              onClick={toggleDescription}
            >
              {expanded
                ? t('Hide description')
                : canHydrate
                  ? t('Load full description')
                  : t('Read full description')}
            </button>
          </div>
        )}

        {expanded && (
          <div className="ats-full-desc">
            {hydrating && <p role="status" className="muted">{t('Loading full description from SmartRecruiters…')}</p>}
            {hydrateError && <p role="alert" className="field-error">{t(hydrateError)}</p>}
            {!hydrating && job.description && (
              <p className="ats-desc-text">{job.description}</p>
            )}
            {!hydrating && !hydrateError && !job.description && (
              <p className="muted">{t('No detailed description was provided by this ATS board.')}</p>
            )}
          </div>
        )}
      </div>

      <div className="ats-card-actions">
        <a
          className="button primary ats-apply-btn"
          href={job.applyUrl}
          target="_blank"
          rel="noopener noreferrer"
        >
          {t('Apply on Company Site')} <span aria-hidden="true">↗</span>
        </a>
      </div>
    </article>
  );
}

function formatCacheLabel(feed: AtsFeedResponse, language: 'en' | 'es'): string {
  if (!feed.cached || feed.cacheAgeSeconds < 30) {
    return language === 'es' ? 'Actualizado ahora' : 'Updated just now';
  }
  const minutes = Math.max(1, Math.round(feed.cacheAgeSeconds / 60));
  return language === 'es'
    ? `En caché hace ${minutes} min`
    : `Cached ${minutes}m ago`;
}
