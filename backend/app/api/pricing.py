from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.database import get_db
from app.schemas.pricing import PriceCalculationRequest, PriceCalculationResponse, MaterialInput, HierarchicalPricingRequest, HierarchicalPricingResponse, MaterialComponentInput
from app.services.pricing_service import PricingService
from app.models.category import MaterialSubCategory
router = APIRouter(prefix='/pricing', tags=['Dynamic Scrap Pricing Engine'])

@router.post('/calculate-hierarchical', response_model=HierarchicalPricingResponse)
def calculate_hierarchical_scrap_price(req: HierarchicalPricingRequest, db: Session=Depends(get_db)):
    return PricingService.calculate_hierarchical_price(req, db)

@router.post('/calculate', response_model=PriceCalculationResponse)
def calculate_scrap_price(req: PriceCalculationRequest, db: Session=Depends(get_db)):
    hier_items = [MaterialComponentInput(subcategory_code=m.material_code, weight_kg=m.weight_kg, purity_override=m.purity_factor, rust_level_percentage=0.0) for m in req.materials]
    hier_req = HierarchicalPricingRequest(items=hier_items, salvage_reusable_parts_value=req.salvage_value, pickup_distance_km=req.distance_km, custom_margin_percentage=req.custom_margin)
    hier_res = PricingService.calculate_hierarchical_price(hier_req, db)
    from app.schemas.pricing import ComponentBreakdown
    breakdown = [ComponentBreakdown(material_code=item.subcategory_code, material_name=item.subcategory_name, weight_kg=item.weight_kg, spot_rate_per_kg=item.spot_rate_per_kg, applied_purity=item.base_purity, gross_value=item.line_subtotal_inr) for item in hier_res.line_items]
    return PriceCalculationResponse(gross_material_value=hier_res.gross_material_value_inr, salvage_value=hier_res.salvage_parts_value_inr, processing_cost=hier_res.processing_and_segregation_cost_inr, logistics_cost=hier_res.logistics_and_haulage_cost_inr, dealer_margin_amount=hier_res.aggregator_margin_inr, net_calculated_price=hier_res.net_valuation_inr, recommended_dealer_payout=hier_res.final_payable_amount_inr, price_range_low=hier_res.estimated_price_range_low_inr, price_range_high=hier_res.estimated_price_range_high_inr, currency='INR', breakdown=breakdown, calculation_summary_hindi=hier_res.audio_summary_hi)

@router.get('/quick-quote', response_model=PriceCalculationResponse)
def quick_quote(material_code: str=Query(..., examples=['copper_bare_bright']), weight_kg: float=Query(..., gt=0, examples=[5.0]), purity_factor: float=Query(1.0, ge=0.1, le=1.0, examples=[0.9]), distance_km: float=Query(0.0, ge=0, examples=[2.0]), db: Session=Depends(get_db)):
    req = PriceCalculationRequest(materials=[MaterialInput(material_code=material_code, weight_kg=weight_kg, purity_factor=purity_factor)], distance_km=distance_km)
    return calculate_scrap_price(req, db)