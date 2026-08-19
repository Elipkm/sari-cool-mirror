from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError
import json
import os

BASE = Path(__file__).parent
ROOT = BASE / "web"
ENV_FILE = BASE / ".env"


def load_env_file():
    if not ENV_FILE.exists():
        return
    for raw in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


load_env_file()
os.chdir(ROOT)


class MirrorHandler(SimpleHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/api/realtime-token":
            self.send_error(404)
            return

        api_key = os.getenv("DECART_API_KEY")
        if not api_key:
            self._json(500, {"error": "DECART_API_KEY is missing. Copy .env.example to .env and add your key."})
            return

        payload = json.dumps({
            "expiresIn": 300,
            "allowedModels": ["lucy-2.1"],
        }).encode("utf-8")

        request = Request(
            "https://api.decart.ai/v1/client/tokens",
            data=payload,
            method="POST",
            headers={
                "x-api-key": api_key,
                "Content-Type": "application/json",
            },
        )

        try:
            with urlopen(request, timeout=15) as response:
                data = json.loads(response.read().decode("utf-8"))
                self._json(200, data)
        except HTTPError as error:
            body = error.read().decode("utf-8", errors="replace")
            self._json(error.code, {"error": f"Decart token request failed: {body}"})
        except Exception as error:
            self._json(502, {"error": f"Decart token request failed: {error}"})

    def _json(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


server = ThreadingHTTPServer(("127.0.0.1", 8000), MirrorHandler)
print("Sari Cool Mirror running at http://localhost:8000")
print("AI prompts require DECART_API_KEY in .env")
print("Press Ctrl+C to stop.")

try:
    server.serve_forever()
except KeyboardInterrupt:
    pass
finally:
    server.server_close()
