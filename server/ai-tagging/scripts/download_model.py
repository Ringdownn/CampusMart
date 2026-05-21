import os
from transformers import CLIPModel, CLIPProcessor

MODEL_NAME = "openai/clip-vit-base-patch32"

# 本地存储路径（相对于项目根目录）
LOCAL_MODEL_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "models", "clip-vit-base-patch32")


def download_model():
    os.makedirs(LOCAL_MODEL_PATH, exist_ok=True)

    model = CLIPModel.from_pretrained(MODEL_NAME)
    processor = CLIPProcessor.from_pretrained(MODEL_NAME)

    model.save_pretrained(LOCAL_MODEL_PATH)
    processor.save_pretrained(LOCAL_MODEL_PATH)


if __name__ == "__main__":
    download_model()
