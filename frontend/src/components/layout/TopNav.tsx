import { Moon, Sun, Globe, Settings, Menu } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useDarkMode } from '@/hooks/useDarkMode';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

interface TopNavProps {
  onMenuClick: () => void;
}

export const TopNav = ({ onMenuClick }: TopNavProps) => {
  const { i18n } = useTranslation();
  const { isDark, toggleTheme } = useDarkMode();

  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng);
    localStorage.setItem('language', lng);
  };

  return (
    <header className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="flex h-16 items-center px-4 gap-4">
        <Button
          variant="ghost"
          size="icon"
          className="lg:hidden"
          onClick={onMenuClick}
        >
          <Menu className="h-6 w-6" />
        </Button>

        <div className="flex items-center gap-2">
          <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-sm">VB</span>
          </div>
          <h1 className="text-xl font-bold hidden md:block">
            Value Investor Bot
          </h1>
        </div>

        <div className="flex-1" />

        <div className="flex items-center gap-2">
          <Select value={i18n.language} onValueChange={changeLanguage}>
            <SelectTrigger className="w-[120px]">
              <div className="flex items-center gap-2">
                <Globe className="h-4 w-4" />
                <SelectValue />
              </div>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="en">English</SelectItem>
              <SelectItem value="zh-TW">繁體中文</SelectItem>
            </SelectContent>
          </Select>

          <Button variant="ghost" size="icon" onClick={toggleTheme}>
            {isDark ? (
              <Sun className="h-5 w-5" />
            ) : (
              <Moon className="h-5 w-5" />
            )}
          </Button>

          <Button variant="ghost" size="icon">
            <Settings className="h-5 w-5" />
          </Button>
        </div>
      </div>
    </header>
  );
};
