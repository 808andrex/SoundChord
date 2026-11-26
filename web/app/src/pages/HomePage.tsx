import { useEffect, useRef } from 'react';
import { PageHeader } from '@/components/PageHeader';
import { Shelves, TrackShelf } from '@/components/music/ShelfView';
import { LoadError, LoadMoreError, NeedsExtension } from '@/components/music/States';
import { useHome, useLibrary, useYouTubeReady } from '@/data/music/queries';
import type { Track } from '@/data/music/types';
import { useTranslation } from '@/i18n';
import { HomeSkeleton } from '@/skeletons/PageSkeletons';
import { ShelfSkeleton } from '@/skeletons/ShelfSkeleton';

const RECENT_TITLE = 'Recently Played';
const RECENT_LIMIT = 20;

/**
 * The account's listening history, newest first, one row per song
 * (YtMusicRepository.recentlyPlayed): the first 20, signed in only. The same
 * query as Library's Recently Played, so either page warms the other.
 */
function useRecentlyPlayed(): Track[] {
  const history = useLibrary('recent');
  const first = history.data?.pages[0]?.data.tracks ?? [];
  const seen = new Set<string>();
  return first.filter((t) => !seen.has(t.videoId) && seen.add(t.videoId)).slice(0, RECENT_LIMIT);
}

/** YouTube Music's home feed, as Apple's Home: shelf after shelf, more as you scroll. */
export default function HomePage() {
  const { t } = useTranslation();
  const { missing } = useYouTubeReady();
  const home = useHome();
  const recent = useRecentlyPlayed();
  const sentinel = useRef<HTMLDivElement>(null);

  const homeTitle = t('nav.home');
  const recentTitle = t('nav.recent');
  const recentSource = { title: recentTitle, type: 'home' as const };

  // Loads the next shelves before the bottom comes into view; after a failed
  // page it waits for Try Again rather than retrying on every scroll.
  useEffect(() => {
    const el = sentinel.current;
    if (!el || !home.hasNextPage || home.isFetchNextPageError) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry?.isIntersecting && !home.isFetchingNextPage) void home.fetchNextPage();
      },
      { root: document.getElementById('scrollable-page'), rootMargin: '600px' },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [home.hasNextPage, home.isFetchingNextPage, home.isFetchNextPageError, home.fetchNextPage]);

  if (missing) {
    return (
      <>
        <PageHeader title={homeTitle} />
        <NeedsExtension />
      </>
    );
  }
  // Only a failed first page replaces the feed; a later one is shown under it.
  if (home.isError && !home.data) {
    return (
      <>
        <PageHeader title={homeTitle} />
        <LoadError error={home.error} onRetry={() => void home.refetch()} />
      </>
    );
  }
  if (!home.data) return <HomeSkeleton />;

  // YouTube's feed can carry its own, older Recently Played; the history wins, as on the phone.
  const shelves = home.data.pages
    .flatMap((page) => page.data)
    .filter((shelf) => {
      const shelfTitleLower = shelf.title.trim().toLowerCase();
      return !(
        recent.length > 0 &&
        (shelfTitleLower === RECENT_TITLE.toLowerCase() ||
          shelfTitleLower === recentTitle.toLowerCase() ||
          shelfTitleLower === 'volver a escuchar' ||
          shelfTitleLower === 'reproducido recientemente')
      );
    });
  return (
    <>
      <PageHeader title={homeTitle} />
      {recent.length > 0 && <TrackShelf title={recentTitle} to="/library/recent" tracks={recent} source={recentSource} />}
      <Shelves shelves={shelves} />
      <div ref={sentinel} />
      {home.isFetchingNextPage && <ShelfSkeleton kind="square" />}
      {home.isFetchNextPageError && <LoadMoreError onRetry={() => void home.fetchNextPage()} />}
    </>
  );
}
