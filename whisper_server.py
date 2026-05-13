"""
Runs on http://localhost:5050.
Endpoints:
    GET  /health -> {"status": "ok", "model": "base"}
    POST /start  -> begin microphone recording
    POST /stop   -> stop recording, transcribe, return text
"""

import sys
import threading

import numpy as np
try:
    import sounddevice as sd
except OSError as e:
    print(f"[INIT] sounddevice error: {e}")
    sd = None
import whisper
from flask import Flask, jsonify

PORT = 5050
SAMPLE_RATE = 16_000
MODEL_SIZE = "base"  #tiny or base
LANGUAGE = "fr"

_recording = False
_audio_buf = []
_lock = threading.Lock()

app = Flask(__name__)
model = None


def _load_model():
    global model
    if model is None:
        print(f"[Whisper] Loading model '{MODEL_SIZE}'...")
        #model = WhisperModel("tiny", device="cpu", compute_type="int8_float32")
        model = whisper.load_model(MODEL_SIZE)
        print("[Whisper] Model ready")


def _audio_callback(indata, frames, time_info, status):
    if status:
        print(f"[Audio] {status}")
    if _recording:
        with _lock:
            _audio_buf.extend(indata[:, 0].tolist())


_stream = None
if sd:
    try:
        _stream = sd.InputStream(
            samplerate=SAMPLE_RATE,
            channels=1,
            dtype="float32",
            callback=_audio_callback,
        )
        _stream.start()
        print("[Audio] Microphone stream open")
    except Exception as e:
        print(f"[Audio] ERROR: Could not open microphone stream - {e}")
        _stream = None
else:
    print("[Audio] sounddevice not available, microphone disabled.")


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": MODEL_SIZE, "language": LANGUAGE})


@app.route("/start", methods=["POST"])
def start():
    global _recording, _audio_buf
    _load_model()
    with _lock:
        _audio_buf = []
        _recording = True
    print("[Record] Recording started")
    return jsonify({"status": "recording"})


@app.route("/stop", methods=["POST"])
def stop():
    global _recording
    _recording = False

    with _lock:
        buf = list(_audio_buf)
        _audio_buf.clear()

    print(f"[Record] Recording stopped - {len(buf) / SAMPLE_RATE:.1f}s of audio")

    if len(buf) < SAMPLE_RATE * 0.4:
        return jsonify({"text": "", "error": "audio_too_short"})

    audio_np = np.array(buf, dtype=np.float32)

    print("[Whisper] Transcribing...")
    result = model.transcribe(audio_np, language=LANGUAGE, fp16=False)
    text = result.get("text", "").strip()
    """"
    print("[DEBUG] Starting transcription")

    segments, info = model.transcribe(audio_np, language=LANGUAGE)

    print("[DEBUG] Got segments, iterating...")

    text = ""
    for segment in segments:
        print("[DEBUG] segment:", segment.text)
        text += segment.text
    """
    text = text.strip()

    print("[DEBUG] Final text:", text)
    #
    print(f"[Whisper] -> \"{text}\"")

    return jsonify({"text": text})


if __name__ == "__main__":
    print("=" * 56)
    print("  7anouti-E Whisper STT Server")
    print(f"  Model: {MODEL_SIZE} | Language: {LANGUAGE}")
    print(f"  URL:   http://localhost:{PORT}")
    print("=" * 56)
    app.run(host="127.0.0.1", port=PORT, threaded=True)
