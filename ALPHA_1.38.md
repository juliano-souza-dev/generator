# Alpha 1.38 — Mobile Tunnel QR Code

- Keeps the Alpha 1.37 mobile layout and Alpha 1.36 desktop behavior unchanged.
- When Cloudflare Quick Tunnel returns the public URL, RUN immediately renders a scannable QR Code in the terminal.
- Saves the same QR Code as `mobile_access_qr.png` in the Generator root.
- QR generation is local; the tunnel URL is not sent to a third-party QR service.
- Adds `qrcode[pil]` to the project dependencies and RUN can bootstrap it automatically once if an existing virtualenv does not have it yet.
