import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // During development the browser calls http://localhost:5173/api/...
    // and Vite forwards it to the Spring Boot backend. Same origin for the browser = no CORS setup needed.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
