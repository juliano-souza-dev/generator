# Alpha 1.37 — Mobile Layout + RUN Quick Tunnel

- Preserves the Alpha 1.36 desktop layout and workflow.
- Adds a dedicated mobile-only responsive layer at `max-width: 767px`.
- Adds mobile stage navigation with horizontal scrolling and automatic centering on the active stage.
- Makes panels, editors, video, wave controls, review cards, materials and dialogs usable on narrow screens.
- `run.bat` / `run.sh` now use `run_launcher.py`.
- The launcher starts Uvicorn locally on port 8080 and opens a Cloudflare Quick Tunnel automatically.
- If `cloudflared` is not installed, the launcher downloads the official current binary into `.tools/` on first use.
- Quick Tunnel URL changes on each RUN and is intended for development/testing.
