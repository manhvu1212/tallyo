import React, { useRef, useState } from 'react';
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

export function NewSessionScreen({ navigation }: ScreenProps<'NewSession'>) {
  const { createSession } = useStore();
  const [name, setName] = useState('');
  const [playerInput, setPlayerInput] = useState('');
  const [players, setPlayers] = useState<string[]>([]);
  const [zeroSum, setZeroSum] = useState(false);
  const playerInputRef = useRef<TextInput>(null);

  const addPlayer = () => {
    const trimmed = playerInput.trim();
    if (!trimmed) return;
    if (players.includes(trimmed)) {
      Alert.alert('Tên đã có', `"${trimmed}" đã có trong danh sách.`);
      return;
    }
    setPlayers((prev) => [...prev, trimmed]);
    setPlayerInput('');
    playerInputRef.current?.focus();
  };

  const removePlayer = (p: string) =>
    setPlayers((prev) => prev.filter((x) => x !== p));

  const canCreate = players.length >= 2;

  const submit = () => {
    if (!canCreate) {
      Alert.alert('Cần ít nhất 2 người chơi');
      return;
    }
    const created = createSession({
      name,
      playerNames: players,
      config: { zeroSum },
    });
    navigation.replace('SessionDetail', { sessionId: created.id });
  };

  return (
    <SafeAreaView style={styles.screen} edges={['left', 'right', 'bottom']}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
          <Section title="Tên buổi chơi">
            <TextInput
              value={name}
              onChangeText={setName}
              placeholder="VD: Tối thứ 7 nhà Hùng"
              placeholderTextColor={colors.textMuted}
              returnKeyType="next"
              onSubmitEditing={() => playerInputRef.current?.focus()}
              style={styles.input}
            />
          </Section>

          <Section title="Người chơi" hint="Tối thiểu 2 người">
            <View style={styles.row}>
              <TextInput
                ref={playerInputRef}
                value={playerInput}
                onChangeText={setPlayerInput}
                onSubmitEditing={addPlayer}
                returnKeyType="done"
                blurOnSubmit={false}
                placeholder="Tên người chơi"
                placeholderTextColor={colors.textMuted}
                style={[styles.input, { flex: 1 }]}
              />
              <Pressable
                onPress={addPlayer}
                style={({ pressed }) => [
                  styles.addBtn,
                  { opacity: pressed ? 0.85 : 1 },
                ]}
              >
                <Text style={styles.addBtnText}>Thêm</Text>
              </Pressable>
            </View>
            {players.length > 0 ? (
              <View style={styles.chipWrap}>
                {players.map((p) => (
                  <Pressable
                    key={p}
                    onPress={() => removePlayer(p)}
                    style={styles.chip}
                  >
                    <Text style={styles.chipText}>{p}</Text>
                    <Text style={styles.chipX}>  ×</Text>
                  </Pressable>
                ))}
              </View>
            ) : (
              <Text style={styles.hint}>Chưa có người chơi nào.</Text>
            )}
          </Section>

          <Section title="Tuỳ chọn ghi điểm">
            <Pressable
              onPress={() => setZeroSum((v) => !v)}
              style={[styles.optionBox, zeroSum && styles.optionBoxActive]}
            >
              <View style={styles.optionHead}>
                <View style={[styles.check, zeroSum && styles.checkActive]}>
                  {zeroSum ? <Text style={styles.checkMark}>✓</Text> : null}
                </View>
                <Text style={styles.optionTitle}>Tổng điểm mỗi ván = 0</Text>
              </View>
              <Text style={styles.optionDesc}>
                Bật khi điểm người thắng đúng bằng tổng điểm người thua trừ.
                Để trống 1 ô khi ghi điểm, app tự cân cho người cuối.
                Hợp với Tiến lên đếm lá, Phỏm, Mạt chược.
              </Text>
            </Pressable>
          </Section>

          <PrimaryButton
            label="Tạo buổi chơi"
            onPress={submit}
            disabled={!canCreate}
            style={{ marginTop: spacing.lg }}
          />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function Section({
  title,
  hint,
  children,
}: {
  title: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <View style={{ marginBottom: spacing.xl }}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {hint ? <Text style={styles.sectionHint}>{hint}</Text> : null}
      <View style={{ marginTop: spacing.sm }}>{children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  body: { padding: spacing.lg, paddingBottom: spacing.xxl * 2 },
  sectionTitle: {
    color: colors.text,
    fontSize: font.h3,
    fontWeight: '600',
  },
  sectionHint: {
    color: colors.textMuted,
    fontSize: font.tiny,
    marginTop: 2,
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
  },
  row: { flexDirection: 'row', gap: spacing.sm, alignItems: 'flex-end' },
  addBtn: {
    backgroundColor: colors.primary,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    borderRadius: radius.md,
  },
  addBtnText: { color: '#fff', fontWeight: '600' },
  chipWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
    marginTop: spacing.md,
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surfaceAlt,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    borderWidth: 1,
    borderColor: colors.border,
  },
  chipText: { color: colors.text, fontSize: font.small, fontWeight: '500' },
  chipX: { color: colors.textMuted, fontSize: font.body },
  hint: { color: colors.textMuted, fontSize: font.tiny, marginTop: spacing.sm },
  optionBox: {
    padding: spacing.md,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surface,
  },
  optionBoxActive: {
    borderColor: colors.primary,
    backgroundColor: '#231D3D',
  },
  optionHead: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  check: {
    width: 20,
    height: 20,
    borderRadius: 4,
    borderWidth: 2,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkActive: {
    borderColor: colors.primary,
    backgroundColor: colors.primary,
  },
  checkMark: { color: '#fff', fontSize: 13, fontWeight: '700', lineHeight: 14 },
  optionTitle: { color: colors.text, fontSize: font.body, fontWeight: '600' },
  optionDesc: {
    color: colors.textMuted,
    fontSize: font.small,
    marginTop: 6,
    marginLeft: 28,
    lineHeight: 18,
  },
});
