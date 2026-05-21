import pika
import json
import logging
from internal.queue.config import RabbitMQConfig

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class IndexProducer:
    _instance = None
    _connection = None
    _channel = None
    
    def __new__(cls):
        if cls._instance is None:
            logger.info("Creating new IndexProducer instance")
            cls._instance = super().__new__(cls)
            
            try:
                cls._connection = pika.BlockingConnection(
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
                cls._channel = cls._connection.channel()
                
                # 声明交换机
                cls._channel.exchange_declare(
                    exchange=RabbitMQConfig.INDEX_EXCHANGE,
                    exchange_type='direct',
                    durable=True
                )
                logger.info("IndexProducer initialized successfully")
            except Exception as e:
                logger.error(f"Failed to initialize IndexProducer: {e}")
                raise
        
        return cls._instance
    
    @property
    def channel(self):
        return self._channel
    
    def publish_index_task(self, task):
        try:
            routing_key = f"{RabbitMQConfig.INDEX_KEY}.{task['source']}"
            
            self._channel.basic_publish(
                exchange=RabbitMQConfig.INDEX_EXCHANGE,
                routing_key=routing_key,
                body=json.dumps(task),
                properties=pika.BasicProperties(
                    delivery_mode=2,  # 持久化
                )
            )
            logger.debug(f"Published index task for doc: {task.get('doc', {}).get('id', 'unknown')}")
        except Exception as e:
            logger.error(f"Failed to publish index task: {e}")
            raise
    
    def close(self):
        """关闭连接"""
        try:
            if self._connection and not self._connection.is_closed:
                self._connection.close()
                logger.info("IndexProducer connection closed")
        except Exception as e:
            logger.error(f"Error closing connection: {e}")
    
    def __del__(self):
        self.close()
