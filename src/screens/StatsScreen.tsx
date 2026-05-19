import React, { useLayoutEffect, useMemo } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { useI18n } from '../i18n';
import { ScreenProps } from '../navigation';
import { computeInsights, computePlayerStats } from '../stats';
import { useStore } from '../store';
import { colors, font, radius, spacing } from '../theme';

export function StatsScreen({ route, navigation }: ScreenProps<'Stats'>) {
  const { sessionId } = route.params;
  const { getSession } = useStore();
  const { t } = useI18n();
  const session = getSession(sessionId);

  useLayoutEffect(() => {
    navigation.setOptions({ title: t('stats.title') });
  }, [navigation, t]);

  const stats = useMemo(() => (session ? computePlayerStats(session) : []), [session]);
  const insights = useMemo(() => (session ? computeInsights(session) : undefined), [session]);
  const ranked = useMemo(
    () => [...stats].sort((a, b) => b.totalPoints - a.totalPoints),
    [stats],
  );

  if (!session) {
    return (
      <SafeAreaView style={styles.screen}>
        <EmptyState title={t('detail.notFound')} />
      </SafeAreaView>
    );
  }

  if (session.rounds.length === 0) {
    return (
      <SafeAreaView style={styles.screen}>
        <EmptyState
          title={t('stats.empty.title')}
          subtitle={t('stats.empty.message')}
        />
      </SafeAreaView>
    );
  }

  const totalRounds = session.rounds.length;

  return (
    <SafeAreaView style={styles.screen} edges={['left', 'right', 'bottom']}>
      <ScrollView contentContainerStyle={styles.body}>
        {/* Headline insights */}
        <View style={styles.kpiRow}>
          <KPI label={t('stats.kpi.rounds')} value={String(totalRounds)} />
          <KPI
            label={t('stats.kpi.exchanged')}
            value={String(insights?.totalPointsExchanged ?? 0)}
          />
        </View>

        {/* Patterns */}
        <Card>
          <Text style={styles.cardTitle}>{t('stats.patterns.title')}</Text>
          <View style={{ gap: spacing.sm, marginTop: spacing.sm }}>
            {insights && insights.leaders.length > 0 ? (
              <Pattern
                emoji="👑"
                title={
                  insights.leaders.length === 1
                    ? t('stats.leader.single', { name: insights.leaders[0].name })
                    : t('stats.leader.multi', {
                        names: insights.leaders.map((p) => p.name).join(', '),
                      })
                }
                desc={t('stats.leader.desc', {
                  points: `${insights.leaders[0].totalPoints > 0 ? '+' : ''}${insights.leaders[0].totalPoints}`,
                  rounds: totalRounds,
                })}
              />
            ) : null}
            {insights?.sweepPlayer ? (
              <Pattern
                emoji="🔥"
                title={t('stats.sweep.title', {
                  name: insights.sweepPlayer.name,
                  rounds: totalRounds,
                })}
                desc={t('stats.sweep.desc')}
              />
            ) : null}
            {insights?.biggestBlowoutRoundIndex !== undefined ? (
              <Pattern
                emoji="💥"
                title={t('stats.blowout', {
                  n: insights.biggestBlowoutRoundIndex + 1,
                })}
                desc={describeRound(session, insights.biggestBlowoutRoundIndex)}
              />
            ) : null}
            {insights?.closestRoundIndex !== undefined &&
            insights.closestRoundIndex !== insights.biggestBlowoutRoundIndex ? (
              <Pattern
                emoji="🤝"
                title={t('stats.closest', {
                  n: insights.closestRoundIndex + 1,
                })}
                desc={describeRound(session, insights.closestRoundIndex)}
              />
            ) : null}
            {insights && insights.trailers.length > 0 ? (
              <Pattern
                emoji="🥶"
                title={
                  insights.trailers.length === 1
                    ? t('stats.trailer.single', { name: insights.trailers[0].name })
                    : t('stats.trailer.multi', {
                        names: insights.trailers.map((p) => p.name).join(', '),
                      })
                }
                desc={t('stats.trailer.desc', {
                  points: insights.trailers[0].totalPoints,
                })}
              />
            ) : null}
          </View>
        </Card>

        {/* Per-player table */}
        <Card>
          <Text style={styles.cardTitle}>{t('stats.table.title')}</Text>
          <View style={styles.tableHeader}>
            <Text style={[styles.th, { flex: 2 }]}>{t('stats.table.player')}</Text>
            <Text style={styles.th}>{t('stats.table.total')}</Text>
            <Text style={styles.th}>{t('stats.table.wins')}</Text>
            <Text style={styles.th}>{t('stats.table.losses')}</Text>
            <Text style={styles.th}>{t('stats.table.avg')}</Text>
          </View>
          {ranked.map((p, i) => (
            <View
              key={p.playerId}
              style={[styles.tr, i === ranked.length - 1 && { borderBottomWidth: 0 }]}
            >
              <Text style={[styles.td, { flex: 2, color: colors.text, fontWeight: '500' }]} numberOfLines={1}>
                {p.name}
              </Text>
              <Text
                style={[
                  styles.td,
                  p.totalPoints > 0 && { color: colors.win },
                  p.totalPoints < 0 && { color: colors.loss },
                  { fontWeight: '700' },
                ]}
              >
                {p.totalPoints > 0 ? '+' : ''}
                {p.totalPoints}
              </Text>
              <Text style={styles.td}>{p.wins}</Text>
              <Text style={styles.td}>{p.losses}</Text>
              <Text style={styles.td}>{p.averagePerRound.toFixed(1)}</Text>
            </View>
          ))}
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}

function KPI({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.kpi}>
      <Text style={styles.kpiValue}>{value}</Text>
      <Text style={styles.kpiLabel}>{label}</Text>
    </View>
  );
}

function Pattern({ emoji, title, desc }: { emoji: string; title: string; desc: string }) {
  return (
    <View style={styles.patternRow}>
      <Text style={styles.patternEmoji}>{emoji}</Text>
      <View style={{ flex: 1 }}>
        <Text style={styles.patternTitle}>{title}</Text>
        <Text style={styles.patternDesc}>{desc}</Text>
      </View>
    </View>
  );
}

function describeRound(session: ReturnType<typeof useStore>['sessions'][number], idx: number): string {
  const r = session.rounds[idx];
  if (!r) return '';
  const max = Math.max(...r.scores.map((s) => s.points));
  const min = Math.min(...r.scores.map((s) => s.points));
  const topName = session.players.find((p) =>
    r.scores.find((s) => s.playerId === p.id && s.points === max),
  )?.name;
  const botName = session.players.find((p) =>
    r.scores.find((s) => s.playerId === p.id && s.points === min),
  )?.name;
  return `${topName ?? '?'} (${max > 0 ? '+' : ''}${max}) vs ${botName ?? '?'} (${min > 0 ? '+' : ''}${min})`;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  body: { padding: spacing.lg, gap: spacing.lg, paddingBottom: spacing.xxl },
  kpiRow: { flexDirection: 'row', gap: spacing.md },
  kpi: {
    flex: 1,
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.lg,
    alignItems: 'center',
  },
  kpiValue: { color: colors.text, fontSize: font.h1, fontWeight: '700' },
  kpiLabel: { color: colors.textMuted, fontSize: font.tiny, marginTop: 4 },
  cardTitle: {
    color: colors.textMuted,
    fontSize: font.small,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  patternRow: {
    flexDirection: 'row',
    gap: spacing.md,
    backgroundColor: colors.surfaceAlt,
    borderRadius: radius.md,
    padding: spacing.md,
    alignItems: 'center',
  },
  patternEmoji: { fontSize: 24 },
  patternTitle: { color: colors.text, fontSize: font.body, fontWeight: '600' },
  patternDesc: { color: colors.textMuted, fontSize: font.small, marginTop: 2 },
  tableHeader: {
    flexDirection: 'row',
    marginTop: spacing.md,
    paddingBottom: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  th: {
    flex: 1,
    color: colors.textMuted,
    fontSize: font.tiny,
    textTransform: 'uppercase',
    textAlign: 'right',
  },
  tr: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  td: {
    flex: 1,
    color: colors.textMuted,
    fontSize: font.small,
    textAlign: 'right',
  },
});
