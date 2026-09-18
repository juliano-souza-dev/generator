from __future__ import annotations

import sys
import traceback
import urllib.request

from desktop.paths import prepare_environment
from desktop.runtime import DesktopRuntime

WINDOW_TITLE = "ImmersionHub Generator"


def run_healthcheck() -> int:
    data_dir = prepare_environment()
    error_log = data_dir / "healthcheck-error.log"
    error_log.unlink(missing_ok=True)
    runtime = DesktopRuntime()
    try:
        url = runtime.start()
        with urllib.request.urlopen(url + "/", timeout=10) as response:
            return 0 if response.status == 200 else 2
    except Exception:
        error_log.write_text(traceback.format_exc(), encoding="utf-8")
        return 1
    finally:
        runtime.stop()


def run_desktop() -> int:
    prepare_environment()

    try:
        import webview
    except ImportError as exc:
        raise RuntimeError("A camada desktop não foi empacotada corretamente.") from exc

    runtime = DesktopRuntime()
    url = runtime.start()

    webview.settings["ALLOW_DOWNLOADS"] = True
    webview.settings["OPEN_EXTERNAL_LINKS_IN_BROWSER"] = True
    webview.create_window(
        WINDOW_TITLE,
        url=url,
        width=1440,
        height=920,
        min_size=(1024, 700),
        resizable=True,
    )

    try:
        webview.start(gui="edgechromium")
        return 0
    finally:
        runtime.stop()


def main() -> int:
    if "--healthcheck" in sys.argv:
        return run_healthcheck()
    return run_desktop()


if __name__ == "__main__":
    raise SystemExit(main())
