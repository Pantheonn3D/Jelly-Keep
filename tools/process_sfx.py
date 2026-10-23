"""Turn raw ElevenLabs takes into tight, game-ready one-shots (mono 44.1 kHz)."""
import soundfile as sf, numpy as np, glob, os, sys, json
from scipy.signal import butter, sosfiltfilt
SR = 44100
SRC, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)

def load(p):
    x, sr = sf.read(p, always_2d=True); x = x.mean(1)
    if sr != SR: raise SystemExit(f'{p}: expected {SR} Hz')
    x = sosfiltfilt(butter(2, 35, 'hp', fs=SR, output='sos'), x)   # DC + rumble
    return x

def env_db(x, ms=3):
    w = max(1, int(ms / 1000 * SR))
    e = np.sqrt(np.convolve(x ** 2, np.ones(w) / w, 'same'))
    return 20 * np.log10(e + 1e-9)

def onset_before(e, peak_i, drop=28):
    """Walk back from the peak to where the sound rises out of the floor."""
    thr = e[peak_i] - drop; i = peak_i
    while i > 0 and e[i] > thr: i -= 1
    return max(0, i - int(0.003 * SR))

def events(x, min_gap=0.15, within=16):
    e = env_db(x, 5); top = e.max(); found = []
    order = np.argsort(e)[::-1]
    for i in order:
        if e[i] < top - within: break
        if all(abs(i - j) > min_gap * SR for j in found): found.append(int(i))
    return sorted(found), e

def finish(x, max_len, floor=-48, fade_min=0.03):
    e = env_db(x); pk = e.max()
    keep = np.where(e > pk + floor)[0]
    end = min(len(x), keep[-1] + int(0.02 * SR) if len(keep) else len(x), int(max_len * SR))
    x = x[:end].copy()
    fo = max(int(fade_min * SR), int(len(x) * 0.25))
    fo = min(fo, len(x) - 1)
    x[-fo:] *= np.cos(np.linspace(0, np.pi / 2, fo)) ** 2
    fi = int(0.0015 * SR); x[:fi] *= np.linspace(0, 1, fi)
    return x

def loud(x):  # loudness of the active part (RMS over > -30 dB region)
    e = env_db(x); a = x[e > e.max() - 30]
    return 20 * np.log10(np.sqrt(np.mean(a ** 2)) + 1e-9)

def normalize(x, target=-16, ceil=-1.0):
    g = 10 ** ((target - loud(x)) / 20); x = x * g
    pk = np.abs(x).max(); lim = 10 ** (ceil / 20)
    if pk > lim: x = x * lim / pk
    return x

def main_hit(x, max_len):
    e = env_db(x); p = int(np.argmax(e)); s = onset_before(e, p)
    return finish(x[s:], max_len)

def takes(prefix): return sorted(glob.glob(f'{SRC}/{prefix}*.wav'))

report = {}
def save(name, xs):
    report[name] = []
    for k, x in enumerate(xs, 1):
        x = normalize(x)
        sf.write(f'{OUT}/{name}_{k}.wav', x.astype(np.float32), SR, subtype='PCM_16')
        report[name].append(round(len(x) / SR, 3))

# --- slingshot: clean snaps, just trim
save('popper_shoot', [main_hit(load(p), 0.16) for p in takes('Small_wooden_slingsh')])

# --- stone bonk: first impact only, fade before the rattle and the noise floor
pb = []
for p in takes('Small_stone_bonking'):
    x = load(p); e = env_db(x); first = int(np.argmax(e[: int(0.25 * SR)]))
    s = onset_before(e, first, 24); y = x[s: s + int(0.16 * SR)].copy()
    fo = int(0.09 * SR); y[-fo:] *= np.cos(np.linspace(0, np.pi / 2, fo)) ** 2
    y[: int(0.0015 * SR)] *= np.linspace(0, 1, int(0.0015 * SR))
    pb.append(y)
from scipy.signal import resample_poly
def bubble(name):
    x, sr = sf.read(f'bubble/wav/02_Clean_Originals/{name}.wav', always_2d=True); x = x.mean(1)
    return resample_poly(x, 147, 160) if sr == 48000 else x
fats = [bubble(f'2_Pops/pop_fat_0{k}') for k in (1, 2, 3, 2)]
layered = []
for k, y in enumerate(pb):
    f = fats[k] / np.abs(fats[k]).max(); y = y / np.abs(y).max()
    out = np.zeros(max(len(y), len(f))); out[:len(f)] += f * 0.9; out[:len(y)] += y * 0.55
    layered.append(finish(out, 0.16))
save('pebble_hit', layered)
save('strike', [bubble(f'1_Bubble_Bloops/squish_0{k}') for k in range(1, 5)])
save('strike_hit', [bubble(f'1_Bubble_Bloops/bloop_0{k}') for k in range(2, 6)])
save('deny', [bubble('9_UI_Kit/ui_error')])
save('click', [bubble(f'4_Clicks_Tactile/click_soft_0{k}') for k in range(1, 4)])
save('panel_open', [bubble('9_UI_Kit/ui_open_panel')])
save('panel_close', [bubble('9_UI_Kit/ui_close_panel')])
save('coin', [bubble(f'6_Glitch_Sparkle/sparkle_ping_0{k}') for k in range(1, 9)])

# --- brass tube: onset relative to the MAIN peak, ignore the pre-blip
save('mortar_launch', [main_hit(load(p), 0.7) for p in takes('Hollow_brass_tube')])

# --- jelly slam
save('wall_slam', [main_hit(load(p), 0.9) for p in takes('Heavy_jelly_body')])

# --- honey: every take holds several splats; slice them all, keep the strongest few
splats = []
for p in takes('Thick_sticky_honey'):
    x = load(p); ev, e = events(x, 0.18, 14)
    for j, i in enumerate(ev):
        s = onset_before(e, i, 26)
        nxt = ev[j + 1] if j + 1 < len(ev) else len(x)
        nxt_on = onset_before(e, nxt, 26) if j + 1 < len(ev) else len(x)
        seg = x[s: min(nxt_on, s + int(0.55 * SR))]
        if len(seg) > 0.12 * SR: splats.append((e[i], finish(seg, 0.19)))
splats.sort(key=lambda t: -t[0])
save('goo_splat', [s for _, s in splats[:6]])

# --- planks + plop: keep the opening knocks, then drop the plop in right after
builds = []
for p in takes('Wooden_planks_knocke'):
    x = load(p); e = env_db(x, 5)
    s = onset_before(e, int(np.argmax(e[: int(0.6 * SR)])), 26)
    knocks = x[s: s + int(0.42 * SR)].copy()
    fo = int(0.08 * SR); knocks[-fo:] *= np.cos(np.linspace(0, np.pi / 2, fo)) ** 2
    late = int(1.2 * SR); pi = late + int(np.argmax(e[late:])); ps = onset_before(e, pi, 26)
    plop = finish(x[ps: ps + int(1.0 * SR)], 0.9)
    at = int(0.30 * SR); y = np.zeros(max(len(knocks), at + len(plop)))
    y[: len(knocks)] += knocks; y[at: at + len(plop)] += plop
    builds.append(finish(y, 1.2))
save('tower_build', builds)

# --- shimmer: shorten the tail
save('tower_upgrade', [finish(load(p)[max(0, onset_before(env_db(load(p)), int(np.argmax(env_db(load(p)))), 30)):], 1.5) for p in takes('Rising_magical_shimm')])

print(json.dumps(report, indent=1))
