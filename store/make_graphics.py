"""Play 스토어용 아이콘(512x512)과 그래픽 이미지(1024x500)를 런처 아이콘 벡터와 같은 모양으로 그립니다.

    python store/make_graphics.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).parent
BLUE = (61, 90, 254)
WHITE = (255, 255, 255)
SS = 4  # 슈퍼샘플링 배율


def draw_mark(d: ImageDraw.ImageDraw, cx: float, cy: float, unit: float) -> None:
    """런처 아이콘 전경(108 뷰포트 기준 반지름 24 링 + 오른쪽 반원)을 그립니다."""
    ro, ri = 24 * unit, 19 * unit
    d.ellipse([cx - ro, cy - ro, cx + ro, cy + ro], fill=WHITE)
    d.ellipse([cx - ri, cy - ri, cx + ri, cy + ri], fill=BLUE)
    d.pieslice([cx - ri, cy - ri, cx + ri, cy + ri], -90, 90, fill=WHITE)


def icon() -> None:
    size = 512 * SS
    img = Image.new("RGB", (size, size), BLUE)
    draw_mark(ImageDraw.Draw(img), size / 2, size / 2, size / 108 * 1.25)
    img.resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png")


def feature() -> None:
    w, h = 1024 * SS, 500 * SS
    img = Image.new("RGB", (w, h), BLUE)
    d = ImageDraw.Draw(img)
    draw_mark(d, 250 * SS, h / 2, 6.2 * SS)
    title = ImageFont.truetype(r"C:\Windows\Fonts\malgunbd.ttf", 92 * SS)
    sub = ImageFont.truetype(r"C:\Windows\Fonts\malgun.ttf", 38 * SS)
    d.text((440 * SS, 168 * SS), "색상 반전", font=title, fill=WHITE)
    d.text((444 * SS, 296 * SS), "버튼 한 번으로 켜고 끄기", font=sub, fill=(222, 228, 255))
    img.resize((1024, 500), Image.LANCZOS).save(OUT / "feature-1024x500.png")


if __name__ == "__main__":
    icon()
    feature()
