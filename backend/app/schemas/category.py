from pydantic import BaseModel, ConfigDict, Field
from typing import List, Optional

class SubCategoryBase(BaseModel):
    code: str = Field(..., examples=['copper_bare_bright'])
    name: str = Field(..., examples=['Copper Bare Bright (Millberry Wire)'])
    name_hi: str = Field(..., examples=['शुद्ध तांबा (बेयर ब्राइट तार)'])
    name_mr: str = Field(..., examples=['शुद्ध तांब्याची ताार (मिलबेरी)'])
    standard_density_kg_m3: float = Field(..., gt=0, examples=[8960.0])
    default_purity: float = Field(..., ge=0.0, le=1.0, examples=[0.99])
    current_spot_rate: float = Field(..., gt=0, examples=[695.0])
    volatility_min_rate: float = Field(..., gt=0, examples=[660.0])
    volatility_max_rate: float = Field(..., gt=0, examples=[740.0])
    unit: str = Field('kg', examples=['kg'])
    degradation_factor_rust: float = Field(0.05, ge=0.0, le=1.0, examples=[0.05])
    oxidation_multiplier: float = Field(1.0, ge=0.0, examples=[1.0])
    contamination_factor: float = Field(0.1, ge=0.0, le=1.0, examples=[0.1])
    precious_metal_yield_gold_g_tonne: float = Field(0.0, ge=0.0, examples=[0.0])
    precious_metal_yield_silver_g_tonne: float = Field(0.0, ge=0.0, examples=[0.0])
    precious_metal_yield_palladium_g_tonne: float = Field(0.0, ge=0.0, examples=[0.0])
    hsn_code: Optional[str] = Field(None, examples=['740400'])

class SubCategoryOut(SubCategoryBase):
    id: int
    category_id: Optional[int] = None
    model_config = ConfigDict(from_attributes=True)

class CategoryBase(BaseModel):
    code: str = Field(..., examples=['non_ferrous'])
    name: str = Field(..., examples=['Non-Ferrous Metals'])
    name_hi: str = Field(..., examples=['अलौह धातुएं'])
    name_mr: str = Field(..., examples=['अलोह धातू'])
    description: Optional[str] = Field(None, examples=['High-value non-magnetic recyclable metals'])

class CategoryOut(CategoryBase):
    id: int
    subcategories: List[SubCategoryOut] = []
    model_config = ConfigDict(from_attributes=True)