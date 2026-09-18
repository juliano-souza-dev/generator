from __future__ import annotations

import socket
import threading
import time
from dataclasses import dataclass

import uvicorn

LOOPBACK_HOST = "127.0.0.1"


def find_free_port(host: str = LOOPBACK_HOST) -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind((host, 0))
        return int(sock.getsockname()[1])


@dataclass
class DesktopRuntime:
    host: str = LOOPBACK_HOST
    port: int = 0
    startup_timeout: float = 25.0

    def __post_init__(self) -> None:
        if not self.port:
            self.port = find_free_port(self.host)
        self._server: uvicorn.Server | None = None
        self._thread: threading.Thread | None = None

    @property
    def url(self) -> str:
        return f"http://{self.host}:{self.port}"

    def start(self) -> str:
        if self._thread and self._thread.is_alive():
            return self.url

        from app import app as fastapi_app

        config = uvicorn.Config(
            fastapi_app,
            host=self.host,
            port=self.port,
            log_level="warning",
            access_log=False,
            log_config=None,
        )
        self._server = uvicorn.Server(config)
        self._thread = threading.Thread(
            target=self._server.run,
            name="generator-backend",
            daemon=True,
        )
        self._thread.start()
        self._wait_until_ready()
        return self.url

    def _wait_until_ready(self) -> None:
        deadline = time.monotonic() + self.startup_timeout
        while time.monotonic() < deadline:
            if self._thread and not self._thread.is_alive():
                raise RuntimeError("O backend do Generator encerrou durante a inicialização.")
            try:
                with socket.create_connection((self.host, self.port), timeout=0.25):
                    return
            except OSError:
                time.sleep(0.1)
        raise RuntimeError("O backend do Generator não iniciou dentro do tempo esperado.")

    def stop(self) -> None:
        if self._server is not None:
            self._server.should_exit = True
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=5)
