import os
import logging
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = 'SIH Smart Scrap & E-Waste Valuation Engine'
    VERSION: str = '2.2.0'
    API_V1_STR: str = '/api/v1'
    SECRET_KEY: str = os.getenv('SECRET_KEY', 'sih2026-cpcb-enterprise-scrap-auth-token-998877')
    ALGORITHM: str = 'HS256'
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7
    DATABASE_URL: str = os.getenv('DATABASE_URL', 'sqlite:///./scrap_system.db')
    DEFAULT_MARGIN: float = 0.15
    BASE_PICKUP_FEE_INR: float = 20.0
    PER_KM_CHARGE_INR: float = 5.0
    MIN_PRICE_FLOOR_INR: float = 10.0
    VOSK_MODEL_PATH_HI: str = os.getenv('VOSK_MODEL_PATH_HI', 'models/vosk-model-small-hi-0.22')
    VOSK_MODEL_PATH_MR: str = os.getenv('VOSK_MODEL_PATH_MR', 'models/vosk-model-small-mr-0.22')
    VISION_MODEL_PATH: str = os.getenv('VISION_MODEL_PATH', 'models/mobilenetv3_scrap_v1.onnx')
    FIREBASE_CREDENTIALS: str = os.getenv('FIREBASE_CREDENTIALS', 'firebase_credentials.json')
    GEMINI_API_KEY: str = os.getenv('GEMINI_API_KEY', '')
    AUDIO_SAMPLE_RATE_HZ: int = 16000
    AUDIO_CHANNELS: int = 1
    LOG_LEVEL: str = os.getenv('LOG_LEVEL', 'INFO')
    model_config = SettingsConfigDict(case_sensitive=True)
settings = Settings()
logging.basicConfig(level=getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO), format='%(asctime)s [%(levelname)s] %(name)s (%(filename)s:%(lineno)d) - %(message)s', datefmt='%Y-%m-%d %H:%M:%S')
logger = logging.getLogger('sih.scrap.backend')