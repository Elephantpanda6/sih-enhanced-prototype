import pytest

def test_nearest_recycler_with_lat_lon_params(client):
    res = client.get('/api/v1/recyclers/nearest?lat=19.0178&lon=72.8478&category=e_waste&max_radius_km=100')
    assert res.status_code == 200
    data = res.json()
    assert data['total_found'] > 0
    top = data['nearby_recyclers'][0]
    assert top['distance_km'] < 50.0
    assert 'google.com/maps' in top['google_maps_directions_url']
    assert 'E-Waste' in top['accepted_materials_summary']
    assert 'अधिकृत' in data['voice_navigation_instruction_mr']
    assert 'अधिकृत' in data['voice_navigation_instruction_hi']

def test_nearest_recycler_with_latitude_longitude_params(client):
    res = client.get('/api/v1/recyclers/nearest?latitude=18.5308&longitude=73.8475&category=e_waste&max_radius_km=60')
    assert res.status_code == 200
    data = res.json()
    assert data['total_found'] > 0
    top = data['nearby_recyclers'][0]
    assert 'Pune' in top['recycler']['district'] or 'Chakan' in top['recycler']['address']
    assert top['estimated_driving_time_mins'] > 0

def test_missing_coordinates_fails(client):
    res = client.get('/api/v1/recyclers/nearest?category=e_waste')
    assert res.status_code == 400
    assert 'missing coordinate' in res.json()['detail'].lower()

def test_cpcb_registry_filter_by_state(client):
    res = client.get('/api/v1/recyclers/?state=Maharashtra')
    assert res.status_code == 200
    recyclers = res.json()
    assert len(recyclers) >= 2
    for r in recyclers:
        assert 'Maharashtra' in r['state']
        assert r['is_active_license'] is True
        assert r['regulatory_board'] in ['MPCB', 'CPCB']

def test_cpcb_registry_filter_by_board(client):
    res = client.get('/api/v1/recyclers/?regulatory_board=MPCB')
    assert res.status_code == 200
    recyclers = res.json()
    assert len(recyclers) >= 2
    for r in recyclers:
        assert r['regulatory_board'] == 'MPCB'