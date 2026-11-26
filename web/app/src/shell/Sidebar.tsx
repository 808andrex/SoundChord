import { ArrowUpRight, ListMusic, Settings } from 'lucide-react';
import { MdOutlineExtension } from 'react-icons/md';
import { PrefetchLink, PrefetchNavLink } from '@/components/PrefetchLink';
import { LogoMark } from '@/components/icons/LogoMark';
import {
  AlbumsIcon,
  AllPlaylistsIcon,
  ArtistsIcon,
  HomeIcon,
  MadeForYouIcon,
  NewIcon,
  RecentlyAddedIcon,
  SearchIcon,
  SongsIcon,
  type NavIconProps,
} from '@/components/icons/NavIcons';
import { hrefFor } from '@/components/music/links';
import { config } from '@/config';
import { LIBRARY_SECTION_KEYS, type LibrarySection } from '@/data/music/library';
import { useLibraryPlaylists, useSignedIn } from '@/data/music/queries';
import { useSpotifyPlaylists } from '@/data/spotify/queries';
import { useExtensionUpdate } from '@/extension/useExtensionUpdate';
import { useSpotifySignedIn } from '@/extension/useSpotifyAuth';
import { useTranslation, type TranslationKey } from '@/i18n';
import { useUi } from '@/state/ui';
import { AccountButton } from './AccountButton';

interface NavItem {
  to: string;
  label: string;
  Icon: (props: NavIconProps) => React.ReactNode;
}

const PRIMARY_KEYS: { to: string; key: TranslationKey; Icon: NavItem['Icon'] }[] = [
  { to: '/search', key: 'nav.search', Icon: SearchIcon },
  { to: '/home', key: 'nav.home', Icon: HomeIcon },
  { to: '/new', key: 'nav.new', Icon: NewIcon },
];

const LIBRARY_ICONS: Record<Exclude<LibrarySection, 'playlists'>, NavItem['Icon']> = {
  recent: RecentlyAddedIcon,
  artists: ArtistsIcon,
  albums: AlbumsIcon,
  songs: SongsIcon,
  'made-for-you': MadeForYouIcon,
};

const LIBRARY_KEYS: { to: string; key: TranslationKey; Icon: NavItem['Icon'] }[] = (
  Object.keys(LIBRARY_ICONS) as (keyof typeof LIBRARY_ICONS)[]
).map((section) => ({
  to: `/library/${section}`,
  key: LIBRARY_SECTION_KEYS[section],
  Icon: LIBRARY_ICONS[section],
}));

// A sidebar row; NavLink sets aria-current on the page's own row.
const item =
  'group flex h-9 items-center gap-2 mb-0.5 p-1 rounded-nav type-body-reduced text-primary hover:bg-quinary aria-[current=page]:bg-nav-selected';
// Apple's glyphs, drawn at 24px (their 24-unit box), which the user preferred to Apple's own 28px.
const icon = 'inline-grid size-6 flex-none place-items-center text-primary group-aria-[current=page]:text-key';
const navLabel = 'min-w-0 truncate';
// "Library", "Playlists" (report §1: 12px/15px 600, secondary, padding 4px 8px).
const sectionHeader = 'mt-4 px-2 py-1 type-callout-emphasized text-secondary';
const list = 'flex flex-col';

function NavList({ items, onNavigate }: { items: NavItem[]; onNavigate: () => void }) {
  return (
    <ul className={list}>
      {items.map(({ to, label, Icon }) => (
        <li key={to}>
          <PrefetchNavLink to={to} className={item} onClick={onNavigate}>
            <Icon size={24} className={icon} />
            <span className={navLabel}>{label}</span>
          </PrefetchNavLink>
        </li>
      ))}
    </ul>
  );
}

/** Library and Playlists (report §1), once YouTube requests are made as the user. */
function LibraryNav({ onNavigate }: { onNavigate: () => void }) {
  const { t } = useTranslation();
  const playlists = useLibraryPlaylists();
  const libraryItems: NavItem[] = LIBRARY_KEYS.map((entry) => ({
    to: entry.to,
    label: t(entry.key),
    Icon: entry.Icon,
  }));

  return (
    <>
      <h2 className={sectionHeader}>{t('nav.library')}</h2>
      <NavList items={libraryItems} onNavigate={onNavigate} />
      <h2 className={sectionHeader}>{t('nav.playlists')}</h2>
      <ul className={list}>
        <li>
          <PrefetchNavLink to="/library/playlists" className={item} onClick={onNavigate}>
            <AllPlaylistsIcon size={24} className={icon} />
            <span className={navLabel}>{t('nav.allPlaylists')}</span>
          </PrefetchNavLink>
        </li>
        {playlists.data?.map((playlist) => {
          const isLikedMusic =
            playlist.id === 'LM' ||
            playlist.id === 'VLLM' ||
            playlist.title.trim().toLowerCase() === 'liked music' ||
            playlist.title.trim().toLowerCase() === 'canciones que me gustan';
          return (
            <li key={playlist.id}>
              <PrefetchNavLink to={hrefFor(playlist) ?? '/library/playlists'} className={item} onClick={onNavigate}>
                <span className={icon}>
                  <ListMusic size={17} strokeWidth={1.8} />
                </span>
                <span className={navLabel}>
                  {isLikedMusic ? t('nav.likedMusic') : playlist.title}
                </span>
              </PrefetchNavLink>
            </li>
          );
        })}
      </ul>
    </>
  );
}

/** Spotify's playlists, once the extension can read them as the user (prompt §7.1). */
function SpotifyNav({ onNavigate }: { onNavigate: () => void }) {
  const { t } = useTranslation();
  const playlists = useSpotifyPlaylists();
  return (
    <>
      <h2 className={sectionHeader}>{t('nav.spotify')}</h2>
      <ul className={list}>
        <li>
          <PrefetchNavLink to="/spotify" end className={item} onClick={onNavigate}>
            <AllPlaylistsIcon size={24} className={icon} />
            <span className={navLabel}>{t('nav.allSpotifyPlaylists')}</span>
          </PrefetchNavLink>
        </li>
        {playlists.data?.map((playlist) => (
          <li key={playlist.id}>
            <PrefetchNavLink to={`/spotify/playlist/${encodeURIComponent(playlist.id)}`} className={item} onClick={onNavigate}>
              <span className={icon}>
                <ListMusic size={17} strokeWidth={1.8} />
              </span>
              <span className={navLabel}>{playlist.name}</span>
            </PrefetchNavLink>
          </li>
        ))}
      </ul>
    </>
  );
}

/** The left glass sidebar (report §1). */
export function Sidebar() {
  const { t } = useTranslation();
  const drawerOpen = useUi((s) => s.drawerOpen);
  const setDrawerOpen = useUi((s) => s.setDrawerOpen);
  const update = useExtensionUpdate();
  const signedIn = useSignedIn();
  const spotifySignedIn = useSpotifySignedIn();
  const close = () => setDrawerOpen(false);

  const primaryItems: NavItem[] = PRIMARY_KEYS.map((entry) => ({
    to: entry.to,
    label: t(entry.key),
    Icon: entry.Icon,
  }));

  return (
    <>
      {/* Below 484px the sidebar is a drawer opened from the top bar's menu button. */}
      <nav
        className="absolute inset-y-0 left-0 z-(--z-web-chrome) flex w-(--web-navigation-width) flex-col glass-material max-sm:w-[min(300px,82vw)] max-sm:-translate-x-full max-sm:shadow-glass max-sm:transition-transform max-sm:duration-(--duration-panel) max-sm:ease-panel max-sm:data-[open=true]:translate-none"
        data-open={drawerOpen}
        aria-label="Main"
      >
        <div className="mt-2 ml-2 flex h-18 items-center px-5">
          <PrefetchLink to="/home" className="inline-flex items-center gap-2 rounded-[6px]" aria-label={`SoundChord, ${t('nav.home')}`} onClick={close}>
            {/* White on dark, as Apple's own wordmark; it follows the label colour so it stays visible in light. */}
            <LogoMark size={22} className="text-primary" />
            <span className="type-[700_19px/24px] tracking-[-0.01em]">SoundChord</span>
          </PrefetchLink>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto pr-3 pl-5 [scrollbar-width:none]">
          <NavList items={primaryItems} onNavigate={close} />
          {signedIn && <LibraryNav onNavigate={close} />}
          {spotifySignedIn && <SpotifyNav onNavigate={close} />}
        </div>
        <div className="flex flex-col gap-2 pt-3 pr-3 pb-5 pl-5">
          <PrefetchNavLink to="/extension" className={item} onClick={close}>
            {/* Chrome's own toolbar glyph for extensions (Material "extension"). */}
            <span className={icon}>
              <MdOutlineExtension size={18} />
            </span>
            <span className={navLabel}>{t('nav.soundchordExtension')}</span>
            {update.state === 'available' && (
              <span
                className="ml-auto rounded-[10px] bg-key px-[7px] py-0.5 type-subhead-emphasized text-white"
                title={t('nav.versionAvailable', { version: update.latest.version })}
              >
                {t('nav.update')}
              </span>
            )}
          </PrefetchNavLink>
          <AccountButton />
          <div className="flex items-center justify-between">
            <a className="inline-flex items-center gap-1 ps-1 type-callout text-primary hover:underline" href={config.androidAppUrl} target="_blank" rel="noreferrer">
              <span>{t('nav.getAndroidApp')}</span>
              <ArrowUpRight size={14} strokeWidth={2} />
            </a>
            <PrefetchNavLink to="/settings" className="grid size-7 place-items-center rounded-nav text-secondary hover:bg-quinary hover:text-primary aria-[current=page]:bg-quinary aria-[current=page]:text-primary" aria-label={t('nav.settings')} title={t('nav.settings')} onClick={close}>
              <Settings size={17} strokeWidth={1.9} />
            </PrefetchNavLink>
          </div>
        </div>
      </nav>
      {drawerOpen && <div className="absolute inset-0 z-[calc(var(--z-web-chrome)-1)] hidden bg-scrim max-sm:block" onClick={close} aria-hidden="true" />}
    </>
  );
}
