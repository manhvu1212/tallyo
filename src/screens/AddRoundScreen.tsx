import React, { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { PrimaryButton } from '../components/PrimaryButton';
import { ScreenProps } from '../navigation';
import { useStore } from '../store';
import { colors, font, radius, spacing } from '../theme';
import type { RoundScore } from '../types';

type DeltaBtn = { label: string; value: number | 'edit' };
const DELTAS: DeltaBtn[] = [
  { label: '-4', value: -4 },
  { label: '-2', value: -2 },
  { label: '-1', value: -1 },
  { label: '✎', value: 'edit' },
  { label: '+1', value: 1 },
  { label: '+2', value: 2 },
  { label: '+4', value: 4 },
];

export function AddRoundScreen({ route, navigation }: ScreenProps<'AddRound'>) {
  const { sessionId, roundId } = route.params;
  const { getSession, addRound, updateRound } = useStore();
  const session = getSession(sessionId);

  const editing = useMemo(
    () => (roundId ? session?.rounds.find((r) => r.id === roundId) : undefined),
    [roundId, session],
  );

  const visiblePlayers = useMemo(() => {
    if (!session) return [];
    if (editing) {
      const ids = new Set(editing.scores.map((s) => s.playerId));
      return session.players.filter((p) => ids.has(p.id));
    }
    return session.players.filter((p) => !p.resting);
  }, [editing, session]);

  const [scores, setScores] = useState<Record<string, string>>(() => {
    const map: Record<string, string> = {};
    const initial = editing
      ? session?.players.filter((p) =>
          editing.scores.some((s) => s.playerId === p.id),
        ) ?? []
      : session?.players.filter((p) => !p.resting) ?? [];
    initial.forEach((p) => {
      const existing = editing?.scores.find((s) => s.playerId === p.id);
      map[p.id] = existing ? String(existing.points) : '';
    });
    return map;
  });

  const [note, setNote] = useState(editing?.note ?? '');
  const [activeId, setActiveId] = useState<string | null>(null);
  const [customMode, setCustomMode] = useState(false);
  const inputRefs = useRef<Record<string, TextInput | null>>({});

  useLayoutEffect(() => {
    navigation.setOptions({
      title: editing ? 'Sửa ván' : 'Thêm ván',
    });
  }, [navigation, editing]);

  useEffect(() => {
    setCustomMode(false);
  }, [activeId]);

  const zeroSum = !!session?.config.zeroSum;

  const liveStatus = useMemo(() => {
    let sum = 0;
    let emptyId: string | null = null;
    let emptyCount = 0;
    let err = false;
    for (const p of visiblePlayers) {
      const raw = (scores[p.id] ?? '').trim();
      if (raw === '') {
        if (emptyId === null) emptyId = p.id;
        emptyCount++;
        continue;
      }
      const n = Number(raw);
      if (Number.isNaN(n)) {
        err = true;
        continue;
      }
      sum += n;
    }
    return { sum, emptyId, emptyCount, parseError: err };
  }, [scores, visiblePlayers]);

  if (!session) {
    return (
      <SafeAreaView style={styles.screen}>
        <Text style={styles.placeholder}>Buổi chơi không tồn tại.</Text>
      </SafeAreaView>
    );
  }

  const adjustScore = (playerId: string, delta: number) => {
    setScores((prev) => {
      const raw = (prev[playerId] ?? '').trim();
      const cur = raw === '' ? 0 : Number(raw);
      const base = Number.isNaN(cur) ? 0 : cur;
      return { ...prev, [playerId]: String(base + delta) };
    });
  };

  const buildScores = (): RoundScore[] | null => {
    if (liveStatus.parseError) {
      Alert.alert('Điểm không hợp lệ', 'Có ô chứa ký tự không phải số.');
      return null;
    }

    const filledCount = visiblePlayers.length - liveStatus.emptyCount;
    if (filledCount === 0) {
      Alert.alert('Chưa có dữ liệu', 'Cần nhập điểm cho ít nhất 1 người.');
      return null;
    }

    if (zeroSum) {
      if (liveStatus.emptyCount >= 2) {
        Alert.alert(
          'Cần nhập đủ',
          `Bật "Tổng = 0" thì cần nhập điểm cho ít nhất ${visiblePlayers.length - 1} người.`,
        );
        return null;
      }
      if (liveStatus.emptyCount === 0 && liveStatus.sum !== 0) {
        Alert.alert(
          'Tổng phải bằng 0',
          `Tổng hiện tại là ${liveStatus.sum > 0 ? '+' : ''}${liveStatus.sum}. Vui lòng chỉnh lại.`,
        );
        return null;
      }
      return visiblePlayers.map((p) => {
        const raw = (scores[p.id] ?? '').trim();
        if (raw === '') return { playerId: p.id, points: -liveStatus.sum };
        return { playerId: p.id, points: Number(raw) };
      });
    }

    return visiblePlayers.map((p) => {
      const raw = (scores[p.id] ?? '').trim();
      return { playerId: p.id, points: raw === '' ? 0 : Number(raw) };
    });
  };

  const save = () => {
    const built = buildScores();
    if (!built) return;
    if (editing) {
      updateRound(sessionId, editing.id, built, note.trim() || undefined);
    } else {
      addRound(sessionId, built, note.trim() || undefined);
    }
    navigation.goBack();
  };

  return (
    <SafeAreaView style={styles.screen} edges={['left', 'right', 'bottom']}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView
          contentContainerStyle={styles.body}
          keyboardShouldPersistTaps="handled"
        >
          <Text style={styles.hint}>
            {zeroSum
              ? 'Tổng điểm phải = 0. Để trống 1 ô để app tự cân.'
              : 'Nhập điểm cho từng người. Có thể là số âm. Bỏ trống = 0.'}
          </Text>

          <View style={{ gap: spacing.md, marginTop: spacing.md }}>
            {visiblePlayers.map((p) => {
              const isAutoFilled =
                zeroSum && liveStatus.emptyCount === 1 && liveStatus.emptyId === p.id;
              const autoVal = -liveStatus.sum;
              const raw = scores[p.id] ?? '';
              const isActive = activeId === p.id;
              const isCustom = isActive && customMode;
              const displayValue =
                raw !== ''
                  ? raw
                  : isAutoFilled
                    ? `= ${autoVal > 0 ? '+' : ''}${autoVal}`
                    : '0';
              const valueColor = (() => {
                if (raw === '' && isAutoFilled) return colors.primary;
                if (raw === '') return colors.textMuted;
                const n = Number(raw);
                if (!Number.isNaN(n) && n > 0) return colors.win;
                if (!Number.isNaN(n) && n < 0) return colors.loss;
                return colors.text;
              })();

              return (
                <View key={p.id}>
                  <Pressable
                    onPress={() => setActiveId(p.id)}
                    style={[styles.freeRow, isActive && styles.freeRowActive]}
                  >
                    <Text style={styles.playerLabel}>{p.name}</Text>
                    {isCustom ? (
                      <TextInput
                        ref={(r) => {
                          inputRefs.current[p.id] = r;
                        }}
                        autoFocus
                        value={raw}
                        onChangeText={(t) =>
                          setScores((prev) => ({ ...prev, [p.id]: t }))
                        }
                        onBlur={() => setCustomMode(false)}
                        keyboardType="numbers-and-punctuation"
                        style={[styles.scoreInput, { color: valueColor }]}
                      />
                    ) : (
                      <Text style={[styles.scoreDisplay, { color: valueColor }]}>
                        {displayValue}
                      </Text>
                    )}
                  </Pressable>

                  {isActive && !isCustom ? (
                    <View style={styles.toolbar}>
                      {DELTAS.map((d) => {
                        const isEdit = d.value === 'edit';
                        const isNeg = !isEdit && (d.value as number) < 0;
                        const isPos = !isEdit && (d.value as number) > 0;
                        return (
                          <Pressable
                            key={d.label}
                            onPress={() => {
                              if (d.value === 'edit') {
                                setCustomMode(true);
                              } else {
                                adjustScore(p.id, d.value);
                              }
                            }}
                            style={({ pressed }) => [
                              styles.toolBtn,
                              isEdit && styles.toolBtnEdit,
                              pressed && { opacity: 0.6 },
                            ]}
                          >
                            <Text
                              style={[
                                styles.toolBtnText,
                                isNeg && { color: colors.loss },
                                isPos && { color: colors.win },
                                isEdit && { color: '#fff' },
                              ]}
                            >
                              {d.label}
                            </Text>
                          </Pressable>
                        );
                      })}
                    </View>
                  ) : null}
                </View>
              );
            })}
          </View>

          <View style={{ marginTop: spacing.xl }}>
            <Text style={styles.subLabel}>Ghi chú (tuỳ chọn)</Text>
            <TextInput
              value={note}
              onChangeText={setNote}
              placeholder="VD: ván tới chia bài lại"
              placeholderTextColor={colors.textMuted}
              style={styles.input}
            />
          </View>

          <PrimaryButton
            label={editing ? 'Cập nhật ván' : 'Lưu ván'}
            onPress={save}
            style={{ marginTop: spacing.xl }}
          />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  body: { padding: spacing.lg, paddingBottom: spacing.xxl * 2 },
  hint: {
    color: colors.textMuted,
    fontSize: font.small,
    lineHeight: 20,
  },
  placeholder: { color: colors.textMuted, padding: spacing.lg },
  freeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    gap: spacing.md,
  },
  freeRowActive: {
    borderColor: colors.primary,
    backgroundColor: '#231D3D',
  },
  playerLabel: {
    color: colors.text,
    fontSize: font.body,
    fontWeight: '500',
    flex: 1,
  },
  scoreInput: {
    minWidth: 80,
    color: colors.text,
    fontSize: font.h3,
    fontWeight: '700',
    textAlign: 'right',
    paddingVertical: spacing.sm,
  },
  scoreDisplay: {
    minWidth: 80,
    color: colors.text,
    fontSize: font.h3,
    fontWeight: '700',
    textAlign: 'right',
    paddingVertical: spacing.sm,
  },
  toolbar: {
    flexDirection: 'row',
    gap: spacing.xs,
    marginTop: spacing.xs,
  },
  toolBtn: {
    flex: 1,
    paddingVertical: spacing.md,
    borderRadius: radius.md,
    backgroundColor: colors.surfaceAlt,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  toolBtnEdit: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  toolBtnText: {
    color: colors.text,
    fontSize: font.body,
    fontWeight: '700',
  },
  input: {
    backgroundColor: colors.surface,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md,
    color: colors.text,
    fontSize: font.body,
    marginTop: spacing.sm,
  },
  subLabel: {
    color: colors.textMuted,
    fontSize: font.small,
  },
});
