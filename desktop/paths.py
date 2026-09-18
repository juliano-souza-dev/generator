from __future__ import annotations

import os
import sys
from pathlib import Path

APP_DIR_NAME = "ImmersionHub Generator"


def resource_dir() -> Path:
    frozen_root = getattr(sys, "_MEIPASS", None)
    if frozen_root:
        return Path(frozen_root).resolve()
    return Path(__file__).resolve().parents[1]


def user_data_dir() -> Path:
    if sys.platform == "win32":
        root = Path(os.environ.get("LOCALAPPDATA") or Path.home() / "AppData" / "Local")
    elif sys.platform == "darwin":
        root = Path.home() / "Library" / "Application Support"
    else:
        root = Path(os.environ.get("XDG_DATA_HOME") or Path.home() / ".local" / "share")
    return (root / APP_DIR_NAME).resolve()


def prepare_environment() -> Path:
    data = user_data_dir()
    data.mkdir(parents=True, exist_ok=True)
    os.environ["GENERATOR_RESOURCE_DIR"] = str(resource_dir())
    os.environ["GENERATOR_DATA_DIR"] = str(data)
    return data
