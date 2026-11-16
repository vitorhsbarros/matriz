# Matriz — Deployment options (PD13)

**Status: pending.** This compares the options for the pending decision PD13, the deployment platform, so the developer can choose. Nothing here is decided. The options come from [implementation-plan.md, section 1.3](implementation-plan.md#13-pending-technical-decisions), here worked out against what Matriz now has (release `v1.1.1`).

## What any platform must provide

From [architecture.md, section 5](architecture.md#5-technology-stack) ("Deployment"), the delivery plan ([delivery-plan.md](delivery-plan.md), section 8) and what the image needs:

| Need | Why | What Matriz already gives |
|---|---|---|
| HTTPS | The session cookie is `Secure` (SEC08, ADR-0002) | The application sends relative `Location` headers and no redirects, so a proxy that ends TLS in front of it should need no extra setting. To confirm on the chosen platform |
| The browser application and the API on the same site | `SameSite=Lax` (ADR-0002) | Both are served by the one container |
| A PostgreSQL 17 that keeps its data, with backups | One database for the whole application (ADR-0001) | Flyway creates or updates the schema at start-up |
| Secrets outside the repository | SEC10, AGENTS.md §7 | Every setting is an environment variable (delivery-plan.md, section 3) |
| Running a container image for `linux/amd64` | The delivery | `ghcr.io/vitorhsbarros/matriz:<version>`, public, with a provenance attestation |
| Health checks | Restart a broken process, hold traffic until it is ready | `/actuator/health/liveness` and `/actuator/health/readiness` (readiness includes the database) |
| Reading the logs | Operation | JSON lines (ECS) on standard output |

The size is small: about 50 users (ASM08–ASM10), one developer.

## Options

| | (a) A platform that runs containers, with managed PostgreSQL | (b) A virtual machine with Docker Compose | (c) A university or company server |
|---|---|---|---|
| **What it is** | A hosting platform that pulls the image and runs it, and offers PostgreSQL as a service. Examples: Render, Railway, Fly.io | A rented Linux machine running the repository's `compose.yaml`, with a reverse proxy for HTTPS | A machine someone else runs, if one is available |
| **HTTPS** | Included, with its own domain name | The developer's job: a reverse proxy (e.g. Caddy or Traefik) with a certificate | Depends on the server |
| **Database and backups** | Managed by the platform, backups depending on the plan | The developer's job: the `matriz-postgres` volume, and a backup script with somewhere to keep the copies | Depends on the server |
| **Secrets** | The platform's environment variables or secret store | A `.env` on the machine, readable by its administrator only | Depends on the server |
| **Health checks** | Most can probe an HTTP path: readiness and liveness can be used | Docker restarts the containers (`restart: unless-stopped`); the image's health check marks them unhealthy | Depends on the server |
| **Changes needed in Matriz** | Probably none: the image and the variables are ready. Possibly the platform's port variable, mapped to `SERVER_PORT` | None to the application. A proxy service and a backup job to add | Unknown |
| **Effort** | Least set-up | Most: updates of the machine, the proxy, the backups and monitoring are the developer's | Depends on who runs it |
| **Cost and limits** | Free tiers exist on some platforms, but often put the application to sleep, or delete a free database after a while. **To check on each platform's current terms before choosing** | A small machine's monthly price, paid for whether used or not | Usually free, under someone else's rules |
| **Risk** | Depending on one provider's terms; moving is easy, because the image runs anywhere | The developer being away when something breaks | Availability, and access from outside the network |

Kubernetes or a managed container service of a large cloud provider is left out: it would add far more to run than 50 users need.

## Recommendation

**(a)**, if the developer accepts the provider's terms. The image, the configuration by environment variables, the health checks and the JSON logs already fit how such platforms work, so the deployment could be done without changing the application. The points to check on the platform chosen are the cost and limits of the free or paid tier, the backups of its PostgreSQL, and whether its proxy needs `server.forward-headers-strategy`.

**(b)** is the fallback if no platform's terms suit, or if full control matters more than effort.

## Once chosen

1. Record the decision as PD13 in [constraints.md, section 4](constraints.md#4-pending-decisions) and [implementation-plan.md, section 1.3](implementation-plan.md#13-pending-technical-decisions).
2. Plan the deployment as its own task: the platform's set-up, the secrets, a deployment job in CI (CD) if wanted, the backups, and a check over HTTPS with `scripts/image-smoke-test.sh`'s application checks.
