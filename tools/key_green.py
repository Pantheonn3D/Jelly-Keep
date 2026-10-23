"""Green-screen key with edge un-mixing + despill, premultiplied downscale, tight crop."""
import numpy as np, sys
from PIL import Image
SRC = '/mnt/user-data/attachments/a9217a08-2e63-5e99-8347-b9b31f29db41/'
OUT = sys.argv[1]
JOBS = {  # file -> (name, max side px)
    'e7e68024': ('gloom', 420), '40c0f950': ('boss', 560), '0b3b2d9f': ('skitter', 360), '053c642b': ('brute', 480),
    '8bb5b82d': ('stump', 380), '47d5f92b': ('coin', 128), '06fa7510': ('pebble', 96), '9fc28c41': ('goo_ball', 160),
    '90b54c52': ('puddle', 360), '63d6de4c': ('chest', 320), 'f6e6d2e3': ('chest_open', 420),
}
def key(path):
    a = np.asarray(Image.open(path).convert('RGB')).astype(np.float32)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    G = g - np.maximum(r, b)                                  # greenness
    alpha = np.clip((150 - G) / 120.0, 0, 1)                   # G<=30 solid, G>=150 gone
    K = np.array([0, 255, 0], np.float32)
    al = np.maximum(alpha, 1e-3)[..., None]
    fg = (a - (1 - alpha[..., None]) * K) / al                 # un-mix the key colour from edges
    fg = np.where(alpha[..., None] > 0.02, fg, 0)
    edge = (G > 5)
    lim = np.maximum(fg[..., 0], fg[..., 2])                   # despill: green can't exceed max(r,b) on edges
    fg[..., 1] = np.where(edge, np.minimum(fg[..., 1], lim), fg[..., 1])
    fg = np.clip(fg, 0, 255)
    alpha[alpha < 0.04] = 0
    return np.dstack([fg, alpha * 255]).astype(np.uint8)
def crop_resize(rgba, size):
    im = Image.fromarray(rgba, 'RGBA'); bb = im.getchannel('A').point(lambda v: 255 if v > 20 else 0).getbbox(); im = im.crop(bb)
    s = size / max(im.size)
    im = im.convert('RGBa').resize((max(1, round(im.size[0] * s)), max(1, round(im.size[1] * s))), Image.LANCZOS).convert('RGBA')
    return im
for f, (name, size) in JOBS.items():
    im = crop_resize(key(SRC + f + '-image.jpg'), size)
    im.save(f'{OUT}/{name}.webp', 'WEBP', quality=92, method=6)
    print(name, im.size)
bg = Image.open(SRC + '81dc1d55-image.jpg').convert('RGB')
bg = bg.resize((1080, round(1080 * bg.size[1] / bg.size[0])), Image.LANCZOS)
bg.save(f'{OUT}/ground.webp', 'WEBP', quality=86, method=6); print('ground', bg.size)
