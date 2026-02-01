from kafka import KafkaConsumer
import json
import logging
from app.config import get_settings
from app.models.schemas import TransactionEvent
from app.services.fraud_service import FraudService
from app.models.database import SessionLocal
from datetime import datetime

logger = logging.getLogger(__name__)
settings = get_settings()


class TransactionConsumer:
    def __init__(self):
        self.fraud_service = FraudService()

        self.consumer = KafkaConsumer(
            settings.kafka_topic_transaction_created,
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id=settings.kafka_group_id,
            auto_offset_reset='earliest',
            enable_auto_commit=True,
            value_deserializer=lambda m: json.loads(m.decode('utf-8'))
        )

        logger.info(f"Kafka consumer initialized for topic: {settings.kafka_topic_transaction_created}")

    def start(self):
        """Start consuming messages"""
        logger.info("Starting Kafka consumer...")

        try:
            for message in self.consumer:
                self._process_message(message.value)
        except KeyboardInterrupt:
            logger.info("Consumer stopped by user")
        except Exception as e:
            logger.error(f"Error in consumer: {e}", exc_info=True)
        finally:
            self.consumer.close()

    def _process_message(self, message_data: dict):
        """Process incoming Kafka message"""
        try:
            logger.info("=" * 50)
            logger.info("🔔 KAFKA MESSAGE RECEIVED")
            logger.info(f"Message: {message_data}")
            logger.info("=" * 50)

            # Parse transaction event
            event = TransactionEvent(**message_data)

            # Get database session
            db = SessionLocal()

            try:
                # Process fraud check
                result = self.fraud_service.handle_transaction_event(event, db)

                logger.info(
                    f"✅ Fraud check complete - "
                    f"Transaction: {event.transactionId}, "
                    f"Risk Score: {result.risk_score:.2f}, "
                    f"Level: {result.risk_level}, "
                    f"Is Fraud: {result.is_fraud}"
                )

                if result.is_fraud:
                    logger.warning(f"⚠️  HIGH RISK TRANSACTION DETECTED: {event.transactionId}")

            finally:
                db.close()

        except Exception as e:
            logger.error(f"❌ Error processing message: {e}", exc_info=True)