import os
import cv2
import numpy as np
import tensorflow as tf
from tensorflow.keras.applications import MobileNetV3Small
from tensorflow.keras.layers import Dense, GlobalAveragePooling2D
from tensorflow.keras.models import Model

CATEGORIES = [
    "copper_bare_bright", "copper_armature", "brass_honey",
    "aluminium_extrusions", "aluminium_castings", "aluminium_utensils",
    "heavy_steel_sariya", "light_iron_patra", "cast_iron",
    "high_grade_server_pcb", "mobile_phone_pcb",
    "lead_acid_battery", "li_ion_cells",
    "cardboard_carton", "pet_plastic"
]

def calculate_rust_oxidation_score(image_path):
    img = cv2.imread(image_path)
    if img is None:
        return 0.0
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    lower_rust = np.array([10, 50, 50])
    upper_rust = np.array([20, 255, 255])
    mask = cv2.inRange(hsv, lower_rust, upper_rust)
    rust_ratio = np.sum(mask == 255) / (img.shape[0] * img.shape[1])
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    edges = cv2.Canny(gray, 100, 200)
    edge_density = np.sum(edges == 255) / (img.shape[0] * img.shape[1])
    score = min(100.0, (rust_ratio * 0.7 + edge_density * 0.3) * 100)
    return score

def get_price_deduction(base_price, rust_score, category):
    deduction_pct = (rust_score / 100.0) * 0.20
    return base_price * (1.0 - deduction_pct)

def build_and_export_model():
    base_model = MobileNetV3Small(input_shape=(224, 224, 3), include_top=False, weights='imagenet')
    x = base_model.output
    x = GlobalAveragePooling2D()(x)
    predictions = Dense(len(CATEGORIES), activation='softmax')(x)
    model = Model(inputs=base_model.input, outputs=predictions)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    with open('scrap_model.tflite', 'wb') as f:
        f.write(tflite_model)
    with open('labels.txt', 'w') as f:
        f.write("\n".join(CATEGORIES))
    print("Model exported to scrap_model.tflite and labels.txt")
    frontend_dir = r"C:\Users\bonth\sih-fullstack-frontend\assets\models"
    os.makedirs(frontend_dir, exist_ok=True)
    import shutil
    shutil.copy('scrap_model.tflite', os.path.join(frontend_dir, 'scrap_model.tflite'))
    shutil.copy('labels.txt', os.path.join(frontend_dir, 'labels.txt'))
    print(f"Artifacts copied to {frontend_dir}")

if __name__ == "__main__":
    build_and_export_model()
