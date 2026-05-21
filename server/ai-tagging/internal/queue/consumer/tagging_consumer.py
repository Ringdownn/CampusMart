import pika
import json
import logging
import asyncio
from internal.queue.config import RabbitMQConfig
from internal.queue.producer.index_producer import IndexProducer
from internal.app.services.tagging import TaggingService

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class TaggingConsumer:
    def __init__(self, tagging_service: TaggingService):
        self.connection = pika.BlockingConnection(
            pika.ConnectionParameters(
                host=RabbitMQConfig.HOST,
                port=RabbitMQConfig.PORT,
                credentials=pika.PlainCredentials(
                    RabbitMQConfig.USER,
                    RabbitMQConfig.PASSWORD
                ),
                virtual_host=RabbitMQConfig.VHOST
            )
        )
        self.channel = self.connection.channel()
        self.tagging_service = tagging_service
        self.index_producer = IndexProducer()
        
        # 声明交换机
        self.channel.exchange_declare(
            exchange=RabbitMQConfig.TAGGING_EXCHANGE,
            exchange_type='direct',
            durable=True
        )
        
        # 声明队列
        self.channel.queue_declare(
            queue=RabbitMQConfig.TAGGING_QUEUE,
            durable=True
        )
        
        # 绑定队列
        self.channel.queue_bind(
            queue=RabbitMQConfig.TAGGING_QUEUE,
            exchange=RabbitMQConfig.TAGGING_EXCHANGE,
            routing_key=RabbitMQConfig.TAGGING_KEY
        )
    
    def start(self):
        def callback(ch, method, properties, body):
            try:
                task = json.loads(body)
                doc_id = task['doc'].get('id', 'unknown')
                logger.info(f"Received tagging task for doc: {doc_id}")
                
                # 调用 AI 打标服务
                if task['doc'].get('imageURL'):
                    try:
                        tags = asyncio.run(
                            self.tagging_service.tag_image_from_url(
                                task['doc']['imageURL']
                            )
                        )
                        task['doc']['tags'] = [tag.label for tag in tags]
                        logger.info(f"Generated {len(tags)} tags for doc: {doc_id}")
                    except Exception as e:
                        logger.error(f"AI 打标失败，docId: {doc_id}, error: {e}")
                        task['doc']['tags'] = []  # 空 tags，继续处理
                else:
                    task['doc']['tags'] = []
                
                # 发送索引更新任务
                self.index_producer.publish_index_task({
                    'database': task['database'],
                    'doc': task['doc'],
                    'source': 'ai'
                })
                
                ch.basic_ack(delivery_tag=method.delivery_tag)
                logger.info(f"Tagging completed for doc: {doc_id}")
                
            except Exception as e:
                logger.error(f"处理消息失败：{e}")
                # 不重新入队，避免无限循环
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        
        self.channel.basic_consume(
            queue=RabbitMQConfig.TAGGING_QUEUE,
            on_message_callback=callback
        )
        
        logger.info('Tagging consumer started')
        self.channel.start_consuming()
    
    def close(self):
        """关闭连接"""
        try:
            if self.connection and not self.connection.is_closed:
                self.connection.close()
                logger.info("TaggingConsumer connection closed")
        except Exception as e:
            logger.error(f"Error closing connection: {e}")
    
    def __del__(self):
        self.close()
