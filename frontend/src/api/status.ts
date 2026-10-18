/** The only API contract implemented in the foundation increment. */
export interface SystemStatus {
  name: 'JobSinton';
  status: 'UP';
}

/**
 * Fetches the live service status. Validate untrusted JSON so a proxy error or
 * incompatible API cannot accidentally be displayed as a successful connection.
 * The caller owns cancellation and timeout behavior.
 */
export async function fetchSystemStatus(signal: AbortSignal): Promise<SystemStatus> {
  const response = await fetch('/api/status', {
    signal,
    headers: { Accept: 'application/json' },
    cache: 'no-store',
  });
  if (!response.ok) throw new Error('The service status request failed.');

  const payload: unknown = await response.json();
  if (
    typeof payload !== 'object' || payload === null ||
    !('name' in payload) || payload.name !== 'JobSinton' ||
    !('status' in payload) || payload.status !== 'UP'
  ) {
    throw new Error('The service returned an unexpected status response.');
  }
  return { name: 'JobSinton', status: 'UP' };
}
