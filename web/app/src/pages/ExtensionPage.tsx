import { ChevronRight } from 'lucide-react';
import { useState, type ComponentType } from 'react';
import { FaChrome, FaFirefoxBrowser } from 'react-icons/fa';
import { SiDiscord, SiSpotify, SiYoutubemusic } from 'react-icons/si';
import type { Capability } from '@soundchord/shared/bridge';
import { PageHeader } from '@/components/PageHeader';
import { config } from '@/config';
import { useExtension, type ExtensionStatus } from '@/extension/useExtension';
import { downloadUrl, useExtensionUpdate, usePublishedExtension, type PublishedExtension } from '@/extension/useExtensionUpdate';
import { useYtAuth, type YtAuthStatus } from '@/extension/useYtAuth';
import { useTranslation, type TranslationKey } from '@/i18n';
import { PrefetchLink } from '@/components/PrefetchLink';
import { DiscordIntegration, SpotifyIntegration } from './IntegrationSettings';
import { settingsUi } from './settingsStyles';

type Browser = 'chrome' | 'firefox';

interface Download {
  browser: Browser;
  titleKey: TranslationKey;
  detailKey: TranslationKey;
  href: string;
  Icon: ComponentType<{ size?: number; className?: string }>;
  steps: string[];
  updateSteps: string[];
}

/**
 * The two builds, as published (extension.json): the zip and the folder in it
 * carry the version, "soundchord-extension-chrome (0.4.3)". Before that file
 * has loaded, the configured URLs and the unversioned names stand in.
 */
function downloadsFor(published: PublishedExtension | null): Download[] {
  const folder = (browser: Browser) => `soundchord-extension-${browser}${published ? ` (${published.version})` : ''}`;
  return [
    {
      browser: 'chrome',
      titleKey: 'extension.downloadForChrome',
      detailKey: 'extension.chromeDetail',
      href: published ? downloadUrl(published.files.chrome) : config.extensionChromeUrl,
      Icon: FaChrome,
      steps: [
        `Extract the zip somewhere you’ll keep it. It holds one folder, ${folder('chrome')}.`,
        'Open chrome://extensions and turn on Developer mode.',
        'Choose Load unpacked and select that folder.',
        'Come back to this page: the status above turns to Installed.',
      ],
      updateSteps: [
        `Extract the new zip. It holds ${folder('chrome')}.`,
        'In chrome://extensions, choose Load unpacked and select that folder. It has the same ID, so it takes the old one’s place and keeps your settings.',
        'Delete the old folder, then come back to this page: the version above updates on its own.',
      ],
    },
    {
      browser: 'firefox',
      titleKey: 'extension.downloadForFirefox',
      detailKey: 'extension.firefoxDetail',
      href: published ? downloadUrl(published.files.firefox) : config.extensionFirefoxUrl,
      Icon: FaFirefoxBrowser,
      steps: [
        `Extract the zip. It holds one folder, ${folder('firefox')}.`,
        'Open about:debugging#/runtime/this-firefox and choose Load Temporary Add-on.',
        'Select manifest.json inside that folder.',
        'Firefox removes temporary add-ons when it quits; a signed version is coming.',
      ],
      updateSteps: [
        `Extract the new zip. It holds ${folder('firefox')}.`,
        'In about:debugging, choose Load Temporary Add-on and select manifest.json in the new folder.',
        'Come back to this page: the version above updates on its own.',
      ],
    },
  ];
}

/** The browser this page is open in, to put its download first. */
const currentBrowser: Browser = /firefox/i.test(navigator.userAgent) ? 'firefox' : 'chrome';

interface Integration {
  name: string;
  Icon: ComponentType<{ size?: number; className?: string }>;
  color: string;
  capability: Capability;
  /** What the row says when the extension serves this integration. */
  readyKey: TranslationKey;
}

const INTEGRATIONS: Integration[] = [
  { name: 'YouTube Music', Icon: SiYoutubemusic, color: '#ff0033', capability: 'yt', readyKey: 'extension.playbackReadyNotSignedIn' },
  { name: 'Spotify', Icon: SiSpotify, color: '#1ed760', capability: 'spotify', readyKey: 'extension.notConnected' },
  { name: 'Discord', Icon: SiDiscord, color: '#5865f2', capability: 'discord', readyKey: 'queue.off' },
];

/** YouTube Music's row: who requests act as, once the extension reports it. */
function youtubeStatus(auth: YtAuthStatus, t: (k: TranslationKey, p?: Record<string, string | number>) => string): string | null {
  if (auth.state !== 'ready') return null;
  const { browserSignedIn, connected, active } = auth.auth;
  if (!browserSignedIn) return t('extension.playbackReadyNotSignedIn');
  if (!connected) return t('settings.disconnected');
  return active ? t('extension.signedInAs', { name: active.name }) : t('extension.signedInNoAccount');
}

/** One integration's state, honest about what this extension version can do. */
function integrationStatus(
  status: ExtensionStatus,
  integration: Integration,
  auth: YtAuthStatus,
  t: (k: TranslationKey, p?: Record<string, string | number>) => string,
): string {
  if (status.state === 'checking') return t('common.checking');
  if (status.state === 'missing') return t('extension.needsTheExtension');
  if (!status.extension.info.capabilities.includes(integration.capability)) return t('extension.notAvailableInVersion');
  return (integration.capability === 'yt' && youtubeStatus(auth, t)) || t(integration.readyKey);
}

export default function ExtensionPage() {
  const { t } = useTranslation();
  const status = useExtension();
  const update = useExtensionUpdate();
  const DOWNLOADS = downloadsFor(usePublishedExtension());
  const auth = useYtAuth();
  const [chosen, setChosen] = useState<Browser | null>(null);
  const [updating, setUpdating] = useState(false);
  const downloads = [...DOWNLOADS].sort((a, b) => Number(b.browser === currentBrowser) - Number(a.browser === currentBrowser));
  const installedBrowser: Browser | null =
    status.state === 'present' ? (status.extension.info.browser === 'firefox' ? 'firefox' : 'chrome') : null;
  const forUpdate = DOWNLOADS.find((d) => d.browser === (installedBrowser ?? currentBrowser))!;
  const steps = DOWNLOADS.find((d) => d.browser === chosen)?.steps;

  return (
    <div className={settingsUi.centered}>
      <PageHeader title={t('extension.title')} />
      <div className={settingsUi.body}>
        <section className={settingsUi.group} aria-live="polite">
          <div className={settingsUi.row}>
            <span>{t('extension.status')}</span>
            <span className={settingsUi.value}>
              {status.state === 'checking' && t('common.checking')}
              {status.state === 'missing' && t('extension.notInstalled')}
              {status.state === 'present' && (
                <>
                  <span className={settingsUi.dot} aria-hidden="true" />
                  {t('extension.installed', { version: status.extension.info.version })}
                </>
              )}
            </span>
          </div>
          {update.state !== 'unknown' && (
            <div className={settingsUi.row}>
              <span>{t('extension.latest')}</span>
              <span className={settingsUi.value}>
                v{update.latest.version}
                {update.state === 'current' && t('extension.upToDate')}
              </span>
            </div>
          )}
          {status.state === 'present' && (
            <div className={settingsUi.row}>
              <span>{t('extension.connection')}</span>
              <span className={settingsUi.value}>
                {(status.extension.bridge.transport === 'external' ? t('extension.direct') : t('extension.pageRelay')) + ' · '}
                {status.extension.info.browser === 'firefox' ? 'Firefox' : 'Chromium'}
              </span>
            </div>
          )}
        </section>
        {update.state === 'available' && (
          <>
            <a
              className={`${settingsUi.group} ${settingsUi.download} mt-3`}
              href={forUpdate.href}
              download
              onClick={() => setUpdating(true)}
            >
              <span className={settingsUi.updateDot} aria-hidden="true" />
              <span className={settingsUi.downloadText}>
                <span className={settingsUi.downloadTitle}>
                  {t('extension.updateTo', { version: update.latest.version })}
                </span>
                <span className={settingsUi.downloadDetail}>
                  {t('extension.updateDetail', {
                    installed: update.installed,
                    browser: forUpdate.browser === 'firefox' ? 'Firefox' : 'Chrome',
                  })}
                </span>
              </span>
              <ChevronRight size={16} strokeWidth={2.4} className={settingsUi.chevron} />
            </a>
            {updating && (
              <ol className={settingsUi.steps}>
                {forUpdate.updateSteps.map((step) => (
                  <li key={step}>{step}</li>
                ))}
              </ol>
            )}
          </>
        )}
        <p className={settingsUi.footer}>{t('extension.footer')}</p>

        <h2 className={settingsUi.sectionHeader}>{t('extension.getTheExtension')}</h2>
        <section className={settingsUi.group}>
          {downloads.map(({ browser, titleKey, detailKey, href, Icon }) => (
            <a
              key={browser}
              className={settingsUi.download}
              href={href}
              download
              onClick={() => setChosen(browser)}
            >
              <Icon size={28} className="text-primary" />
              <span className={settingsUi.downloadText}>
                <span className={settingsUi.downloadTitle}>{t(titleKey)}</span>
                <span className={settingsUi.downloadDetail}>
                  {t(detailKey)}
                  {browser === currentBrowser && t('extension.thisBrowser')}
                </span>
              </span>
              <ChevronRight size={16} strokeWidth={2.4} className={settingsUi.chevron} />
            </a>
          ))}
        </section>
        {steps ? (
          <ol className={settingsUi.steps}>
            {steps.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
        ) : (
          <p className={settingsUi.footer}>{t('extension.pickBrowserFooter')}</p>
        )}

        <h2 className={settingsUi.sectionHeader}>{t('extension.integrations')}</h2>
        <section className={settingsUi.group}>
          {INTEGRATIONS.map((integration) => (
            <div key={integration.name} className={settingsUi.integration}>
              <span className={settingsUi.integrationIcon} style={{ backgroundColor: integration.color }}>
                <integration.Icon size={15} />
              </span>
              <span className={settingsUi.integrationName}>{integration.name}</span>
              {status.state === 'present' && integration.capability === 'spotify' ? (
                <SpotifyIntegration />
              ) : status.state === 'present' && integration.capability === 'discord' ? (
                <DiscordIntegration />
              ) : (
                <span className={settingsUi.value}>{integrationStatus(status, integration, auth, t)}</span>
              )}
            </div>
          ))}
        </section>
        {auth.state === 'ready' && auth.auth.problem && (
          <p className={settingsUi.footer}>YouTube Music: {auth.auth.problem}.</p>
        )}
        <p className={settingsUi.footer}>{t('extension.privacyFooter')}</p>
        <p className={settingsUi.footer}>
          {t('extension.moreIntegrationsInSettings')}{' '}
          <PrefetchLink to="/settings" className={settingsUi.inlineLink}>
            {t('nav.settings')}
          </PrefetchLink>
          .
        </p>
      </div>
    </div>
  );
}
