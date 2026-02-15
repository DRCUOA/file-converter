#!/usr/bin/env python3
"""Serve log-viewer.html and the log file. Run from project root, then open http://localhost:8765"""
import http.server
import os
import re
import socketserver
import xml.etree.ElementTree as ET

PORT = 8765
PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
LOGBACK_PATH = os.path.join(PROJECT_ROOT, "src", "main", "resources", "logback.xml")


def resolve_log_path():
    """Read log file path from logback.xml and resolve ${user.home}."""
    tree = ET.parse(LOGBACK_PATH)
    root = tree.getroot()
    for prop in root.findall(".//{*}property"):
        if prop.get("name") == "LOG_FILE":
            raw = prop.get("value", "")
            return re.sub(r"\$\{user\.home\}", os.path.expanduser("~"), raw)
    return os.path.expanduser("~/.file-converter.log")


class LogHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=PROJECT_ROOT, **kwargs)

    def do_GET(self):
        if self.path in ("/", "/index.html", "/log-viewer.html"):
            self.path = "/log-viewer.html"
            return super().do_GET()
        if self.path == "/log":
            self.send_log()
            return
        return super().do_GET()

    def send_log(self):
        log_path = resolve_log_path()
        try:
            with open(log_path, "r", encoding="utf-8", errors="replace") as f:
                body = f.read()
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", len(body.encode("utf-8")))
            self.end_headers()
            self.wfile.write(body.encode("utf-8"))
        except FileNotFoundError:
            self.send_error(404, "Log file not found")
        except OSError as e:
            self.send_error(500, str(e))


if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), LogHandler) as httpd:
        print(f"Log viewer: http://localhost:{PORT}")
        print(f"Log file:   {resolve_log_path()} (from {LOGBACK_PATH})")
        httpd.serve_forever()
