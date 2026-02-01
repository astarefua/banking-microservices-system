from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import logging
import threading
import asyncio
from app.config import get_settings
from app.api.routes import router
from app.models.database import init_db
from app.kafka_consumer.consumer import TransactionConsumer
from app.services.eureka_http_client import register_with_eureka  # Changed this import

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)
settings = get_settings()

# Kafka consumer thread
kafka_thread = None


async def heartbeat_task():
    """Background task to send heartbeats to Eureka every 30 seconds"""
    from app.services.eureka_http_client import send_heartbeat
    await asyncio.sleep(10)  # Wait 10 seconds after startup

    while True:
        try:
            send_heartbeat()
        except Exception as e:
            logger.error(f"Error in heartbeat task: {e}")
        await asyncio.sleep(30)  # Send heartbeat every 30 seconds


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifespan events for startup and shutdown
    """
    # Startup
    logger.info("=" * 60)
    logger.info(f"🚀 Starting {settings.app_name} v{settings.app_version}")
    logger.info("=" * 60)

    # Initialize database
    logger.info("Initializing database...")
    init_db()
    logger.info("✅ Database initialized")

    # Register with Eureka
    try:
        register_with_eureka()
    except Exception as e:
        logger.warning(f"Could not register with Eureka: {e}")

    # Start Kafka consumer in a separate thread
    logger.info("Starting Kafka consumer...")
    global kafka_thread
    consumer = TransactionConsumer()
    kafka_thread = threading.Thread(target=consumer.start, daemon=True)
    kafka_thread.start()
    logger.info("✅ Kafka consumer started")

    # Start heartbeat task
    asyncio.create_task(heartbeat_task())
    logger.info("✅ Heartbeat task started")

    logger.info(f"✅ {settings.app_name} is ready!")
    logger.info(f"📍 Running on http://{settings.host}:{settings.port}")
    logger.info("=" * 60)

    yield

    # Shutdown
    logger.info("Shutting down...")


# Create FastAPI app
app = FastAPI(
    title="Fraud Detection Service",
    description="ML-based fraud detection microservice for banking transactions",
    version=settings.app_version,
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routes
app.include_router(router, prefix="/api", tags=["fraud-detection"])


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": settings.app_name,
        "version": settings.app_version,
        "status": "running",
        "endpoints": {
            "health": "/health",
            "fraud_check": "/api/fraud/check",
            "alerts": "/api/fraud/alerts",
            "stats": "/api/fraud/stats"
        }
    }


@app.get("/health")
async def health():
    """Health check endpoint"""
    return {
        "status": "UP",
        "service": settings.app_name,
        "version": settings.app_version
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=True,
        log_level="info"
    )