from pydantic_settings import BaseSettings  # ← Change back to this
from functools import lru_cache


class Settings(BaseSettings):
    # Application
    app_name: str = "fraud-detection-service"
    app_version: str = "1.0.0"
    host: str = "0.0.0.0"
    port: int = 8084

    # Database
    database_url: str = "sqlite:///./fraud_detection.db"

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic_transaction_created: str = "transaction-created"
    kafka_group_id: str = "fraud-detection-group"

    # Eureka
    eureka_server_url: str = "http://localhost:8761/eureka"

    # Fraud Detection Settings
    fraud_threshold: float = 0.7
    max_transaction_amount: float = 50000.0

    class Config:
        env_file = ".env"


@lru_cache()
def get_settings():
    return Settings()