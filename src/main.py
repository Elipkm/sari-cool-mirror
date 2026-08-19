from __future__ import annotations

import time

import cv2

from .camera import LatestFrameCamera
from .vton import PlaceholderVtonProcessor

WINDOW = "Sari Cool Mirror"


def draw_metrics(frame, fps: float, frame_age_ms: float, sequence: int, vton_enabled: bool) -> None:
    lines = [
        f"FPS: {fps:5.1f}",
        f"Frame age: {frame_age_ms:5.1f} ms",
        f"Frame: {sequence}",
        f"VTON: {'ON' if vton_enabled else 'OFF'}",
    ]
    for index, text in enumerate(lines):
        cv2.putText(
            frame,
            text,
            (30, 35 + index * 26),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.65,
            (255, 255, 255),
            2,
            cv2.LINE_AA,
        )


def main() -> None:
    camera = LatestFrameCamera().start()
    vton = PlaceholderVtonProcessor()

    mirrored = True
    fullscreen = False
    vton_enabled = False
    last_sequence = -1
    previous_render = time.perf_counter()
    smoothed_fps = 0.0

    cv2.namedWindow(WINDOW, cv2.WINDOW_NORMAL)

    try:
        while True:
            captured = camera.latest()
            if captured is None or captured.sequence == last_sequence:
                time.sleep(0.001)
                continue

            last_sequence = captured.sequence
            frame = captured.image.copy()

            if mirrored:
                frame = cv2.flip(frame, 1)

            if vton_enabled:
                frame = vton.process(frame)

            now = time.perf_counter()
            delta = max(now - previous_render, 1e-6)
            instantaneous_fps = 1.0 / delta
            smoothed_fps = instantaneous_fps if smoothed_fps == 0 else 0.9 * smoothed_fps + 0.1 * instantaneous_fps
            previous_render = now

            frame_age_ms = (now - captured.captured_at) * 1000.0
            draw_metrics(frame, smoothed_fps, frame_age_ms, captured.sequence, vton_enabled)
            cv2.imshow(WINDOW, frame)

            key = cv2.waitKey(1) & 0xFF
            if key in (27, ord("q")):
                break
            if key == ord("m"):
                mirrored = not mirrored
            elif key == ord("v"):
                vton_enabled = not vton_enabled
            elif key == ord("f"):
                fullscreen = not fullscreen
                mode = cv2.WINDOW_FULLSCREEN if fullscreen else cv2.WINDOW_NORMAL
                cv2.setWindowProperty(WINDOW, cv2.WND_PROP_FULLSCREEN, mode)
    finally:
        camera.stop()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
