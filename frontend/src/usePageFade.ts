import { useLayoutEffect, useRef, type RefObject } from 'react';

/** Animate the incoming page without remounting drafts or delaying navigation. */
export function usePageFade(target: RefObject<HTMLElement | null>, pageKey: string) {
  const previousPage = useRef(pageKey);

  useLayoutEffect(() => {
    const changed = previousPage.current !== pageKey;
    previousPage.current = pageKey;
    const element = target.current;
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
    // Initial loads and in-page updates are immediate; older browsers keep normal navigation.
    if (!changed || !element || preference.matches || typeof element.animate !== 'function') return;

    const animation = element.animate(
      [{ opacity: 0 }, { opacity: 1 }],
      { duration: 200, easing: 'ease-out' },
    );
    const reduceMotion = () => {
      if (preference.matches) animation.cancel();
    };
    preference.addEventListener('change', reduceMotion);
    return () => {
      preference.removeEventListener('change', reduceMotion);
      // A newer page replaces the current transition; opacity returns to its ordinary style.
      animation.cancel();
    };
  }, [target, pageKey]);
}
