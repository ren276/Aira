# Phase 11 Discussion: Aira Assistant & Gemini Integration

## Strategic Topic: Privacy vs Intelligence Spectrum

The user has requested the "Aira Assistant" using Gemini. Given our "Privacy-First" core value, we have two paths for this feature:

### Option A: Pure On-Device (Recommended)
- **Engine**: Gemini Nano (via AICore) or Gemma (via MediaPipe).
- **Behavior**: The LLM runs entirely on the phone. Health data never leaves the device.
- **Limitation**: Only available on newer/flagship devices. Knowledge is limited to what the model knows + injected local context.
- **Aesthetic**: Can use "Private AI" branding.

### Option B: Cloud-Hybrid (High Feature)
- **Engine**: Gemini 2.0 Flash (via Ktor REST).
- **Behavior**: Uses the existing cloud gateway for complex queries. Requires sending a subset of context (non-identifiable) to the cloud.
- **Limitation**: Violates the "raw bio-data isolation" rule if we send specific HRV/Sleep numbers, but could be safe for "general" questions.

## Proposed Plan for Phase 11

1.  **Requirement ASSIST-01**: Implement a Chat UI that feels premium and responsive.
2.  **Requirement ASSIST-02**: Integrate the **Google AI Edge SDK** (Gemini Nano) as the primary runtime.
3.  **Requirement ASSIST-03**: Create a "Context Anchor" that automatically feeds the last 24h of scores into the assistant's memory *without* user typing.

## User Decisions Needed

- **Decision 1**: Should we strictly enforce **On-Device only** for the Assistant, even if it means unsupported devices can't use it, or should we offer a Cloud toggle?
- **Decision 2**: Design-wise, should the Assistant row at the bottom of the screens be a "Floating Action Button" (FAB) style or a "Featured Row" in the Insights list?
- **Decision 3**: Should the bot have a personality (e.g., "Aira - your athlete performance guide") or be purely clinical?

## Next Steps
- [ ] Finalize the decision on On-Device vs Cloud.
- [ ] Create `11-01-PLAN.md` for the chosen implementation path.
