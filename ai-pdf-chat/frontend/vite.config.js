import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The React app runs on port 5173.
// Every request that starts with /api is forwarded to Spring Boot on port 8080,
// so in the code we can simply write "/api/...".
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
