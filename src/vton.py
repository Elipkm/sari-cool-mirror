from __future__ import annotations

from abc import ABC, abstractmethod

import cv2
import numpy as np


class VtonProcessor(ABC):
    @abstractmethod
    def process(self, frame: np.ndarray) -> np.ndarray:
        """Return a frame containing the virtual try-on result."""
        raise NotImplementedError


class PlaceholderVtonProcessor(VtonProcessor):
    """Temporary integration point for a local or remote VTON engine."""

    def process(self, frame: np.ndarray) -> np.ndarray:
        output = frame.copy()
        cv2.putText(
            output,
            "VTON PLACEHOLDER",
            (30, 110),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.8,
            (255, 255, 255),
            2,
            cv2.LINE_AA,
        )
        return output
