#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "usage: $0 start <output.mp4> | stop" >&2
  exit 64
}

action="${1:-}"
pid_file="${AUTOPTU_CAPTURE_PID_FILE:-test-evidence/visual/.ffmpeg.pid}"

case "$action" in
  start)
    output="${2:-}"
    [ -n "$output" ] || usage
    command -v ffmpeg >/dev/null 2>&1 || {
      echo "ffmpeg is required for visual evidence capture" >&2
      exit 69
    }
    : "${DISPLAY:?DISPLAY must point to the graphical Minecraft test display}"

    mkdir -p "$(dirname "$output")" "$(dirname "$pid_file")"
    if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
      echo "visual evidence capture is already running" >&2
      exit 73
    fi

    size="${AUTOPTU_CAPTURE_SIZE:-1280x720}"
    fps="${AUTOPTU_CAPTURE_FPS:-30}"
    input_display="${AUTOPTU_CAPTURE_DISPLAY:-${DISPLAY}.0}"

    ffmpeg \
      -hide_banner \
      -loglevel warning \
      -y \
      -f x11grab \
      -video_size "$size" \
      -framerate "$fps" \
      -i "$input_display" \
      -c:v libx264 \
      -preset veryfast \
      -pix_fmt yuv420p \
      "$output" \
      > "${output%.mp4}.ffmpeg.log" 2>&1 &

    capture_pid=$!
    echo "$capture_pid" > "$pid_file"
    sleep 1
    if ! kill -0 "$capture_pid" 2>/dev/null; then
      cat "${output%.mp4}.ffmpeg.log" >&2 || true
      rm -f "$pid_file"
      exit 70
    fi
    echo "visual evidence capture started: $output"
    ;;

  stop)
    if [ ! -f "$pid_file" ]; then
      echo "no visual evidence capture is running"
      exit 0
    fi
    capture_pid="$(cat "$pid_file")"
    if kill -0 "$capture_pid" 2>/dev/null; then
      kill -INT "$capture_pid" 2>/dev/null || true
      for _ in $(seq 1 30); do
        if ! kill -0 "$capture_pid" 2>/dev/null; then
          break
        fi
        sleep 1
      done
      if kill -0 "$capture_pid" 2>/dev/null; then
        kill "$capture_pid" 2>/dev/null || true
      fi
    fi
    rm -f "$pid_file"
    echo "visual evidence capture stopped"
    ;;

  *)
    usage
    ;;
esac
