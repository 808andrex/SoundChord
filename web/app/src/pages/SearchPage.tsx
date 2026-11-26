import { useSearchParams } from 'react-router';
import { PrefetchLink } from '@/components/PrefetchLink';
import { Artwork } from '@/components/music/Artwork';
import { CategoryTiles } from '@/components/music/CategoryTiles';
import { hrefFor } from '@/components/music/links';
import { playTrack, toTrack } from '@/components/music/play';
import { SearchField } from '@/components/music/SearchField';
import { Shelves } from '@/components/music/ShelfView';
import { TrackMenu } from '@/components/music/TrackMenu';
import { EmptyState, LoadError, NeedsExtension } from '@/components/music/States';
import { useCategories, useSearch, useYouTubeReady } from '@/data/music/queries';
import type { MusicItem, SearchResults, Shelf } from '@/data/music/types';
import { useTranslation, type TranslationKey } from '@/i18n';
import { SkeletonBlock, SkeletonLine } from '@/components/Skeleton';
import { ShelfSkeleton } from '@/skeletons/ShelfSkeleton';

function kindLabel(kind: MusicItem['kind'], t: (k: TranslationKey) => string): string {
  switch (kind) {
    case 'song':
      return t('common.song');
    case 'video':
      return t('common.video');
    case 'album':
      return t('common.album');
    case 'playlist':
      return t('nav.playlists');
    case 'spotify-playlist':
      return t('common.spotifyPlaylist');
    case 'artist':
      return t('common.artist');
    case 'episode':
      return t('common.episode');
    case 'podcast':
      return t('common.podcast');
    default:
      return '';
  }
}

/** Apple's result shelves, in its order, from results grouped by kind. */
function shelvesFor(results: SearchResults, t: (k: TranslationKey) => string): Shelf[] {
  const shelf = (title: string, items: MusicItem[], layout: Shelf['layout']): Shelf | null =>
    items.length ? { title, strapline: null, layout, items, seeAll: null } : null;
  return [
    shelf(t('nav.artists'), results.artists, 'circle'),
    shelf(t('nav.albums'), results.albums, 'square'),
    shelf(t('nav.songs'), results.songs, 'tracks'),
    shelf(t('nav.playlists'), results.playlists, 'square'),
    shelf(t('common.videos'), results.videos, 'video'),
  ].filter((s): s is Shelf => !!s);
}

const sectionTitle = 'mx-(--body-gutter) mt-3 type-title-2-emphasized';
// Top Results: 3 columns, gap 20px; 118px cards with 90px artwork.
const topGrid =
  'mx-(--body-gutter) mt-3 mb-8 grid grid-cols-3 gap-5 max-lg:grid-cols-2 max-[607px]:grid-cols-[minmax(0,1fr)] max-[607px]:gap-1';
// The card is a relative box and the play/link layer fills it: a playable
// item's track menu is a sibling over the card's bottom-right corner, because a
// button can't be nested inside the card's own link or button.
const topCard = 'group/top relative rounded-[8px] bg-quinary hover:bg-quaternary';
const top =
  'grid min-h-[118px] w-full grid-cols-[90px_minmax(0,1fr)] items-center gap-x-3.5 px-3 py-3.5 text-left';
const topMenu =
  'absolute right-2 bottom-2 z-3 grid size-7 place-items-center rounded-full text-key opacity-0 transition-opacity duration-100 ease-in group-hover/top:opacity-100 focus-visible:opacity-100 hover:bg-quinary data-open:opacity-100';
const topText = 'flex min-w-0 flex-col gap-0.5';

function TopResult({ item }: { item: MusicItem }) {
  const { t } = useTranslation();
  const href = hrefFor(item);
  const playable = item.kind === 'song' || item.kind === 'video';
  const body = (
    <>
      <div className="w-[90px]">
        <Artwork src={item.thumbnail} displayWidth={90} radius={item.kind === 'artist' ? '50%' : 'var(--radius-artwork-small)'} />
      </div>
      <div className={playable ? `${topText} pr-9` : topText}>
        <span className="line-clamp-2 type-title-3">{item.title}</span>
        <span className="truncate type-callout text-secondary">
          {[kindLabel(item.kind, t), item.kind === 'artist' ? null : item.artists.map((a) => a.name).join(', ')].filter(Boolean).join(' · ')}
        </span>
      </div>
    </>
  );
  return (
    <div className={topCard}>
      {href ? (
        <PrefetchLink to={href} className={top}>
          {body}
        </PrefetchLink>
      ) : (
        <button className={top} onClick={() => playTrack(item)}>
          {body}
        </button>
      )}
      {playable && <TrackMenu track={toTrack(item)} className={topMenu} />}
    </div>
  );
}

function ResultsSkeleton() {
  return (
    <div aria-busy="true">
      <div className={sectionTitle}>
        <SkeletonLine lineHeight={22} fontSize={17} width={130} />
      </div>
      <div className={topGrid} aria-hidden="true">
        {Array.from({ length: 6 }, (_, i) => (
          <div key={i} className={`${topCard} ${top}`}>
            <SkeletonBlock width={90} height={90} radius="var(--radius-artwork-small)" />
            <div className={topText}>
              <SkeletonLine lineHeight={20} fontSize={15} width="70%" />
              <SkeletonLine lineHeight={15} fontSize={12} width="40%" />
            </div>
          </div>
        ))}
      </div>
      <ShelfSkeleton kind="circle" />
      <ShelfSkeleton kind="square" />
    </div>
  );
}

function Results({ query }: { query: string }) {
  const { t } = useTranslation();
  const search = useSearch(query);
  if (search.isError) return <LoadError error={search.error} onRetry={() => void search.refetch()} />;
  if (!search.data) return <ResultsSkeleton />;
  const results = search.data;
  const shelves = shelvesFor(results, t);
  if (!results.top.length && !shelves.length) {
    return <EmptyState title={t('search.noResultsTitle')} detail={t('search.noResultsDetail', { query })} />;
  }
  return (
    <>
      {results.top.length > 0 && (
        <section aria-label={t('search.topResults')}>
          <h2 className={sectionTitle}>{t('search.topResults')}</h2>
          <div className={topGrid}>
            {results.top.map((item) => (
              <TopResult key={item.id} item={item} />
            ))}
          </div>
        </section>
      )}
      <Shelves shelves={shelves} />
    </>
  );
}

function Landing() {
  const { t } = useTranslation();
  const categories = useCategories();
  if (categories.isError) return <LoadError error={categories.error} onRetry={() => void categories.refetch()} />;
  if (!categories.data) {
    return (
      <div className="mx-(--body-gutter) mt-[46px] mb-8 grid grid-cols-4 gap-5" aria-busy="true">
        {Array.from({ length: 12 }, (_, i) => (
          <SkeletonBlock key={i} aspectRatio="16 / 9" radius="var(--radius-artwork-large)" />
        ))}
      </div>
    );
  }
  return <CategoryTiles groups={categories.data.map((g) => ({ ...g, title: g.title || t('search.browseCategories') }))} />;
}

/** Search (report §6): a sticky bar with the field, then categories or grouped results. */
export default function SearchPage() {
  const { t } = useTranslation();
  const { missing } = useYouTubeReady();
  const [params, setParams] = useSearchParams();
  const query = params.get('q') ?? '';

  return (
    <>
      {/*
        The sticky scope bar holding the field (report §6: 62px, padding 12px 0). Its glass sits on
        a layer behind it, not on the bar: an element with a backdrop-filter is a backdrop root, so
        the suggestions inside it could only blur the bar itself and showed the page underneath unblurred.
      */}
      <div className="sticky top-0 z-[calc(var(--z-default)+5)] flex justify-center px-(--body-gutter) py-3 before:absolute before:inset-0 before:-z-1 before:bg-header-material before:[backdrop-filter:saturate(180%)_blur(20px)]">
        <SearchField initial={query} onSearch={(q) => setParams(q ? { q } : {})} />
      </div>
      {missing ? <NeedsExtension what={t('search.needsExtensionWhat')} /> : query ? <Results query={query} /> : <Landing />}
    </>
  );
}
