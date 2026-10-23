"""Adaptive green-screen key: samples each image's own backdrop, un-mixes it from edges, despills."""
import numpy as np
from PIL import Image
def key(path):
    a = np.asarray(Image.open(path).convert('RGB')).astype(np.float32)
    h, w = a.shape[:2]
    pts = np.concatenate([a[:12, :12].reshape(-1, 3), a[:12, -12:].reshape(-1, 3), a[-12:, :12].reshape(-1, 3), a[-12:, -12:].reshape(-1, 3),
                          a[:6, w // 2 - 20:w // 2 + 20].reshape(-1, 3)])
    K = np.median(pts, 0)
    gK = K[1] - max(K[0], K[2])
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    G = g - np.maximum(r, b)
    lo, hi = gK * 0.18, gK * 0.82
    alpha = np.clip((hi - G) / (hi - lo), 0, 1)
    al = np.maximum(alpha, 1e-3)[..., None]
    fg = (a - (1 - alpha[..., None]) * K) / al
    fg = np.where(alpha[..., None] > 0.02, fg, 0)
    lim = np.maximum(fg[..., 0], fg[..., 2])
    fg[..., 1] = np.where(G > 4, np.minimum(fg[..., 1], lim), fg[..., 1])
    fg = np.clip(fg, 0, 255); alpha[alpha < 0.04] = 0
    return np.dstack([fg, alpha * 255]).astype(np.uint8), K
def cut(path, size, out):
    rgba, K = key(path)
    im = Image.fromarray(rgba, 'RGBA'); bb = im.getchannel('A').point(lambda v: 255 if v > 24 else 0).getbbox(); im = im.crop(bb)
    s = size / max(im.size)
    im = im.convert('RGBa').resize((max(1, round(im.size[0] * s)), max(1, round(im.size[1] * s))), Image.LANCZOS).convert('RGBA')
    im.save(out, 'WEBP', quality=92, method=6)
    return im.size, K
