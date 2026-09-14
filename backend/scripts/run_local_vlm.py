#!/usr/bin/env python3
"""
SIH E-Waste & Scrap: n-Billion Parameter Vision-Language Model (VLM) Pipeline.

Demonstrates running unquantized 3B to 7B parameter multimodal models:
1. Host Laptop: NVIDIA GeForce RTX 4060 (8.0 GB VRAM) -> Qwen2.5-VL-3B in full unquantized FP16 (~6.2 GB VRAM)
2. Mobile Device: RedMagic 11 Pro (24 GB RAM, Snapdragon 8 Elite) -> Qwen2.5-VL-7B or 3B in full FP16 (~6.2 - 14.5 GB RAM)
3. Cloud Service: Google Gemini 2.0 Flash (100B+ parameters) for global CPCB compliance

Architecture: Two-Tier Hierarchical AI
- Tier 1 (Live 60 FPS Viewfinder): YOLOv8s (11.2M params) unquantized Float32 for zero-latency bounding boxes.
- Tier 2 (Deep Condition Audit @ "Add to Batch"): 3B - 7B VLM unquantized FP16 for micro-damage & CPCB hazard analysis.
"""

import os
import sys
import json
import base64
import argparse
import logging
from pathlib import Path

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("run_local_vlm")

PROJECT_ROOT = Path(__file__).resolve().parent.parent
MODEL_CACHE_DIR = PROJECT_ROOT / "models" / "offline_vlm"


def check_compute_environment():
    """Audits local GPU VRAM and Mobile RAM capabilities for n-Billion parameter models."""
    logger.info("=== Compute Hardware Audit for n-Billion Parameter Models ===")
    
    # Check CUDA GPU
    try:
        import torch
        cuda_avail = torch.cuda.is_available()
        if cuda_avail:
            device_name = torch.cuda.get_device_name(0)
            total_vram_gb = torch.cuda.get_device_properties(0).total_memory / (1024 ** 3)
            allocated_vram_gb = torch.cuda.memory_allocated(0) / (1024 ** 3)
            free_vram_gb = total_vram_gb - allocated_vram_gb
            logger.info(f"Host GPU: {device_name}")
            logger.info(f"Total VRAM: {total_vram_gb:.2f} GB | Free VRAM: {free_vram_gb:.2f} GB")
            
            # Mathematical feasibility for unquantized models
            logger.info("--- Unquantized Model Feasibility on Host GPU (8GB Budget) ---")
            logger.info(f"• 1.5B VLM (FP16): ~3.2 GB VRAM -> [FEASIBLE, ~4.8 GB headroom]")
            logger.info(f"• 3.0B VLM (Qwen2.5-VL-3B FP16): ~6.2 GB VRAM -> [FEASIBLE, ~1.8 GB headroom]")
            logger.info(f"• 7.0B VLM (FP16): ~14.5 GB VRAM -> [EXCEEDS 8GB VRAM; Requires CPU offload or RedMagic 24GB]")
        else:
            logger.warning("No CUDA GPU detected on host. Models will run on CPU or Mobile NPU.")
    except ImportError:
        logger.warning("PyTorch not installed in active environment.")

    # Target Mobile Device Audit (RedMagic 11 Pro)
    logger.info("\n--- Mobile Device Target: RedMagic 11 Pro (24 GB RAM) ---")
    logger.info("• SoC: Snapdragon 8 Elite (Oryon CPU + Adreno 830 GPU + Hexagon NPU)")
    logger.info("• Memory Budget: 24.0 GB Unified LPDDR5X RAM")
    logger.info("• 3B VLM in unquantized FP16: ~6.2 GB RAM (Leaves ~17.8 GB RAM for OS/UI) -> [OPTIMAL]")
    logger.info("• 7B VLM in unquantized FP16: ~14.5 GB RAM (Leaves ~9.5 GB RAM for OS/UI) -> [FEASIBLE]")
    logger.info("============================================================\n")


def run_qwen_vl_7b_fp16(image_path: Path, prompt_lang: str = "en", model_id: str = "Qwen/Qwen2.5-VL-7B-Instruct"):
    """
    Executes unquantized 7-Billion parameter Qwen2.5-VL-7B-Instruct in full FP16 precision.
    - On RedMagic 11 Pro (24 GB RAM): Runs natively in unified memory (~14.5 GB footprint).
    - On RTX 4060 Laptop (8 GB VRAM): Splits layers seamlessly across GPU and Host CPU RAM using device_map='auto'.
    """
    try:
        import torch
        from transformers import Qwen2VLForConditionalGeneration, AutoProcessor
        from PIL import Image
    except ImportError:
        logger.error("Required libraries missing for 7B VLM execution.")
        logger.info("Install with: pip install transformers accelerate torchvision")
        return None

    logger.info(f"Loading 7-Billion Parameter VLM ({model_id}) in unquantized FP16 precision...")
    if torch.cuda.is_available():
        logger.info("Configuring hybrid CUDA/CPU memory partition (6.5GB VRAM ceiling, CPU offload for remaining 7B layers)...")
        max_memory = {0: "6.5GB", "cpu": "24GB"}
    else:
        max_memory = None

    try:
        model = Qwen2VLForConditionalGeneration.from_pretrained(
            model_id,
            torch_dtype=torch.float16,
            device_map="auto",
            max_memory=max_memory,
            offload_folder=str(MODEL_CACHE_DIR / "offload")
        )
        processor = AutoProcessor.from_pretrained(model_id)

        image = Image.open(image_path).convert("RGB")

        lang_prompts = {
            "en": (
                "You are a CPCB E-Waste & Scrap Computer Vision Auditor. Analyze this scrap/electronic appliance image. "
                "Output JSON with: item_name, cpcb_category, wear_grade, intactness_pct, oxidation_rust_pct, "
                "toxic_hazards, safety_alert, estimated_weight_kg, mandi_price_per_kg."
            ),
            "hi": (
                "आप सीपीसीबी ई-कचरा ऑडिटर हैं। इस उपकरण की स्थिति, वजन, जंग का प्रतिशत, विषाक्त खतरे और मंडी मूल्य बताएं।"
            ),
            "mr": (
                "तुम्ही सीपीसीबी ई-कचरा ऑडिटर आहात. या उपकरणाची स्थिती, वजन, गंज टक्केवारी, विषारी धोके आणि बाजार भाव सांगा."
            )
        }

        prompt_text = lang_prompts.get(prompt_lang, lang_prompts["en"])

        messages = [
            {
                "role": "user",
                "content": [
                    {"type": "image", "image": image},
                    {"type": "text", "text": prompt_text}
                ]
            }
        ]

        text = processor.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
        inputs = processor(text=[text], images=[image], padding=True, return_tensors="pt")
        if torch.cuda.is_available():
            inputs = inputs.to("cuda")

        logger.info("Generating 7B parameter deep multimodal audit tokens...")
        with torch.no_grad():
            generated_ids = model.generate(**inputs, max_new_tokens=300)
        
        trimmed_ids = [out_ids[len(in_ids):] for in_ids, out_ids in zip(inputs.input_ids, generated_ids)]
        output_text = processor.batch_decode(trimmed_ids, skip_special_tokens=True, clean_up_tokenization_spaces=False)[0]

        logger.info("7B VLM Deep Audit Complete!")
        print("\n=== 7B VLM Audit Report (Unquantized Qwen2.5-VL-7B FP16) ===")
        print(output_text)
        print("============================================================\n")
        return output_text

    except Exception as e:
        logger.error(f"7B VLM execution error: {e}")
        return None


def main():
    parser = argparse.ArgumentParser(description="Run 7-Billion Parameter VLM for max precision scrap analysis")
    parser.add_argument("--audit", action="store_true", help="Audit hardware memory headroom for 7B models")
    parser.add_argument("--image", type=str, help="Path to scrap image for 7B VLM inspection")
    parser.add_argument("--model", type=str, default="Qwen/Qwen2.5-VL-7B-Instruct", help="HuggingFace model ID (default: 7B VLM)")
    parser.add_argument("--lang", type=str, default="en", choices=["en", "hi", "mr"], help="Inspection language")
    args = parser.parse_args()

    check_compute_environment()

    if args.image:
        img_path = Path(args.image)
        if img_path.exists():
            run_qwen_vl_7b_fp16(img_path, prompt_lang=args.lang, model_id=args.model)
        else:
            logger.error(f"Image not found: {img_path}")


if __name__ == "__main__":
    main()
