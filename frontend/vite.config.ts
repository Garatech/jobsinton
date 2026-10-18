import { defineConfig } from 'vite';

// Relative API requests also work through private Codespaces port forwarding.
// This proxy is for development/preview; production routing is a later decision.
const proxy = { '/api': { target: 'http://127.0.0.1:8080' } };

export default defineConfig({
  server: { host: '0.0.0.0', port: 5173, strictPort: true, proxy },
  preview: { host: '0.0.0.0', port: 4173, strictPort: true, proxy },
});
