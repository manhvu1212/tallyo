import type { PlayerStats, Round, Session, SessionInsights } from './types';

export function computePlayerStats(session: Session): PlayerStats[] {
  const map: Record<string, PlayerStats> = {};
  for (const p of session.players) {
    map[p.id] = {
      playerId: p.id,
      name: p.name,
      totalPoints: 0,
      roundsPlayed: 0,
      wins: 0,
      losses: 0,
      bestRound: Number.NEGATIVE_INFINITY,
      worstRound: Number.POSITIVE_INFINITY,
      averagePerRound: 0,
    };
  }

  for (const round of session.rounds) {
    if (round.scores.length === 0) continue;
    const max = Math.max(...round.scores.map((s) => s.points));
    const min = Math.min(...round.scores.map((s) => s.points));
    for (const s of round.scores) {
      const stats = map[s.playerId];
      if (!stats) continue;
      stats.totalPoints += s.points;
      stats.roundsPlayed += 1;
      if (s.points > stats.bestRound) stats.bestRound = s.points;
      if (s.points < stats.worstRound) stats.worstRound = s.points;
      if (s.points === max && max !== min) stats.wins += 1;
      if (s.points === min && max !== min) stats.losses += 1;
    }
  }

  return Object.values(map).map((s) => ({
    ...s,
    bestRound: s.roundsPlayed === 0 ? 0 : s.bestRound,
    worstRound: s.roundsPlayed === 0 ? 0 : s.worstRound,
    averagePerRound:
      s.roundsPlayed === 0 ? 0 : s.totalPoints / s.roundsPlayed,
  }));
}

function roundSpread(round: Round): number {
  if (round.scores.length === 0) return 0;
  const pts = round.scores.map((s) => s.points);
  return Math.max(...pts) - Math.min(...pts);
}

export function computeInsights(session: Session): SessionInsights {
  const stats = computePlayerStats(session);
  const played = stats.filter((s) => s.roundsPlayed > 0);
  const sorted = [...played].sort((a, b) => b.totalPoints - a.totalPoints);

  const leaders: PlayerStats[] = [];
  const trailers: PlayerStats[] = [];
  if (sorted.length > 0) {
    const top = sorted[0].totalPoints;
    const bottom = sorted[sorted.length - 1].totalPoints;
    for (const s of sorted) if (s.totalPoints === top) leaders.push(s);
    if (top !== bottom) {
      for (const s of sorted) if (s.totalPoints === bottom) trailers.push(s);
    }
  }

  let biggestBlowoutRoundIndex: number | undefined;
  let closestRoundIndex: number | undefined;
  let biggestSpread = -1;
  let smallestSpread = Number.POSITIVE_INFINITY;
  session.rounds.forEach((r, i) => {
    if (r.scores.length < 2) return;
    const spread = roundSpread(r);
    if (spread > biggestSpread) {
      biggestSpread = spread;
      biggestBlowoutRoundIndex = i;
    }
    if (spread < smallestSpread) {
      smallestSpread = spread;
      closestRoundIndex = i;
    }
  });

  let sweepPlayer: PlayerStats | undefined;
  if (session.rounds.length >= 2) {
    for (const s of stats) {
      if (s.wins === session.rounds.length) {
        sweepPlayer = s;
        break;
      }
    }
  }

  const totalPointsExchanged = session.rounds.reduce(
    (acc, r) => acc + r.scores.reduce((sum, s) => sum + Math.abs(s.points), 0),
    0,
  );

  return {
    totalRounds: session.rounds.length,
    totalPointsExchanged,
    leaders,
    trailers,
    biggestBlowoutRoundIndex,
    closestRoundIndex,
    sweepPlayer,
  };
}
