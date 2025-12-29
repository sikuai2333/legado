#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generate Android icons in multiple sizes
"""
from PIL import Image
import os
import sys

# Fix encoding for Windows
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def resize_image(source_path, output_dir, base_name, sizes):
    """
    Resize image to multiple sizes

    Args:
        source_path: Source image path
        output_dir: Output directory
        base_name: Base name (without extension)
        sizes: List of sizes, format: [(size, folder_suffix), ...]
    """
    try:
        img = Image.open(source_path)
        print(f"Original image size: {img.size}")

        for size, folder_suffix in sizes:
            # Create output folder
            output_folder = os.path.join(output_dir, f"drawable-{folder_suffix}")
            os.makedirs(output_folder, exist_ok=True)

            # Resize image
            resized_img = img.resize((size, size), Image.Resampling.LANCZOS)

            # Save image
            output_path = os.path.join(output_folder, f"{base_name}.png")
            resized_img.save(output_path, "PNG", quality=95)
            print(f"Generated: {output_path} ({size}x{size})")

    except Exception as e:
        print(f"Error: {e}")

def main():
    # Project path
    project_root = r"c:\Users\sikuai\AndroidStudioProjects\legado"
    res_dir = os.path.join(project_root, "app", "src", "main", "res")
    logo_dir = os.path.join(project_root, "logo")

    # Define size mapping - reduced by 50%
    # Based on Android density classification
    sizes = [
        (36, "ldpi"),      # 0.75x
        (48, "mdpi"),      # 1x
        (72, "hdpi"),      # 1.5x
        (96, "xhdpi"),     # 2x
        (144, "xxhdpi"),   # 3x
        (192, "xxxhdpi"),  # 4x
    ]

    # Process shumiao.jpg
    print("=" * 50)
    print("Processing shumiao.jpg...")
    print("=" * 50)
    shumiao_path = os.path.join(logo_dir, "shumiao.jpg")
    resize_image(shumiao_path, res_dir, "shumiao", sizes)

    # Process zhigeyun.jpg
    print("\n" + "=" * 50)
    print("Processing zhigeyun.jpg...")
    print("=" * 50)
    zhigeyun_path = os.path.join(logo_dir, "zhigeyun.jpg")
    resize_image(zhigeyun_path, res_dir, "zhigeyun", sizes)

    print("\n" + "=" * 50)
    print("All icons generated successfully!")
    print("=" * 50)

if __name__ == "__main__":
    main()
