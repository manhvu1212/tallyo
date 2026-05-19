import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Localization from 'expo-localization';
import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';

const STORAGE_KEY = 'tallyo:locale:v1';

export type Locale = 'vi' | 'en';
export const LOCALES: readonly Locale[] = ['vi', 'en'] as const;
export const LOCALE_NAMES: Record<Locale, string> = {
  vi: 'Tiếng Việt',
  en: 'English',
};

type Strings = Record<string, string>;

const VI: Strings = {
  'common.cancel': 'Huỷ',
  'app.subtitle': 'Ghi điểm cho mọi cuộc chơi',

  'sessions.empty.title': 'Chưa có buổi chơi nào',
  'sessions.empty.hint':
    'Nhấn nút "+" ở góc dưới để bắt đầu buổi chơi mới với nhóm của bạn.',
  'sessions.new.fab': 'Buổi mới',
  'sessions.leader': 'Dẫn đầu',
  'sessions.meta.people': '{{count}} người',
  'sessions.meta.rounds': '{{count}} ván',
  'sessions.meta.zeroSum': 'Tổng = 0',
  'sessions.delete.message': 'Xoá khỏi máy? Hành động này không thể hoàn tác.',
  'sessions.delete.confirm': 'Xoá buổi chơi',

  'date.today': 'Hôm nay, {{time}}',
  'date.yesterday': 'Hôm qua',

  'new.title': 'Buổi chơi mới',
  'new.name.label': 'Tên buổi chơi',
  'new.name.placeholder': 'VD: Tối thứ 7 nhà Hùng',
  'new.players.label': 'Người chơi',
  'new.players.hint': 'Tối thiểu 2 người',
  'new.players.placeholder': 'Tên người chơi',
  'new.players.add': 'Thêm',
  'new.players.empty': 'Chưa có người chơi nào.',
  'new.players.duplicate.title': 'Tên đã có',
  'new.players.duplicate.message': '"{{name}}" đã có trong danh sách.',
  'new.options.label': 'Tuỳ chọn ghi điểm',
  'new.options.zeroSum.title': 'Tổng điểm mỗi ván = 0',
  'new.options.zeroSum.desc':
    'Bật khi điểm người thắng đúng bằng tổng điểm người thua trừ. Để trống 1 ô khi ghi điểm, app tự cân cho người cuối. Hợp với Tiến lên đếm lá, Phỏm, Mạt chược.',
  'new.submit': 'Tạo buổi chơi',
  'new.minPlayers': 'Cần ít nhất 2 người chơi',

  'detail.title.fallback': 'Buổi chơi',
  'detail.notFound': 'Buổi chơi không tồn tại',
  'detail.stats': 'Thống kê',
  'detail.board.title': 'Bảng điểm',
  'detail.resting': 'Tạm nghỉ',
  'detail.addPlayer.cta': '+ Thêm người chơi',
  'detail.addPlayer.placeholder': 'Tên người mới',
  'detail.addPlayer.submit': 'Thêm',
  'detail.rounds.empty': 'Chưa có ván nào',
  'detail.rounds.count': '{{count}} ván đã chơi',
  'detail.round.idx': 'Ván #{{n}}',
  'detail.firstRound.hint':
    'Nhấn "Thêm ván" ở dưới để ghi điểm ván đầu tiên.',
  'detail.addRound.fab': 'Thêm ván',
  'detail.menu.rest': 'Tạm nghỉ',
  'detail.menu.resume': 'Chơi tiếp',
  'detail.menu.deleteRound': 'Xoá ván',

  'round.title.new': 'Thêm ván',
  'round.title.edit': 'Sửa ván',
  'round.hint.zeroSum': 'Tổng điểm phải = 0. Để trống 1 ô để app tự cân.',
  'round.hint.free':
    'Nhập điểm cho từng người. Có thể là số âm. Bỏ trống = 0.',
  'round.note.label': 'Ghi chú (tuỳ chọn)',
  'round.note.placeholder': 'VD: ván tới chia bài lại',
  'round.save.new': 'Lưu ván',
  'round.save.edit': 'Cập nhật ván',
  'round.notFound': 'Buổi chơi không tồn tại.',
  'round.invalid.title': 'Điểm không hợp lệ',
  'round.invalid.message': 'Có ô chứa ký tự không phải số.',
  'round.empty.title': 'Chưa có dữ liệu',
  'round.empty.message': 'Cần nhập điểm cho ít nhất 1 người.',
  'round.needMore.title': 'Cần nhập đủ',
  'round.needMore.message':
    'Bật "Tổng = 0" thì cần nhập điểm cho ít nhất {{n}} người.',
  'round.unbalanced.title': 'Tổng phải bằng 0',
  'round.unbalanced.message':
    'Tổng hiện tại là {{sum}}. Vui lòng chỉnh lại.',

  'stats.title': 'Thống kê',
  'stats.empty.title': 'Chưa có dữ liệu',
  'stats.empty.message':
    'Thêm vài ván chơi rồi quay lại đây để xem thống kê.',
  'stats.kpi.rounds': 'Số ván',
  'stats.kpi.exchanged': 'Tổng điểm trao đổi',
  'stats.patterns.title': 'Pattern nổi bật',
  'stats.table.title': 'Bảng chi tiết',
  'stats.table.player': 'Người chơi',
  'stats.table.total': 'Tổng',
  'stats.table.wins': 'Thắng',
  'stats.table.losses': 'Thua',
  'stats.table.avg': 'TB/ván',
  'stats.leader.single': '{{name}} dẫn đầu',
  'stats.leader.multi': '{{names}} đồng dẫn đầu',
  'stats.leader.desc': '{{points}} điểm sau {{rounds}} ván',
  'stats.sweep.title': '{{name}} thắng cả {{rounds}} ván',
  'stats.sweep.desc': 'Nhất ăn tất, không cho ai cơ hội.',
  'stats.blowout': 'Ván chênh lệch nhất: ván #{{n}}',
  'stats.closest': 'Ván sát nút nhất: ván #{{n}}',
  'stats.trailer.single': '{{name}} đang xếp cuối',
  'stats.trailer.multi': '{{names}} đồng xếp cuối',
  'stats.trailer.desc': '{{points}} điểm — gỡ gấp!',

  'lang.title': 'Ngôn ngữ',
};

const EN: Strings = {
  'common.cancel': 'Cancel',
  'app.subtitle': 'Score keeper for every game',

  'sessions.empty.title': 'No sessions yet',
  'sessions.empty.hint':
    'Tap the "+" button at the bottom to start a new session with your group.',
  'sessions.new.fab': 'New session',
  'sessions.leader': 'Leading',
  'sessions.meta.people': '{{count}} player',
  'sessions.meta.people_other': '{{count}} players',
  'sessions.meta.rounds': '{{count}} round',
  'sessions.meta.rounds_other': '{{count}} rounds',
  'sessions.meta.zeroSum': 'Sum = 0',
  'sessions.delete.message': 'Delete from device? This cannot be undone.',
  'sessions.delete.confirm': 'Delete session',

  'date.today': 'Today, {{time}}',
  'date.yesterday': 'Yesterday',

  'new.title': 'New session',
  'new.name.label': 'Session name',
  'new.name.placeholder': 'e.g. Saturday at Hung\'s',
  'new.players.label': 'Players',
  'new.players.hint': 'At least 2 players',
  'new.players.placeholder': 'Player name',
  'new.players.add': 'Add',
  'new.players.empty': 'No players yet.',
  'new.players.duplicate.title': 'Name already used',
  'new.players.duplicate.message': '"{{name}}" is already in the list.',
  'new.options.label': 'Scoring options',
  'new.options.zeroSum.title': 'Round totals zero',
  'new.options.zeroSum.desc':
    "Turn on when the winner's gain equals what the losers give up. Leave one cell empty when entering scores and the app fills in the last player. Good for Tiến lên, Phỏm, Mahjong.",
  'new.submit': 'Create session',
  'new.minPlayers': 'Need at least 2 players',

  'detail.title.fallback': 'Session',
  'detail.notFound': 'Session not found',
  'detail.stats': 'Stats',
  'detail.board.title': 'Scoreboard',
  'detail.resting': 'Resting',
  'detail.addPlayer.cta': '+ Add player',
  'detail.addPlayer.placeholder': 'New player name',
  'detail.addPlayer.submit': 'Add',
  'detail.rounds.empty': 'No rounds yet',
  'detail.rounds.count': '{{count}} round played',
  'detail.rounds.count_other': '{{count}} rounds played',
  'detail.round.idx': 'Round #{{n}}',
  'detail.firstRound.hint':
    'Tap "Add round" below to record the first round.',
  'detail.addRound.fab': 'Add round',
  'detail.menu.rest': 'Rest',
  'detail.menu.resume': 'Resume',
  'detail.menu.deleteRound': 'Delete round',

  'round.title.new': 'Add round',
  'round.title.edit': 'Edit round',
  'round.hint.zeroSum':
    'Total must be 0. Leave one cell empty for the app to balance.',
  'round.hint.free':
    'Enter a score for each player. Can be negative. Empty = 0.',
  'round.note.label': 'Note (optional)',
  'round.note.placeholder': 'e.g. reshuffle next round',
  'round.save.new': 'Save round',
  'round.save.edit': 'Update round',
  'round.notFound': 'Session not found.',
  'round.invalid.title': 'Invalid score',
  'round.invalid.message': 'A cell contains non-numeric characters.',
  'round.empty.title': 'No data',
  'round.empty.message': 'Enter a score for at least 1 player.',
  'round.needMore.title': 'Not enough',
  'round.needMore.message':
    'With "Sum = 0" enabled, enter scores for at least {{n}} players.',
  'round.unbalanced.title': 'Total must be 0',
  'round.unbalanced.message':
    'Current total is {{sum}}. Please adjust.',

  'stats.title': 'Stats',
  'stats.empty.title': 'No data',
  'stats.empty.message':
    'Add some rounds and come back to see stats.',
  'stats.kpi.rounds': 'Rounds',
  'stats.kpi.exchanged': 'Points exchanged',
  'stats.patterns.title': 'Highlights',
  'stats.table.title': 'Details',
  'stats.table.player': 'Player',
  'stats.table.total': 'Total',
  'stats.table.wins': 'Wins',
  'stats.table.losses': 'Losses',
  'stats.table.avg': 'Avg/round',
  'stats.leader.single': '{{name}} is leading',
  'stats.leader.multi': '{{names}} tied for the lead',
  'stats.leader.desc': '{{points}} points after {{rounds}} rounds',
  'stats.sweep.title': '{{name}} won all {{rounds}} rounds',
  'stats.sweep.desc': 'Clean sweep — no one had a chance.',
  'stats.blowout': 'Biggest blowout: round #{{n}}',
  'stats.closest': 'Closest round: round #{{n}}',
  'stats.trailer.single': '{{name}} is at the bottom',
  'stats.trailer.multi': '{{names}} tied for last',
  'stats.trailer.desc': '{{points}} points — comeback time!',

  'lang.title': 'Language',
};

const STRINGS: Record<Locale, Strings> = { vi: VI, en: EN };

function detectDeviceLocale(): Locale {
  const locales = Localization.getLocales();
  for (const loc of locales) {
    const code = (loc.languageCode ?? '').toLowerCase();
    if (code === 'vi') return 'vi';
    if (code === 'en') return 'en';
  }
  return 'vi';
}

function format(str: string, params?: Record<string, string | number>): string {
  if (!params) return str;
  return str.replace(/{{(\w+)}}/g, (_, key) => {
    const v = params[key];
    return v === undefined ? '' : String(v);
  });
}

interface I18nContextValue {
  locale: Locale;
  ready: boolean;
  setLocale: (loc: Locale) => void;
  t: (key: string, params?: Record<string, string | number>) => string;
}

const I18nContext = createContext<I18nContextValue | null>(null);

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>('vi');
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let mounted = true;
    AsyncStorage.getItem(STORAGE_KEY).then((stored) => {
      if (!mounted) return;
      if (stored === 'vi' || stored === 'en') setLocaleState(stored);
      else setLocaleState(detectDeviceLocale());
      setReady(true);
    });
    return () => {
      mounted = false;
    };
  }, []);

  const setLocale = useCallback((loc: Locale) => {
    setLocaleState(loc);
    AsyncStorage.setItem(STORAGE_KEY, loc).catch(() => {});
  }, []);

  const t = useCallback<I18nContextValue['t']>(
    (key, params) => {
      const map = STRINGS[locale];
      let lookup = key;
      if (params && params.count !== undefined && params.count !== 1) {
        const pluralKey = `${key}_other`;
        if (pluralKey in map) lookup = pluralKey;
      }
      const raw = map[lookup] ?? STRINGS.vi[lookup] ?? STRINGS.vi[key] ?? key;
      return format(raw, params);
    },
    [locale],
  );

  const value = useMemo<I18nContextValue>(
    () => ({ locale, ready, setLocale, t }),
    [locale, ready, setLocale, t],
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nContextValue {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error('useI18n must be used inside I18nProvider');
  return ctx;
}
