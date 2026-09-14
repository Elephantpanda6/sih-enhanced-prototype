import sys
import io
import json
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
from fastapi.testclient import TestClient
from app.main import app
client = TestClient(app)

def run_tests():
    print('=======================================================')
    print('SIH ENTERPRISE SMART SCRAP & E-WASTE BACKEND TEST SUITE')
    print('=======================================================')
    print('\n--- 1. Testing Root Healthcheck & Documentation ---')
    res = client.get('/')
    assert res.status_code == 200
    root_data = res.json()
    print('Status:', root_data['status'])
    print('Service:', root_data['service'])
    print('Version:', root_data['version'])
    print('Compliance:', root_data['compliance'])
    print('\n--- 2. Testing Two-Tier Hierarchical Taxonomy ---')
    res = client.get('/api/v1/rates/categories')
    assert res.status_code == 200
    categories = res.json()
    print(f'Loaded {len(categories)} Top-Tier Categories:')
    for cat in categories:
        print(f"  * {cat['name']} ({cat['name_mr']}) - {len(cat['subcategories'])} Subcategories")
    print('\n--- 3. Testing Marathi & Hindi Voice-to-Command Recognition ---')
    mr_phrase = 'don kilo tamba aani paach kilo lokhand bhav sanga'
    res_mr = client.post('/api/v1/voice/parse', json={'transcript': mr_phrase, 'language': 'mr'})
    assert res_mr.status_code == 200
    mr_data = res_mr.json()
    print('Marathi Input:', mr_phrase)
    print('Detected Intent:', mr_data['detected_intent'])
    print('Detected Language:', mr_data['detected_language'])
    print(f"Detected Items ({len(mr_data['detected_items'])}):")
    for item in mr_data['detected_items']:
        print(f"  - {item['subcategory_name']} ({item['subcategory_name_local']}): {item['weight_kg']} kg @ Rs. {item['spot_rate_per_kg']}/kg = Rs. {item['subtotal_inr']}")
    print('Total Valuation: Rs.', mr_data['total_estimated_price_inr'])
    print('Marathi Spoken TTS:', mr_data['feedback_audio_text_mr'])
    print('Audio WAV Data URI generated:', mr_data['feedback_audio_base64'][:50] + '...')
    hi_phrase = 'das kilo gatta aur teen kilo loha pickup bhejo'
    res_hi = client.post('/api/v1/voice/parse', json={'transcript': hi_phrase, 'language': 'hi'})
    assert res_hi.status_code == 200
    hi_data = res_hi.json()
    print('\nHindi Input:', hi_phrase)
    print('Detected Intent:', hi_data['detected_intent'])
    print('Hindi Spoken TTS:', hi_data['feedback_audio_text_hi'])
    print('\n--- 4. Testing Hierarchical Pricing with Precious Metal Recovery ---')
    pricing_payload = {'items': [{'subcategory_code': 'copper_bare_bright', 'weight_kg': 4.0, 'purity_override': 0.98, 'rust_level_percentage': 0.0}, {'subcategory_code': 'iron_hms_heavy', 'weight_kg': 20.0, 'rust_level_percentage': 25.0}, {'subcategory_code': 'e_waste_pcb_high', 'weight_kg': 5.0, 'rust_level_percentage': 0.0}], 'salvage_reusable_parts_value': 100.0, 'pickup_distance_km': 6.0, 'custom_margin_percentage': 0.15}
    res_pricing = client.post('/api/v1/pricing/calculate-hierarchical', json=pricing_payload)
    assert res_pricing.status_code == 200
    p_data = res_pricing.json()
    print('Gross Material Value: Rs.', p_data['gross_material_value_inr'])
    print('Logistics Haulage Cost: Rs.', p_data['logistics_and_haulage_cost_inr'])
    print('Net Valuation: Rs.', p_data['net_valuation_inr'])
    print('Final Payable Amount: Rs.', p_data['final_payable_amount_inr'])
    print('Estimated Price Range: Rs.', p_data['estimated_price_range_low_inr'], 'to Rs.', p_data['estimated_price_range_high_inr'])
    print(f"Precious Metal Recovery: Gold={p_data['total_precious_gold_grams']}g, Silver={p_data['total_precious_silver_grams']}g, Palladium={p_data['total_precious_palladium_grams']}g")
    print(f"CO2 Emissions Abated: {p_data['co2_emissions_saved_kg']} kg CO2")
    print('\n--- 5. Testing CPCB Geospatial Recycler Locator ---')
    res_geo = client.get('/api/v1/recyclers/nearest?lat=19.0178&lon=72.8478&category=e_waste&max_radius_km=100')
    assert res_geo.status_code == 200
    geo_data = res_geo.json()
    print(f"Found {geo_data['total_found']} Authorized Recyclers within 100km radius:")
    for item in geo_data['nearby_recyclers'][:3]:
        r = item['recycler']
        print(f"  * {r['name']} ({r['district']}, {r['state']})")
        print(f"    - Authorization: {r['authorization_number']} [{r['regulatory_board']}]")
        print(f"    - Distance: {item['distance_km']} km (Est. Driving Time: {item['estimated_driving_time_mins']} mins)")
        print(f"    - Directions: {item['google_maps_directions_url']}")
    print('Voice Navigation (Marathi):', geo_data['voice_navigation_instruction_mr'])
    print('\n--- 6. Testing Offline Computer Vision Scrap Classifier ---')
    from PIL import Image
    img = Image.new('RGB', (224, 224), color=(160, 50, 30))
    buf = io.BytesIO()
    img.save(buf, format='JPEG')
    res_vision = client.post('/api/v1/vision/analyze-image', files={'file': ('rusty_iron_beam.jpg', buf.getvalue(), 'image/jpeg')})
    assert res_vision.status_code == 200
    v_data = res_vision.json()
    print('Detected Category:', v_data['primary_category_detected'])
    print('Surface Rust:', f"{v_data['surface_rust_percentage']}%")
    print('Contamination Grade:', v_data['cleanliness_grade'])
    print('Edge Density:', v_data['texture_edge_density'])
    print('Voice Tip (Marathi):', v_data['voice_feedback_mr'])
    print('Operational Next Step:', v_data['next_step_action'])
    print('\n--- 7. Testing User Authentication & Scrap Pickup Order ---')
    login_res = client.post('/api/v1/auth/login-json', json={'phone_number': '9876543210', 'password': '1234'})
    assert login_res.status_code == 200
    token = login_res.json()['access_token']
    print('Login Successful for user:', login_res.json()['user']['full_name'])
    headers = {'Authorization': f'Bearer {token}'}
    tx_payload = {'material_code': 'copper_bare_bright', 'weight_kg': 5.0, 'pickup_address': 'Shop 14, Dharavi Kumbharwada, Mumbai', 'pickup_latitude': 19.0434, 'pickup_longitude': 72.8566, 'voice_input_transcript': 'पाच किलो तांबे पिकअप पाठवा'}
    tx_res = client.post('/api/v1/transactions/', json=tx_payload, headers=headers)
    assert tx_res.status_code == 201
    tx_data = tx_res.json()
    print('Pickup Order Created! Order ID:', tx_data['id'])
    print('Material:', tx_data['material_name'])
    print('Calculated Payout: Rs.', tx_data['calculated_price'])
    print('\n=======================================================')
    print('ALL ENTERPRISE SPECIFICATION TESTS PASSED SUCCESSFULLY!')
    print('=======================================================')
if __name__ == '__main__':
    run_tests()