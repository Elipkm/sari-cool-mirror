import {
  FaceLandmarker,
  FilesetResolver,
} from "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/+esm";

const video = document.querySelector("#camera");
const canvas = document.querySelector("#stage");
const ctx = canvas.getContext("2d", { alpha: false });
const statusEl = document.querySelector("#status");
const fileInput = document.querySelector("#file");
const removeButton = document.querySelector("#remove");
const modeSelect = document.querySelector("#mode");
const scaleInput = document.querySelector("#scale");

let faceLandmarker = null;
let accessory = null;
let accessoryUrl = null;
let lastVideoTime = -1;
let lastLandmarks = null;
let smoothed = null;

const MODEL_URL =
  "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task";

function setStatus(message) {
  statusEl.textContent = message;
}

async function startCamera() {
  const stream = await navigator.mediaDevices.getUserMedia({
    audio: false,
    video: {
      facingMode: "user",
      width: { ideal: 1280 },
      height: { ideal: 720 },
    },
  });

  video.srcObject = stream;
  await video.play();

  canvas.width = video.videoWidth || 1280;
  canvas.height = video.videoHeight || 720;
}

async function startFaceTracking() {
  const vision = await FilesetResolver.forVisionTasks(
    "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm"
  );

  faceLandmarker = await FaceLandmarker.createFromOptions(vision, {
    baseOptions: {
      modelAssetPath: MODEL_URL,
      delegate: "GPU",
    },
    runningMode: "VIDEO",
    numFaces: 1,
    minFaceDetectionConfidence: 0.5,
    minFacePresenceConfidence: 0.5,
    minTrackingConfidence: 0.5,
  });
}

function mirroredPoint(landmark) {
  return {
    x: (1 - landmark.x) * canvas.width,
    y: landmark.y * canvas.height,
  };
}

function distance(a, b) {
  return Math.hypot(a.x - b.x, a.y - b.y);
}

function lerp(a, b, amount) {
  return a + (b - a) * amount;
}

function smoothTransform(target) {
  if (!smoothed) {
    smoothed = { ...target };
    return smoothed;
  }

  const amount = 0.28;
  smoothed.x = lerp(smoothed.x, target.x, amount);
  smoothed.y = lerp(smoothed.y, target.y, amount);
  smoothed.width = lerp(smoothed.width, target.width, amount);
  smoothed.angle = lerp(smoothed.angle, target.angle, amount);
  return smoothed;
}

function calculateTransform(landmarks) {
  const leftEye = mirroredPoint(landmarks[33]);
  const rightEye = mirroredPoint(landmarks[263]);
  const forehead = mirroredPoint(landmarks[10]);

  const eyeDistance = distance(leftEye, rightEye);
  const eyeCenter = {
    x: (leftEye.x + rightEye.x) / 2,
    y: (leftEye.y + rightEye.y) / 2,
  };

  const angle = Math.atan2(rightEye.y - leftEye.y, rightEye.x - leftEye.x);
  const userScale = Number(scaleInput.value) / 100;
  const mode = modeSelect.value;

  if (mode === "glasses") {
    return {
      x: eyeCenter.x,
      y: eyeCenter.y,
      width: eyeDistance * 2.15 * userScale,
      angle,
    };
  }

  if (mode === "hat") {
    return {
      x: forehead.x,
      y: forehead.y - eyeDistance * 0.75,
      width: eyeDistance * 3.5 * userScale,
      angle,
    };
  }

  if (mode === "crown") {
    return {
      x: forehead.x,
      y: forehead.y - eyeDistance * 0.62,
      width: eyeDistance * 2.8 * userScale,
      angle,
    };
  }

  return {
    x: eyeCenter.x,
    y: eyeCenter.y,
    width: eyeDistance * 2.5 * userScale,
    angle,
  };
}

function drawAccessory(landmarks) {
  if (!accessory || !landmarks) return;

  const transform = smoothTransform(calculateTransform(landmarks));
  const aspect = accessory.naturalHeight / accessory.naturalWidth;
  const height = transform.width * aspect;

  ctx.save();
  ctx.translate(transform.x, transform.y);
  ctx.rotate(transform.angle);
  ctx.drawImage(
    accessory,
    -transform.width / 2,
    -height / 2,
    transform.width,
    height
  );
  ctx.restore();
}

function renderCamera() {
  ctx.save();
  ctx.translate(canvas.width, 0);
  ctx.scale(-1, 1);
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
  ctx.restore();
}

function updateTracking() {
  if (!faceLandmarker || video.readyState < 2) return;
  if (video.currentTime === lastVideoTime) return;

  lastVideoTime = video.currentTime;
  const result = faceLandmarker.detectForVideo(video, performance.now());
  lastLandmarks = result.faceLandmarks?.[0] ?? null;
}

function render() {
  if (video.readyState >= 2) {
    renderCamera();
    updateTracking();
    drawAccessory(lastLandmarks);

    if (faceLandmarker) {
      setStatus(lastLandmarks ? "Face tracked" : "Show your face to the camera");
    }
  }

  requestAnimationFrame(render);
}

function removeAccessory() {
  accessory = null;
  smoothed = null;
  fileInput.value = "";
  removeButton.disabled = true;

  if (accessoryUrl) {
    URL.revokeObjectURL(accessoryUrl);
    accessoryUrl = null;
  }
}

fileInput.addEventListener("change", () => {
  const file = fileInput.files?.[0];
  if (!file) return;

  if (accessoryUrl) URL.revokeObjectURL(accessoryUrl);
  accessoryUrl = URL.createObjectURL(file);

  const image = new Image();
  image.onload = () => {
    accessory = image;
    smoothed = null;
    removeButton.disabled = false;
  };
  image.src = accessoryUrl;
});

removeButton.addEventListener("click", removeAccessory);
modeSelect.addEventListener("change", () => { smoothed = null; });
scaleInput.addEventListener("input", () => { smoothed = null; });

async function main() {
  try {
    setStatus("Starting camera…");
    await startCamera();

    setStatus("Loading face tracking…");
    await startFaceTracking();

    setStatus("Ready — upload an accessory");
    requestAnimationFrame(render);
  } catch (error) {
    console.error(error);
    setStatus(`Could not start: ${error.message}`);
  }
}

main();
