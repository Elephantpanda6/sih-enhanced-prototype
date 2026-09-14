from datetime import datetime, timezone
from sqlalchemy import Column, Integer, String, Float, DateTime
from app.database import Base

def utc_now():
    return datetime.now(timezone.utc)

class MarketRate(Base):
    __tablename__ = 'market_rates'
    id = Column(Integer, primary_key=True, index=True)
    code = Column(String, unique=True, index=True, nullable=False)
    name = Column(String, nullable=False)
    category = Column(String, index=True, nullable=False)
    rate_per_kg = Column(Float, nullable=False)
    default_purity = Column(Float, default=1.0)
    unit = Column(String, default='kg')
    updated_at = Column(DateTime, default=utc_now, onupdate=utc_now)