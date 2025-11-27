# ProxyAI — Offline-only fork

This fork of ProxyAI is intentionally offline-only and removes telemetry and all remote/online LLM provider integrations. It keeps support for local model backends only.

Supported backends
- Ollama (local endpoint or self-hosted Ollama host). Configure host/endpoint in plugin settings (example: http://localhost:11434).
- Local LLaMA backends (e.g., llama.cpp / local GGUF models). Default models path: ~/.codegpt/models/gguf (configurable in settings).

What's removed
- Telemetry module and telemetry UI/options (completely removed).
- All remote/online LLM provider integrations (OpenAI, Anthropic, Mistral, DeepInfra, Fireworks, Groq, OpenRouter, Together, Anyscale, Azure, Hugging Face remote endpoints, You.com, Replicate, AWS/Bedrock, Vertex AI, etc.).
- Sample configs, sample tests, and docs referencing remote providers (pruned where applicable).

Configuration
- Ollama: In plugin settings → Ollama Service, set Host to your Ollama endpoint (for example: http://localhost:11434). Click "Refresh models" to populate available models.
- LLaMA / llama.cpp: Place local GGUF models in the models path (default: ~/.codegpt/models/gguf). Configure the LLaMA settings if you want to use a custom path.

Notes
- CHANGELOG.md is preserved as historical.
- Other local backends are kept. If you later need a remote provider, you can reintroduce the code manually; this fork guarantees offline-only behavior by removing remote provider integrations and telemetry.