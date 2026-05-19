export interface Player {
  id: string;
  name: string;
  resting?: boolean;
}

export interface RoundScore {
  playerId: string;
  points: number;
}

export interface Round {
  id: string;
  createdAt: number;
  scores: RoundScore[];
  note?: string;
}

export interface ScoringConfig {
  zeroSum?: boolean;
}

export interface Session {
  id: string;
  name: string;
  createdAt: number;
  updatedAt: number;
  config: ScoringConfig;
  players: Player[];
  rounds: Round[];
}

export interface PlayerStats {
  playerId: string;
  name: string;
  totalPoints: number;
  roundsPlayed: number;
  wins: number;
  losses: number;
  bestRound: number;
  worstRound: number;
  averagePerRound: number;
}

export interface SessionInsights {
  totalRounds: number;
  totalPointsExchanged: number;
  leaders: PlayerStats[];
  trailers: PlayerStats[];
  biggestBlowoutRoundIndex?: number;
  closestRoundIndex?: number;
  sweepPlayer?: PlayerStats;
}
