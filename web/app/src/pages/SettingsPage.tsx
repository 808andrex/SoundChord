import { ChevronRight, Users } from 'lucide-react';
import { lazy, Suspense, useState, type ComponentType, type ReactNode } from 'react';
import { MdOutlineExtension } from 'react-icons/md';
import { SiApplemusic, SiDiscord, SiSpotify, SiYoutubemusic } from 'react-icons/si';
import { PageHeader } from '@/components/PageHeader';
import { PrefetchLink } from '@/components/PrefetchLink';
import { useDiscord } from '@/extension/useDiscord';
import { useExtension } from '@/extension/useExtension';
import { useYtAuth } from '@/extension/useYtAuth';
import { useTranslation, type LanguagePreference, type TranslationKey } from '@/i18n';
import { useParty } from '@/party/useParty';
import { useHistorySettings } from '@/playback/historyReporting';
import { Select } from '@/components/Select';
import { AUTOMIX_BARS } from '@/playback/automix/choices';
import { CROSSFADE_CHOICES, usePlayer } from '@/state/player';
import { AppleMusicIntegration, AppleMusicToggles, CanvasToggle, LyricSourcesAccess, SpotifyIntegration, Toggle } from './IntegrationSettings';
import { settingsUi } from './settingsStyles';

const LyricsSettings = lazy(() => import('@/shell/LyricsSettings').then((m) => ({ default: m.LyricsSettings })));

/** An integration's coloured glyph, as on the Extension page. */
function Glyph({ Icon, color, ink }: { Icon: ComponentType<{ size?: number }>; color: string; ink?: string }) {
  return (
    <span className={settingsUi.integrationIcon} style={{ backgroundColor: color, ...(ink && { color: ink }) }}>
      <Icon size={15} />
    </span>
  );
}

/** A row that opens a page of its own. */
function LinkRow({ to, icon, title, value }: { to: string; icon?: ReactNode; title: string; value?: string | null }) {
  return (
    <PrefetchLink to={to} className={`${icon ? settingsUi.integration : settingsUi.integrationPlain} ${settingsUi.linkRow}`}>
      {icon}
      <span className={settingsUi.integrationName}>{title}</span>
      <span className={settingsUi.value}>
        {value}
        <ChevronRight size={16} />
      </span>
    </PrefetchLink>
  );
}

function youtubeText(auth: ReturnType<typeof useYtAuth>, t: (k: TranslationKey, p?: Record<string, string | number>) => string): string {
  if (auth.state === 'checking') return t('common.checking');
  if (auth.state === 'unavailable') return t('extension.needsTheExtension');
  const { browserSignedIn, connected, active } = auth.auth;
  if (!browserSignedIn) return t('settings.notSignedIn');
  if (!connected) return t('settings.disconnected');
  return active ? active.name : t('settings.signedIn');
}

/**
 * Settings (SettingsSheet on the phone): the account and its integrations,
 * Listen Together, playback, lyrics, and language, in the same grouped rows.
 */
export default function SettingsPage() {
  const { t, preference, setPreference } = useTranslation();
  const extension = useExtension();
  const yt = useYtAuth();
  const discord = useDiscord();
  const party = useParty();
  const autoplay = usePlayer((s) => s.autoplay);
  const crossfadeSec = usePlayer((s) => s.crossfadeSec);
  const automix = usePlayer((s) => s.automix);
  const automixBars = usePlayer((s) => s.automixBars);
  const showNerdStats = usePlayer((s) => s.showNerdStats);
  const shareHistory = useHistorySettings((s) => s.share);
  const setShareHistory = useHistorySettings((s) => s.setShare);
  const { setAutoplay, setCrossfade, setAutomix, setAutomixBars, setShowNerdStats } = usePlayer.getState();
  const [lyrics, setLyrics] = useState(false);
  const present = extension.state === 'present';
  const discordText =
    discord.state === 'ready' && discord.discord.connected
      ? discord.discord.enabled
        ? t('queue.on')
        : t('queue.off')
      : discord.state === 'ready' && discord.discord.available
        ? t('extension.notConnected')
        : null;

  return (
    <div className={settingsUi.centered}>
      <PageHeader title={t('settings.title')} />
      <div className={settingsUi.body}>
        <h2 className={settingsUi.sectionHeader}>{t('settings.accountAndIntegrations')}</h2>
        <section className={settingsUi.group}>
          <div className={settingsUi.integration}>
            <Glyph Icon={SiYoutubemusic} color="#ffffff" ink="#ff0033" />
            <span className={settingsUi.integrationName}>YouTube Music</span>
            <span className={settingsUi.value}>{youtubeText(yt, t)}</span>
          </div>
          <div className={settingsUi.integration}>
            <Glyph Icon={SiSpotify} color="#1ed760" />
            <span className={settingsUi.integrationName}>Spotify</span>
            {present ? <SpotifyIntegration /> : <span className={settingsUi.value}>{t('extension.needsTheExtension')}</span>}
          </div>
          {present && discord.state === 'ready' && discord.discord.available && (
            <LinkRow to="/settings/discord" icon={<Glyph Icon={SiDiscord} color="#5865f2" />} title="Discord" value={discordText} />
          )}
          <LinkRow
            to="/extension"
            icon={<Glyph Icon={MdOutlineExtension} color="#636366" />}
            title={t('nav.soundchordExtension')}
            value={present ? `v${extension.extension.info.version}` : t('extension.notInstalled')}
          />
        </section>
        <section className={`${settingsUi.group} mt-3 empty:hidden`}>
          <Toggle
            label={t('settings.shareListening')}
            detail={
              present && !extension.extension.info.capabilities.includes('yt.stats')
                ? t('settings.shareListeningNeedUpdate')
                : t('settings.shareListeningDetail')
            }
            checked={shareHistory}
            onChange={setShareHistory}
          />
          <CanvasToggle />
        </section>

        <h2 className={settingsUi.sectionHeader}>{t('settings.appleMusic')}</h2>
        <section className={settingsUi.group}>
          <div className={settingsUi.integration}>
            <Glyph Icon={SiApplemusic} color="#fa2d48" />
            <span className={settingsUi.integrationName}>{t('settings.extensionAccess')}</span>
            {present ? <AppleMusicIntegration /> : <span className={settingsUi.value}>{t('extension.needsTheExtension')}</span>}
          </div>
          <AppleMusicToggles />
        </section>
        <p className={settingsUi.footer}>{t('settings.appleMusicFooter')}</p>

        <h2 className={settingsUi.sectionHeader}>{t('party.title')}</h2>
        <section className={settingsUi.group}>
          <LinkRow
            to="/settings/listen-together"
            icon={<Glyph Icon={Users} color="#fa2d48" />}
            title={t('party.title')}
            value={party.code ? t('party.inParty', { code: party.code }) : t('party.desc')}
          />
        </section>

        <h2 className={settingsUi.sectionHeader}>{t('settings.playback')}</h2>
        <section className={settingsUi.group}>
          <Toggle
            label={t('settings.autoplay')}
            detail={t('settings.autoplayDetail')}
            checked={autoplay}
            onChange={setAutoplay}
          />
          <label className={settingsUi.controlRow}>
            <span>{t('settings.crossfade')}</span>
            <Select<number | 'automix'>
              label={t('settings.crossfade')}
              value={automix ? 'automix' : crossfadeSec}
              options={[
                ...CROSSFADE_CHOICES.map((seconds) => ({
                  value: seconds,
                  label: seconds === 0 ? t('queue.off') : `${seconds} ${t('queue.seconds')}`,
                })),
                { value: 'automix', label: t('queue.automix') },
              ]}
              onChange={(value) => (value === 'automix' ? setAutomix(true) : setCrossfade(value))}
            />
          </label>
          {automix && (
            <label className={settingsUi.controlRow}>
              <span>{t('settings.blendLength')}</span>
              <Select
                label={t('settings.blendLength')}
                value={automixBars}
                options={AUTOMIX_BARS.map((bars) => ({ value: bars, label: t('settings.upToBars', { bars }) }))}
                onChange={setAutomixBars}
              />
            </label>
          )}
          <Toggle
            label={t('settings.showNerdStats')}
            detail={t('settings.showNerdStatsDetail')}
            checked={showNerdStats}
            onChange={setShowNerdStats}
          />
        </section>
        {automix && (
          <p className={settingsUi.footer}>{t('settings.automixFooter')}</p>
        )}

        <h2 className={settingsUi.sectionHeader}>{t('settings.lyrics')}</h2>
        <section className={settingsUi.group}>
          <button className={`${settingsUi.integrationPlain} ${settingsUi.linkRow}`} onClick={() => setLyrics(true)}>
            <span className={settingsUi.integrationName}>{t('settings.lyricsSources')}</span>
            <span className={settingsUi.value}>
              {t('settings.lyricsSourcesDetail')}
              <ChevronRight size={16} />
            </span>
          </button>
          <div className={settingsUi.integrationPlain}>
            <span className={settingsUi.integrationName}>{t('settings.lyricsSourcesAccess')}</span>
            {present ? <LyricSourcesAccess /> : <span className={settingsUi.value}>{t('extension.needsTheExtension')}</span>}
          </div>
        </section>

        <h2 className={settingsUi.sectionHeader}>{t('settings.language')}</h2>
        <section className={settingsUi.group}>
          <label className={settingsUi.controlRow}>
            <span>{t('settings.language')}</span>
            <Select<LanguagePreference>
              label={t('settings.language')}
              value={preference}
              options={[
                { value: 'auto', label: t('settings.languageAuto') },
                { value: 'es', label: t('settings.languageEs') },
                { value: 'en', label: t('settings.languageEn') },
              ]}
              onChange={setPreference}
            />
          </label>
        </section>
        <p className={settingsUi.footer}>{t('settings.languageFooter')}</p>
      </div>
      {lyrics && (
        <Suspense fallback={null}>
          <LyricsSettings onClose={() => setLyrics(false)} />
        </Suspense>
      )}
    </div>
  );
}
