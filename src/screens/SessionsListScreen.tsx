import React, { useMemo } from 'react';
import {
  Alert,
  FlatList,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Card } from '../components/Card';
import { DotsButton } from '../components/DotsButton';
import { EmptyState } from '../components/EmptyState';
import { FAB } from '../components/FAB';
import { ScreenProps } from '../navigation';
import { useStore } from '../store';
import { colors, font, spacing } from '../theme';

function formatDate(ts: number): string {
  const d = new Date(ts);
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);

  const sameDay = (a: Date, b: Date) =>
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate();

  if (sameDay(d, today)) return `Hôm nay, ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  if (sameDay(d, yesterday)) return 'Hôm qua';
  return `${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`;
}

export function SessionsListScreen({ navigation }: ScreenProps<'SessionsList'>) {
  const { sessions, ready, deleteSession } = useStore();

  const sorted = useMemo(
    () => [...sessions].sort((a, b) => b.updatedAt - a.updatedAt),
    [sessions],
  );

  const openMenu = (id: string, name: string) => {
    Alert.alert(
      name,
      'Xoá khỏi máy? Hành động này không thể hoàn tác.',
      [
        { text: 'Huỷ', style: 'cancel' },
        {
          text: 'Xoá buổi chơi',
          style: 'destructive',
          onPress: () => deleteSession(id),
        },
      ],
    );
  };

  return (
    <SafeAreaView style={styles.screen} edges={['top', 'left', 'right']}>
      <View style={styles.header}>
        <Text style={styles.appTitle}>Tallyo</Text>
        <Text style={styles.appSubtitle}>Ghi điểm cho mọi cuộc chơi</Text>
      </View>

      {!ready ? null : sorted.length === 0 ? (
        <EmptyState
          title="Chưa có buổi chơi nào"
          subtitle={'Nhấn nút "+" ở góc dưới để bắt đầu buổi chơi mới với nhóm của bạn.'}
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
                    <Text style={styles.meta}>
                      {item.players.length} người · {item.rounds.length} ván
                      {item.config.zeroSum ? ' · Tổng = 0' : ''}
                    </Text>
                    <Text style={styles.metaDim}>{formatDate(item.updatedAt)}</Text>
                  </View>
                  {leader && item.rounds.length > 0 ? (
                    <View style={styles.leaderBadge}>
                      <Text style={styles.leaderLabel}>Dẫn đầu</Text>
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

      <FAB label="Buổi mới" onPress={() => navigation.navigate('NewSession')} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  header: {
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
