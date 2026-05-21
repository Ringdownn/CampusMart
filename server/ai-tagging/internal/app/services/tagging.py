import json
from typing import Optional
import asyncio
import logging

import numpy as np

from internal.app.config import settings
from internal.app.models.clip_model import CLIPService
from internal.app.services.image_loader import ImageLoader

logger = logging.getLogger(__name__)

class TagResult:
    def __init__(self, label: str, confidence: float):
        self.label = label
        self.confidence = confidence

    def to_dict(self):
        return {"label": self.label, "confidence": float(self.confidence)}

class TaggingService:
    def __init__(self, clip_service: Optional[CLIPService], labels: list[str], model_error: Optional[str] = None):
        self.clip = clip_service
        self.labels = labels
        self.model_error = model_error

    @classmethod
    async def create(cls, labels_file: Optional[str]= None) -> "TaggingService":
        labels_file = labels_file or settings.LABEL_FILE
        
        # 读取标签文件
        with open(labels_file, "r", encoding="utf-8") as f:
            labels = json.load(f)

        # 在线程池中加载模型，避免阻塞事件循环。模型不可用时服务仍启动，
        # /api/tag 会退化为规则标签，避免健康检查和 MQ 消费者整体不可用。
        loop = asyncio.get_event_loop()
        try:
            clip_service = await loop.run_in_executor(
                None,
                lambda: CLIPService(settings.MODEL_NAME)
            )
            return cls(clip_service, labels)
        except Exception as e:
            logger.exception("CLIP model unavailable, running in fallback tagging mode")
            return cls(None, labels, str(e))

    def is_model_available(self) -> bool:
        return self.clip is not None

    def mode(self) -> str:
        return "clip" if self.is_model_available() else "fallback"

    async def tag_image_from_url(self, image_url: str, top_k: int = 3) -> list[TagResult]:
        image = await ImageLoader.load_image_from_url(image_url, settings.REQUEST_TIMEOUT)

        if image is None:
            logger.error(f"Failed to load image from {image_url}")
            return []
        
        if self.clip is None:
            return self.fallback_tags(top_k)

        # 编码图片
        image_features = self.clip.encode_image(image)
        # 编码所有标签文本
        text_features = self.clip.encode_text(self.labels)
        
        # 计算相似度
        similarity = self.clip.get_similarity(image_features, text_features)
        top_indices = similarity.argsort()[::-1][:top_k]

        result = []

        for index in top_indices:
            result.append(TagResult(
                label=self.labels[index],
                confidence=similarity[index])
            )

        return result

    def fallback_tags(self, top_k: int) -> list[TagResult]:
        limit = max(1, min(top_k, len(self.labels)))
        return [
            TagResult(label=label, confidence=max(0.01, 0.2 - index * 0.01))
            for index, label in enumerate(self.labels[:limit])
        ]