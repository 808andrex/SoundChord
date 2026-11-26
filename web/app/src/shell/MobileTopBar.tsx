import { Menu } from 'lucide-react';
import { PrefetchLink } from '@/components/PrefetchLink';
import { LogoMark } from '@/components/icons/LogoMark';
import { useTranslation } from '@/i18n';
import { useUi } from '@/state/ui';

/** Below 484px the sidebar folds into this bar and a drawer (report §11). */
export function MobileTopBar() {
  const { t } = useTranslation();
  const setDrawerOpen = useUi((s) => s.setDrawerOpen);
  return (
    <header className="hidden max-sm:absolute max-sm:inset-x-0 max-sm:top-0 max-sm:z-[calc(var(--z-web-chrome)-2)] max-sm:grid max-sm:h-(--mobile-top-bar-height) max-sm:grid-cols-[44px_1fr_44px] max-sm:items-center max-sm:px-3.5 max-sm:glass-material">
      <button className="grid size-11 place-items-center rounded-full" aria-label={t('nav.openMenu')} onClick={() => setDrawerOpen(true)}>
        <Menu size={22} strokeWidth={2} />
      </button>
      <PrefetchLink to="/home" className="inline-flex items-center gap-1.5 justify-self-center type-[700_16px/20px]" aria-label={`SoundChord, ${t('nav.home')}`}>
        <LogoMark size={20} className="text-primary" />
        <span>SoundChord</span>
      </PrefetchLink>
      <span className="w-11" />
    </header>
  );
}
