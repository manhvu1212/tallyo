import React, { useMemo } from 'react';
import {
  Alert,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Card } from '../components/Card';
import { DotsButton } from '../components/DotsButton';
import { EmptyState } from '../components/EmptyState';
import { FAB } from '../components/FAB';
import { LOCALES, LOCALE_NAMES, useI18n, type Locale } from '../i18n';
import { ScreenProps } from '../navigation';
import { useStore } from '../store';
import { colors, font, spacing } from '../theme';

function useFormatDate() {
  const { t } = useI18n();
  return (ts: number): string => {
    const d = new Date(ts);
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);

    const sameDay = (a: Date, b: Date) =>
      a.getFullYear() === b.getFullYear() &&
      a.getMonth() === b.getMonth() &&
      a.getDate() === b.getDate();

    if (sameDay(d, today)) {
      const time = `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
      return t('date.today', { time });
    }
    if (sameDay(d, yesterday)) return t('date.yesterday');
    return `${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`;
  };
}

export function SessionsListScreen({ navigation }: ScreenProps<'SessionsList'>) {
  const { sessions, ready, deleteSession } = useStore();
  const { t, locale, setLocale } = useI18n();
  const formatDate = useFormatDate();

  const sorted = useMemo(
    () => [...sessions].sort((a, b) => b.updatedAt - a.updatedAt),
    [sessions],
  );

  const openMenu = (id: string, name: string) => {
    Alert.alert(name, t('sessions.delete.message'), [
      { text: t('common.cancel'), style: 'cancel' },
      {
        text: t('sessions.delete.confirm'),
        style: 'destructive',
        onPress: () => deleteSession(id),
      },
    ]);
  };

  const openLangPicker = () => {
    Alert.alert(t('lang.title'), undefined, [
      { text: t('common.cancel'), style: 'cancel' },
      ...LOCALES.filter((l) => l !== locale).map((l: Locale) => ({
        text: LOCALE_NAMES[l],
        onPress: () => setLocale(l),
      })),
    ]);
  };

  return (
    <SafeAreaView style={styles.screen} edges={['top', 'left', 'right']}>
      <View style={styles.header}>
        <View style={{ flex: 1 }}>
          <Text style={styles.appTitle}>Tallyo</Text>
          <Text style={styles.appSubtitle}>{t('app.subtitle')}</Text>
        </View>
        <Pressable
          onPress={openLangPicker}
          hitSlop={10}
          style={({ pressed }) => [styles.langBtn, pressed && { opacity: 0.6 }]}
        >
          <Text style={styles.langGlyph}>🌐</Text>
        </Pressable>
      </View>

      {!ready ? null : sorted.length === 0 ? (
        <EmptyState
          title={t('sessions.empty.title')}
          subtitle={t('sessions.empty.hint')}
        />
      ) : (
        <FlatList
          data={sorted}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => {
            const totals: Record<string, number> = {};
            for (const r of item.rounds) {
              for (const s of r.scores) {
                totals[s.playerId] = (totals[s.playerId] ?? 0) + s.points;
              }
            }
            const leader = item.players
              .map((p) => ({ name: p.name, pts: totals[p.id] ?? 0 }))
              .sort((a, b) => b.pts - a.pts)[0];
            const metaParts = [
              t('sessions.meta.people', { count: item.players.length }),
              t('sessions.meta.rounds', { count: item.rounds.length }),
              item.config.zeroSum ? t('sessions.meta.zeroSum') : null,
            ].filter(Boolean);
            return (
              <Card
                onPress={() =>
                  navigation.navigate('SessionDetail', { sessionId: item.id })
                }
                onLongPress={() => openMenu(item.id, item.name)}
              >
                <View style={styles.cardRow}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.sessionName} numberOfLines={1}>
                      {item.name}
                    </Text>
                    <Text style={styles.meta}>{metaParts.join(' · ')}</Text>
                    <Text style={styles.metaDim}>{formatDate(item.updatedAt)}</Text>
                  </View>
                  {leader && item.rounds.length > 0 ? (
                    <View style={styles.leaderBadge}>
                      <Text style={styles.leaderLabel}>{t('sessions.leader')}</Text>
                      <Text style={styles.leaderName} numberOfLines={1}>
                        {leader.name}
                      </Text>
                      <Text style={styles.leaderPts}>
                        {leader.pts > 0 ? '+' : ''}
                        {leader.pts}
                      </Text>
                    </View>
                  ) : null}
                  <DotsButton onPress={() => openMenu(item.id, item.name)} />
                </View>
              </Card>
            );
          }}
          ItemSeparatorComponent={() => <View style={{ height: spacing.md }} />}
        />
      )}

      <FAB
        label={t('sessions.new.fab')}
        onPress={() => navigation.navigate('NewSession')}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.lg,
    paddingBottom: spacing.md,
  },
  appTitle: {
    color: colors.text,
    fontSize: font.h1,
    fontWeight: '700',
  },
  appSubtitle: {
    color: colors.textMuted,
    fontSize: font.small,
    marginTop: 2,
  },
  langBtn: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  langGlyph: {
    fontSize: 22,
  },
  list: {
    paddingHorizontal: spacing.lg,
    paddingBottom: 120,
  },
  cardRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  sessionName: {
    color: colors.text,
    fontSize: font.h3,
    fontWeight: '600',
  },
  meta: {
    color: colors.textMuted,
    fontSize: font.small,
    marginTop: 4,
  },
  metaDim: {
    color: colors.textMuted,
    fontSize: font.tiny,
    marginTop: 2,
    opacity: 0.7,
  },
  leaderBadge: {
    alignItems: 'flex-end',
    minWidth: 80,
  },
  leaderLabel: {
    color: colors.textMuted,
    fontSize: font.tiny,
  },
  leaderName: {
    color: colors.text,
    fontSize: font.small,
    fontWeight: '600',
    maxWidth: 100,
  },
  leaderPts: {
    color: colors.accent,
    fontSize: font.h3,
    fontWeight: '700',
  },
});
