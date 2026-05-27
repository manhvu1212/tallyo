import SwiftUI
import SwiftData

enum Screen: Hashable {
    case sessionsList
    case newSession
    case sessionDetail(sessionId: String)
    case stats(sessionId: String)
    case addRound(sessionId: String, roundId: String?)
}

@main
struct TallyoApp: App {
    var sharedModelContainer: ModelContainer = {
        let schema = Schema([
            Session.self,
            Player.self,
            Round.self,
            Score.self,
            RoundEvent.self,
            CustomGame.self
        ])
        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)

        do {
            return try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()

    var body: some Scene {
        WindowGroup {
            RootRouterView()
        }
        .modelContainer(sharedModelContainer)
    }
}

struct RootRouterView: View {
    @State private var path: [Screen] = []
    
    var body: some View {
        NavigationStack(path: $path) {
            SessionsListScreen(
                onOpenSession: { id in
                    path.append(.sessionDetail(sessionId: id))
                },
                onNewSession: {
                    path.append(.newSession)
                }
            )
            .navigationDestination(for: Screen.self) { screen in
                switch screen {
                case .newSession:
                    NewSessionScreen(
                        onBack: {
                            if !path.isEmpty { path.removeLast() }
                        },
                        onCreated: { id in
                            if !path.isEmpty { path.removeLast() } // Pop newSession
                            path.append(.sessionDetail(sessionId: id)) // Push sessionDetail
                        }
                    )
                case .sessionDetail(let sessionId):
                    SessionDetailScreen(
                        sessionId: sessionId,
                        onBack: {
                            if !path.isEmpty { path.removeLast() }
                        },
                        onOpenStats: {
                            path.append(.stats(sessionId: sessionId))
                        },
                        onAddRound: { roundId in
                            path.append(.addRound(sessionId: sessionId, roundId: roundId))
                        }
                    )
                case .stats(let sessionId):
                    StatsScreen(
                        sessionId: sessionId,
                        onBack: {
                            if !path.isEmpty { path.removeLast() }
                        }
                    )
                case .addRound(let sessionId, let roundId):
                    AddRoundScreen(
                        sessionId: sessionId,
                        roundId: roundId,
                        onBack: {
                            if !path.isEmpty { path.removeLast() }
                        }
                    )
                }
            }
        }
    }
}
