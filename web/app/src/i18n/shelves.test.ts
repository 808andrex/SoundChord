import { describe, expect, it } from 'vitest';
import { translateShelfTitle, translateSubtitle } from './shelves';

describe('translateShelfTitle', () => {
  it('translates common YouTube Music shelf headers to Spanish', () => {
    expect(translateShelfTitle('Listen again', 'es')).toBe('Volver a escuchar');
    expect(translateShelfTitle('listen again', 'es')).toBe('Volver a escuchar');
    expect(translateShelfTitle('New releases', 'es')).toBe('Nuevos lanzamientos');
    expect(translateShelfTitle('From the community', 'es')).toBe('De la comunidad');
    expect(translateShelfTitle('Fresh finds, old favorites', 'es')).toBe('Novedades y favoritos');
    expect(translateShelfTitle('New albums & singles', 'es')).toBe('Nuevos álbumes y sencillos');
    expect(translateShelfTitle('Trending', 'es')).toBe('Tendencias');
    expect(translateShelfTitle('New music videos', 'es')).toBe('Nuevos videos musicales');
    expect(translateShelfTitle('Quick picks', 'es')).toBe('Selección rápida');
    expect(translateShelfTitle('Recommended albums', 'es')).toBe('Álbumes recomendados');
    expect(translateShelfTitle('Charts', 'es')).toBe('Listas de éxitos');
  });

  it('handles dynamic prefixes like "Similar to" and "More from"', () => {
    expect(translateShelfTitle('Similar to Dua Lipa', 'es')).toBe('Similar a Dua Lipa');
    expect(translateShelfTitle('More from Bad Bunny', 'es')).toBe('Más de Bad Bunny');
  });

  it('keeps proper names and unmapped titles untouched', () => {
    expect(translateShelfTitle('GTA VI', 'es')).toBe('GTA VI');
    expect(translateShelfTitle('Cyberpunk 2077', 'es')).toBe('Cyberpunk 2077');
  });

  it('returns original title when language is English', () => {
    expect(translateShelfTitle('Listen again', 'en')).toBe('Listen again');
    expect(translateShelfTitle('New releases', 'en')).toBe('New releases');
  });
});

describe('translateSubtitle', () => {
  it('translates release types to Spanish', () => {
    expect(translateSubtitle('Album • Taylor Swift', 'es')).toBe('Álbum • Taylor Swift');
    expect(translateSubtitle('Single • Bad Bunny', 'es')).toBe('Sencillo • Bad Bunny');
    expect(translateSubtitle('EP • Billie Eilish', 'es')).toBe('EP • Billie Eilish');
  });

  it('translates views counters to Spanish', () => {
    expect(translateSubtitle('914K views', 'es')).toBe('914K vistas');
    expect(translateSubtitle('1.2M views • 2 days ago', 'es')).toBe('1.2M vistas • 2 days ago');
    expect(translateSubtitle('1 view', 'es')).toBe('1 vista');
  });

  it('returns untouched subtitle when language is English', () => {
    expect(translateSubtitle('Album • Taylor Swift', 'en')).toBe('Album • Taylor Swift');
    expect(translateSubtitle('914K views', 'en')).toBe('914K views');
  });
});
