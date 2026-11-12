// Where the browser application finds the API: the only place the address is set.
//
// The API must be on the same site as the application, because the session cookie is SameSite=Lax (ADR-0002),
// so the default is the relative path "/api". VITE_API_BASE can change it at build time (for example a path
// prefix behind a proxy); the address in openapi.yaml (localhost:8080) is only an example.
export const API_BASE: string = import.meta.env.VITE_API_BASE ?? "/api";
