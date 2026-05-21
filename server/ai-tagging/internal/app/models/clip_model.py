import torch
import torch.nn.functional as F
import numpy as np
from transformers import CLIPModel, CLIPProcessor
from PIL import Image
from internal.app.config import settings


class CLIPService:
    def __init__(self, model_name: str = settings.MODEL_NAME):
        self.device = settings.DEVICE
        self.model = CLIPModel.from_pretrained(model_name, local_files_only=True).to(self.device)
        self.processor = CLIPProcessor.from_pretrained(model_name, local_files_only=True)
        self.model.eval()

    def encode_image(self, image: Image.Image) -> np.ndarray:
        #将图片嵌入为张量
        inputs = self.processor(images=image, return_tensors="pt")
        pixel_values = inputs["pixel_values"].to(self.device)

        #关闭梯度下降
        with torch.no_grad():
            # 获取图片特征 - 直接使用模型方法
            image_features = self.model.get_image_features(pixel_values=pixel_values)
            
            # 确保 image_features 是 tensor 类型
            if hasattr(image_features, 'image_embeds'):
                image_features = image_features.image_embeds
            elif hasattr(image_features, 'pooler_output'):
                image_features = image_features.pooler_output
            elif hasattr(image_features, 'last_hidden_state'):
                # 使用最后一个隐藏层的 [CLS] token
                image_features = image_features.last_hidden_state[:, 0, :]

            # 归一化
            image_features = F.normalize(image_features, dim=-1)

        return image_features.cpu().numpy()[0]

    def encode_text(self, texts: list[str]) -> np.ndarray:
        inputs = self.processor(text=texts, return_tensors="pt", padding=True)
        input_ids = inputs["input_ids"].to(self.device)
        attention_mask = inputs["attention_mask"].to(self.device)

        with torch.no_grad():
            # 获取文本特征 - 直接使用模型方法
            text_features = self.model.get_text_features(input_ids=input_ids, attention_mask=attention_mask)
            
            # 确保 text_features 是 tensor 类型
            if hasattr(text_features, 'text_embeds'):
                text_features = text_features.text_embeds
            elif hasattr(text_features, 'pooler_output'):
                text_features = text_features.pooler_output
            elif hasattr(text_features, 'last_hidden_state'):
                # 使用最后一个隐藏层的 <[BOS_never_used_51bce0c785ca2f68081bfa7d91973934]> token
                text_features = text_features.last_hidden_state[:, 0, :]

            text_features = F.normalize(text_features, dim=-1)

        return text_features.cpu().numpy()

    def get_similarity(self, image_features: np.ndarray, text_features: np.ndarray) -> np.ndarray:
        if image_features.ndim == 1:
            image_features = image_features.reshape(1, -1)
        similarity = (image_features @ text_features.T).squeeze(0)
        return similarity



