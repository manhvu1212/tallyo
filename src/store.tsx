import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { loadSessions, saveSessions, uid } from './storage';
import type {
  Player,
  Round,
  RoundScore,
  ScoringConfig,
  Session,
} from './types';

interface Store {
  ready: boolean;
  sessions: Session[];
  getSession: (id: string) => Session | undefined;
  createSession: (input: {
    name: string;
    playerNames: string[];
    config: ScoringConfig;
  }) => Session;
  deleteSession: (id: string) => void;
  renameSession: (id: string, name: string) => void;
  addRound: (sessionId: string, scores: RoundScore[], note?: string) => Round | undefined;
  deleteRound: (sessionId: string, roundId: string) => void;
  updateRound: (sessionId: string, roundId: string, scores: RoundScore[], note?: string) => void;
  addPlayer: (sessionId: string, name: string) => Player | undefined;
  removePlayer: (sessionId: string, playerId: string) => void;
  setPlayerResting: (sessionId: string, playerId: string, resting: boolean) => void;
}

const StoreContext = createContext<Store | null>(null);

export function StoreProvider({ children }: { children: React.ReactNode }) {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [ready, setReady] = useState(false);
  const saveQueue = useRef<Session[]>(sessions);

  useEffect(() => {
    let mounted = true;
    loadSessions().then((data) => {
      if (!mounted) return;
      setSessions(data);
      saveQueue.current = data;
      setReady(true);
    });
    return () => {
      mounted = false;
    };
  }, []);

  const persist = useCallback((next: Session[]) => {
    saveQueue.current = next;
    setSessions(next);
    saveSessions(next).catch(() => {
      // Silent fail; in-memory state stays correct.
    });
  }, []);

  const getSession = useCallback(
    (id: string) => sessions.find((s) => s.id === id),
    [sessions],
  );

  const createSession: Store['createSession'] = useCallback(
    ({ name, playerNames, config }) => {
      const now = Date.now();
      const session: Session = {
        id: uid(),
        name: name.trim() || 'Buổi chơi mới',
        createdAt: now,
        updatedAt: now,
        config,
        players: playerNames
          .map((n) => n.trim())
          .filter(Boolean)
          .map((n) => ({ id: uid(), name: n })),
        rounds: [],
      };
      persist([session, ...saveQueue.current]);
      return session;
    },
    [persist],
  );

  const deleteSession = useCallback(
    (id: string) => {
      persist(saveQueue.current.filter((s) => s.id !== id));
    },
    [persist],
  );

  const renameSession = useCallback(
    (id: string, name: string) => {
      persist(
        saveQueue.current.map((s) =>
          s.id === id ? { ...s, name: name.trim() || s.name, updatedAt: Date.now() } : s,
        ),
      );
    },
    [persist],
  );

  const addRound: Store['addRound'] = useCallback(
    (sessionId, scores, note) => {
      const round: Round = {
        id: uid(),
        createdAt: Date.now(),
        scores,
        note,
      };
      persist(
        saveQueue.current.map((s) =>
          s.id === sessionId
            ? { ...s, rounds: [...s.rounds, round], updatedAt: Date.now() }
            : s,
        ),
      );
      return round;
    },
    [persist],
  );

  const deleteRound = useCallback(
    (sessionId: string, roundId: string) => {
      persist(
        saveQueue.current.map((s) =>
          s.id === sessionId
            ? {
                ...s,
                rounds: s.rounds.filter((r) => r.id !== roundId),
                updatedAt: Date.now(),
              }
            : s,
        ),
      );
    },
    [persist],
  );

  const updateRound = useCallback(
    (sessionId: string, roundId: string, scores: RoundScore[], note?: string) => {
      persist(
        saveQueue.current.map((s) =>
          s.id === sessionId
            ? {
                ...s,
                rounds: s.rounds.map((r) =>
                  r.id === roundId ? { ...r, scores, note } : r,
                ),
                updatedAt: Date.now(),
              }
            : s,
        ),
      );
    },
    [persist],
  );

  const addPlayer: Store['addPlayer'] = useCallback(
    (sessionId, name) => {
      const trimmed = name.trim();
      if (!trimmed) return undefined;
      const player: Player = { id: uid(), name: trimmed };
      persist(
        saveQueue.current.map((s) =>
          s.id === sessionId
            ? { ...s, players: [...s.players, player], updatedAt: Date.now() }
            : s,
        ),
      );
      return player;
    },
    [persist],
  );

  const removePlayer = useCallback(
    (sessionId: string, playerId: string) => {
      persist(
        saveQueue.current.map((s) =>
          s.id === sessionId
            ? {
                ...s,
                players: s.players.filter((p) => p.id !== playerId),
                rounds: s.rounds.map((r) => ({
                  ...r,
                  scores: r.scores.filter((sc) => sc.playerId !== playerId),
                })),
                updatedAt: Date.now(),
              }
            : s,
        ),
      );
    },
    [persist],
  );

  const setPlayerResting: Store['setPlayerResting'] = useCallback(
    (sessionId, playerId, resting) => {
      persist(
        saveQueue.current.map((s) =>
          s.id === sessionId
            ? {
                ...s,
                players: s.players.map((p) =>
                  p.id === playerId ? { ...p, resting } : p,
                ),
                updatedAt: Date.now(),
              }
            : s,
        ),
      );
    },
    [persist],
  );

  const value = useMemo<Store>(
    () => ({
      ready,
      sessions,
      getSession,
      createSession,
      deleteSession,
      renameSession,
      addRound,
      deleteRound,
      updateRound,
      addPlayer,
      removePlayer,
      setPlayerResting,
    }),
    [
      ready,
      sessions,
      getSession,
      createSession,
      deleteSession,
      renameSession,
      addRound,
      deleteRound,
      updateRound,
      addPlayer,
      removePlayer,
      setPlayerResting,
    ],
  );

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
}

export function useStore(): Store {
  const ctx = useContext(StoreContext);
  if (!ctx) throw new Error('useStore must be used inside StoreProvider');
  return ctx;
}
