# Sari Cool Mirror

A latency-first smart mirror / mobile AR prototype.

The current MVP is a local web app: it opens the front camera, tracks one face with MediaPipe Face Landmarker, lets you upload an accessory image, and attaches it to the live video as glasses, a hat, a crown, or a generic face overlay.

## Web MVP

### What works now

- live webcam / phone front-camera preview
- upload PNG, WebP, or JPEG
- automatic face tracking
- presets for glasses, hat, crown, and free overlay
- overlay follows face position, size, and head tilt
- size slider
- remove button
- mobile-friendly layout
- all processing happens in the browser; uploaded accessory images are not sent to this project's server

Transparent PNG/WebP images work best for accessories.

### Start it

Python 3 is enough for the web MVP. No Python packages are required.

```bash
git clone https://github.com/Elipkm/sari-cool-mirror.git
cd sari-cool-mirror
python serve.py
```

Then open:

```text
http://localhost:8000
```

Allow camera access in the browser, select an accessory type, and press **Upload image**.

> Note: camera access through `getUserMedia()` requires a secure context. Browsers treat `localhost` as a trusted local context, so this MVP works locally. For testing from another phone over your LAN, use HTTPS or a secure development tunnel rather than plain `http://<laptop-ip>:8000`.

## Architecture

```text
Front camera
    ↓
Browser getUserMedia()
    ↓
MediaPipe Face Landmarker
    ↓
face landmarks
    ↓
accessory transform
(position + scale + rotation)
    ↓
Canvas compositor
    ↓
live mirror view
```

The browser implementation is intentionally the primary MVP because it maps well to the final mobile-phone target and avoids an unnecessary Python/OpenCV video round trip.

## Legacy OpenCV prototype

The repository also still contains the initial Python/OpenCV latency prototype:

```bash
python -m venv .venv
pip install -r requirements.txt
python -m src.main
```

It provides threaded latest-frame capture, mirrored display, FPS/frame-age instrumentation, and a VTON integration placeholder.

## Next milestones

1. Improve accessory calibration and add manual X/Y adjustment.
2. Add touch gestures for drag, pinch-to-scale, and rotation.
3. Add multiple simultaneous accessories.
4. Add face occlusion so, for example, glasses arms can appear behind the head where appropriate.
5. Package the web app as a PWA or mobile wrapper.
6. Add clothing/body tracking separately for full virtual try-on.
