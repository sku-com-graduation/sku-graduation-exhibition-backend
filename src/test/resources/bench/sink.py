# S3 자리에 놓는 최소 서버. PUT 본문을 받아서 버린다.
# 벤치가 커넥션을 여러 개 동시에 열기 때문에 accept 큐를 넉넉히 잡는다.
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class Sink(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def do_PUT(self):
        # /cut/<바이트>/... 로 오면 그만큼만 읽고 연결을 끊는다. 업로드 중 실패를 만든다.
        parts = self.path.split('/')
        if len(parts) > 2 and parts[1] == 'cut':
            self._read_exactly(int(parts[2]))
            self.close_connection = True
            return
        if self.headers.get('Transfer-Encoding', '').lower() == 'chunked':
            self._drain_chunked()
        else:
            left = int(self.headers.get('Content-Length', 0))
            while left > 0:
                chunk = self.rfile.read(min(1 << 16, left))
                if not chunk:
                    return
                left -= len(chunk)
        self.send_response(200)
        self.send_header('ETag', '"stub"')
        self.send_header('Content-Length', '0')
        self.end_headers()

    def _read_exactly(self, count):
        left = count
        while left > 0:
            chunk = self.rfile.read(min(1 << 16, left))
            if not chunk:
                return
            left -= len(chunk)

    def _drain_chunked(self):
        while True:
            size = int(self.rfile.readline().split(b';')[0].strip() or b'0', 16)
            if size == 0:
                self.rfile.readline()
                return
            left = size
            while left > 0:
                chunk = self.rfile.read(min(1 << 16, left))
                if not chunk:
                    return
                left -= len(chunk)
            self.rfile.readline()

    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-Length', '2')
        self.end_headers()
        self.wfile.write(b'ok')

    def log_message(self, *args):
        pass


class Server(ThreadingHTTPServer):
    daemon_threads = True
    request_queue_size = 256
    allow_reuse_address = True


Server(('0.0.0.0', 9000), Sink).serve_forever()
