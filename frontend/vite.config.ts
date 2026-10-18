import { defineConfig } from 'vite';

// Relative API requests also work through private Codespaces port forwarding.
// This proxy is for development/preview; production routing is a later decision.
const proxy = { '/api': { target: 'http://127.0.0.1:8080' } };
const codespace = process.env.CODESPACE_NAME;
const forwardingDomain = process.env.GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN;

/** Allow only this Codespace's hostname, without disabling Vite's host checks. */
function allowedHosts(port: number): string[] {
  return codespace && forwardingDomain
    ? [`${codespace}-${port}.${forwardingDomain}`]
    : [];
}

export default defineConfig({
  server: {
    host: '0.0.0.0', port: 5173, strictPort: true,
    allowedHosts: allowedHosts(5173), proxy,
  },
  preview: {
    host: '0.0.0.0', port: 4173, strictPort: true,
    allowedHosts: allowedHosts(4173), proxy,
  },
});
