from materials_final import _write_wbw_ass


def test_wbw_ass_renders_english_words_without_pt_translation(tmp_path):
    path = tmp_path / "wbw.ass"
    words = [
        {"text": "I", "start_ms": 0, "end_ms": 900},
        {"text": "know", "start_ms": 900, "end_ms": 2000},
        {"text": "this", "start_ms": 2000, "end_ms": 3000},
    ]

    _write_wbw_ass(
        {
            "cues": [{
                "speech_start_ms": 0,
                "speech_end_ms": 4000,
                "words": words,
                "pt": "NAO EXIBIR TRADUCAO",
            }]
        },
        0,
        4000,
        path,
    )

    rendered = path.read_text(encoding="utf-8-sig")
    assert "I know this" in rendered.replace(r"{\c&H55E6AA&}", "").replace(r"{\c&HFFFFFF&}", "")
    assert "NAO EXIBIR TRADUCAO" not in rendered
