import { useTranslation } from 'react-i18next';
import { useOnlineStatus } from '../useOnlineStatus';

export function OfflineBanner() {
  const online = useOnlineStatus();
  const { t } = useTranslation();
  if (online) return null;
  return (
    <div role="status" className="bg-warn-soft px-4 py-2 text-center text-sm text-warn">
      {t('offline')}
    </div>
  );
}
