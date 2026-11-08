import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Em desenvolvimento, repassa /api para o back-end Java (evita configurar CORS)
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
