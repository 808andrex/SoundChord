// Zips the built extension for the site's Extension page and writes the
// manifest the page checks for updates:
//
//   web/app/public/soundchord-extension-chrome.zip
//   web/app/public/soundchord-extension-firefox.zip
//   web/app/public/extension.json
//   web/app/public/downloads/soundchord-extension-chrome.zip
//   web/app/public/downloads/soundchord-extension-firefox.zip
//   web/app/public/downloads/soundchord-extension-chrome (<version>).zip
//   web/app/public/downloads/soundchord-extension-firefox (<version>).zip
//   web/app/public/downloads/extension.json    { version, builtAt, files }
//
// Each zip holds a single folder (soundchord-extension-<browser> (<version>)/) rather than
// loose files, so "Extract here" leaves one tidy folder to load unpacked.

import { mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { zipSync } from 'fflate';

const output = fileURLToPath(new URL('../.output/', import.meta.url));
const publicDir = fileURLToPath(new URL('../../app/public/', import.meta.url));
const downloadsDir = join(publicDir, 'downloads');

mkdirSync(publicDir, { recursive: true });
mkdirSync(downloadsDir, { recursive: true });

// Clean previous extension zips in both public and downloads
for (const dir of [publicDir, downloadsDir]) {
  for (const name of readdirSync(dir)) {
    if (/^(soundchord|bitchord)-extension-.*\.zip$/.test(name)) {
      rmSync(join(dir, name));
    }
  }
}

function filesUnder(dir) {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    return statSync(path).isDirectory() ? filesUnder(path) : [path];
  });
}

const builds = { chrome: 'chrome-mv3', firefox: 'firefox-mv3' };
const files = {};
let version;

for (const [browser, folder] of Object.entries(builds)) {
  const root = join(output, folder);
  const manifest = JSON.parse(readFileSync(join(root, 'manifest.json'), 'utf8'));
  if (version && manifest.version !== version) {
    throw new Error(`Chrome and Firefox builds disagree on the version (${version} vs ${manifest.version}); rebuild both`);
  }
  version = manifest.version;

  // The version in brackets, on the zip and the folder inside it: "soundchord-extension-chrome (0.4.3)".
  const parent = `soundchord-extension-${browser} (${manifest.version})`;
  const entries = {};
  for (const file of filesUnder(root)) {
    entries[`${parent}/${relative(root, file).split(sep).join('/')}`] = readFileSync(file);
  }

  const standardName = `soundchord-extension-${browser}.zip`;
  const versionedName = `${parent}.zip`;
  const zipBuffer = zipSync(entries, { level: 9 });

  // 1. Direct file at web/app/public/ (served at /soundchord-extension-<browser>.zip)
  writeFileSync(join(publicDir, standardName), zipBuffer);
  // 2. Compatibility copies in web/app/public/downloads/
  writeFileSync(join(downloadsDir, standardName), zipBuffer);
  writeFileSync(join(downloadsDir, versionedName), zipBuffer);

  files[browser] = standardName;
  console.log(`${folder} -> app/public/${standardName} and downloads/ (${parent}/, v${version})`);
}

const manifestPayload = `${JSON.stringify({ version, builtAt: new Date().toISOString(), files }, null, 2)}\n`;
writeFileSync(join(publicDir, 'extension.json'), manifestPayload);
writeFileSync(join(downloadsDir, 'extension.json'), manifestPayload);
console.log(`app/public/extension.json and app/public/downloads/extension.json (v${version})`);
