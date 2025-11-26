import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useDarkMode } from '@/hooks/useDarkMode';

export const SettingsPage = () => {
  const { t, i18n } = useTranslation();
  const { theme, toggleTheme } = useDarkMode();

  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng);
    localStorage.setItem('language', lng);
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">{t('settings.title')}</h2>
        <p className="text-muted-foreground">Manage your preferences</p>
      </div>

      <div className="grid gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Appearance</CardTitle>
            <CardDescription>Customize how the app looks</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label>{t('settings.theme')}</Label>
                <p className="text-sm text-muted-foreground">
                  {theme === 'dark' ? t('settings.dark') : t('settings.light')} mode
                </p>
              </div>
              <Switch checked={theme === 'dark'} onCheckedChange={toggleTheme} />
            </div>

            <div className="space-y-2">
              <Label>{t('settings.language')}</Label>
              <Select value={i18n.language} onValueChange={changeLanguage}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="en">English</SelectItem>
                  <SelectItem value="zh-TW">繁體中文</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Trading</CardTitle>
            <CardDescription>Trading mode and preferences</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-2">
              <Label>{t('settings.tradingMode')}</Label>
              <Select
                value={localStorage.getItem('trading_mode') || 'SIMULATION'}
                onValueChange={(value) => localStorage.setItem('trading_mode', value)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SIMULATION">{t('settings.simulation')}</SelectItem>
                  <SelectItem value="LIVE">{t('settings.live')}</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                Simulation mode for testing, Live mode for real trading
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
