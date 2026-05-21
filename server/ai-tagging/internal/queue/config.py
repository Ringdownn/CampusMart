import os

class RabbitMQConfig:
    HOST = os.environ.get('RABBITMQ_HOST', 'localhost')
    PORT = int(os.environ.get('RABBITMQ_PORT', 5672))
    USER = os.environ.get('RABBITMQ_USER') or os.environ.get('RABBITMQ_USERNAME', 'guest')
    PASSWORD = os.environ.get('RABBITMQ_PASSWORD', 'guest')
    VHOST = '/'
    
    TAGGING_EXCHANGE = 'tagging_exchange'
    INDEX_EXCHANGE = 'index_exchange'
    
    TAGGING_QUEUE = 'tagging_queue'
    INDEX_QUEUE = 'index_queue'
    
    TAGGING_KEY = 'tagging.task'
    INDEX_KEY = 'index.update'