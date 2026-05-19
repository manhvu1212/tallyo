import React, { useLayoutEffect, useMemo, useState } from 'react';
import {
  Alert,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Card } from '../components/Card';
import { DotsButton } from '../components/DotsButton';
import { EmptyState } from '../components/EmptyState';
import { FAB } from '../components/FAB';
import { ScreenProps } from '../navigation';
import { computePlayerStats } from '../stats';
import { useStore } from '../store';
import { colors, font, radius, spacing } from '../theme';

export function SessionDetailScreen({ route, navigation }: ScreenProps<'SessionDetail'>) {
  const { sessionId } = route.params;
  const { getSession, deleteRound, addPlayer, setPlayerResting } = useStore();
  const session = getSession(sessionId);
  const [adding, setAdding] = useState(false);
  const [newName, setNewName] = useState('');

  useLayoutEffect(() => {
    navigation.setOptions({
      title: session?.name ?? 'Buổi chơi',
      headerRight: () =>
        session ? (
          <Pressable
            onPress={() => navigation.navigate('Stats', { sessionId })}
            hitSlop={12}
          >
            <Text style={styles.headerAction}>Thống kê</Text>
          </Pressable>
        ) : null,
    });
  }, [navigation, session, sessionId]);

  const stats = useMemo(() => (session ? computePlayerStats(session) : []), [session]);
  const ranked = useMemo(
    () => [...stats].sort((a, b) => b.totalPoints - a.totalPoints),
    [stats],
  );
  const roundsNewestFirst = useMemo(
    () =>
      session
        ? session.rounds.map((round, i) => ({ round, idx: i })).reverse()
        : [],
    [session],
  );

  if (!session) {
    return (
      <SafeAreaView style={styles.screen}>
        <EmptyState title="Buổi chơi không tồn tại" />
      </SafeAreaView>
    );
  }

  const openRoundMenu = (roundId: string, idx: number) => {
    Alert.alert(`Ván #${idx + 1}`, undefined, [
      { text: 'Huỷ', style: 'cancel' },
      {
        text: 'Xoá ván',
        style: 'destructive',
        onPress: () => deleteRound(sessionId, roundId),
      },
    ]);
  };

  const openPlayerMenu = (playerId: string, name: string, resting: boolean) => {
    Alert.alert(name, undefined, [
      { text: 'Huỷ', style: 'cancel' },
      {
        text: resting ? 'Chơi tiếp' : 'Tạm nghỉ',
        onPress: () => setPlayerResting(sessionId, playerId, !resting),
      },
    ]);
  };

  const submitNewPlayer = () => {
    if (!session) return;
    const trimmed = newName.trim();
    if (!trimmed) {
      setAdding(false);
      return;
    }
    if (session.players.some((p) => p.name === trimmed)) {
      Alert.alert('Tên đã có', `"${trimmed}" đã có trong danh sách.`);
      return;
    }
    addPlayer(sessionId, trimmed);
    setNewName('');
    setAdding(false);
  };

  return (
    <SafeAreaView style={styles.screen} edges={['left', 'right', 'bottom']}>
      <FlatList
        ListHeaderComponent={
          <View style={{ gap: spacing.lg }}>
            <Card style={{ padding: spacing.md }}>
              <Text style={styles.boardTitle}>Bảng điểm</Text>
              <View style={styles.board}>
                {ranked.map((p, i) => {
                  const resting = !!session.players.find(
                    (sp) => sp.id === p.playerId,
                  )?.resting;
                  return (
                    <Pressable
                      key={p.playerId}
                      onLongPress={() =>
                        openPlayerMenu(p.playerId, p.name, resting)
                      }
                      style={styles.boardRow}
                    >
                      <View style={styles.boardLeft}>
                        <View
                          style={[
                            styles.rankPill,
                            i === 0 && session.rounds.length > 0 && styles.rankPillLead,
                          ]}
                        >
                          <Text style={styles.rankText}>{i + 1}</Text>
                        </View>
                        <Text style={styles.playerName} numberOfLines={1}>
                          {p.name}
                        </Text>
                        {resting ? (
                          <View style={styles.restBadge}>
                            <Text style={styles.restBadgeText}>Tạm nghỉ</Text>
                          </View>
                        ) : null}
                      </View>
                      <Text
                        style={[
                          styles.boardPoints,
                          p.totalPoints > 0 && { color: colors.win },
                          p.totalPoints < 0 && { color: colors.loss },
                        ]}
                      >
                        {p.totalPoints > 0 ? '+' : ''}
                        {p.totalPoints}
                      </Text>
                      <DotsButton
                        onPress={() => openPlayerMenu(p.playerId, p.name, resting)}
                      />
                    </Pressable>
                  );
                })}
              </View>

              {adding ? (
                <View style={styles.addRow}>
                  <TextInput
                    autoFocus
                    value={newName}
                    onChangeText={setNewName}
                    onSubmitEditing={submitNewPlayer}
                    onBlur={() => {
                      if (newName.trim() === '') setAdding(false);
                    }}
                    returnKeyType="done"
                    blurOnSubmit={false}
                    placeholder="Tên người mới"
                    placeholderTextColor={colors.textMuted}
                    style={styles.addInput}
                  />
                  <Pressable
                    onPress={submitNewPlayer}
                    style={({ pressed }) => [
                      styles.addBtn,
                      pressed && { opacity: 0.85 },
                    ]}
                  >
                    <Text style={styles.addBtnText}>Thêm</Text>
                  </Pressable>
                </View>
              ) : (
                <Pressable
                  onPress={() => setAdding(true)}
                  style={({ pressed }) => [
                    styles.addPlayerCta,
                    pressed && { opacity: 0.7 },
                  ]}
                >
                  <Text style={styles.addPlayerCtaText}>+ Thêm người chơi</Text>
                </Pressable>
              )}
            </Card>

            <View style={styles.roundsHeader}>
              <Text style={styles.sectionLabel}>
                {session.rounds.length === 0
                  ? 'Chưa có ván nào'
                  : `${session.rounds.length} ván đã chơi`}
              </Text>
            </View>
          </View>
        }
        data={roundsNewestFirst}
        keyExtractor={(item) => item.round.id}
        contentContainerStyle={styles.list}
        renderItem={({ item: { round, idx } }) => (
          <Card
            onPress={() =>
              navigation.navigate('AddRound', { sessionId, roundId: round.id })
            }
            onLongPress={() => openRoundMenu(round.id, idx)}
            style={{ padding: spacing.md }}
          >
            <View style={styles.roundHead}>
              <Text style={styles.roundIdx}>Ván #{idx + 1}</Text>
              {round.note ? (
                <Text style={styles.roundNote} numberOfLines={1}>
                  {round.note}
                </Text>
              ) : null}
              <DotsButton onPress={() => openRoundMenu(round.id, idx)} />
            </View>
            <View style={styles.scoreGrid}>
              {round.scores.map((s) => {
                const player = session.players.find((p) => p.id === s.playerId);
                if (!player) return null;
                return (
                  <View key={s.playerId} style={styles.scoreCell}>
                    <Text style={styles.scoreName} numberOfLines={1}>
                      {player.name}
                    </Text>
                    <Text
                      style={[
                        styles.scoreVal,
                        s.points > 0 && { color: colors.win },
                        s.points < 0 && { color: colors.loss },
                      ]}
                    >
                      {s.points > 0 ? '+' : ''}
                      {s.points}
                    </Text>
                  </View>
                );
              })}
            </View>
          </Card>
        )}
        ItemSeparatorComponent={() => <View style={{ height: spacing.sm }} />}
        ListEmptyComponent={
          <View style={{ paddingTop: spacing.xl }}>
            <Text style={styles.emptyHint}>
              Nhấn "Thêm ván" ở dưới để ghi điểm ván đầu tiên.
            </Text>
          </View>
        }
      />

      <FAB
        label="Thêm ván"
        onPress={() => navigation.navigate('AddRound', { sessionId })}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  list: { padding: spacing.lg, paddingBottom: 120, gap: spacing.md },
  headerAction: {
    color: colors.primary,
    fontSize: font.body,
    fontWeight: '600',
    marginRight: spacing.sm,
  },
  restBadge: {
    backgroundColor: colors.surfaceAlt,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    borderWidth: 1,
    borderColor: colors.border,
  },
  restBadgeText: {
    color: colors.textMuted,
    fontSize: font.tiny,
    fontWeight: '600',
  },
  addPlayerCta: {
    marginTop: spacing.sm,
    paddingVertical: spacing.sm,
    alignItems: 'center',
    borderRadius: radius.sm,
  },
  addPlayerCtaText: {
    color: colors.primary,
    fontSize: font.small,
    fontWeight: '600',
  },
  addRow: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginTop: spacing.sm,
    alignItems: 'center',
  },
  addInput: {
    flex: 1,
    backgroundColor: colors.surfaceAlt,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    color: colors.text,
    fontSize: font.body,
  },
  addBtn: {
    backgroundColor: colors.primary,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: radius.sm,
  },
  addBtnText: { color: '#fff', fontWeight: '600', fontSize: font.small },
  boardTitle: {
    color: colors.textMuted,
    fontSize: font.small,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: spacing.sm,
    paddingHorizontal: spacing.xs,
  },
  board: { gap: spacing.xs },
  boardRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.xs,
    borderRadius: radius.sm,
  },
  boardLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    flex: 1,
  },
  rankPill: {
    width: 26,
    height: 26,
    borderRadius: 13,
    backgroundColor: colors.surfaceAlt,
    alignItems: 'center',
    justifyContent: 'center',
  },
  rankPillLead: { backgroundColor: colors.primary },
  rankText: { color: colors.text, fontSize: font.small, fontWeight: '700' },
  playerName: { color: colors.text, fontSize: font.body, fontWeight: '500', flex: 1 },
  boardPoints: { color: colors.text, fontSize: font.h3, fontWeight: '700' },
  sectionLabel: {
    color: colors.textMuted,
    fontSize: font.small,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  roundsHeader: { paddingHorizontal: spacing.xs },
  roundHead: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: spacing.sm,
  },
  roundIdx: { color: colors.text, fontSize: font.small, fontWeight: '600' },
  roundNote: {
    color: colors.textMuted,
    fontSize: font.tiny,
    flex: 1,
    textAlign: 'right',
    marginLeft: spacing.sm,
  },
  scoreGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  scoreCell: {
    backgroundColor: colors.surfaceAlt,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: radius.sm,
    minWidth: 80,
  },
  scoreName: { color: colors.textMuted, fontSize: font.tiny },
  scoreVal: { color: colors.text, fontSize: font.h3, fontWeight: '700', marginTop: 2 },
  emptyHint: {
    color: colors.textMuted,
    textAlign: 'center',
    fontSize: font.small,
  },
});
