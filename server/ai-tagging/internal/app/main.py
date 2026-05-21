import logging
import sys
import os
import threading
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from sympy import false

from internal.app.api.routes import router, init_tagging_service
from internal.app.config import settings
from internal.app.services.tagging import TaggingService
from internal.queue.consumer.tagging_consumer import TaggingConsumer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

consumer_thread = None
tagging_consumer = None

def start_rabbitmq_consumer(tagging_service):
    """在后台线程启动 RabbitMQ 消费者"""
    global tagging_consumer
    try:
        tagging_consumer = TaggingConsumer(tagging_service)
        logger.info("Starting RabbitMQ consumer...")
        tagging_consumer.start()
    except Exception as e:
        logger.error(f"RabbitMQ consumer error: {e}")

@asynccontextmanager
async def lifespan(app: FastAPI):
    global consumer_thread
    logger.info("Starting AI Tagging Service...")
    tagging_service = await TaggingService.create()
    init_tagging_service(tagging_service)
    
    # 启动 RabbitMQ 消费者（在后台线程中）
    consumer_thread = threading.Thread(target=start_rabbitmq_consumer, args=(tagging_service,), daemon=True)
    consumer_thread.start()
    logger.info("RabbitMQ consumer thread started")
    
    yield
    logger.info("Stopping AI Tagging Service...")
    if tagging_consumer:
        tagging_consumer.close()

app = FastAPI(
    title="AI Tagging Service",
    version="1.0",
    lifespan=lifespan
)

app.include_router(router)

if __name__ == "__main__":
    uvicorn.run(
        "internal.app.main:app",
        host=settings.API_HOST,
        port=settings.API_PORT,
        reload=False,
    )
