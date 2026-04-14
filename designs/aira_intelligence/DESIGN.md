```markdown
# Design System Specification: The Invisible Intelligence

## 1. Overview & Creative North Star
**Creative North Star: "The Clinical Ghost"**

In the landscape of 2026, user interfaces should no longer compete for attention; they should anticipate it. This design system moves away from the "Dashboard-as-a-Control-Room" trope toward an editorial, predictive experience. We call this "The Clinical Ghost"—a system that is whisper-quiet when idle but authoritative and precise when active.

The design breaks the "template" look by prioritizing **intentional asymmetry**. We lean into high-contrast typographic scales and significant negative space ("air") to create a sense of premium calm. This isn't just a UI; it's a digital concierge that feels more like a high-end medical journal or a luxury lifestyle publication than a software application.

---

## 2. Colors & Tonal Logic

The palette is dynamic and contextual. While the foundation is rooted in Deep Charcoal (`#131318`), the system breathes through its "Post-Material" state logic.

### State-Based Color Theory
*   **Predictive/Healthy (Teal):** Use `primary` (`#47eaed`) for optimistic states and data.
*   **Cautionary/Observation (Amber):** Use `secondary` (`#ffe2ab`) for neutral alerts and predictive shifts.
*   **Critical/Action (Coral):** Use `tertiary` (`#ffc9b7`) for high-priority health metrics or errors.

### The "No-Line" Rule
**Explicit Instruction:** Traditional 1px solid borders are prohibited for sectioning. 
Structure is defined through:
1.  **Background Shifts:** Using `surface-container-low` against `background`.
2.  **Tonal Transitions:** Defining edges through subtle gradient bleeds.
3.  **Soft Shadows:** Ambient occlusion rather than outlines.

### The Glass & Gradient Rule
To achieve a "Post-Material" feel, floating elements (Nav bars, Floating Action Buttons) must use **Glassmorphism 2.0**.
*   **Token:** `surface_variant` at 60% opacity.
*   **Effect:** Backdrop blur of 24px + a 0.5px "Ghost Border" using `outline_variant` at 15% opacity.
*   **Signature Glow:** Main CTAs should use a subtle linear gradient from `primary` (`#47eaed`) to `primary_container` (`#00ced1`) to add depth and "soul."

---

## 3. Typography
We utilize **Manrope** for its unique athletic-yet-clinical geometry. It provides the "Expert" personality required by the system.

| Level | Size | Weight | Tracking | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Display-LG** | 3.5rem | 800 | -0.04em | Impactful data points / Hero states |
| **Headline-MD** | 1.75rem | 600 | -0.02em | Section starts |
| **Title-SM** | 1.0rem | 600 | 0.01em | Card headers / Small labels |
| **Body-LG** | 1.0rem | 400 | 0 | Long-form predictive insights |
| **Label-MD** | 0.75rem | 700 | 0.05em | Micro-data / All Caps metadata |

**Editorial Hierarchy:** Always pair a `Display-LG` numeric value with a `Label-MD` descriptor. This high-contrast pairing creates the "Premium Clinical" aesthetic.

---

## 4. Elevation & Depth

### The Layering Principle
Hierarchy is achieved through **Tonal Layering**. Instead of a flat grid, treat the UI as stacked sheets of frosted glass.
*   **Base:** `surface_dim` (`#131318`)
*   **Sectioning:** `surface_container_low` (`#1b1b20`)
*   **Interactive Cards:** `surface_container` (`#1f1f25`)
*   **Active Overlays:** `surface_bright` (`#39383e`)

### Ambient Shadows
For "floating" predictive cards, use shadows that mimic natural light:
*   **Y-Offset:** 16px | **Blur:** 40px
*   **Color:** `on_surface` at 6% opacity. 
*   **Result:** A soft, diffused lift that feels integrated into the background rather than hovering over it.

---

## 5. Components

### Buttons
*   **Primary:** Pill-shaped (999px), Gradient from `primary` to `primary_container`, `on_primary` text. No shadow.
*   **Secondary:** Ghost-style. No background. `outline_variant` (20% opacity) border.
*   **Interactive State:** On hover, primary buttons should emit a soft 12px glow using the `primary` color at 30% opacity.

### Invisible Cards & Lists
*   **Rule:** Forbid divider lines.
*   **Implementation:** Separate list items using 16px of vertical whitespace or a subtle background shift to `surface_container_highest` on hover.
*   **Radius:** Large cards use `lg` (32px / 2rem); compact elements use `DEFAULT` (16px / 1rem).

### Predictive Chips
*   **Style:** `surface_container_high` background.
*   **Indicator:** A 4px "Live Pulse" dot using the state color (Teal/Amber/Coral) to indicate real-time data streaming.

### Input Fields
*   **Structure:** No "box." Only a bottom stroke using `outline_variant` at 40%.
*   **Focus State:** The stroke transitions to `primary` (`#47eaed`) with a subtle 4px blur glow beneath the text.

---

## 6. Do’s and Don’ts

### Do
*   **Do** use asymmetrical layouts. Place large display text on the left with significant whitespace on the right to lead the eye.
*   **Do** use "Air." If you think there is enough margin, double it. Premium feel is synonymous with unused space.
*   **Do** use color purposefully. If a metric is "Healthy," the entire card can have a 2% teal tint bleed.

### Don’t
*   **Don't** use 100% black. Use the `surface` tones provided to maintain depth and "inkiness."
*   **Don't** use heavy dropshadows. If the elevation isn't clear through tonal shifting, your layout is too crowded.
*   **Don't** use icons without purpose. Icons should be secondary to the clinical data; use ultra-thin (200 weight) stroke icons only.

---

## 7. Signature Implementation: The Health Gradient
To truly embody the "Invisible" identity, the background of the app should subtly shift between `surface` and a very dark tint of the current "Health State" (e.g., a #051a1a teal-dark blend). This ensures the UI feels alive and reactive to the user's data without ever displaying a "Loading" spinner or a loud alert.```