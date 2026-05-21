import aiohttp
from PIL import Image
from io import BytesIO
from typing import Optional
import logging
import ssl

logger = logging.getLogger(__name__)


class ImageLoader:
    @staticmethod
    async def load_image_from_url(url: str, timeout: int = 30) -> Optional[Image.Image]:
        try:
            ssl_context = ssl.create_default_context()
            ssl_context.check_hostname = False
            ssl_context.verify_mode = ssl.CERT_NONE
            connector = aiohttp.TCPConnector(ssl=ssl_context)
            async with aiohttp.ClientSession(connector=connector) as session:
                async with session.get(url, timeout=aiohttp.ClientTimeout(total=timeout)) as response:
                    if response.status != 200:
                        logger.warning(f"Failed to download image: {url}, status: {response.status}")
                        return None
                    image_data = await response.read()
                    image = Image.open(BytesIO(image_data))
                    if image.mode != "RGB":
                        image = image.convert("RGB")
                    return image
        except Exception as e:
            logger.error(f"Error loading image from {url}: {str(e)}")
            return None




