# ---------- Etapa 1: build ----------
FROM node:22-alpine AS build

WORKDIR /app

# Copia primero los manifiestos para aprovechar la cache de capas de Docker
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# ---------- Etapa 2: runtime ----------
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -qO- http://localhost/healthz >/dev/null || exit 1
