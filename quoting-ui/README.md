# Pinta Quoting UI

Vue + Vite UI for the Quoting API in the parent project.

## Prerequisites

- Node.js 20+
- The backend API running from the parent directory (`quoting`)

## Run UI

```bash
npm install
npm run dev
```

The Vite app runs on `http://localhost:5173`.

## API wiring

By default, frontend calls use `/api` and Vite proxies to `http://localhost:8080`.

Set environment values if needed:

```bash
# direct base path used by fetch
VITE_API_BASE=/api

# backend target for Vite dev proxy
VITE_PROXY_TARGET=http://localhost:8080
```

## Features

- Battery catalog with make/model filter
- Quote header creation
- Quote header listing and selection
- Quote detail creation for selected quote
- Quote detail listing with total selected quote value
