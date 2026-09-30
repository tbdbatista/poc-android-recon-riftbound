# Riftbound Recon — Design System & Visual Steering

> **Authoritative visual identity reference for all UI development.**
> All contributors and AI agents MUST comply with the rules defined in this document when creating or modifying screens.

---

## 1. Design Philosophy

### Concept: "Void & Magic"

The visual identity draws from the Riftbound TCG / League of Legends aesthetic — deep Void darkness (navy-black backgrounds) combined with arcane energy accents (purple, teal, gold). The result is a **dark-first, premium, and functional** interface.

### Core Principles

| Principle | Description |
|---|---|
| **Dark-First** | Dark theme is the default. Light theme is derived, maintaining the same visual hierarchy. |
| **Function over Decoration** | Every visual decision must serve usability. No purely decorative elements that harm readability. |
| **Hierarchy through Color** | Accent colors (purple, teal, gold) are used surgically to highlight key information, not as background decoration. |
| **Position is Protagonist** | The scan position index (#1, #2, #3…) is the app's key differentiator and must be visually prominent wherever it appears. |
| **Offline-First Visual** | No network loading spinners, connectivity indicators, or skeleton loaders. The UI assumes everything is local and instantaneous. |

---

## 2. Color Palette

### 2.1 Dark Theme Tokens (Default)

```
Token                     Hex        Usage
─────────────────────── ────────── ───────────────────────────────
background               #0F111A    Main screen background
surface                  #161A26    Cards, panels, bottom sheets
surfaceVariant           #22283A    Unselected chips, table headers
primary                  #9F5CFC    Buttons, active icons, #N position
secondary                #00BFA6    Position #N in search, set codes
tertiary / accent        #FFD700    Ownership badge, special highlights
error                    #FF5252    Destructive buttons, delete icons
onPrimary                #FFFFFF    Text on primary buttons
onBackground             #F1F3F9    Primary text (titles)
onSurface                #F1F3F9    Text on cards
onSurfaceVariant         #A0A5C0    Secondary text, labels, placeholders
outlineVariant           #2E3348    Subtle card borders
```

### 2.2 Light Theme Tokens

```
Token                     Hex        Usage
─────────────────────── ────────── ───────────────────────────────
background               #F6F8FC    Main background
surface                  #FFFFFF    Cards, panels
surfaceVariant           #E9EDF5    Chips, headers
primary                  #7B3BDB    Slightly darker purple for contrast
secondary                #009B86    Darker teal for legibility on white
tertiary / accent        #E6A800    Darker gold
onBackground             #1E212B    Primary text
onSurface                #1E212B    Text on cards
onSurfaceVariant         #6B7280    Secondary text
```

### 2.3 Functional Colors (Constants)

```kotlin
// Set gradients for Compendium thumbnails
val SetGradients = mapOf(
    "Origins"         to Color(0xFF1E3A8A),
    "Proving Grounds" to Color(0xFF7F1D1D),
    "Spiritforged"    to Color(0xFF14532D),
    "Vendetta"        to Color(0xFF831843),
    "Unleashed"       to Color(0xFF701A75),
)

// Scanner console log colors
val LogSuccess   = Color(0xFF00E676)   // Green — "SUCESSO"
val LogError     = Color(0xFFFF5252)   // Red — "FALHA"
val LogMetadata  = Color(0xFF00E5FF)   // Cyan — "Cód. Rodapé", "Energia"
val LogBoundary  = Color(0xFFFFEB3B)   // Yellow — "INICIANDO", "FIM"
val LogDefault   = Color.LightGray
val ConsoleBackground = Color.Black.copy(alpha = 0.85f)
```

---

## 3. Typography

### Font Families

| Context | Recommended Font | Fallback |
|---|---|---|
| Titles and headings | **Inter** (Bold/ExtraBold) | System default sans-serif |
| Body and labels | **Inter** (Regular/Medium) | System default sans-serif |
| Console logs | **JetBrains Mono** | Monospace system |
| Positional numbers (#N) | **Inter** (Bold) | System default sans-serif |

### Type Scale (Material 3)

| Style | Size | Weight | Where to Use |
|---|---|---|---|
| titleLarge | 22sp | Bold | TopAppBar titles, card name in dialogs |
| titleMedium | 16sp | Bold | Collection name, card name in list, stat chip value |
| titleSmall | 14sp | Bold | Section subtitles |
| bodyLarge | 16sp | Regular | Long descriptive text |
| bodyMedium | 14sp | Regular | Standard body text, **position #N** |
| bodySmall | 12sp | Regular | Metadata, console logs |
| labelMedium | 12sp | Medium | Table headers, sort labels |
| labelSmall | 11sp | Bold | Tags, badges, chips, button labels |

---

## 4. Component Specifications

### 4.1 Corner Radius Scale

```
 4dp — Tags, inline badges
 6dp — Small thumbnails (shelf, list)
 8dp — Chips, stat cards, console overlay
10dp — List rows (CollectionCardRow)
12dp — TextFields, buttons, grid thumbnails
16dp — Content cards (CollectionItem, SearchResult)
20dp — Dialogs, modals
24dp — Top corners of bottom sheets
```

### 4.2 Button Types

| Type | Shape | Background | Text Color | Usage |
|---|---|---|---|---|
| Primary (filled) | Rounded 12dp | `primary` | `onPrimary` (white) | "Salvar", "Iniciar Captura" |
| Shutter (capture) | Circle 64dp | `primary` | Inner white circle | Capture card button |
| Action Circle | Circle 46dp | `primary @ 20%` | `primary` | Undo, confirm |
| Destructive Circle | Circle 46dp | `error @ 20%` | `error` | Delete |
| Outlined | Rounded 12dp | Transparent | `error` + border `error @ 60%` | "Excluir" in dialogs |
| Text Button | — | Transparent | `primary` | "Cancelar", "Voltar" |
| FAB | Circle 56dp | `primary` | white | Start new capture |

### 4.3 Input Fields (TextField)

```
Shape:              RoundedCornerShape(12.dp)
Border (focused):   primary (#9F5CFC)
Border (unfocused): surfaceVariant (#22283A)
Background:         Transparent (OutlinedTextField)
Leading icon:       Icons.Default.Search in onSurfaceVariant
Placeholder text:   onSurfaceVariant (#A0A5C0)
```

### 4.4 Filter Chips / Pills

```
Shape:             RoundedCornerShape(20.dp)
Selected:          Background primary, text White, fontWeight Bold
Unselected:        Background surfaceVariant, text onSurfaceVariant
Padding:           horizontal 16dp, vertical 6dp
```

### 4.5 Badges

| Badge | Shape | Background | Text | Usage |
|---|---|---|---|---|
| Ownership count | Circle 22dp | `tertiary` (gold) | Black, Bold | Compendium grid (top-right) |
| Energy cost | Circle 20dp | `#8C52FF` | White, Bold | Compendium grid (top-left) |
| Stat chip | Rounded 8dp | `color @ 15%` + border `color @ 30%` | Principal color, ExtraBold | Details dialog |

### 4.6 Bottom Navigation Bar

```
containerColor:        surface (#161A26)
selectedIconColor:     primary (#9F5CFC)
selectedTextColor:     primary (#9F5CFC)
unselectedIconColor:   onSurfaceVariant (#A0A5C0)
unselectedTextColor:   onSurfaceVariant (#A0A5C0)
indicatorColor:        surfaceVariant (#22283A)

Tabs:
  Route         │ Label      │ Icon
  collections   │ Coleções   │ Icons.Default.Home
  scan          │ Capturar   │ Icons.Default.List (or CameraAlt)
  search        │ Buscar     │ Icons.Default.Search
  compendium    │ Compêndio  │ Icons.Default.Info
```

### 4.7 Spacing & Padding

```
Content horizontal padding:  16dp
Card internal padding:       16dp (normal), 20dp (dialogs)
Vertical spacing (cards):    12dp
Item spacing in Row:         6–8dp
Section spacing:             16dp
```

---

## 5. Steering Rules (MANDATORY)

### RULE 1 — Never use hardcoded colors
Always reference `MaterialTheme.colorScheme` tokens. Never write `Color(0xFF...)` inline except for console log colors and set gradients, which MUST be defined as public constants in `Theme.kt`.

### RULE 2 — Position (#N) is sacred
On any screen displaying the scan position (`scanOrder`), the "#N" text must be:
- Color: `primary` (purple) in collection/detail context
- Color: `secondary` (teal) in reverse search context
- FontWeight: **Bold** always
- Font size: minimum `bodyMedium` (14sp)
- Never hidden, truncated, or relegated to secondary text

### RULE 3 — Dark-first, light as derivation
Any new screen or component must be designed for the dark theme first. The light theme is generated by derivation, maintaining the same visual hierarchy and adjusting contrast.

### RULE 4 — No network loading indicators
The app is 100% offline-first. Never add shimmer/skeleton loaders, "loading from network" indicators, or messages implying connectivity dependency.

### RULE 5 — Corner radius consistency
Follow the scale defined in Section 4.1. Do not invent new radius values.

### RULE 6 — Console logs only on ScanScreen
The terminal-style console overlay (black background with colored logs) is exclusive to `ScanScreen`. Never replicate on other screens.

### RULE 7 — Navigation Bar icons
Use Material Icons (filled). Do not replace with custom icons without explicit approval. The 4 tabs are fixed: Coleções, Capturar, Buscar, Compêndio.

### RULE 8 — Text in Brazilian Portuguese
All labels, placeholders, messages, and UI text must be in **Brazilian Portuguese**. Card names and set names remain in English (they are official product names).

### RULE 9 — Accessibility minimum
- Every `Icon` must have a descriptive `contentDescription` in pt-BR
- Every `AsyncImage` must have `contentDescription` with the card name
- Minimum text contrast: 4.5:1 (WCAG AA)
- Disabled buttons use `Color.Gray` or `@ 40% alpha`, never invisible

### RULE 10 — Dialog consistency
- Shape: 20dp corners
- Shadow: 16dp
- Background: `surface`
- Internal padding: 20dp
- Title: `titleLarge`, Bold, `onSurface`
- Confirm button: `primary` or `error` (per context)
- Cancel button: TextButton with `primary` text color
