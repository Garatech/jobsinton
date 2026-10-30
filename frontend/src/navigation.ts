import { useEffect, useState } from 'react';
export type Route = { page: string; id: string; query: string; hash: string; revision: number };
export function readRoute(): Route {
  const hash = window.location.hash.slice(1);
  const [pathname, query = ''] = hash.split('?');
  const parts = pathname.split('/');
  const [page = '', id = ''] = parts;
  return { page: parts.length > 2 ? 'not-found' : !page || page === 'main' ? 'home' : page, id, query, hash, revision: 0 };
}
export function useRoute() {
  const [route, setRoute] = useState(readRoute);
  useEffect(() => {
    const change = () => setRoute(current => ({ ...readRoute(), revision: current.revision + 1 }));
    window.addEventListener('hashchange', change);
    return () => window.removeEventListener('hashchange', change);
  }, []);
  return route;
}
