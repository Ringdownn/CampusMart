import os
import torch
from typing import Optional
from pydantic_settings import BaseSettings

# 获取项目根目录
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 本地模型路径
LOCAL_MODEL_PATH = os.path.join(BASE_DIR, "data", "models", "clip-vit-base-patch32")

def get_device() -> str:
    if torch.cuda.is_available():
        return "cuda"
    elif torch.backends.mps.is_available():
        return "mps"
    else:
        return "cpu"

class Settings(BaseSettings):
    MODEL_NAME: str = LOCAL_MODEL_PATH if os.path.exists(LOCAL_MODEL_PATH) else "openai/clip-vit-base-patch32"
    LABEL_FILE: str = os.path.join(BASE_DIR, "data", "labels.json")
    DEFAULT_TOP_K: int = 3
    MAX_IMAGE_SIZE: int = 512
    DEVICE: str = get_device()
    API_HOST: str = "0.0.0.0"
    API_PORT: int = 8080
    REQUEST_TIMEOUT: int = 30

    class Config:
        env_file = ".env"
        case_sensitive = True

settings = Settings()