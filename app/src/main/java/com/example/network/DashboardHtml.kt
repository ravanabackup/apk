package com.example.network

object DashboardHtml {
  fun render(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>WaveBridge · Remote Media Console</title>
  <style>
    :root {
      --bg: #070b14;
      --surface: #0e1525;
      --surface-2: #151f35;
      --border: #253352;
      --text: #f1f5f9;
      --muted: #94a3b8;
      --cyan: #22d3ee;
      --cobalt: #3b82f6;
      --emerald: #10b981;
      --amber: #f59e0b;
      --red: #ef4444;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Inter, sans-serif;
      background: radial-gradient(circle at 15% 0%, rgba(34, 211, 238, 0.12), transparent 45%),
                  radial-gradient(circle at 85% 15%, rgba(59, 130, 246, 0.14), transparent 40%),
                  var(--bg);
      color: var(--text);
      min-height: 100vh;
      line-height: 1.5;
    }
    .container {
      max-width: 1180px;
      margin: 0 auto;
      padding: 24px 20px 48px;
    }
    header {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 24px;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--border);
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .logo {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      background: linear-gradient(135deg, var(--cyan), var(--cobalt));
      display: grid;
      place-items: center;
      font-weight: 800;
      color: #04101a;
      font-size: 18px;
      box-shadow: 0 0 24px rgba(34, 211, 238, 0.25);
    }
    .brand h1 { font-size: 20px; letter-spacing: -0.02em; }
    .brand p { font-size: 12px; color: var(--muted); }
    .status-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 6px 14px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
      background: var(--surface-2);
      border: 1px solid var(--border);
    }
    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--amber);
    }
    .dot.connected { background: var(--emerald); box-shadow: 0 0 10px var(--emerald); }
    .dot.offline { background: var(--red); }
    .grid {
      display: grid;
      grid-template-columns: 1.2fr 0.8fr;
      gap: 20px;
      align-items: start;
    }
    @media (max-width: 900px) {
      .grid { grid-template-columns: 1fr; }
    }
    .card {
      background: rgba(14, 21, 37, 0.88);
      backdrop-filter: blur(12px);
      border: 1px solid var(--border);
      border-radius: 20px;
      padding: 22px;
      box-shadow: 0 16px 40px rgba(0, 0, 0, 0.35);
    }
    .card-title {
      font-size: 13px;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: var(--muted);
      margin-bottom: 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
    }
    .now-playing {
      display: flex;
      gap: 22px;
      align-items: center;
    }
    @media (max-width: 600px) {
      .now-playing { flex-direction: column; align-items: stretch; }
    }
    .art-wrap {
      width: 180px;
      height: 180px;
      flex-shrink: 0;
      border-radius: 16px;
      background: linear-gradient(135deg, #13203b, #0c1426);
      border: 1px solid var(--border);
      overflow: hidden;
      display: grid;
      place-items: center;
      position: relative;
      margin: 0 auto;
    }
    .art-wrap img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: none;
    }
    .art-placeholder {
      color: var(--cyan);
      opacity: 0.6;
      font-size: 44px;
    }
    .meta {
      flex: 1;
      min-width: 0;
    }
    .badge-row {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 10px;
    }
    .badge {
      font-size: 11px;
      font-weight: 600;
      padding: 4px 10px;
      border-radius: 999px;
      background: rgba(34, 211, 238, 0.12);
      color: var(--cyan);
      border: 1px solid rgba(34, 211, 238, 0.25);
    }
    .badge.state {
      background: rgba(16, 185, 129, 0.14);
      color: var(--emerald);
      border-color: rgba(16, 185, 129, 0.3);
    }
    .badge.paused {
      background: rgba(245, 158, 11, 0.14);
      color: var(--amber);
      border-color: rgba(245, 158, 11, 0.3);
    }
    .track-title {
      font-size: 24px;
      font-weight: 700;
      letter-spacing: -0.02em;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .track-artist {
      font-size: 16px;
      color: var(--muted);
      margin-top: 4px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .track-album {
      font-size: 13px;
      color: #64748b;
      margin-top: 2px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .progress-section {
      margin-top: 20px;
    }
    input[type="range"] {
      width: 100%;
      accent-color: var(--cyan);
      cursor: pointer;
    }
    .time-row {
      display: flex;
      justify-content: space-between;
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 12px;
      color: var(--muted);
      margin-top: 4px;
    }
    .controls {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 14px;
      margin-top: 18px;
    }
    .btn {
      border: 1px solid var(--border);
      background: var(--surface-2);
      color: var(--text);
      padding: 10px 16px;
      border-radius: 12px;
      cursor: pointer;
      font-weight: 600;
      font-size: 14px;
      transition: transform 0.1s ease, background 0.15s ease, border-color 0.15s ease;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }
    .btn:hover { background: #1e2d4a; border-color: #3a4f7a; }
    .btn:active { transform: scale(0.97); }
    .btn-primary {
      background: linear-gradient(135deg, var(--cyan), var(--cobalt));
      color: #03111c;
      border: none;
      width: 56px;
      height: 56px;
      border-radius: 50%;
      font-size: 20px;
      box-shadow: 0 8px 24px rgba(34, 211, 238, 0.25);
    }
    .btn-primary:hover { filter: brightness(1.08); }
    .btn-icon {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      padding: 0;
      font-size: 16px;
    }
    .btn-sm { padding: 6px 12px; font-size: 12px; border-radius: 9px; }
    .volume-row {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-top: 18px;
      padding-top: 14px;
      border-top: 1px solid rgba(37, 51, 82, 0.6);
      font-size: 13px;
      color: var(--muted);
    }
    .sessions-bar {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 14px;
    }
    .session-chip {
      font-size: 12px;
      padding: 6px 12px;
      border-radius: 999px;
      border: 1px solid var(--border);
      background: var(--surface-2);
      color: var(--muted);
      cursor: pointer;
    }
    .session-chip.active {
      background: rgba(34, 211, 238, 0.15);
      border-color: var(--cyan);
      color: var(--cyan);
      font-weight: 600;
    }
    .audio-panel {
      margin-top: 20px;
    }
    .audio-controls {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }
    canvas#vuCanvas {
      width: 100%;
      height: 48px;
      margin-top: 14px;
      border-radius: 10px;
      background: #080d18;
      border: 1px solid var(--border);
    }
    .history-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      max-height: 480px;
      overflow-y: auto;
      padding-right: 4px;
    }
    .history-item {
      padding: 12px 14px;
      border-radius: 12px;
      background: var(--surface-2);
      border: 1px solid rgba(37, 51, 82, 0.7);
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 10px;
    }
    .history-main { min-width: 0; flex: 1; }
    .history-title {
      font-size: 14px;
      font-weight: 600;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .history-sub {
      font-size: 12px;
      color: var(--muted);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .history-meta {
      text-align: right;
      flex-shrink: 0;
    }
    .history-app {
      font-size: 11px;
      color: var(--cyan);
      font-weight: 600;
    }
    .history-time {
      font-size: 11px;
      color: #64748b;
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    }
    .empty {
      text-align: center;
      padding: 36px 12px;
      color: var(--muted);
      font-size: 14px;
    }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div class="brand">
        <div class="logo">WB</div>
        <div>
          <h1>WaveBridge Console</h1>
          <p>Live Android Media Session &amp; Scrobble Bridge</p>
        </div>
      </div>
      <div class="status-pill">
        <span id="connDot" class="dot"></span>
        <span id="connText">Connecting...</span>
      </div>
    </header>

    <div class="grid">
      <div>
        <div class="card">
          <div class="card-title">
            <span>Now Playing</span>
            <span id="sessionCountText">0 active apps</span>
          </div>

          <div class="now-playing">
            <div class="art-wrap">
              <img id="albumArt" alt="Album Art" />
              <div id="artFallback" class="art-placeholder">&#9835;</div>
            </div>

            <div class="meta">
              <div class="badge-row">
                <span id="appBadge" class="badge">No Active App</span>
                <span id="stateBadge" class="badge state">IDLE</span>
              </div>
              <div id="trackTitle" class="track-title">Waiting for media playback...</div>
              <div id="trackArtist" class="track-artist">Play Spotify, YouTube, or any media app on your phone</div>
              <div id="trackAlbum" class="track-album"></div>
            </div>
          </div>

          <div class="progress-section">
            <input id="seekSlider" type="range" min="0" max="1000" value="0" />
            <div class="time-row">
              <span id="elapsedTime">0:00</span>
              <span id="totalTime">0:00</span>
            </div>
          </div>

          <div class="controls">
            <button id="prevBtn" class="btn btn-icon" title="Previous Track">&#9198;</button>
            <button id="playPauseBtn" class="btn btn-primary" title="Play / Pause">&#9654;</button>
            <button id="nextBtn" class="btn btn-icon" title="Next Track">&#9197;</button>
          </div>

          <div class="volume-row">
            <span>Device Volume</span>
            <input id="volSlider" type="range" min="0" max="15" value="8" style="flex:1" />
            <span id="volLabel" style="min-width:44px;text-align:right;">8/15</span>
          </div>

          <div id="sessionsBar" class="sessions-bar"></div>
        </div>

        <div class="card audio-panel">
          <div class="card-title">
            <span>Live Phone Audio Stream (Web Audio API)</span>
            <span id="audioPhoneStatus" style="color:var(--amber);">Off on phone</span>
          </div>
          <div class="audio-controls">
            <button id="listenToggleBtn" class="btn">Listen Live in Browser</button>
            <div style="display:flex;align-items:center;gap:10px;min-width:180px;">
              <span style="font-size:12px;color:var(--muted);">Gain</span>
              <input id="browserGainSlider" type="range" min="0" max="150" value="100" />
            </div>
          </div>
          <p id="audioNoteText" style="font-size:12px;color:var(--muted);margin-top:10px;">
            Enable "Live Audio Stream" in the WaveBridge Android app to stream system audio over WebSocket.
          </p>
          <canvas id="vuCanvas" width="600" height="48"></canvas>
        </div>
      </div>

      <div class="card">
        <div class="card-title">
          <span>Scrobble History</span>
          <div style="display:flex;gap:8px;">
            <a class="btn btn-sm" href="/api/history/export?format=json" download="wavebridge-scrobbles.json">JSON</a>
            <a class="btn btn-sm" href="/api/history/export?format=csv" download="wavebridge-scrobbles.csv">CSV</a>
          </div>
        </div>
        <div id="historyList" class="history-list">
          <div class="empty">No tracks scrobbled yet.</div>
        </div>
      </div>
    </div>
  </div>

  <script>
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsControlUrl = `${'$'}{proto}//${'$'}{location.host}/ws/control`;
    const wsAudioUrl = `${'$'}{proto}//${'$'}{location.host}/ws/audio`;

    const connDot = document.getElementById('connDot');
    const connText = document.getElementById('connText');
    const albumArt = document.getElementById('albumArt');
    const artFallback = document.getElementById('artFallback');
    const appBadge = document.getElementById('appBadge');
    const stateBadge = document.getElementById('stateBadge');
    const trackTitle = document.getElementById('trackTitle');
    const trackArtist = document.getElementById('trackArtist');
    const trackAlbum = document.getElementById('trackAlbum');
    const seekSlider = document.getElementById('seekSlider');
    const elapsedTime = document.getElementById('elapsedTime');
    const totalTime = document.getElementById('totalTime');
    const playPauseBtn = document.getElementById('playPauseBtn');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const volSlider = document.getElementById('volSlider');
    const volLabel = document.getElementById('volLabel');
    const sessionsBar = document.getElementById('sessionsBar');
    const sessionCountText = document.getElementById('sessionCountText');
    const historyList = document.getElementById('historyList');
    const audioPhoneStatus = document.getElementById('audioPhoneStatus');
    const audioNoteText = document.getElementById('audioNoteText');
    const listenToggleBtn = document.getElementById('listenToggleBtn');
    const browserGainSlider = document.getElementById('browserGainSlider');
    const vuCanvas = document.getElementById('vuCanvas');
    const vuCtx = vuCanvas.getContext('2d');

    let controlWs = null;
    let audioWs = null;
    let isSeeking = false;
    let currentDuration = 0;
    let lastArtKey = '';
    let isListening = false;
    let audioCtx = null;
    let gainNode = null;
    let nextPlayTime = 0;
    let streamSampleRate = 44100;
    let streamChannels = 2;

    function fmtTime(ms) {
      if (!ms || ms < 0) return '0:00';
      const totalSec = Math.floor(ms / 1000);
      const mins = Math.floor(totalSec / 60);
      const secs = totalSec % 60;
      return `${'$'}{mins}:${'$'}{secs < 10 ? '0' : ''}${'$'}{secs}`;
    }

    function fmtClock(ms) {
      try {
        return new Date(ms).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      } catch (e) {
        return '';
      }
    }

    function sendCmd(payload) {
      if (controlWs && controlWs.readyState === WebSocket.OPEN) {
        controlWs.send(JSON.stringify(payload));
      }
    }

    function connectControl() {
      controlWs = new WebSocket(wsControlUrl);
      controlWs.onopen = () => {
        connDot.className = 'dot connected';
        connText.textContent = 'Connected to Phone';
      };
      controlWs.onclose = () => {
        connDot.className = 'dot offline';
        connText.textContent = 'Disconnected · Reconnecting...';
        setTimeout(connectControl, 2000);
      };
      controlWs.onerror = () => {
        controlWs.close();
      };
      controlWs.onmessage = (ev) => {
        try {
          const data = JSON.parse(ev.data);
          if (data.type === 'state') {
            renderState(data);
          }
        } catch (e) {}
      };
    }

    function renderState(data) {
      const active = data.activeSession;
      const sessions = data.sessions || [];
      sessionCountText.textContent = `${'$'}{sessions.length} active app${'$'}{sessions.length === 1 ? '' : 's'}`;

      if (active) {
        appBadge.textContent = active.appName || active.packageName;
        stateBadge.textContent = active.status;
        stateBadge.className = 'badge ' + (active.status === 'PLAYING' ? 'state' : 'paused');
        trackTitle.textContent = active.title || 'Unknown Track';
        trackArtist.textContent = active.artist || '';
        trackAlbum.textContent = active.album || '';
        playPauseBtn.innerHTML = active.status === 'PLAYING' ? '&#10074;&#10074;' : '&#9654;';

        currentDuration = active.durationMs || 0;
        if (!isSeeking) {
          seekSlider.max = currentDuration > 0 ? currentDuration : 1000;
          seekSlider.value = currentDuration > 0 ? Math.min(active.positionMs, currentDuration) : 0;
          elapsedTime.textContent = fmtTime(active.positionMs);
          totalTime.textContent = fmtTime(currentDuration);
        }

        volSlider.max = active.maxVolume || 15;
        volSlider.value = active.volume || 0;
        volLabel.textContent = `${'$'}{active.volume}/${'$'}{active.maxVolume}`;

        const artKey = active.hasArtwork ? `${'$'}{active.sessionId}_${'$'}{active.artworkHash}` : '';
        if (artKey !== lastArtKey) {
          lastArtKey = artKey;
          if (active.hasArtwork) {
            albumArt.src = `/api/artwork/${'$'}{encodeURIComponent(active.sessionId)}?h=${'$'}{encodeURIComponent(active.artworkHash)}`;
            albumArt.style.display = 'block';
            artFallback.style.display = 'none';
          } else {
            albumArt.style.display = 'none';
            artFallback.style.display = 'block';
          }
        }
      } else {
        appBadge.textContent = 'No Active App';
        stateBadge.textContent = 'IDLE';
        stateBadge.className = 'badge paused';
        trackTitle.textContent = 'Waiting for media playback...';
        trackArtist.textContent = 'Start playing audio on your Android device';
        trackAlbum.textContent = '';
        albumArt.style.display = 'none';
        artFallback.style.display = 'block';
        lastArtKey = '';
      }

      // Render session switcher chips safely
      sessionsBar.textContent = '';
      sessions.forEach((s) => {
        const chip = document.createElement('button');
        chip.className = 'session-chip' + (s.sessionId === data.activeSessionId ? ' active' : '');
        chip.textContent = `${'$'}{s.appName} · ${'$'}{s.status}`;
        chip.onclick = () => sendCmd({ action: 'selectSession', sessionId: s.sessionId });
        sessionsBar.appendChild(chip);
      });

      // Live audio status
      if (data.liveAudioActive) {
        audioPhoneStatus.textContent = 'Streaming Live';
        audioPhoneStatus.style.color = 'var(--emerald)';
      } else {
        audioPhoneStatus.textContent = 'Off on phone';
        audioPhoneStatus.style.color = 'var(--amber)';
      }
      audioNoteText.textContent = data.liveAudioNote || '';

      // Render Scrobble history safely using textContent
      const scrobbles = data.recentScrobbles || [];
      historyList.textContent = '';
      if (scrobbles.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'empty';
        empty.textContent = 'No tracks scrobbled yet.';
        historyList.appendChild(empty);
      } else {
        scrobbles.forEach((item) => {
          const row = document.createElement('div');
          row.className = 'history-item';

          const main = document.createElement('div');
          main.className = 'history-main';
          const titleEl = document.createElement('div');
          titleEl.className = 'history-title';
          titleEl.textContent = item.title;
          const subEl = document.createElement('div');
          subEl.className = 'history-sub';
          subEl.textContent = item.album ? `${'$'}{item.artist} · ${'$'}{item.album}` : item.artist;
          main.appendChild(titleEl);
          main.appendChild(subEl);

          const meta = document.createElement('div');
          meta.className = 'history-meta';
          const appEl = document.createElement('div');
          appEl.className = 'history-app';
          appEl.textContent = item.appName;
          const timeEl = document.createElement('div');
          timeEl.className = 'history-time';
          timeEl.textContent = fmtClock(item.playedAtMs);
          meta.appendChild(appEl);
          meta.appendChild(timeEl);

          row.appendChild(main);
          row.appendChild(meta);
          historyList.appendChild(row);
        });
      }
    }

    playPauseBtn.onclick = () => sendCmd({ action: 'toggle' });
    prevBtn.onclick = () => sendCmd({ action: 'previous' });
    nextBtn.onclick = () => sendCmd({ action: 'next' });

    seekSlider.addEventListener('input', () => {
      isSeeking = true;
      elapsedTime.textContent = fmtTime(Number(seekSlider.value));
    });
    seekSlider.addEventListener('change', () => {
      isSeeking = false;
      sendCmd({ action: 'seek', positionMs: Number(seekSlider.value) });
    });

    volSlider.addEventListener('change', () => {
      sendCmd({ action: 'volume', volume: Number(volSlider.value) });
    });

    browserGainSlider.addEventListener('input', () => {
      if (gainNode) {
        gainNode.gain.value = Number(browserGainSlider.value) / 100;
      }
    });

    listenToggleBtn.onclick = () => {
      if (isListening) {
        stopAudioStream();
      } else {
        startAudioStream();
      }
    };

    function startAudioStream() {
      isListening = true;
      listenToggleBtn.textContent = 'Stop Listening';
      listenToggleBtn.style.borderColor = 'var(--cyan)';

      if (!audioCtx) {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: streamSampleRate });
        gainNode = audioCtx.createGain();
        gainNode.gain.value = Number(browserGainSlider.value) / 100;
        gainNode.connect(audioCtx.destination);
      }
      if (audioCtx.state === 'suspended') {
        audioCtx.resume();
      }
      nextPlayTime = audioCtx.currentTime + 0.08;

      audioWs = new WebSocket(wsAudioUrl);
      audioWs.binaryType = 'arraybuffer';
      audioWs.onmessage = (ev) => {
        if (typeof ev.data === 'string') {
          try {
            const cfg = JSON.parse(ev.data);
            if (cfg.sampleRate) streamSampleRate = cfg.sampleRate;
            if (cfg.channels) streamChannels = cfg.channels;
          } catch (e) {}
          return;
        }
        playPcmFrame(ev.data);
      };
      audioWs.onclose = () => {
        if (isListening) stopAudioStream();
      };
    }

    function stopAudioStream() {
      isListening = false;
      listenToggleBtn.textContent = 'Listen Live in Browser';
      listenToggleBtn.style.borderColor = 'var(--border)';
      if (audioWs) {
        audioWs.close();
        audioWs = null;
      }
      drawVu(0);
    }

    function playPcmFrame(arrayBuffer) {
      if (!audioCtx || !isListening) return;
      const int16 = new Int16Array(arrayBuffer);
      const frameCount = Math.floor(int16.length / streamChannels);
      if (frameCount <= 0) return;

      const buffer = audioCtx.createBuffer(streamChannels, frameCount, streamSampleRate);
      let peak = 0;
      for (let ch = 0; ch < streamChannels; ch++) {
        const channelData = buffer.getChannelData(ch);
        for (let i = 0; i < frameCount; i++) {
          const val = int16[i * streamChannels + ch] / 32768.0;
          channelData[i] = val;
          const abs = Math.abs(val);
          if (abs > peak) peak = abs;
        }
      }

      const src = audioCtx.createBufferSource();
      src.buffer = buffer;
      src.connect(gainNode);

      const now = audioCtx.currentTime;
      if (nextPlayTime < now || nextPlayTime > now + 0.35) {
        nextPlayTime = now + 0.05;
      }
      src.start(nextPlayTime);
      nextPlayTime += buffer.duration;
      drawVu(peak);
    }

    function drawVu(level) {
      const w = vuCanvas.width;
      const h = vuCanvas.height;
      vuCtx.clearRect(0, 0, w, h);
      const bars = 36;
      const gap = 4;
      const barW = (w - gap * (bars + 1)) / bars;
      const activeBars = Math.round(level * bars * 1.4);
      for (let i = 0; i < bars; i++) {
        const x = gap + i * (barW + gap);
        const lit = i < activeBars;
        vuCtx.fillStyle = lit
          ? (i > bars * 0.8 ? '#ef4444' : i > bars * 0.6 ? '#f59e0b' : '#22d3ee')
          : '#162038';
        const barH = lit ? h * (0.4 + 0.5 * Math.sin((i / bars) * Math.PI)) : h * 0.25;
        vuCtx.fillRect(x, (h - barH) / 2, barW, barH);
      }
    }

    drawVu(0);
    connectControl();
  </script>
</body>
</html>
  """.trimIndent()
}
