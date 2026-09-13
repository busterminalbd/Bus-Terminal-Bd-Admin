# Shared design system — read this before adding a new tool screen

This app has multiple tools (খাবার বিল মেমো, অগ্রিম বেতন আবেদন, মেডিকেল ওয়ার্ক
রিপোর্ট, and more to come). Some parts of the UI are meant to look and behave
**identically** across every tool. Those parts live in this folder as shared
composables. **Do not copy-paste a new version of these into a new tool
screen** — import and use the ones listed here. If a shared component is
missing a feature a new tool needs, extend the shared component instead of
forking it.

## Components that must be the same everywhere

| Component | File | What it standardizes |
|---|---|---|
| `ToolTopAppBar` | `ToolTopAppBar.kt` | Every tool's header: primary-theme background color, white title, back button on the left, a global-settings gear (⚙️) on the far right that opens Main App Settings. Tool-specific icons go in `extraActions`, to the left of the settings gear. |
| `DateNavigatorBar` | `DateNavigatorBar.kt` | The prev/calendar-chip/next day switcher used by any tool that works on a per-day basis. The date is shown inside the calendar chip itself — no separate "তারিখ: ..." label needed elsewhere on the screen. |
| `ToolCardItem` (+ `AppToolItem`) | `ToolsHubScreen.kt` | The tool cards on the "ডিজিটাল টুল" home page. All tools use the same card shape/elevation/badge; only `icon`, `title`, `subtitle`, and `accentColor` differ per tool. **Always set `accentColor` from `MaterialTheme.colorScheme.*` (primary/secondary/tertiary), never a hardcoded `Color(0x...)`** — a hardcoded color won't respond to theme changes and will look inconsistent with the other cards (this exact bug existed for Medical Work Report and was fixed). |
| Main App Settings | `GlobalSettingsScreen.kt`, opened via `onOpenGlobalSettings` | The one and only settings screen for the whole app. Tools must **not** build their own separate settings screen — route settings-like needs into this one, or open it via the gear icon that `ToolTopAppBar` already provides. |

## Adding a new tool: checklist

1. Header → use `ToolTopAppBar`, not a hand-built `TopAppBar`.
2. If the tool works per-day → use `DateNavigatorBar`.
3. Home page card → add an `AppToolItem` entry in `ToolsHubScreen.kt` with a
   theme-based `accentColor`. Don't build a separate card composable.
4. Settings → route through `onOpenGlobalSettings`, not a new settings screen.
5. Before writing any new "common-looking" UI (buttons, bars, chips), check
   this folder first — it may already exist. If it doesn't but the new piece
   is genuinely reusable, add it here (with a doc comment like the ones in
   `ToolTopAppBar.kt` / `DateNavigatorBar.kt`) and list it in the table above.
