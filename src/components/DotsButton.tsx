import React from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';
import { colors, font } from '../theme';

interface Props {
  onPress: () => void;
}

export function DotsButton({ onPress }: Props) {
  return (
    <Pressable
      onPress={onPress}
      hitSlop={10}
      style={({ pressed }) => [styles.btn, pressed && { opacity: 0.5 }]}
    >
      <Text style={styles.dots}>⋮</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  btn: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dots: {
    color: colors.textMuted,
    fontSize: font.h2,
    fontWeight: '700',
    lineHeight: 24,
  },
});
