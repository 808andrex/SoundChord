/** The web player's production origin on Vercel. */
export const PRODUCTION_ORIGIN = 'https://soundchord.vercel.app';

/** Vercel deploy address. */
const VERCEL_ORIGIN = 'https://soundchord.vercel.app';

/** The Vite dev servers (web/app/vite.config.ts pins the port). */
const DEV_ORIGIN = 'http://localhost:5173';
const DEV_ORIGIN_LOCAL = 'http://127.0.0.1:5173';

/**
 * The web app origins this extension serves. Every message is checked against
 * this list, and it becomes externally_connectable (Chromium), the relay's
 * content-script matches (Firefox) and the DNR rules' initiatorDomains.
 *
 * soundchord.vercel.app always, and localhost / 127.0.0.1 dev servers except in a store build
 * (SOUNDCHORD_STORE_BUILD=1), so a published extension never answers a page on
 * localhost. SOUNDCHORD_SITE_ORIGINS (comma-separated) replaces the list. The
 * first origin is the one the popup and homepage_url link to.
 */
export const SITE_ORIGINS: string[] = [
  ...new Set(
    (
      process.env.SOUNDCHORD_SITE_ORIGINS ??
      process.env.BITCHORD_SITE_ORIGINS ??
      [
        PRODUCTION_ORIGIN,
        VERCEL_ORIGIN,
        ...(process.env.SOUNDCHORD_STORE_BUILD === '1' || process.env.BITCHORD_STORE_BUILD === '1'
          ? []
          : [DEV_ORIGIN, DEV_ORIGIN_LOCAL]),
      ].join(',')
    )
      .split(',')
      .map((o) => o.trim().replace(/\/+$/, ''))
      .filter(Boolean),
  ),
];

/**
 * Match patterns can't carry a port, so `http://localhost:5173` becomes
 * `http://localhost/*`. The exact origin, port included, is still enforced at
 * runtime against SITE_ORIGINS.
 */
export function matchPatternsFor(origins: string[]): string[] {
  return [...new Set(origins.map((o) => {
    const url = new URL(o);
    return `${url.protocol}//${url.hostname}/*`;
  }))];
}
