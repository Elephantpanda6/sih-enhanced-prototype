import logging
from typing import List
from sqlalchemy.orm import Session
from app.config import settings
from app.models.category import MaterialSubCategory
from app.schemas.pricing import HierarchicalPricingRequest, HierarchicalPricingResponse, SubCategoryValuationLine
logger = logging.getLogger('sih.scrap.pricing')

class PricingService:
    CO2_SAVINGS_PER_KG = {'copper': 4.5, 'aluminum': 9.0, 'iron': 1.6, 'e_waste': 14.0, 'battery': 3.8, 'plastic': 2.2, 'paper': 1.1}

    @staticmethod
    def calculate_hierarchical_price(req: HierarchicalPricingRequest, db: Session) -> HierarchicalPricingResponse:
        logger.info(f'Computing hierarchical scrap price for {len(req.items)} items, distance={req.pickup_distance_km}km')
        gross_material_val = 0.0
        total_weight = 0.0
        total_gold_grams = 0.0
        total_silver_grams = 0.0
        total_palladium_grams = 0.0
        total_co2_saved = 0.0
        processing_fee = 0.0
        line_items: List[SubCategoryValuationLine] = []
        margin_pct = req.custom_margin_percentage if req.custom_margin_percentage is not None else settings.DEFAULT_MARGIN
        for item in req.items:
            subcat = db.query(MaterialSubCategory).filter(MaterialSubCategory.code == item.subcategory_code).first()
            if subcat:
                spot_rate = subcat.current_spot_rate
                name_en = subcat.name
                name_hi = subcat.name_hi
                name_mr = subcat.name_mr
                default_purity = subcat.default_purity
                rust_penalty_factor = subcat.degradation_factor_rust
                oxidation_multiplier = getattr(subcat, 'oxidation_multiplier', 1.0) or 1.0
                contamination_factor = getattr(subcat, 'contamination_factor', 0.1) or 0.1
                gold_yield_rate = subcat.precious_metal_yield_gold_g_tonne
                silver_yield_rate = subcat.precious_metal_yield_silver_g_tonne
                palladium_yield_rate = getattr(subcat, 'precious_metal_yield_palladium_g_tonne', 0.0) or 0.0
            else:
                spot_rate = 35.0
                name_en = item.subcategory_code.replace('_', ' ').title()
                name_hi = name_en
                name_mr = name_en
                default_purity = 0.9
                rust_penalty_factor = 0.15
                oxidation_multiplier = 1.0
                contamination_factor = 0.1
                gold_yield_rate = 0.0
                silver_yield_rate = 0.0
                palladium_yield_rate = 0.0
            base_purity = item.purity_override if item.purity_override is not None else default_purity
            rust_pct = min(max(item.rust_level_percentage or 0.0, 0.0), 100.0)
            contam_pct = min(max(item.contamination_level_percentage or 0.0, 0.0), 100.0)
            rust_discount = rust_pct / 100.0 * rust_penalty_factor * oxidation_multiplier
            contam_discount = contam_pct / 100.0 * contamination_factor
            effective_purity = max(base_purity * (1.0 - rust_discount - contam_discount), 0.1)
            effective_rate = round(spot_rate * effective_purity, 2)
            line_total = round(item.weight_kg * effective_rate, 2)
            gross_material_val += line_total
            total_weight += item.weight_kg
            weight_in_tonnes = item.weight_kg / 1000.0
            line_gold = round(weight_in_tonnes * gold_yield_rate, 4)
            line_silver = round(weight_in_tonnes * silver_yield_rate, 4)
            line_palladium = round(weight_in_tonnes * palladium_yield_rate, 4)
            total_gold_grams += line_gold
            total_silver_grams += line_silver
            total_palladium_grams += line_palladium
            if 'e_waste' in item.subcategory_code:
                processing_fee += item.weight_kg * 6.0
            elif 'battery' in item.subcategory_code:
                processing_fee += item.weight_kg * 4.0
            else:
                processing_fee += item.weight_kg * 1.5
            cat_key = 'iron'
            if 'copper' in item.subcategory_code:
                cat_key = 'copper'
            elif 'aluminum' in item.subcategory_code:
                cat_key = 'aluminum'
            elif 'e_waste' in item.subcategory_code:
                cat_key = 'e_waste'
            elif 'battery' in item.subcategory_code:
                cat_key = 'battery'
            elif 'plastic' in item.subcategory_code:
                cat_key = 'plastic'
            elif 'paper' in item.subcategory_code:
                cat_key = 'paper'
            total_co2_saved += item.weight_kg * PricingService.CO2_SAVINGS_PER_KG.get(cat_key, 1.5)
            line_items.append(SubCategoryValuationLine(subcategory_code=item.subcategory_code, subcategory_name=name_en, subcategory_name_hi=name_hi, subcategory_name_mr=name_mr, weight_kg=item.weight_kg, spot_rate_per_kg=spot_rate, base_purity=base_purity, rust_penalty_deduction=round(rust_discount * 100, 1), effective_rate_per_kg=effective_rate, line_subtotal_inr=line_total, estimated_gold_yield_grams=line_gold, estimated_silver_yield_grams=line_silver, estimated_palladium_yield_grams=line_palladium))
        if req.pickup_distance_km > 0:
            logistics_fee = round(settings.BASE_PICKUP_FEE_INR + settings.PER_KM_CHARGE_INR * req.pickup_distance_km, 2)
        else:
            logistics_fee = 0.0
        total_gross_asset = gross_material_val + req.salvage_reusable_parts_value
        margin_amount = round(total_gross_asset * margin_pct, 2)
        net_val = round((1.0 - margin_pct) * total_gross_asset - (processing_fee + logistics_fee), 2)
        payable_amount = round(max(net_val, settings.MIN_PRICE_FLOOR_INR), 2)
        price_low = round(payable_amount * 0.96, 2)
        price_high = round(payable_amount * 1.04, 2)
        precious_note_mr = ''
        precious_note_hi = ''
        if total_gold_grams > 0 or total_palladium_grams > 0:
            precious_note_mr = f' यातून {total_gold_grams:.2f} ग्रॅम सोने आणि {total_palladium_grams:.2f} ग्रॅम पॅलॅडियम पुनर्प्राप्त होईल.'
            precious_note_hi = f' इससे लगभग {total_gold_grams:.2f} ग्राम सोना और {total_palladium_grams:.2f} ग्राम पैलेडियम रिकवर होगा।'
        audio_mr = f'एकूण {total_weight:.1f} किलो भंगाराचे अंदाजे मूल्य ₹{int(payable_amount)} आहे. बाजार चढउतारानुसार हे ₹{int(price_low)} ते ₹{int(price_high)} राहील.{precious_note_mr} यामुळे {round(total_co2_saved, 1)} किलो कार्बन उत्सर्जन वाचवले गेले आहे.'
        audio_hi = f'कुल {total_weight:.1f} किलो कबाड़ का अनुमानित मूल्य ₹{int(payable_amount)} है। बाजार भाव के अनुसार यह ₹{int(price_low)} से ₹{int(price_high)} के बीच रहेगा।{precious_note_hi} इससे {round(total_co2_saved, 1)} किलो कार्बन उत्सर्जन की बचत हुई है।'
        logger.info(f'Valuation complete: Payable=₹{payable_amount}, Net=₹{net_val}, CO2 Saved={total_co2_saved:.1f}kg')
        return HierarchicalPricingResponse(gross_material_value_inr=round(gross_material_val, 2), salvage_parts_value_inr=req.salvage_reusable_parts_value, processing_and_segregation_cost_inr=round(processing_fee, 2), logistics_and_haulage_cost_inr=logistics_fee, aggregator_margin_inr=margin_amount, net_valuation_inr=net_val, guaranteed_floor_price_inr=settings.MIN_PRICE_FLOOR_INR, final_payable_amount_inr=payable_amount, estimated_price_range_low_inr=price_low, estimated_price_range_high_inr=price_high, total_scrap_weight_kg=round(total_weight, 2), total_precious_gold_grams=round(total_gold_grams, 4), total_precious_silver_grams=round(total_silver_grams, 4), total_precious_palladium_grams=round(total_palladium_grams, 4), co2_emissions_saved_kg=round(total_co2_saved, 2), line_items=line_items, audio_summary_hi=audio_hi, audio_summary_mr=audio_mr)