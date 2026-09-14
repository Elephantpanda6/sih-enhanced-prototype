import pytest

def test_hierarchical_pricing_with_rust_and_ewaste(client):
    payload = {'items': [{'subcategory_code': 'copper_bare_bright', 'weight_kg': 3.0, 'purity_override': 0.98, 'rust_level_percentage': 0.0}, {'subcategory_code': 'iron_hms_heavy', 'weight_kg': 25.0, 'rust_level_percentage': 30.0, 'contamination_level_percentage': 5.0}, {'subcategory_code': 'e_waste_pcb_high', 'weight_kg': 5.0, 'rust_level_percentage': 0.0}], 'salvage_reusable_parts_value': 75.0, 'pickup_distance_km': 5.0, 'custom_margin_percentage': 0.15}
    res = client.post('/api/v1/pricing/calculate-hierarchical', json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data['gross_material_value_inr'] > 3000.0
    assert data['logistics_and_haulage_cost_inr'] == 45.0
    assert data['final_payable_amount_inr'] > 2500.0
    assert data['total_scrap_weight_kg'] == 33.0
    assert data['total_precious_gold_grams'] >= 0.85
    assert data['total_precious_silver_grams'] >= 3.0
    assert data['total_precious_palladium_grams'] >= 0.15
    assert data['co2_emissions_saved_kg'] > 50.0
    lines_by_code = {line['subcategory_code']: line for line in data['line_items']}
    assert lines_by_code['iron_hms_heavy']['rust_penalty_deduction'] > 0.0
    assert lines_by_code['e_waste_pcb_high']['estimated_palladium_yield_grams'] > 0.0
    assert '₹' in data['audio_summary_hi']
    assert '₹' in data['audio_summary_mr']
    assert 'कार्बन' in data['audio_summary_hi'] or 'कार्बन' in data['audio_summary_mr']

def test_floor_price_enforcement(client):
    payload = {'items': [{'subcategory_code': 'paper_corrugated_carton', 'weight_kg': 1.0, 'rust_level_percentage': 0.0}], 'pickup_distance_km': 15.0}
    res = client.post('/api/v1/pricing/calculate-hierarchical', json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data['net_valuation_inr'] < 0.0
    assert data['final_payable_amount_inr'] == 10.0
    assert data['guaranteed_floor_price_inr'] == 10.0

def test_quick_quote_endpoint(client):
    res = client.get('/api/v1/pricing/quick-quote?material_code=copper_bare_bright&weight_kg=5.0&purity_factor=0.95')
    assert res.status_code == 200
    data = res.json()
    assert data['gross_material_value'] > 3000.0
    assert data['recommended_dealer_payout'] > 2500.0