#!/usr/bin/env python3
"""
SIH E-Waste Deep Learning Training & Edge Export Pipeline.

Trained on 77 E-Waste Categories:
- Air-Conditioner, Ceiling-Fan, Exhaust-Fan, Floor-Fan
- Computer-Mouse, Computer-Keyboard, Laptop, Desktop-PC
- Smartphone, Bar-Phone, Tablet, Smart-Watch
- CRT-Monitor, Flat-Panel-Monitor, CRT-TV, Flat-Panel-TV
- Microwave, Refrigerator, Freezer, Washing-Machine
- Router, Network-Switch, Power-Adapter, Printer
- PCB, Battery, HDD, SSD, Vacuum-Cleaner, etc.

Target Hardware: NVIDIA RTX 4060 Laptop GPU (8GB VRAM) -> RedMagic 11 Pro (TFLite/ONNX)
"""

import os
import sys
import zipfile
import argparse
import logging
from pathlib import Path

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("train_ewaste_yolov8")

DATASET_ZIP_DEFAULT = Path(r"C:\Users\bonth\Downloads\E-Waste Dataset.v44-fix-annotations-of-some-bar-phones-incorrectly-labelled-as-smartphones.yolov8.zip")
PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATASET_DIR = PROJECT_ROOT / "dataset" / "ewaste"
EXPORT_DIR = PROJECT_ROOT / "models" / "ewaste_yolo"


def extract_dataset(zip_path: Path, target_dir: Path):
    if not zip_path.exists():
        logger.error(f"Dataset zip not found at: {zip_path}")
        return False
    
    yaml_check = target_dir / "data.yaml"
    if yaml_check.exists():
        logger.info(f"Dataset already extracted at: {target_dir}")
        return True

    logger.info(f"Extracting {zip_path.name} to {target_dir}...")
    target_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path, 'r') as zf:
        zf.extractall(target_dir)
    logger.info("Extraction complete.")
    return True


def train_model(epochs: int = 15, batch_size: int = 16, img_size: int = 640):
    try:
        import torch
        from ultralytics import YOLO
    except ImportError:
        logger.error("torch or ultralytics not installed. Run: pip install ultralytics torch")
        return

    device = 0 if torch.cuda.is_available() else "cpu"
    device_name = torch.cuda.get_device_name(0) if torch.cuda.is_available() else "CPU"
    logger.info(f"Using Compute Device: {device_name} (device={device})")

    yaml_path = DATASET_DIR / "data.yaml"
    if not yaml_path.exists():
        logger.error(f"data.yaml not found at: {yaml_path}. Run with --extract first.")
        return

    # Fix relative paths in data.yaml if necessary
    with open(yaml_path, "r", encoding="utf-8") as f:
        lines = f.readlines()
    
    with open(yaml_path, "w", encoding="utf-8") as f:
        for line in lines:
            if line.startswith("train:"):
                f.write(f"train: {str(DATASET_DIR / 'train' / 'images')}\n")
            elif line.startswith("val:"):
                f.write(f"val: {str(DATASET_DIR / 'valid' / 'images')}\n")
            elif line.startswith("test:"):
                f.write(f"test: {str(DATASET_DIR / 'test' / 'images')}\n")
            else:
                f.write(line)

    logger.info(f"Initializing YOLOv8n backbone (fine-tuning for 77 E-Waste classes)...")
    model = YOLO("yolov8n.pt")

    logger.info(f"Starting training for {epochs} epochs (Batch: {batch_size}, Image Size: {img_size})...")
    results = model.train(
        data=str(yaml_path),
        epochs=epochs,
        batch=batch_size,
        imgsz=img_size,
        device=device,
        project=str(EXPORT_DIR),
        name="ewaste_yolov8n",
        workers=2,
        half=True if torch.cuda.is_available() else False, # FP16 mixed precision on RTX 4060
        exist_ok=True
    )

    logger.info("Training complete! Evaluating model...")
    best_weights = EXPORT_DIR / "ewaste_yolov8n" / "weights" / "best.pt"
    if not best_weights.exists():
        best_weights = EXPORT_DIR / "ewaste_yolov8n" / "weights" / "last.pt"

    if best_weights.exists():
        logger.info(f"Exporting best model to ONNX & TFLite for RedMagic 11 Pro mobile deployment...")
        trained_model = YOLO(str(best_weights))
        
        # 1. Export ONNX
        onnx_path = trained_model.export(format="onnx", imgsz=img_size, half=False)
        logger.info(f"ONNX Model saved to: {onnx_path}")

        # 2. Export TFLite (LiteRT for Android)
        try:
            tflite_path = trained_model.export(format="tflite", imgsz=img_size, int8=True)
            logger.info(f"TFLite Model saved to: {tflite_path}")
        except Exception as e:
            logger.warning(f"TFLite export note: {e}")


def main():
    parser = argparse.ArgumentParser(description="Train and export YOLOv8 E-Waste Model")
    parser.add_argument("--extract", action="store_true", help="Extract dataset zip")
    parser.add_argument("--train", action="store_true", help="Run training on GPU")
    parser.add_argument("--epochs", type=int, default=10, help="Number of training epochs")
    parser.add_argument("--batch", type=int, default=16, help="Batch size (fits 8GB VRAM)")
    parser.add_argument("--zip", type=str, default=str(DATASET_ZIP_DEFAULT), help="Path to e-waste dataset zip")
    args = parser.parse_args()

    zip_path = Path(args.zip)
    if args.extract or not DATASET_DIR.exists():
        extract_dataset(zip_path, DATASET_DIR)

    if args.train:
        train_model(epochs=args.epochs, batch_size=args.batch)
    else:
        logger.info("Pipeline ready. Run with --extract --train to fine-tune on RTX 4060 GPU.")


if __name__ == "__main__":
    main()
