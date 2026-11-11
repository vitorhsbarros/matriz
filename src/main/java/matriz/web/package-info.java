/**
 * Web entry (shared, not a business module): routing, session and CSRF check, errors as Problem Details
 * (04-solution-architecture.md, section 3.3).
 *
 * <p>Modules get the current user's identifier from the request's {@code Authentication}:
 * {@code UUID.fromString(authentication.getName())}. It comes from the session, never from the request body
 * or URL (NFR03). Modules refuse a request by throwing Spring's {@code ErrorResponseException}, so no module
 * depends on this one.
 */
package matriz.web;
