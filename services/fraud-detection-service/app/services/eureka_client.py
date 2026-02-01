import py_eureka_client.eureka_client as eureka_client
from app.config import get_settings
import logging
import asyncio
from concurrent.futures import ThreadPoolExecutor

logger = logging.getLogger(__name__)
settings = get_settings()

executor = ThreadPoolExecutor(max_workers=1)


async def register_with_eureka_async():
    """Register this service with Eureka asynchronously"""
    loop = asyncio.get_event_loop()
    await loop.run_in_executor(executor, _register_sync)


def _register_sync():
    """Synchronous Eureka registration to run in thread"""
    try:
        logger.info("Registering with Eureka Server...")

        eureka_client.init(
            eureka_server=settings.eureka_server_url,
            app_name=settings.app_name,
            instance_port=settings.port,
            instance_host="localhost",
            instance_ip="127.0.0.1",
            health_check_url=f"http://localhost:{settings.port}/health",
            status_page_url=f"http://localhost:{settings.port}/health",
            home_page_url=f"http://localhost:{settings.port}/",
        )

        logger.info(f"✅ Successfully registered with Eureka: {settings.app_name}")

    except Exception as e:
        logger.error(f"❌ Failed to register with Eureka: {e}")