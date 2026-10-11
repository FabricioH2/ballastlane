# ---------- Stage 1: build the static files (HTML, JS, CSS) with Node ----------
FROM node:24-slim AS build
WORKDIR /app

# First only the dependency list: this layer is reused while package*.json do not change.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# tsc checks the types, then Vite writes the final files to /app/dist
RUN npm run build

# ---------- Stage 2: serve them with nginx (no Node needed at runtime) ----------
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
