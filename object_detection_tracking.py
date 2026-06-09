"""
TASK 4: Object Detection and Tracking
======================================
Real-time object detection and tracking using:
  - OpenCV  : video capture, display, and output recording
  - YOLOv8  : pre-trained object detection (ultralytics)
  - YOLO    : built-in ByteTrack / BoT-SORT tracking (no extra deps required)

Requirements:
    pip install ultralytics opencv-python

Usage:
    # Webcam (default, index 0):
    python object_detection_tracking.py

    # Specific webcam index:
    python object_detection_tracking.py --source 1

    # Video file:
    python object_detection_tracking.py --source path/to/video.mp4

    # Save annotated output to a file (alongside live display):
    python object_detection_tracking.py --save

    # Custom output filename:
    python object_detection_tracking.py --save --output my_output.mp4

    # Log every detection to a CSV file for later analysis:
    python object_detection_tracking.py --log

    # Custom CSV filename:
    python object_detection_tracking.py --log --log-file detections.csv

    # Change YOLO model (e.g. yolov8m, yolov8l, yolov8x):
    python object_detection_tracking.py --model yolov8m.pt

    # Change tracker (bytetrack or botsort):
    python object_detection_tracking.py --tracker bytetrack.yaml

    # Run headless (save only, no display window — useful on servers):
    python object_detection_tracking.py --save --no-display

Press 'q' while the output window is focused to quit.

CSV log columns
---------------
timestamp_ms  : wall-clock time (milliseconds since epoch) when the frame was processed
frame_index   : sequential frame number (1-based)
track_id      : unique tracking ID assigned by ByteTrack/BoT-SORT (empty if unavailable)
class_id      : YOLO class index
class_name    : human-readable class label (e.g. "person", "car")
confidence    : detection confidence score (0.0 – 1.0)
x1, y1        : top-left corner of the bounding box (pixels)
x2, y2        : bottom-right corner of the bounding box (pixels)
"""

import argparse
import csv
import sys
import time
from datetime import datetime
from pathlib import Path

import cv2

# ---------------------------------------------------------------------------
# Dependency check — provide a helpful error if ultralytics is missing
# ---------------------------------------------------------------------------
try:
    from ultralytics import YOLO
except ImportError:
    print(
        "[ERROR] The 'ultralytics' package is not installed.\n"
        "Install it with:  pip install ultralytics\n"
    )
    sys.exit(1)


# ---------------------------------------------------------------------------
# Configuration — tweak these defaults or override via CLI flags
# ---------------------------------------------------------------------------

# Video source: integer = webcam index, string path = video file
DEFAULT_SOURCE = 0              # 0 = first webcam; use "video.mp4" for a file

# YOLOv8 model weights (downloaded automatically on first run if absent)
DEFAULT_MODEL = "yolov8n.pt"    # 'n' = nano (fastest); try 'm', 'l', 'x' for accuracy

# Tracker config bundled with ultralytics
# Choices: "bytetrack.yaml"  (default, fast, good for crowded scenes)
#          "botsort.yaml"    (more robust ReID, slightly slower)
DEFAULT_TRACKER = "bytetrack.yaml"

# Detection confidence threshold (0.0 – 1.0)
CONFIDENCE_THRESHOLD = 0.3

# Frames-per-second cap for display (0 = no cap, run as fast as possible)
MAX_DISPLAY_FPS = 30

# Video writer settings
# MP4V codec works on all platforms; use 'XVID' if MP4V causes issues on Windows
OUTPUT_CODEC = "mp4v"
OUTPUT_EXTENSION = ".mp4"

# CSV log settings
CSV_EXTENSION = ".csv"
# Column names written as the header row of the CSV file
CSV_COLUMNS = [
    "timestamp_ms",   # wall-clock milliseconds since epoch
    "frame_index",    # 1-based sequential frame number
    "track_id",       # tracker-assigned ID (empty string if unavailable)
    "class_id",       # YOLO integer class index
    "class_name",     # human-readable label, e.g. "person"
    "confidence",     # float 0.0–1.0
    "x1", "y1",       # top-left corner (pixels)
    "x2", "y2",       # bottom-right corner (pixels)
]

# Bounding-box and label rendering settings
BOX_THICKNESS   = 2
FONT            = cv2.FONT_HERSHEY_SIMPLEX
FONT_SCALE      = 0.6
FONT_THICKNESS  = 2
TEXT_PADDING    = 4     # pixels of padding around the label text

# Colour palette — one BGR colour per class ID (cycles if more classes exist)
COLOUR_PALETTE = [
    (  0, 255,   0),   # green
    (255,   0,   0),   # blue
    (  0,   0, 255),   # red
    (  0, 255, 255),   # yellow
    (255,   0, 255),   # magenta
    (255, 128,   0),   # orange
    (128,   0, 255),   # purple
    (  0, 128, 255),   # sky-blue
]


# ---------------------------------------------------------------------------
# Helper utilities
# ---------------------------------------------------------------------------

def get_colour(class_id: int) -> tuple[int, int, int]:
    """Return a consistent BGR colour for a given class ID."""
    return COLOUR_PALETTE[class_id % len(COLOUR_PALETTE)]


def make_log_path(custom_path: str | None) -> Path:
    """
    Build the CSV log file path.
    If *custom_path* is given, use it directly (adding .csv if needed).
    Otherwise, generate a timestamped filename like: detections_20260608_143021.csv
    """
    if custom_path:
        p = Path(custom_path)
        if p.suffix.lower() != CSV_EXTENSION:
            p = p.with_suffix(CSV_EXTENSION)
        return p

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    return Path(f"detections_{timestamp}{CSV_EXTENSION}")


def open_csv_log(log_path: Path):
    """
    Open *log_path* for writing and return (file_handle, csv.DictWriter).
    The header row is written immediately so the file is valid even if
    no detections occur.
    """
    log_path.parent.mkdir(parents=True, exist_ok=True)
    fh = open(log_path, "w", newline="", encoding="utf-8")
    writer = csv.DictWriter(fh, fieldnames=CSV_COLUMNS)
    writer.writeheader()
    return fh, writer


def log_detections(
    csv_writer: csv.DictWriter,
    frame_index: int,
    timestamp_ms: int,
    boxes,
    model_names: dict,
) -> int:
    """
    Write one CSV row per detected object in the current frame.

    Parameters
    ----------
    csv_writer   : csv.DictWriter already opened for writing
    frame_index  : 1-based sequential frame counter
    timestamp_ms : wall-clock time in milliseconds (int)
    boxes        : ultralytics Boxes object from results[0].boxes
    model_names  : dict mapping class_id → class_name

    Returns the number of rows written (= number of detections logged).
    """
    count = 0
    for box in boxes:
        x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
        class_id   = int(box.cls[0])
        class_name = model_names[class_id]
        confidence = float(box.conf[0])
        track_id   = int(box.id[0]) if box.id is not None else ""

        csv_writer.writerow({
            "timestamp_ms": timestamp_ms,
            "frame_index":  frame_index,
            "track_id":     track_id,
            "class_id":     class_id,
            "class_name":   class_name,
            "confidence":   f"{confidence:.4f}",
            "x1": x1, "y1": y1,
            "x2": x2, "y2": y2,
        })
        count += 1
    return count


def make_output_path(custom_path: str | None) -> Path:
    """
    Build the output video file path.
    If *custom_path* is given, use it directly (adding the extension if needed).
    Otherwise, generate a timestamped filename like: output_20260608_143021.mp4
    """
    if custom_path:
        p = Path(custom_path)
        # Ensure the correct extension is present
        if p.suffix.lower() != OUTPUT_EXTENSION:
            p = p.with_suffix(OUTPUT_EXTENSION)
        return p

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    return Path(f"output_{timestamp}{OUTPUT_EXTENSION}")


def create_video_writer(
    output_path: Path,
    width: int,
    height: int,
    fps: float,
) -> cv2.VideoWriter:
    """
    Create and return an OpenCV VideoWriter.

    The four-character codec code (fourcc) tells OpenCV how to compress the
    video.  'mp4v' produces an H.264-compatible MP4 on most systems.
    """
    fourcc = cv2.VideoWriter_fourcc(*OUTPUT_CODEC)
    writer = cv2.VideoWriter(str(output_path), fourcc, fps, (width, height))

    if not writer.isOpened():
        raise RuntimeError(
            f"VideoWriter could not open '{output_path}'.\n"
            "  - Check that the codec is supported on your system.\n"
            "  - Try changing OUTPUT_CODEC to 'XVID' and OUTPUT_EXTENSION to '.avi'."
        )
    return writer


def draw_detection(
    frame,
    x1: int, y1: int, x2: int, y2: int,
    track_id: int | None,
    class_name: str,
    confidence: float,
    colour: tuple[int, int, int],
) -> None:
    """
    Draw a bounding box and label onto *frame* in-place.

    The label format is:
        [ID <track_id>] <class_name> <confidence>%
    If no tracking ID is available (detection-only mode), the ID part is omitted.
    """
    # --- Bounding box ---
    cv2.rectangle(frame, (x1, y1), (x2, y2), colour, BOX_THICKNESS)

    # --- Label text ---
    if track_id is not None:
        label = f"[ID {int(track_id)}] {class_name} {confidence:.0%}"
    else:
        label = f"{class_name} {confidence:.0%}"

    # Measure text so we can draw a filled background behind it
    (text_w, text_h), baseline = cv2.getTextSize(
        label, FONT, FONT_SCALE, FONT_THICKNESS
    )
    # Position label just above the top-left corner of the box
    label_x = x1
    label_y = max(y1 - TEXT_PADDING, text_h + TEXT_PADDING)

    # Filled rectangle as label background
    cv2.rectangle(
        frame,
        (label_x, label_y - text_h - TEXT_PADDING),
        (label_x + text_w + TEXT_PADDING, label_y + baseline),
        colour,
        thickness=cv2.FILLED,
    )

    # Decide text colour (white on dark colours, black on light ones)
    brightness = 0.299 * colour[2] + 0.587 * colour[1] + 0.114 * colour[0]
    text_colour = (0, 0, 0) if brightness > 160 else (255, 255, 255)

    cv2.putText(
        frame, label,
        (label_x + TEXT_PADDING // 2, label_y),
        FONT, FONT_SCALE, text_colour, FONT_THICKNESS, cv2.LINE_AA,
    )


def draw_fps_overlay(frame, fps: float, recording: bool) -> None:
    """
    Render a semi-transparent FPS counter in the top-right corner.
    When recording is active a red REC indicator is shown beside it.
    """
    fps_text = f"FPS: {fps:.1f}"
    rec_text = "● REC" if recording else ""

    (tw, th), _ = cv2.getTextSize(fps_text, FONT, FONT_SCALE, FONT_THICKNESS)
    h, w = frame.shape[:2]
    margin = 10
    x = w - tw - margin * 2
    y = th + margin

    # Dark background strip
    overlay = frame.copy()
    cv2.rectangle(overlay, (x - margin, margin), (w - margin, y + margin),
                  (0, 0, 0), cv2.FILLED)
    cv2.addWeighted(overlay, 0.5, frame, 0.5, 0, frame)

    cv2.putText(frame, fps_text, (x, y), FONT, FONT_SCALE,
                (0, 255, 0), FONT_THICKNESS, cv2.LINE_AA)

    # Draw REC badge in the top-left corner
    if rec_text:
        cv2.putText(frame, rec_text, (margin, y), FONT, FONT_SCALE,
                    (0, 0, 255), FONT_THICKNESS, cv2.LINE_AA)


# ---------------------------------------------------------------------------
# Core pipeline
# ---------------------------------------------------------------------------

def open_video_source(source) -> cv2.VideoCapture:
    """
    Open a VideoCapture for either a webcam index or a file path.
    Raises RuntimeError if the source cannot be opened.
    """
    # Convert numeric string arguments (e.g. "0", "1") to int
    if isinstance(source, str) and source.isdigit():
        source = int(source)

    cap = cv2.VideoCapture(source)
    if not cap.isOpened():
        raise RuntimeError(
            f"Cannot open video source: {source!r}\n"
            "  - For a webcam, ensure the index is correct and the camera is connected.\n"
            "  - For a file, verify the path exists and OpenCV supports the codec."
        )
    return cap


def run_detection_and_tracking(
    source=DEFAULT_SOURCE,
    model_path: str = DEFAULT_MODEL,
    tracker: str = DEFAULT_TRACKER,
    confidence: float = CONFIDENCE_THRESHOLD,
    save: bool = False,
    output_path: str | None = None,
    no_display: bool = False,
    log: bool = False,
    log_file: str | None = None,
) -> None:
    """
    Main loop — reads frames from *source*, runs YOLOv8 tracking on each
    frame, annotates detections/tracks, displays and/or records the result,
    and optionally writes per-detection rows to a CSV log file.

    Parameters
    ----------
    source       : int or str  — webcam index or video file path
    model_path   : str         — path / name of the YOLOv8 weights file
    tracker      : str         — tracker YAML bundled with ultralytics
    confidence   : float       — minimum detection confidence to display
    save         : bool        — write annotated frames to a video file
    output_path  : str | None  — custom video filename (auto-generated if None)
    no_display   : bool        — skip imshow (headless / save-only mode)
    log          : bool        — write per-detection rows to a CSV file
    log_file     : str | None  — custom CSV filename (auto-generated if None)
    """

    # ------------------------------------------------------------------ #
    # 1. Load the YOLO model
    #    Weights are downloaded automatically on first use.
    # ------------------------------------------------------------------ #
    print(f"[INFO] Loading model: {model_path}")
    model = YOLO(model_path)
    print(f"[INFO] Model loaded. Classes: {len(model.names)}")

    # ------------------------------------------------------------------ #
    # 2. Open the video source
    # ------------------------------------------------------------------ #
    print(f"[INFO] Opening video source: {source}")
    cap = open_video_source(source)

    frame_width  = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    frame_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    source_fps   = cap.get(cv2.CAP_PROP_FPS) or 30
    print(f"[INFO] Source resolution: {frame_width}x{frame_height} @ {source_fps:.1f} fps")

    # ------------------------------------------------------------------ #
    # 3. Set up VideoWriter (only when --save is requested)
    # ------------------------------------------------------------------ #
    writer: cv2.VideoWriter | None = None

    if save:
        out_path = make_output_path(output_path)
        writer = create_video_writer(out_path, frame_width, frame_height, source_fps)
        print(f"[INFO] Recording to: {out_path.resolve()}")

    # ------------------------------------------------------------------ #
    # 4. Set up CSV log (only when --log is requested)
    #
    #    We open the file here so the header is written immediately.
    #    The file handle (csv_fh) must be closed in the clean-up block.
    # ------------------------------------------------------------------ #
    csv_fh         = None
    csv_writer_obj = None
    total_logged   = 0       # running count of rows written to CSV

    if log:
        csv_path       = make_log_path(log_file)
        csv_fh, csv_writer_obj = open_csv_log(csv_path)
        print(f"[INFO] Logging detections to: {csv_path.resolve()}")

    # ------------------------------------------------------------------ #
    # 5. Open live display window (unless --no-display)
    # ------------------------------------------------------------------ #
    window_title = "Object Detection & Tracking — press 'q' to quit"
    if not no_display:
        cv2.namedWindow(window_title, cv2.WINDOW_NORMAL)

    # ------------------------------------------------------------------ #
    # 6. Real-time processing loop
    # ------------------------------------------------------------------ #
    frame_index    = 0        # 1-based frame counter used in CSV rows
    frame_count    = 0        # counter that resets every 0.5 s for FPS calc
    fps_display    = 0.0
    fps_time_start = time.time()

    print("[INFO] Starting detection & tracking loop …")

    while True:
        ret, frame = cap.read()

        # End of file or capture error
        if not ret:
            print("[INFO] No more frames. Exiting …")
            break

        frame_index += 1
        frame_count += 1

        # Wall-clock timestamp in milliseconds for this frame
        timestamp_ms = int(time.time() * 1000)

        # -------------------------------------------------------------- #
        # 7. Run YOLOv8 tracking on the current frame
        #
        #    model.track() returns a list of Results objects (one per frame).
        #    persist=True is REQUIRED for tracking: it tells the tracker to
        #    maintain state across successive calls so IDs stay consistent.
        # -------------------------------------------------------------- #
        results = model.track(
            source=frame,           # single BGR frame (numpy array)
            persist=True,           # maintain tracker state between frames
            tracker=tracker,        # tracker algorithm config
            conf=confidence,        # confidence threshold
            verbose=False,          # suppress per-frame console output
        )

        # -------------------------------------------------------------- #
        # 8. Annotate detections and tracking IDs on the frame
        #    AND optionally write each detection to the CSV log.
        # -------------------------------------------------------------- #
        if results and results[0].boxes is not None:
            boxes = results[0].boxes  # ultralytics Boxes object

            # ---- CSV logging (runs before annotation, same boxes) ---- #
            if csv_writer_obj is not None:
                total_logged += log_detections(
                    csv_writer_obj,
                    frame_index,
                    timestamp_ms,
                    boxes,
                    model.names,
                )

            # ---- Visual annotation ---- #
            for box in boxes:
                # Bounding box in pixel coordinates [x1, y1, x2, y2]
                x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())

                # Class ID and human-readable name
                class_id   = int(box.cls[0])
                class_name = model.names[class_id]

                # Detection confidence
                conf_score = float(box.conf[0])

                # Tracking ID (None if tracking was not available this frame)
                track_id = int(box.id[0]) if box.id is not None else None

                # Pick a colour based on the class so the same object type
                # always gets the same colour regardless of tracking ID
                colour = get_colour(class_id)

                # Draw box + label onto the frame
                draw_detection(
                    frame,
                    x1, y1, x2, y2,
                    track_id,
                    class_name,
                    conf_score,
                    colour,
                )

        # -------------------------------------------------------------- #
        # 9. Compute and overlay FPS + REC badge
        # -------------------------------------------------------------- #
        elapsed = time.time() - fps_time_start
        if elapsed >= 0.5:                 # update display twice per second
            fps_display    = frame_count / elapsed
            frame_count    = 0
            fps_time_start = time.time()

        draw_fps_overlay(frame, fps_display, recording=(writer is not None))

        # -------------------------------------------------------------- #
        # 10. Write annotated frame to output video (if recording)
        #
        #     This happens AFTER all annotations (including the FPS overlay)
        #     so the saved file looks identical to what is shown on screen.
        # -------------------------------------------------------------- #
        if writer is not None:
            writer.write(frame)

        # -------------------------------------------------------------- #
        # 11. Show the annotated frame (unless headless mode)
        # -------------------------------------------------------------- #
        if not no_display:
            cv2.imshow(window_title, frame)

            # Throttle display to MAX_DISPLAY_FPS if a cap is set
            wait_ms = max(1, int(1000 / MAX_DISPLAY_FPS)) if MAX_DISPLAY_FPS > 0 else 1

            # Break on 'q' key press
            if cv2.waitKey(wait_ms) & 0xFF == ord("q"):
                print("[INFO] 'q' pressed — quitting …")
                break
        else:
            # In headless mode we still need a tiny sleep so the OS can
            # breathe; also allows Ctrl-C to interrupt cleanly.
            time.sleep(0.001)

    # ------------------------------------------------------------------ #
    # 12. Clean up — release all resources
    # ------------------------------------------------------------------ #
    cap.release()

    if writer is not None:
        writer.release()
        print(f"[INFO] Saved output video: {out_path.resolve()}")

    if csv_fh is not None:
        csv_fh.close()
        print(f"[INFO] Saved detection log : {csv_path.resolve()}")
        print(f"[INFO] Total rows logged   : {total_logged}")

    if not no_display:
        cv2.destroyAllWindows()

    print("[INFO] Done.")


# ---------------------------------------------------------------------------
# CLI entry point
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Real-time object detection and tracking with YOLOv8 + OpenCV",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument(
        "--source",
        default=DEFAULT_SOURCE,
        help=(
            "Video source. Use an integer for a webcam index (e.g. 0, 1) "
            "or a file path for a recorded video (e.g. video.mp4)."
        ),
    )
    parser.add_argument(
        "--model",
        default=DEFAULT_MODEL,
        help=(
            "YOLOv8 model weights. Built-in options: "
            "yolov8n.pt (nano/fastest), yolov8s.pt, yolov8m.pt, "
            "yolov8l.pt, yolov8x.pt (largest/most accurate). "
            "Custom .pt files are also accepted."
        ),
    )
    parser.add_argument(
        "--tracker",
        default=DEFAULT_TRACKER,
        choices=["bytetrack.yaml", "botsort.yaml"],
        help="Tracker algorithm config bundled with ultralytics.",
    )
    parser.add_argument(
        "--conf",
        type=float,
        default=CONFIDENCE_THRESHOLD,
        help="Minimum detection confidence threshold (0.0 – 1.0).",
    )
    parser.add_argument(
        "--save",
        action="store_true",
        help="Save the annotated output to a video file alongside the live display.",
    )
    parser.add_argument(
        "--output",
        default=None,
        metavar="FILE",
        help=(
            "Output video filename (used only with --save). "
            "Defaults to a timestamped name like output_20260608_143021.mp4"
        ),
    )
    parser.add_argument(
        "--no-display",
        action="store_true",
        help=(
            "Disable the live preview window (headless mode). "
            "Requires --save or --log so there is somewhere for the output to go."
        ),
    )
    parser.add_argument(
        "--log",
        action="store_true",
        help=(
            "Write a CSV log of every detection to disk. "
            "Each row contains: timestamp_ms, frame_index, track_id, "
            "class_id, class_name, confidence, x1, y1, x2, y2."
        ),
    )
    parser.add_argument(
        "--log-file",
        default=None,
        metavar="FILE",
        help=(
            "CSV output filename (used only with --log). "
            "Defaults to a timestamped name like detections_20260608_143021.csv"
        ),
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()

    # Guard: headless with no output destination is pointless
    if args.no_display and not args.save and not args.log:
        print(
            "[ERROR] --no-display requires at least --save or --log "
            "(otherwise there is no output at all)."
        )
        sys.exit(1)

    run_detection_and_tracking(
        source=args.source,
        model_path=args.model,
        tracker=args.tracker,
        confidence=args.conf,
        save=args.save,
        output_path=args.output,
        no_display=args.no_display,
        log=args.log,
        log_file=args.log_file,
    )
