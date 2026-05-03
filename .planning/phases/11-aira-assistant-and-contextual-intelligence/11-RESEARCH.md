# Phase 11 Research: Aira Assistant & Gemini Integration

## Objective
Analyze the feasibility and implementation patterns for an on-device "Aira Assistant" powered by Gemini Nano, ensuring adherence to the privacy-first core value.

## Technology Options

### 1. Gemini Nano via Google AI Edge SDK (AICore)
- **Status**: Production-ready on Google Pixel 8/9 and Samsung S24+ series.
- **Pros**: 
    - Managed via Android System Service (AICore).
    - Hardened safety filters.
    - Zero impact on app binary size (model is system-managed).
- **Cons**: 
    - Limited device availability (flagships only).
    - Requires `com.google.ai.edge.aicore` dependency.
- **Implementation**: Uses `Generation.getClient()` and `generateContent`.

### 2. ML Kit LLM Inference (MediaPipe)
- **Status**: Stable.
- **Pros**:
    - Wider device compatibility (works on most modern Android devices via CPU/GPU).
    - Can run various weights (Gemma 2B, Phi-2, Falcon).
- **Cons**: 
    - Large asset download (1GB+ for model weights).
    - Manual lifecycle management.
- **Implementation**: Uses `LlmInference` from MediaPipe Tasks GenAI.

### 3. Gemini Cloud REST (Existing Foundation)
- **Status**: Implemented in `GeminiCloudRuntimeGateway`.
- **Pros**: Works on all devices with internet.
- **Cons**: Not privacy-first for raw biometric telemetry.

## Recommendations

1. **Hybrid On-Device Strategy**: 
    - **Primary**: Use **Gemini Nano** (AICore) if the device supports it.
    - **Secondary/Fallback**: Use **ML Kit LLM Inference** with a small model (e.g., Gemma 2B) for compatible devices without native Gemini Nano support.
    - **No Cloud Fallback**: For privacy reasons, raw biometric/score discussions will NOT fall back to the cloud unless explicitly opted-in for non-sensitive topics.

2. **Aira Assistant Architecture**:
    - Build a `AssistantViewModel` that selects the best available `AiRuntimeGateway`.
    - Implement a "Contextual Injection" layer that anchors the chat in current recovery/stress scores before sending the prompt to the on-device model.

## Open Questions

- Should we allow the user to choose between "Maximum Privacy (On-Device only)" and "Maximum Intelligence (Cloud Gemini 2.0 Flash)"?
- Can we leverage the existing `GeminiRestModels.kt` for local structured data responses?

## Resources
- [Android Developer Guide: Gemini Nano](https://developer.android.com/ai/gemini-nano)
- [MediaPipe LLM Inference Guide](https://developers.google.com/mediapipe/solutions/genai/llm_inference/android)
