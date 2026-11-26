import markSvg from '@/assets/logo-mark.svg?raw';

// SoundChord's audio waveform mark, matching the app's visual identity,
// recoloured to currentColor so it follows the text colour it sits in.
const markup = markSvg.replace(/width="\d+" height="\d+"/, 'width="100%" height="100%"');

export function LogoMark({ size = 20, className }: { size?: number; className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={className}
      style={{ display: 'inline-flex', width: size, height: (size * 497) / 720 }}
      dangerouslySetInnerHTML={{ __html: markup }}
    />
  );
}
