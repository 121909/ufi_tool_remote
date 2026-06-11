# UFI Remote Design System

## Product Pattern
- Real-time operations dashboard for remote device control and SMS workflow.
- Dense, utility-first surfaces.
- Primary goal: show status, expose actions, keep navigation fast.

## Visual Direction
- Style: data-dense dashboard.
- Layout: compact cards, clear section headers, short vertical rhythm.
- Radius: 12dp for primary surfaces, 8dp for compact chips and stat tiles.
- Borders: 1dp outline on all major cards; avoid heavy elevation.
- Background: cool neutral base with subtle contrast between background, surface, and surfaceVariant.

## Color Roles
- Primary: teal/green for connection and success states.
- Accent: blue for secondary actions and informational highlights.
- Destructive: red for delete/failure states.
- Use semantic tokens, not per-screen ad hoc colors.

## Typography
- Prefer semibold labels, medium titles, and compact body copy.
- Use monospaced-looking emphasis for IDs, timestamps, and technical values when possible.
- Avoid oversized hero text; keep headers proportional to the panel they live in.

## Interaction
- Search and filter controls belong in a single dense toolbar.
- Prefer segmented controls, chips, and compact buttons over large marketing-style CTAs.
- Empty states should explain the absence and give a recovery action.
- Loading states should appear inline inside the same card or header.

## Page Rules
- Dashboard: first screen should show connection status, device summary, EasyTier status, and quick actions.
- SMS list: search, unread/failed filters, and message count must be visible together.
- SMS detail: message metadata, content, extraction helpers, and actions must be grouped cleanly.
- Compose: recipient, body, and send/cancel actions should feel like a form sheet, not a marketing page.

## Anti-Patterns
- Oversized hero layouts.
- Floating decorative cards.
- Purely ornamental gradients or blobs.
- Overly loose spacing that wastes screen real estate.
- Low-contrast status text.
- Blank empty screens with no next step.
