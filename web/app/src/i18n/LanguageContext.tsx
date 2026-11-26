import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { en } from './en';
import { es } from './es';
import type {
  I18nContextValue,
  LanguagePreference,
  ResolvedLanguage,
  TranslationKey,
  Translations,
} from './types';

const STORAGE_KEY = 'soundchord.language';

const DICTIONARIES: Record<ResolvedLanguage, Translations> = {
  en,
  es,
};

export function detectBrowserLanguage(): ResolvedLanguage {
  if (typeof navigator === 'undefined') return 'en';
  const languages = navigator.languages?.length ? navigator.languages : [navigator.language];
  for (const lang of languages) {
    if (!lang) continue;
    const normalized = lang.trim().toLowerCase();
    if (normalized.startsWith('es')) return 'es';
  }
  return 'en';
}

function getInitialPreference(): LanguagePreference {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'es' || saved === 'en' || saved === 'auto') {
      return saved;
    }
  } catch {
    // LocalStorage might be disabled or unavailable
  }
  return 'auto';
}

const LanguageContext = createContext<I18nContextValue | null>(null);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [preference, setPreferenceState] = useState<LanguagePreference>(getInitialPreference);
  const [systemLang, setSystemLang] = useState<ResolvedLanguage>(detectBrowserLanguage);

  useEffect(() => {
    const handleLanguageChange = () => {
      setSystemLang(detectBrowserLanguage());
    };
    window.addEventListener('languagechange', handleLanguageChange);
    return () => window.removeEventListener('languagechange', handleLanguageChange);
  }, []);

  const setPreference = useCallback((nextPref: LanguagePreference) => {
    setPreferenceState(nextPref);
    try {
      localStorage.setItem(STORAGE_KEY, nextPref);
    } catch {
      // LocalStorage error ignore
    }
  }, []);

  const language: ResolvedLanguage = useMemo(() => {
    return preference === 'auto' ? systemLang : preference;
  }, [preference, systemLang]);

  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.documentElement.lang = language;
    }
  }, [language]);

  const t = useCallback(
    (key: TranslationKey, params?: Record<string, string | number>): string => {
      const dict = DICTIONARIES[language] ?? DICTIONARIES.en;
      let text = dict[key] ?? DICTIONARIES.en[key] ?? key;
      if (params) {
        for (const [paramKey, paramValue] of Object.entries(params)) {
          text = text.replaceAll(`{${paramKey}}`, String(paramValue));
        }
      }
      return text;
    },
    [language],
  );

  const value = useMemo<I18nContextValue>(
    () => ({
      language,
      preference,
      setPreference,
      t,
    }),
    [language, preference, setPreference, t],
  );

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useTranslation(): I18nContextValue {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useTranslation must be used within a LanguageProvider');
  }
  return context;
}
