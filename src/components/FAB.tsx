import React from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';
import { colors, radius } from '../theme';

interface Props {
  onPress: () => void;
  label?: string;
  icon?: string;
}

export function FAB({ onPress, label, icon = '+' }: Props) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.fab,
        label ? styles.fabExtended : styles.fabRound,
        { opacity: pressed ? 0.85 : 1 },
      ]}
    >
      <Text style={styles.icon}>{icon}</Text>
      {label ? <Text style={styles.label}>{label}</Text> : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fab: {
    position: 'absolute',
    right: 20,
    bottom: 28,
    backgroundColor: colors.primary,
    shadowColor: '#000',
    shadowOpacity: 0.25,
    shadowOffset: { width: 0, height: 4 },
    shadowRadius: 8,
    elevation: 6,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  fabRound: {
    width: 60,
    height: 60,
    borderRadius: 30,
  },
  fabExtended: {
    paddingHorizontal: 18,
    paddingVertical: 14,
    borderRadius: radius.pill,
    gap: 6,
  },
  icon: {
    color: '#fff',
    fontSize: 24,
    fontWeight: '700',
    lineHeight: 26,
  },
  label: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
  },
});
