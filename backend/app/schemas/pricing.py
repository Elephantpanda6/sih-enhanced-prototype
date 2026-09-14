from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional

class MaterialInput(BaseModel):
    material_code: str = Field(..., examples=['copper_bare_bright'])
    weight_kg: float = Field(..., gt=0, examples=[2.5])
    purity_factor: Optional[float] = Field(None, ge=0.05, le=1.0, examples=[0.9])

class ComponentBreakdown(BaseModel):
    material_code: str
    material_name: str
    weight_kg: float
    spot_rate_per_kg: float
    applied_purity: float
    gross_value: float

class PriceCalculationRequest(BaseModel):
    materials: List[MaterialInput]
    salvage_value: float = Field(0.0, ge=0, examples=[50.0])
    distance_km: float = Field(0.0, ge=0, examples=[3.5])
    custom_margin: Optional[float] = Field(None, ge=0, le=0.5, examples=[0.15])
    item_description: Optional[str] = Field(None, examples=['Ceiling Fan or Laptop Scrap'])

class PriceCalculationResponse(BaseModel):
    gross_material_value: float
    salvage_value: float
    processing_cost: float
    logistics_cost: float
    dealer_margin_amount: float
    net_calculated_price: float
    recommended_dealer_payout: float
    price_range_low: float
    price_range_high: float
    currency: str = 'INR'
    breakdown: List[ComponentBreakdown]
    calculation_summary_hindi: str

class MaterialComponentInput(BaseModel):
    subcategory_code: str = Field(..., examples=['copper_bare_bright', 'e_waste_pcb_high'])
    weight_kg: float = Field(..., gt=0, examples=[4.5])
    purity_override: Optional[float] = Field(None, ge=0.05, le=1.0, examples=[0.9])
    rust_level_percentage: Optional[float] = Field(0.0, ge=0.0, le=100.0, examples=[15.0])
    contamination_level_percentage: Optional[float] = Field(0.0, ge=0.0, le=100.0, examples=[5.0])

class SubCategoryValuationLine(BaseModel):
    subcategory_code: str
    subcategory_name: str
    subcategory_name_hi: str
    subcategory_name_mr: str
    weight_kg: float
    spot_rate_per_kg: float
    base_purity: float
    rust_penalty_deduction: float
    effective_rate_per_kg: float
    line_subtotal_inr: float
    estimated_gold_yield_grams: float = 0.0
    estimated_silver_yield_grams: float = 0.0
    estimated_palladium_yield_grams: float = 0.0

class HierarchicalPricingRequest(BaseModel):
    items: List[MaterialComponentInput]
    salvage_reusable_parts_value: float = Field(0.0, ge=0.0, examples=[50.0])
    pickup_distance_km: float = Field(0.0, ge=0.0, examples=[4.0])
    custom_margin_percentage: Optional[float] = Field(None, ge=0.0, le=0.5, examples=[0.15])
    pickup_latitude: Optional[float] = None
    pickup_longitude: Optional[float] = None

class HierarchicalPricingResponse(BaseModel):
    gross_material_value_inr: float
    salvage_parts_value_inr: float
    processing_and_segregation_cost_inr: float
    logistics_and_haulage_cost_inr: float
    aggregator_margin_inr: float
    net_valuation_inr: float
    guaranteed_floor_price_inr: float
    final_payable_amount_inr: float
    estimated_price_range_low_inr: float
    estimated_price_range_high_inr: float
    total_scrap_weight_kg: float
    total_precious_gold_grams: float
    total_precious_silver_grams: float
    total_precious_palladium_grams: float
    co2_emissions_saved_kg: float
    line_items: List[SubCategoryValuationLine]
    audio_summary_hi: str
    audio_summary_mr: str
    audio_base64: Optional[str] = None