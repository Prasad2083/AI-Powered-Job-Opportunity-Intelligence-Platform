#!/usr/bin/env python3
"""Explicit test fixture only. Never used as an application fallback."""
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import sys

class Fixture(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200); self.end_headers(); self.wfile.write(b'{"models":[]}')
    def do_POST(self):
        if self.headers.get('Transfer-Encoding', '').lower() == 'chunked':
            chunks = []
            while True:
                size = int(self.rfile.readline().strip().split(b';')[0], 16)
                if size == 0:
                    while self.rfile.readline().strip():
                        pass
                    break
                chunks.append(self.rfile.read(size)); self.rfile.read(2)
            raw = b''.join(chunks)
        else:
            raw = self.rfile.read(int(self.headers.get('Content-Length', '0')))
        payload = json.loads(raw)
        if self.path != '/api/chat' or payload.get('model') != 'qwen3:4b':
            self.send_error(400); return
        result = {'message': {'role': 'assistant', 'content': json.dumps({'highlightLineNumbers': [1]})}}
        self.send_response(200); self.send_header('Content-Type', 'application/json'); self.end_headers()
        self.wfile.write(json.dumps(result).encode())
    def log_message(self, *_args):
        pass

HTTPServer(('127.0.0.1', int(sys.argv[1]) if len(sys.argv)>1 else 11435), Fixture).serve_forever()
