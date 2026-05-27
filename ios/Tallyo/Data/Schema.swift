import Foundation
import SwiftData

@Model
final class Session {
    @Attribute(.unique) var id: String
    var name: String
    var game: String
    var createdAt: Date
    var updatedAt: Date
    var zeroSum: Bool
    
    @Relationship(deleteRule: .cascade, inverse: \Player.session)
    var players: [Player] = []
    
    @Relationship(deleteRule: .cascade, inverse: \Round.session)
    var rounds: [Round] = []
    
    @Relationship(deleteRule: .cascade, inverse: \RoundEvent.session)
    var events: [RoundEvent] = []
    
    init(id: String = UUID().uuidString, name: String, game: String, createdAt: Date = Date(), zeroSum: Bool = false) {
        self.id = id
        self.name = name
        self.game = game
        self.createdAt = createdAt
        self.updatedAt = createdAt
        self.zeroSum = zeroSum
    }
}

@Model
final class Player {
    @Attribute(.unique) var id: String
    var name: String
    var resting: Bool
    var orderIndex: Int
    var session: Session?
    
    init(id: String = UUID().uuidString, name: String, resting: Bool = false, orderIndex: Int = 0) {
        self.id = id
        self.name = name
        self.resting = resting
        self.orderIndex = orderIndex
    }
}

@Model
final class Round {
    @Attribute(.unique) var id: String
    var createdAt: Date
    var note: String?
    var orderIndex: Int
    var session: Session?
    
    @Relationship(deleteRule: .cascade, inverse: \Score.round)
    var scores: [Score] = []
    
    @Relationship(deleteRule: .nullify, inverse: \RoundEvent.round)
    var events: [RoundEvent] = []
    
    init(id: String = UUID().uuidString, createdAt: Date = Date(), note: String? = nil, orderIndex: Int = 0) {
        self.id = id
        self.createdAt = createdAt
        self.note = note
        self.orderIndex = orderIndex
    }
}

@Model
final class Score {
    @Attribute(.unique) var id: String
    var roundId: String
    var playerId: String
    var points: Int
    var round: Round?
    
    init(roundId: String, playerId: String, points: Int) {
        self.id = "\(roundId)_\(playerId)"
        self.roundId = roundId
        self.playerId = playerId
        self.points = points
    }
}

@Model
final class RoundEvent {
    @Attribute(.unique) var id: String
    var sessionId: String
    var roundId: String?
    var playerId: String
    var points: Int
    var note: String?
    var createdAt: Date
    
    var session: Session?
    var round: Round?
    
    init(id: String = UUID().uuidString, sessionId: String, roundId: String? = nil, playerId: String, points: Int, note: String? = nil, createdAt: Date = Date()) {
        self.id = id
        self.sessionId = sessionId
        self.roundId = roundId
        self.playerId = playerId
        self.points = points
        self.note = note
        self.createdAt = createdAt
    }
}

@Model
final class CustomGame {
    @Attribute(.unique) var name: String
    var defaultZeroSum: Bool
    
    init(name: String, defaultZeroSum: Bool = false) {
        self.name = name
        self.defaultZeroSum = defaultZeroSum
    }
}
