import React from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
  ViewStyle,
} from 'react-native';
import { colors, font, radius, spacing } from '../theme';

interface Props {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  disabled?: boolean;
  loading?: boolean;
  style?: ViewStyle;
}

export function PrimaryButton({
  label,
  onPress,
  variant = 'primary',
  disabled,
  loading,
  style,
}: Props) {
  const palette = (() => {
    switch (variant) {
      case 'secondary':
        return { bg: colors.surfaceAlt, fg: colors.text, border: colors.border };
      case 'ghost':
        return { bg: 'transparent', fg: colors.text, border: colors.border };
      case 'danger':
        return { bg: colors.danger, fg: '#fff', border: colors.danger };
      default:
        return { bg: colors.primary, fg: '#fff', border: colors.primary };
    }
  })();

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [
        styles.btn,
        {
          backgroundColor: palette.bg,
          borderColor: palette.border,
          opacity: disabled ? 0.5 : pressed ? 0.85 : 1,
        },
        style,
      ]}
    >
      <View style={styles.inner}>
        {loading ? (
          <ActivityIndicator color={palette.fg} />
        ) : (
          <Text style={[styles.label, { color: palette.fg }]}>{label}</Text>
        )}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  btn: {
    borderRadius: radius.md,
    borderWidth: 1,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.lg,
  },
  inner: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  label: {
    fontSize: font.body,
    fontWeight: '600',
  },
});
