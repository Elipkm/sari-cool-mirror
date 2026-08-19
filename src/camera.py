from __future__ import annotations

import threading
import time
from dataclasses import dataclass

import cv2
import numpy as np


@dataclass(frozen=True)
class CapturedFrame:
    image: np.ndarray
    captured_at: float
    sequence: int


class LatestFrameCamera:
    """Capture continuously and expose only the newest frame.

    This deliberately has no frame queue: if processing is slow, old frames are
    overwritten instead of accumulating latency.
    """

    def __init__(self, device: int = 0, width: int = 1280, height: int = 720, fps: int = 30):
        self.capture = cv2.VideoCapture(device)
        self.capture.set(cv2.CAP_PROP_FRAME_WIDTH, width)
        self.capture.set(cv2.CAP_PROP_FRAME_HEIGHT, height)
        self.capture.set(cv2.CAP_PROP_FPS, fps)
        self.capture.set(cv2.CAP_PROP_BUFFERSIZE, 1)

        if not self.capture.isOpened():
            raise RuntimeError(f"Could not open camera device {device}")

        self._lock = threading.Lock()
        self._latest: CapturedFrame | None = None
        self._running = False
        self._thread: threading.Thread | None = None
        self._sequence = 0

    def start(self) -> "LatestFrameCamera":
        self._running = True
        self._thread = threading.Thread(target=self._capture_loop, name="camera-capture", daemon=True)
        self._thread.start()
        return self

    def _capture_loop(self) -> None:
        while self._running:
            ok, image = self.capture.read()
            captured_at = time.perf_counter()
            if not ok:
                continue

            self._sequence += 1
            frame = CapturedFrame(image=image, captured_at=captured_at, sequence=self._sequence)
            with self._lock:
                self._latest = frame

    def latest(self) -> CapturedFrame | None:
        with self._lock:
            return self._latest

    def stop(self) -> None:
        self._running = False
        if self._thread is not None:
            self._thread.join(timeout=1.0)
        self.capture.release()
