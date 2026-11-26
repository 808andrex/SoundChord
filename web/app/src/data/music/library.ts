import type { TranslationKey } from '@/i18n/types';

/**
 * The library sections, signed in (prompt §5.3), in Apple's sidebar order
 * (report §1: Recently Added · Artists · Albums · Songs · Made for You). Each
 * is a YouTube Music library page; YouTube has no "recently added" view of its
 * own, so that slot is the listening history, which is what the clock means
 * there anyway.
 */
export const LIBRARY_SECTIONS = {
  recent: { title: 'Recently Played', browseId: 'FEmusic_history' },
  artists: { title: 'Artists', browseId: 'FEmusic_library_corpus_track_artists' },
  albums: { title: 'Albums', browseId: 'FEmusic_liked_albums' },
  songs: { title: 'Songs', browseId: 'FEmusic_liked_videos' },
  'made-for-you': { title: 'Made for You', browseId: 'FEmusic_mixed_for_you' },
  playlists: { title: 'All Playlists', browseId: 'FEmusic_liked_playlists' },
} as const;

export type LibrarySection = keyof typeof LIBRARY_SECTIONS;

export const LIBRARY_SECTION_KEYS: Record<LibrarySection, TranslationKey> = {
  recent: 'nav.recent',
  artists: 'nav.artists',
  albums: 'nav.albums',
  songs: 'nav.songs',
  'made-for-you': 'nav.madeForYou',
  playlists: 'nav.allPlaylists',
};

export function isLibrarySection(value: string): value is LibrarySection {
  return Object.hasOwn(LIBRARY_SECTIONS, value);
}

/** YouTube's "Liked Music" auto-playlist. */
export const LIKED_MUSIC_PLAYLIST = 'LM';
