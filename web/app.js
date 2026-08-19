import { FaceLandmarker, FilesetResolver } from "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/+esm";
import { createDecartClient, models } from "https://esm.sh/@decartai/sdk";

const video = document.querySelector("#camera");
const aiOutput = document.querySelector("#ai-output");
const canvas = document.querySelector("#stage");
const ctx = canvas.getContext("2d", { alpha: false });
const statusEl = document.querySelector("#status");
const fileInput = document.querySelector("#file");
const removeButton = document.querySelector("#remove");
const modeSelect = document.querySelector("#mode");
const scaleInput = document.querySelector("#scale");
const aiForm = document.querySelector("#ai-form");
const promptInput = document.querySelector("#prompt");
const applyAiButton = document.querySelector("#apply-ai");
const stopAiButton = document.querySelector("#stop-ai");

let faceLandmarker = null;
let accessory = null;
let accessoryUrl = null;
let lastVideoTime = -1;
let lastLandmarks = null;
let smoothed = null;
let cameraStream = null;
let realtimeClient = null;

const MODEL_URL = "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task";
const AI_MODEL = models.realtime("lucy-2.1");

function setStatus(message) { statusEl.textContent = message; }

async function startCamera() {
  cameraStream = await navigator.mediaDevices.getUserMedia({
    audio: false,
    video: {
      facingMode: "user",
      frameRate: { ideal: AI_MODEL.fps || 30 },
      width: { ideal: AI_MODEL.width || 1280 },
      height: { ideal: AI_MODEL.height || 720 },
    },
  });
  video.srcObject = cameraStream;
  await video.play();
  canvas.width = video.videoWidth || 1280;
  canvas.height = video.videoHeight || 720;
}

async function startFaceTracking() {
  const vision = await FilesetResolver.forVisionTasks("https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm");
  faceLandmarker = await FaceLandmarker.createFromOptions(vision, {
    baseOptions: { modelAssetPath: MODEL_URL, delegate: "GPU" },
    runningMode: "VIDEO",
    numFaces: 1,
    minFaceDetectionConfidence: 0.5,
    minFacePresenceConfidence: 0.5,
    minTrackingConfidence: 0.5,
  });
}

function mirroredPoint(l) { return { x: (1 - l.x) * canvas.width, y: l.y * canvas.height }; }
function distance(a, b) { return Math.hypot(a.x - b.x, a.y - b.y); }
function lerp(a, b, amount) { return a + (b - a) * amount; }

function smoothTransform(target) {
  if (!smoothed) smoothed = { ...target };
  else {
    const a = 0.28;
    smoothed.x = lerp(smoothed.x, target.x, a);
    smoothed.y = lerp(smoothed.y, target.y, a);
    smoothed.width = lerp(smoothed.width, target.width, a);
    smoothed.angle = lerp(smoothed.angle, target.angle, a);
  }
  return smoothed;
}

function calculateTransform(landmarks) {
  const leftEye = mirroredPoint(landmarks[33]);
  const rightEye = mirroredPoint(landmarks[263]);
  const forehead = mirroredPoint(landmarks[10]);
  const eyeDistance = distance(leftEye, rightEye);
  const eyeCenter = { x: (leftEye.x + rightEye.x) / 2, y: (leftEye.y + rightEye.y) / 2 };
  const angle = Math.atan2(rightEye.y - leftEye.y, rightEye.x - leftEye.x);
  const userScale = Number(scaleInput.value) / 100;
  const mode = modeSelect.value;
  if (mode === "glasses") return { x: eyeCenter.x, y: eyeCenter.y, width: eyeDistance * 2.15 * userScale, angle };
  if (mode === "hat") return { x: forehead.x, y: forehead.y - eyeDistance * 0.75, width: eyeDistance * 3.5 * userScale, angle };
  if (mode === "crown") return { x: forehead.x, y: forehead.y - eyeDistance * 0.62, width: eyeDistance * 2.8 * userScale, angle };
  return { x: eyeCenter.x, y: eyeCenter.y, width: eyeDistance * 2.5 * userScale, angle };
}

function drawAccessory(landmarks) {
  if (!accessory || !landmarks) return;
  const t = smoothTransform(calculateTransform(landmarks));
  const height = t.width * (accessory.naturalHeight / accessory.naturalWidth);
  ctx.save();
  ctx.translate(t.x, t.y);
  ctx.rotate(t.angle);
  ctx.drawImage(accessory, -t.width / 2, -height / 2, t.width, height);
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
  if (!faceLandmarker || video.readyState < 2 || video.currentTime === lastVideoTime) return;
  lastVideoTime = video.currentTime;
  const result = faceLandmarker.detectForVideo(video, performance.now());
  lastLandmarks = result.faceLandmarks?.[0] ?? null;
}

function render() {
  if (!realtimeClient && video.readyState >= 2) {
    renderCamera();
    updateTracking();
    drawAccessory(lastLandmarks);
  }
  requestAnimationFrame(render);
}

function removeAccessory() {
  accessory = null;
  smoothed = null;
  fileInput.value = "";
  removeButton.disabled = true;
  if (accessoryUrl) URL.revokeObjectURL(accessoryUrl);
  accessoryUrl = null;
}

fileInput.addEventListener("change", () => {
  const file = fileInput.files?.[0];
  if (!file) return;
  if (accessoryUrl) URL.revokeObjectURL(accessoryUrl);
  accessoryUrl = URL.createObjectURL(file);
  const image = new Image();
  image.onload = () => { accessory = image; smoothed = null; removeButton.disabled = false; };
  image.src = accessoryUrl;
});

async function getRealtimeToken() {
  const response = await fetch("/api/realtime-token", { method: "POST" });
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || "Could not create realtime token");
  return data.apiKey;
}

async function connectAi(prompt) {
  if (!cameraStream) throw new Error("Camera is not ready");
  setStatus("Connecting AI…");
  applyAiButton.disabled = true;
  const apiKey = await getRealtimeToken();
  const client = createDecartClient({ apiKey });
  realtimeClient = await client.realtime.connect(cameraStream, {
    model: AI_MODEL,
    mirror: "auto",
    initialState: { prompt: { text: prompt, enhance: true } },
    onRemoteStream: async (stream) => {
      aiOutput.srcObject = stream;
      aiOutput.classList.add("active");
      await aiOutput.play();
      setStatus("AI live");
    },
  });
  stopAiButton.disabled = false;
  applyAiButton.disabled = false;
}

async function applyAiPrompt(prompt) {
  if (!realtimeClient) return connectAi(prompt);
  setStatus("Updating AI…");
  await realtimeClient.set({ prompt, enhance: true });
  setStatus("AI live");
}

function stopAi() {
  if (realtimeClient) realtimeClient.disconnect();
  realtimeClient = null;
  aiOutput.srcObject = null;
  aiOutput.classList.remove("active");
  stopAiButton.disabled = true;
  applyAiButton.disabled = false;
  setStatus("Local mirror");
}

aiForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const prompt = promptInput.value.trim();
  if (!prompt) return;
  try { await applyAiPrompt(prompt); }
  catch (error) {
    console.error(error);
    setStatus(`AI error: ${error.message}`);
    applyAiButton.disabled = false;
  }
});

stopAiButton.addEventListener("click", stopAi);
removeButton.addEventListener("click", removeAccessory);
modeSelect.addEventListener("change", () => { smoothed = null; });
scaleInput.addEventListener("input", () => { smoothed = null; });

async function main() {
  try {
    setStatus("Starting camera…");
    await startCamera();
    setStatus("Loading face tracking…");
    await startFaceTracking();
    setStatus("Ready — local AR or enter an AI prompt");
    requestAnimationFrame(render);
  } catch (error) {
    console.error(error);
    setStatus(`Could not start: ${error.message}`);
  }
}

main();
