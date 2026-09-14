import os
from ultralytics import YOLO

def train_and_export():
    print("Initializing YOLOv8 Nano Object Detection Model...")
    model = YOLO('yolov8n.pt')
    
    dataset_yaml = 'dataset/data.yaml' 
    
    if not os.path.exists(dataset_yaml):
        print(f"Error: {dataset_yaml} not found.")
        print("Please download a segmentation dataset from Roboflow (YOLOv8 PyTorch format)")
        print("and place the unzipped dataset and data.yaml in this directory.")
        return

    print(f"Starting training on {dataset_yaml}...")
    model.train(
        data=dataset_yaml,
        epochs=1,
        imgsz=224, 
        batch=16,
        device='cpu',
        name='scrap_seg_model'
    )
    
    print("Training complete. Exporting model to TensorFlow Lite (LiteRT)...")
    export_path = model.export(
        format='litert',
        imgsz=224,
        int8=True
    )
    
    print(f"Export successful! TFLite model saved at: {export_path}")
    print("1. Copy the generated .tflite file into sih-fullstack-frontend/app/src/main/assets/")
    print("2. Update labels.txt with your dataset's class names.")

if __name__ == '__main__':
    train_and_export()
