from datetime import datetime, timezone
from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Text
from app.database import Base

def utc_now():
    return datetime.now(timezone.utc)

class ScrapTransaction(Base):
    __tablename__ = 'scrap_transactions'
    id = Column(Integer, primary_key=True, index=True)
    dealer_id = Column(Integer, ForeignKey('users.id'), nullable=False)
    buyer_id = Column(Integer, ForeignKey('users.id'), nullable=True)
    material_code = Column(String, nullable=False)
    material_name = Column(String, nullable=False)
    weight_kg = Column(Float, nullable=False)
    purity_factor = Column(Float, default=1.0)
    calculated_price = Column(Float, nullable=False)
    final_agreed_price = Column(Float, nullable=True)
    status = Column(String, default='pending')
    pickup_address = Column(Text, nullable=True)
    pickup_latitude = Column(Float, nullable=True)
    pickup_longitude = Column(Float, nullable=True)
    voice_input_transcript = Column(String, nullable=True)
    image_url = Column(String, nullable=True)
    notes = Column(Text, nullable=True)
    created_at = Column(DateTime, default=utc_now)
    updated_at = Column(DateTime, default=utc_now, onupdate=utc_now)