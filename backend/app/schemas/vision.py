from pydantic import BaseModel, Field
from typing import List, Tuple, Optional

class VisualDetectionItem(BaseModel):
    detected_subcategory_code: str
    detected_name: str
    detected_name_hi: str
    detected_name_mr: str
    confidence_score: float
    surface_rust_oxidation_pct: float
    recommended_purity_factor: float
    estimated_weight_range_kg: Tuple[float, float]
    dismantling_and_segregation_tip_hi: str
    dismantling_and_segregation_tip_mr: str

class OfflineVisionAnalysisResponse(BaseModel):
    filename: str
    image_resolution: str
    primary_category_detected: str
    surface_rust_percentage: float
    contamination_percentage: float
    metallic_reflectance_score: float
    texture_edge_density: float
    cleanliness_grade: str
    detected_components: List[VisualDetectionItem]
    estimated_price_range_inr: Tuple[float, float]
    voice_feedback_hi: str
    voice_feedback_mr: str
    next_step_action: str
    model_architecture: str = 'MobileNet-V3-Small / Embedded CPU Multi-Spectral Heuristic'

class ConditionAssessment(BaseModel):
    wear_grade: str
    casing_intactness_pct: float
    oxidation_rust_pct: float
    purity_factor: float
    damage_observations: List[str]

class HazardSafetyAlert(BaseModel):
    has_toxic_hazards: bool
    hazard_level: str
    toxic_substances: List[str]
    alert_en: str
    alert_hi: str
    alert_mr: str
    safe_handling_protocol: str

class ScrapValuationQuote(BaseModel):
    material_code: str
    material_name: str
    base_mandi_rate_inr_per_kg: float
    estimated_weight_range_kg: Tuple[float, float]
    estimated_payout_range_inr: Tuple[float, float]
    carbon_offset_kg: float

class MultimodalVisionAnalysisResponse(BaseModel):
    success: bool
    item_name: str
    item_name_hi: str
    item_name_mr: str
    cpcb_category: str
    condition: ConditionAssessment
    safety_hazard: HazardSafetyAlert
    valuation: ScrapValuationQuote
    authorized_recycler_channel: str
    ai_engine: str