# Sari Cool Mirror

Latency-first DIY AI smart mirror prototype for real-time virtual try-on.

## MVP

```text
Camera
  -> OpenCV capture thread
  -> latest-frame buffer (stale frames are dropped)
  -> VTON processor interface
  -> FPS / latency overlay
  -> fullscreen digital mirror
```

The first goal is a responsive digital mirror. AI features are added without allowing slow inference to build a delayed frame queue.

## Features

- OpenCV webcam capture
- asynchronous capture thread
- latest-frame-only processing
- mirrored display
- FPS and frame-age instrumentation
- fullscreen mode
- pluggable VTON processor placeholder

## Setup

Python 3.11+ recommended.

```bash
python -m venv .venv
pip install -r requirements.txt
python -m src.main
```

Windows PowerShell activation:

```powershell
.\.venv\Scripts\Activate.ps1
```

Linux/macOS activation:

```bash
source .venv/bin/activate
```

## Controls

- `q` / `Esc`: quit
- `f`: fullscreen/windowed
- `m`: mirror on/off
- `v`: VTON placeholder on/off

## Roadmap

1. Establish and measure baseline camera-to-display latency.
2. Add MediaPipe pose/body tracking.
3. Integrate a real real-time VTON backend.
4. Add GStreamer/hardware-accelerated capture.
5. Add local GPU or WebRTC remote-GPU inference.
6. Add temporal stabilization/interpolation.
7. Add garment selection and shopping catalog integration.
8. Add body/garment measurements for actual size recommendations.
