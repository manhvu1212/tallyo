import SwiftUI

struct TallyoPalette {
    let bg: Color
    let surface: Color
    let surfaceAlt: Color
    let border: Color
    let text: Color
    let textMuted: Color
    let primary: Color
    let primaryDim: Color
    let accent: Color
    let warning: Color
    let danger: Color
    let win: Color
    let loss: Color
    let primaryTintBg: Color
}

let DarkPalette = TallyoPalette(
    bg: Color(red: 0.059, green: 0.067, blue: 0.082), // 0xFF0F1115
    surface: Color(red: 0.102, green: 0.114, blue: 0.141), // 0xFF1A1D24
    surfaceAlt: Color(red: 0.133, green: 0.149, blue: 0.184), // 0xFF22262F
    border: Color(red: 0.176, green: 0.196, blue: 0.235), // 0xFF2D323C
    text: Color(red: 0.961, green: 0.965, blue: 0.973), // 0xFFF5F6F8
    textMuted: Color(red: 0.604, green: 0.631, blue: 0.675), // 0xFF9AA1AC
    primary: Color(red: 0.486, green: 0.361, blue: 1.000), // 0xFF7C5CFF
    primaryDim: Color(red: 0.353, green: 0.271, blue: 0.722), // 0xFF5A45B8
    accent: Color(red: 0.306, green: 0.824, blue: 0.659), // 0xFF4ED2A8
    warning: Color(red: 0.961, green: 0.651, blue: 0.137), // 0xFFF5A623
    danger: Color(red: 0.898, green: 0.282, blue: 0.302), // 0xFFE5484D
    win: Color(red: 0.306, green: 0.824, blue: 0.659), // 0xFF4ED2A8
    loss: Color(red: 0.898, green: 0.282, blue: 0.302), // 0xFFE5484D
    primaryTintBg: Color(red: 0.137, green: 0.114, blue: 0.239) // 0xFF231D3D
)

let LightPalette = TallyoPalette(
    bg: Color(red: 0.969, green: 0.973, blue: 0.980), // 0xFFF7F8FA
    surface: Color(red: 1.000, green: 1.000, blue: 1.000), // 0xFFFFFFFF
    surfaceAlt: Color(red: 0.937, green: 0.945, blue: 0.961), // 0xFFEFF1F5
    border: Color(red: 0.886, green: 0.898, blue: 0.922), // 0xFFE2E5EB
    text: Color(red: 0.063, green: 0.071, blue: 0.094), // 0xFF101218
    textMuted: Color(red: 0.420, green: 0.447, blue: 0.502), // 0xFF6B7280
    primary: Color(red: 0.420, green: 0.278, blue: 1.000), // 0xFF6B47FF
    primaryDim: Color(red: 0.718, green: 0.659, blue: 1.000), // 0xFFB7A8FF
    accent: Color(red: 0.122, green: 0.667, blue: 0.518), // 0xFF1FAA84
    warning: Color(red: 0.851, green: 0.533, blue: 0.110), // 0xFFD9881C
    danger: Color(red: 0.851, green: 0.176, blue: 0.196), // 0xFFD92D32
    win: Color(red: 0.122, green: 0.667, blue: 0.518), // 0xFF1FAA84
    loss: Color(red: 0.851, green: 0.176, blue: 0.196), // 0xFFD92D32
    primaryTintBg: Color(red: 0.929, green: 0.910, blue: 1.000) // 0xFFEDE8FF
)

struct TallyoTheme {
    static func palette(for scheme: ColorScheme) -> TallyoPalette {
        return scheme == .dark ? DarkPalette : LightPalette
    }
}

// Utility extension to make it simple in SwiftUI
extension View {
    func tallyoBackground(_ scheme: ColorScheme) -> some View {
        self.background(TallyoTheme.palette(for: scheme).bg)
    }
}
