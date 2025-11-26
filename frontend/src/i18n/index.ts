import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import enTranslations from './locales/en.json';
import zhTWTranslations from './locales/zh-TW.json';

// Get saved language or default to 'en'
const savedLanguage = localStorage.getItem('language') || 'en';

const resources = {
  en: enTranslations,
  'zh-TW': zhTWTranslations,
};

i18n
  .use(initReactI18next)
  .init({
    resources,
    lng: savedLanguage,
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false,
    },
  });

export default i18n;
