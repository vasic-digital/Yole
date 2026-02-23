# Module 8: Advanced UI Customization (10 videos)

## Video 8.1: Theme System Architecture (15 min)

### Timestamps
- 0:00 Design token-based theme systems
- 2:00 Color tokens: primary, secondary, background, surface, error
- 4:00 Typography tokens: heading, body, code, caption
- 6:00 Spacing and sizing tokens
- 8:00 Light/dark mode with system preference detection
- 10:00 Custom theme creation and persistence
- 12:00 Theme switching at runtime
- 14:00 Summary

### Code Example: Security-Conscious Theme Implementation

```kotlin
// Theme tokens are validated to prevent injection through custom themes
data class ThemeColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val onPrimary: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val error: Color,
) {
    init {
        // Validate all colors are in valid range (security: prevent malformed values)
        require(background.alpha in 0f..1f) { "Invalid background alpha" }
    }
}
```

---

## Video 8.2: Custom Components (12 min)

### Timestamps
- 0:00 Compose Multiplatform component design principles
- 2:00 Building reusable components in shared code
- 4:00 Component API: parameters, callbacks, modifiers
- 6:00 Slot-based composition pattern
- 8:00 Component testing with ComposeTestRule
- 10:00 Screenshot tests for visual consistency
- 11:30 Summary

### Code Example: Accessible Component

```kotlin
// Accessibility-first component design
@Composable
fun FormatBadge(
    format: TextFormat,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics {
            contentDescription = "File format: ${format.name}"
            role = Role.Image
        },
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = format.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
```

---

## Video 8.3: Animation and Transitions (10 min)

### Timestamps
- 0:00 Compose animation APIs overview
- 1:30 `animateAsState` for value-based animations
- 3:00 `AnimatedVisibility` for enter/exit transitions
- 4:30 `Crossfade` for switching between editor views
- 6:00 Page transitions: slide, fade, scale
- 7:30 Loading indicators and progress animations
- 9:00 Performance: avoiding unnecessary recompositions during animation
- 9:45 Summary

### Code Example: Animation Constants

```kotlin
// Animations.kt defines consistent animation specs across the app
object YoleAnimations {
    val defaultDuration = 300 // ms
    val fastDuration = 150 // ms
    val slowDuration = 500 // ms

    val defaultEasing = FastOutSlowInEasing
    val enterTransition = fadeIn(tween(defaultDuration)) + slideInVertically()
    val exitTransition = fadeOut(tween(fastDuration)) + slideOutVertically()
}
```

---

## Videos 8.4-8.10: Advanced UI Topics

### Video 8.4: Accessibility (12 min)

#### Timestamps
- 0:00 Why accessibility matters: screen readers, keyboard navigation
- 2:00 Compose semantics API: contentDescription, role, stateDescription
- 4:00 Keyboard navigation and focus management
- 6:00 High contrast and large text support
- 8:00 Testing accessibility: TalkBack, VoiceOver, automated checks
- 10:00 WCAG compliance considerations
- 11:30 Summary

### Video 8.5: Responsive Layouts (12 min)

#### Timestamps
- 0:00 Adaptive layouts for phone, tablet, desktop
- 2:00 WindowSizeClass API in Compose
- 4:00 Compact, medium, and expanded layouts
- 6:00 List-detail pattern for large screens
- 8:00 Orientation changes and state preservation
- 10:00 Testing responsive behavior
- 11:30 Summary

### Video 8.6: Custom Text Rendering (15 min)

#### Timestamps
- 0:00 Line numbers and gutter rendering
- 3:00 Custom text layout with Canvas
- 6:00 Syntax highlighting integration
- 9:00 Performance: virtualized rendering for large documents
- 12:00 Cursor and selection handling
- 14:30 Summary

### Video 8.7: Split-Pane Editor (12 min)

#### Timestamps
- 0:00 Split-pane layout design
- 2:00 Resizable panel divider
- 4:00 Source + preview side-by-side
- 6:00 Synchronized scrolling between panels
- 8:00 Panel state persistence
- 10:00 Collapse/expand animations
- 11:30 Summary

### Video 8.8: Command Palette (12 min)

#### Timestamps
- 0:00 Command palette UX (Ctrl+Shift+P)
- 2:00 Command registration system
- 4:00 Fuzzy search for commands
- 6:00 Recently used commands
- 8:00 Keyboard shortcut display
- 10:00 Custom command providers
- 11:30 Summary

### Video 8.9: Plugin System UI (12 min)

#### Timestamps
- 0:00 Plugin panel in settings
- 2:00 Plugin discovery and installation UI
- 4:00 Plugin configuration screens
- 6:00 Plugin status indicators
- 8:00 Enable/disable toggle
- 10:00 Plugin marketplace concept
- 11:30 Summary

### Video 8.10: Internationalization (12 min)

#### Timestamps
- 0:00 i18n in Compose Multiplatform
- 2:00 String resource management
- 4:00 Right-to-left layout support
- 6:00 Date and number formatting
- 8:00 Locale-aware sorting
- 10:00 Testing with different locales
- 11:30 Summary
