import AlokEngine
import Foundation

/// A recommendation the user kept, with the name they gave it. Newest first in the store.
struct SavedRecipe: Identifiable, Hashable {
    let id: Int64
    var name: String
    var goal: ShotGoal
    var recommendation: Recommendation
}

/// Saved recipes as one small JSON file in the app's storage: a handful of rows, read once at start, so a database
/// would be overkill. Works offline. The file format matches the Android app's `recipes.json`.
@MainActor
final class RecipeStore: ObservableObject {
    @Published private(set) var recipes: [SavedRecipe] = []
    private let url: URL

    init(url: URL = RecipeStore.defaultURL) {
        self.url = url
        recipes = Self.load(from: url)
    }

    nonisolated static var defaultURL: URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("recipes.json")
    }

    var last: SavedRecipe? { recipes.first }

    func add(name: String, goal: ShotGoal, recommendation: Recommendation) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let id = Int64(Date().timeIntervalSince1970 * 1000)
        recipes.insert(SavedRecipe(id: id, name: String(trimmed.prefix(40)), goal: goal, recommendation: recommendation), at: 0)
        persist()
    }

    func delete(_ recipe: SavedRecipe) {
        recipes.removeAll { $0.id == recipe.id }
        persist()
    }

    private func persist() {
        let rows = recipes.map(Row.init)
        guard let data = try? JSONEncoder().encode(rows) else { return }
        // Write then rename, so a crash mid-write never leaves a half file behind.
        let tmp = url.appendingPathExtension("tmp")
        do {
            try data.write(to: tmp, options: .atomic)
            _ = try FileManager.default.replaceItemAt(url, withItemAt: tmp)
        } catch {
            try? data.write(to: url, options: .atomic)
        }
    }

    private static func load(from url: URL) -> [SavedRecipe] {
        guard let data = try? Data(contentsOf: url), let rows = try? JSONDecoder().decode([FailableRow].self, from: data) else { return [] }
        return rows.compactMap { $0.row?.recipe }
    }

    /// One recipe on disk, with the same keys as Android.
    private struct Row: Codable {
        var id: Int64
        var name: String
        var goal: String
        var iso: Int
        var shutter: Double
        var wb: Int
        var focus: String
        var ev: Double
        var lens: String
        var format: String
        var stability: String
        var light: String

        init(_ r: SavedRecipe) {
            let rec = r.recommendation
            id = r.id; name = r.name; goal = r.goal.rawValue
            iso = rec.iso; shutter = rec.shutterSeconds; wb = rec.whiteBalanceK
            focus = rec.focus.rawValue; ev = rec.evBias; lens = rec.lens.rawValue
            format = rec.format.rawValue; stability = rec.stability.rawValue; light = rec.lightLevel.name
        }

        var recipe: SavedRecipe? {
            guard let goal = ShotGoal(rawValue: goal), let focus = FocusMode(rawValue: focus), let lens = LensType(rawValue: lens),
                  let format = CaptureFormat(rawValue: format), let stability = Stability(rawValue: stability),
                  let level = [LightLevel.veryDark, .low, .medium, .bright, .sunny].first(where: { $0.name == light })
            else { return nil }
            return SavedRecipe(
                id: id, name: name, goal: goal,
                // Advice was about the light at the time; a recipe is reused later, so it is not kept.
                recommendation: Recommendation(
                    iso: iso, shutterSeconds: shutter, whiteBalanceK: wb, focus: focus, evBias: ev, lens: lens,
                    format: format, stability: stability, advice: [], lightLevel: level
                )
            )
        }
    }

    /// Skips a row that cannot be read instead of dropping the whole file.
    private struct FailableRow: Decodable {
        let row: Row?
        init(from decoder: Decoder) throws { row = try? Row(from: decoder) }
    }
}
