import type { ResolvedLanguage } from './types';

/**
 * Case-insensitive mapping for YouTube Music shelf headers when the active
 * language is Spanish. Unmapped titles (e.g. "GTA VI", artist names, custom
 * mix titles) remain untouched.
 */
export const SHELF_TITLES_ES: Record<string, string> = {
  'listen again': 'Volver a escuchar',
  'new releases': 'Nuevos lanzamientos',
  'from the community': 'De la comunidad',
  'fresh finds, old favorites': 'Novedades y favoritos',
  'fresh finds': 'Novedades',
  'new albums & singles': 'Nuevos álbumes y sencillos',
  'new albums and singles': 'Nuevos álbumes y sencillos',
  'trending': 'Tendencias',
  'new music videos': 'Nuevos videos musicales',
  'quick picks': 'Selección rápida',
  'recommended albums': 'Álbumes recomendados',
  'charts': 'Listas de éxitos',
  'top charts': 'Listas de éxitos',
  'top music videos': 'Mejores videos musicales',
  'top songs': 'Canciones más populares',
  'top artists': 'Artistas principales',
  'artists on the rise': 'Artistas emergentes',
  'live performances': 'Actuaciones en vivo',
  'hits by decade': 'Éxitos por década',
  'forgotten favorites': 'Favoritos olvidados',
  'mixed for you': 'Mezclado para ti',
  'today\'s biggest hits': 'Grandes éxitos de hoy',
  'feel-good music': 'Música para sentirte bien',
  'energy boosters': 'Música energética',
  'relaxing music': 'Música relajante',
  'focus': 'Concentración',
  'commute': 'Camino al trabajo',
  'workout': 'Entrenamiento',
  'party': 'Fiesta',
  'sleep': 'Dormir',
  'romance': 'Romance',
  'sad': 'Tristeza',
  'podcasts': 'Pódcasts',
  'episodes for you': 'Episodios para ti',
  'saved episodes': 'Episodios guardados',
  'new episodes': 'Nuevos episodios',
  'recent activity': 'Actividad reciente',
  'recently played': 'Reproducido recientemente',
  'singles & eps': 'Sencillos y EPs',
  'singles and eps': 'Sencillos y EPs',
  'albums': 'Álbumes',
  'singles': 'Sencillos',
  'videos': 'Videos',
  'featured on': 'Aparece en',
  'fans might also like': 'A los fans también les gusta',
  'you might also like': 'También te podría gustar',
  'recommended music videos': 'Videos musicales recomendados',
  'recommended music': 'Música recomendada',
};

/**
 * Translates a shelf title when language is Spanish, preserving original
 * capitalization when unmapped or containing proper names.
 */
export function translateShelfTitle(title: string, language: ResolvedLanguage): string {
  if (language !== 'es' || !title) return title;

  const normalized = title.trim().toLowerCase();
  const directMatch = SHELF_TITLES_ES[normalized];
  if (directMatch) return directMatch;

  if (normalized.startsWith('similar to ')) {
    const target = title.trim().slice('similar to '.length);
    return `Similar a ${target}`;
  }

  if (normalized.startsWith('more from ')) {
    const target = title.trim().slice('more from '.length);
    return `Más de ${target}`;
  }

  return title;
}

/**
 * Localizes release types and counters in card subtitles (e.g. "Album" -> "Álbum",
 * "Single" -> "Sencillo", "914K views" -> "914K vistas").
 */
export function translateSubtitle(subtitle: string, language: ResolvedLanguage): string {
  if (language !== 'es' || !subtitle) return subtitle;

  let result = subtitle;

  // Release types as words
  result = result.replace(/\bAlbum\b/g, 'Álbum');
  result = result.replace(/\balbum\b/g, 'álbum');
  result = result.replace(/\bSingle\b/g, 'Sencillo');
  result = result.replace(/\bsingle\b/g, 'sencillo');

  // View count counters (e.g. 914K views -> 914K vistas)
  result = result.replace(/(\d+(?:[.,]\d+)?\s*[KMBkmb]?)\s+views\b/gi, '$1 vistas');
  result = result.replace(/(\d+(?:[.,]\d+)?\s*[KMBkmb]?)\s+view\b/gi, '$1 vista');
  result = result.replace(/\bviews\b/gi, 'vistas');
  result = result.replace(/\bview\b/gi, 'vista');

  return result;
}
