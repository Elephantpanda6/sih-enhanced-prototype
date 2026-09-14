from datetime import datetime, timezone
from sqlalchemy import Column, Integer, String, Float, ForeignKey, DateTime, Text
from sqlalchemy.orm import relationship
from app.database import Base

def utc_now():
    return datetime.now(timezone.utc)

class MaterialCategory(Base):
    __tablename__ = 'material_categories'
    id = Column(Integer, primary_key=True, index=True)
    code = Column(String, unique=True, index=True, nullable=False)
    name = Column(String, nullable=False)
    name_hi = Column(String, nullable=False)
    name_mr = Column(String, nullable=False)
    description = Column(Text, nullable=True)
    created_at = Column(DateTime, default=utc_now)
    subcategories = relationship('MaterialSubCategory', back_populates='category', cascade='all, delete-orphan', order_by='MaterialSubCategory.id')

class MaterialSubCategory(Base):
    __tablename__ = 'material_subcategories'
    id = Column(Integer, primary_key=True, index=True)
    category_id = Column(Integer, ForeignKey('material_categories.id'), nullable=False)
    code = Column(String, unique=True, index=True, nullable=False)
    name = Column(String, nullable=False)
    name_hi = Column(String, nullable=False)
    name_mr = Column(String, nullable=False)
    standard_density_kg_m3 = Column(Float, nullable=False, default=1000.0)
    default_purity = Column(Float, nullable=False, default=0.95)
    current_spot_rate = Column(Float, nullable=False)
    volatility_min_rate = Column(Float, nullable=False)
    volatility_max_rate = Column(Float, nullable=False)
    unit = Column(String, default='kg')
    degradation_factor_rust = Column(Float, default=0.15)
    oxidation_multiplier = Column(Float, default=1.0)
    contamination_factor = Column(Float, default=0.1)
    precious_metal_yield_gold_g_tonne = Column(Float, default=0.0)
    precious_metal_yield_silver_g_tonne = Column(Float, default=0.0)
    precious_metal_yield_palladium_g_tonne = Column(Float, default=0.0)
    hsn_code = Column(String, nullable=True)
    updated_at = Column(DateTime, default=utc_now, onupdate=utc_now)
    category = relationship('MaterialCategory', back_populates='subcategories')