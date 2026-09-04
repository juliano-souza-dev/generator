# Alpha 1.39 — Cue Review → WbW Synchronization

- Base: Alpha 1.38 Mobile Tunnel + QR Code.
- Editing `approved_en` in Cue Review now reconciles `words[]` immediately on save.
- Lexically preserved words keep existing timing and translation metadata.
- Capitalization/punctuation-only changes update the visual token without destroying timing.
- Replaced/inserted words return as pending and receive a temporary timing seed for later WbW Timing review.
- Removed words are removed from `words[]`.
- PT groups touched or split by the edit are dissolved; individual PT backups are preserved when available.
- Downstream Word Review, Cue Timing and WbW Timing are invalidated exactly as before.
- Shared behavior: Dialogue and Music use the same implementation.
- Mobile UI, Quick Tunnel and QR generation remain unchanged.
