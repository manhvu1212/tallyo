import type { NativeStackScreenProps } from '@react-navigation/native-stack';

export type RootStackParamList = {
  SessionsList: undefined;
  NewSession: undefined;
  SessionDetail: { sessionId: string };
  AddRound: { sessionId: string; roundId?: string };
  Stats: { sessionId: string };
};

export type ScreenProps<T extends keyof RootStackParamList> =
  NativeStackScreenProps<RootStackParamList, T>;
