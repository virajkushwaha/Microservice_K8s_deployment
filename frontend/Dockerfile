# -------- Stage 1: Build --------
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build


# -------- Stage 2: Run --------
FROM node:20-alpine
WORKDIR /app

COPY --from=build /app/dist ./dist
COPY --from=build /app/package*.json ./

RUN npm ci --omit=dev

EXPOSE 4000
ENV PORT=4000

# ✅ ENV direct from K8s (NO .env)
CMD ["/bin/sh", "-c", "\
echo \"(function(window){window.__env=window.__env||{};window.__env.apiBaseUrl='${API_BASE_URL}';}(this));\" \
> /app/dist/Baratie/browser/assets/env.js && \
node dist/Baratie/server/server.mjs \
"]