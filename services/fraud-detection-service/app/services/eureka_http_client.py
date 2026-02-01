import requests
import socket
import logging
from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()


def register_with_eureka():
    """Register service with Eureka using direct HTTP calls"""
    try:
        logger.info("Registering with Eureka Server via HTTP...")

        # Eureka registration payload
        instance_data = {
            "instance": {
                "instanceId": f"{settings.app_name}:{settings.port}",
                "hostName": "localhost",
                "app": settings.app_name.upper(),
                "ipAddr": "127.0.0.1",
                "status": "UP",
                "port": {
                    "$": settings.port,
                    "@enabled": "true"
                },
                "healthCheckUrl": f"http://localhost:{settings.port}/health",
                "statusPageUrl": f"http://localhost:{settings.port}/health",
                "homePageUrl": f"http://localhost:{settings.port}/",
                "dataCenterInfo": {
                    "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                    "name": "MyOwn"
                },
                "vipAddress": settings.app_name,
                "secureVipAddress": settings.app_name,
                "metadata": {
                    "instanceId": f"{settings.app_name}:{settings.port}"
                }
            }
        }

        # Register with Eureka
        eureka_url = settings.eureka_server_url.replace("/eureka", "")
        register_url = f"{eureka_url}/eureka/apps/{settings.app_name.upper()}"

        response = requests.post(
            register_url,
            json=instance_data,
            headers={"Content-Type": "application/json"},
            timeout=5
        )

        if response.status_code in [200, 204]:
            logger.info(f"✅ Successfully registered with Eureka: {settings.app_name.upper()}")
            return True
        else:
            logger.error(f"❌ Failed to register with Eureka. Status: {response.status_code}, Response: {response.text}")
            return False

    except Exception as e:
        logger.error(f"❌ Failed to register with Eureka: {e}")
        return False


def send_heartbeat():
    """Send heartbeat to Eureka to keep registration alive"""
    try:
        eureka_url = settings.eureka_server_url.replace("/eureka", "")
        heartbeat_url = f"{eureka_url}/eureka/apps/{settings.app_name.upper()}/{settings.app_name}:{settings.port}"

        response = requests.put(heartbeat_url, timeout=5)

        if response.status_code == 200:
            logger.debug("💓 Heartbeat sent to Eureka")
            return True
        else:
            logger.warning(f"⚠️ Heartbeat failed. Status: {response.status_code}")
            return False

    except Exception as e:
        logger.error(f"❌ Heartbeat error: {e}")
        return False