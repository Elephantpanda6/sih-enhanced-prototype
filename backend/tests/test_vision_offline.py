import io
from PIL import Image
import pytest

def create_synthetic_scrap_image(color: tuple, size=(224, 224)) -> bytes:
    img = Image.new('RGB', size, color=color)
    buf = io.BytesIO()
    img.save(buf, format='JPEG')
    return buf.getvalue()

def test_offline_vision_rust_analysis(client):
    rust_bytes = create_synthetic_scrap_image((160, 50, 30))
    res = client.post('/api/v1/vision/analyze-image', files={'file': ('rusty_beam.jpg', rust_bytes, 'image/jpeg')})
    assert res.status_code == 200
    data = res.json()
    assert data['surface_rust_percentage'] > 50.0
    assert data['primary_category_detected'] == 'ferrous'
    assert 'गंज' in data['voice_feedback_mr']
    assert 'जंग' in data['voice_feedback_hi']
    assert 'Grade C' in data['cleanliness_grade'] or 'Grade B' in data['cleanliness_grade']
    assert 'MobileNet' in data['model_architecture']

def test_offline_vision_pcb_analysis(client):
    pcb_bytes = create_synthetic_scrap_image((30, 140, 40))
    res = client.post('/api/v1/vision/analyze-image', files={'file': ('server_board.jpg', pcb_bytes, 'image/jpeg')})
    assert res.status_code == 200
    data = res.json()
    assert data['primary_category_detected'] == 'e_waste'
    assert len(data['detected_components']) >= 1
    assert data['detected_components'][0]['detected_subcategory_code'] == 'e_waste_pcb_high'
    assert 'सर्किट बोर्ड' in data['voice_feedback_hi']
    assert 'सर्किट बोर्ड' in data['voice_feedback_mr']
    assert data['detected_components'][0]['confidence_score'] > 0.9

def test_offline_vision_copper_analysis(client):
    copper_bytes = create_synthetic_scrap_image((180, 105, 50))
    res = client.post('/api/v1/vision/analyze-image', files={'file': ('copper_wire.jpg', copper_bytes, 'image/jpeg')})
    assert res.status_code == 200
    data = res.json()
    assert data['primary_category_detected'] == 'non_ferrous'
    assert data['detected_components'][0]['detected_subcategory_code'] == 'copper_bare_bright'
    assert 'तांब' in data['voice_feedback_mr'] or 'तांब' in data['voice_feedback_hi']

def test_offline_vision_invalid_file_rejected(client):
    res = client.post('/api/v1/vision/analyze-image', files={'file': ('document.pdf', b'%PDF-1.4 dummy', 'application/pdf')})
    assert res.status_code == 400
    assert 'valid jpeg/png image' in res.json()['detail'].lower()

def test_multimodal_vision_analysis_pcb(client):
    pcb_bytes = create_synthetic_scrap_image((30, 140, 40))
    res = client.post('/api/v1/vision/analyze-multimodal', files={'file': ('pcb_board.jpg', pcb_bytes, 'image/jpeg')})
    assert res.status_code == 200
    data = res.json()
    assert data['success'] is True
    assert 'PCB' in data['item_name'] or 'Circuit' in data['item_name']
    assert data['condition']['wear_grade'] is not None
    assert data['safety_hazard']['has_toxic_hazards'] is True
    assert 'Lead' in str(data['safety_hazard']['toxic_substances'])
    assert len(data['safety_hazard']['alert_hi']) > 0
    assert len(data['safety_hazard']['alert_mr']) > 0
    assert data['valuation']['base_mandi_rate_inr_per_kg'] > 0
    assert data['valuation']['estimated_payout_range_inr'][0] > 0
    assert data['valuation']['carbon_offset_kg'] > 0