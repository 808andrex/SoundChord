import { PageHeader } from '@/components/PageHeader';
import { Shelves } from '@/components/music/ShelfView';
import { LoadError, NeedsExtension } from '@/components/music/States';
import { useExplore, useYouTubeReady } from '@/data/music/queries';
import { useTranslation } from '@/i18n';
import { NewSkeleton } from '@/skeletons/PageSkeletons';

/** New: YouTube Music's Explore (new albums and singles, trending, new videos). */
export default function NewPage() {
  const { t } = useTranslation();
  const { missing } = useYouTubeReady();
  const explore = useExplore();

  const title = t('nav.new');

  if (missing) {
    return (
      <>
        <PageHeader title={title} />
        <NeedsExtension />
      </>
    );
  }
  if (explore.isError) {
    return (
      <>
        <PageHeader title={title} />
        <LoadError error={explore.error} onRetry={() => void explore.refetch()} />
      </>
    );
  }
  if (!explore.data) return <NewSkeleton />;
  return (
    <>
      <PageHeader title={title} />
      <Shelves shelves={explore.data} />
    </>
  );
}
