import { PageHeader } from '@/components/PageHeader';
import { SquareLockup } from '@/components/music/Lockups';
import { EmptyState, LoadError, NeedsExtension, stateStyles } from '@/components/music/States';
import type { MusicItem } from '@/data/music/types';
import { useSpotifyPlaylists } from '@/data/spotify/queries';
import { useExtension } from '@/extension/useExtension';
import { openSpotifySignIn, useRefreshSpotifyAuth, useSetSpotifyConnected, useSpotifyAuth } from '@/extension/useSpotifyAuth';
import { useTranslation } from '@/i18n';
import { PlainSkeleton } from '@/skeletons/PageSkeletons';
import { seeAllGrid } from './pageStyles';

/**
 * The signed-in Spotify account's playlists (SpotifyLibraryScreen): this page
 * is only the way in; a playlist opens as an ordinary playlist page whose
 * songs are matched to YouTube Music.
 */
export default function SpotifyPage() {
  const { t } = useTranslation();
  const extension = useExtension();
  const auth = useSpotifyAuth();
  const playlists = useSpotifyPlaylists();
  const connect = useSetSpotifyConnected();
  const retry = useRefreshSpotifyAuth();

  if (extension.state === 'missing') return <NeedsExtension what={t('spotify.yourSpotifyPlaylists')} />;
  if (auth.state === 'checking') return <PlainSkeleton />;
  if (auth.state === 'unavailable') {
    return (
      <>
        <PageHeader title={t('nav.spotify')} />
        <EmptyState title={t('spotify.updateExtensionTitle')} detail={t('spotify.updateExtensionDetail')} />
      </>
    );
  }
  const state = auth.auth;
  if (!state.signedIn) {
    const [title, detail, action] = !state.browserSignedIn
      ? [t('spotify.signInTitle'), t('spotify.signInDetail'), t('spotify.signInButton')]
      : !state.connected
        ? [t('spotify.disconnectedTitle'), t('spotify.disconnectedDetail'), t('spotify.connectButton')]
        : [t('spotify.couldNotReadTitle'), state.problem ?? t('spotify.couldNotReadDefaultDetail'), t('common.tryAgain')];
    return (
      <>
        <PageHeader title={t('nav.spotify')} />
        <div className={stateStyles.state}>
          <p className={stateStyles.title}>{title}</p>
          <p className={stateStyles.detail}>{detail}</p>
          <button
            className={stateStyles.button}
            disabled={connect.isPending || retry.isPending}
            onClick={() =>
              !state.browserSignedIn ? openSpotifySignIn() : !state.connected ? connect.mutate(true) : retry.mutate()
            }
          >
            {action}
          </button>
        </div>
      </>
    );
  }
  if (playlists.isError && !playlists.data) return <LoadError error={playlists.error} onRetry={() => void playlists.refetch()} />;
  if (!playlists.data) return <PlainSkeleton />;

  const items: MusicItem[] = playlists.data.map((p) => ({
    kind: 'spotify-playlist',
    id: p.id,
    title: p.name,
    subtitle: p.owner ?? 'Spotify',
    artists: [],
    album: null,
    thumbnail: p.imageUrl,
    wide: false,
    durationSec: null,
    explicit: false,
  }));
  return (
    <>
      <PageHeader title={t('nav.spotify')} />
      {!items.length && <EmptyState title={t('spotify.noPlaylistsYet')} />}
      <ul className={seeAllGrid}>
        {items.map((item) => (
          <li key={item.id}>
            <SquareLockup item={item} />
          </li>
        ))}
      </ul>
    </>
  );
}
