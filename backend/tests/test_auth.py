import pytest
from fastapi.testclient import TestClient
from app.main import app

def test_user_registration_and_login(client):
    phone = '9988776655'
    pin = '4321'
    reg_res = client.post('/api/v1/auth/register', json={'phone_number': phone, 'full_name': 'Tukaram Shinde', 'password': pin, 'role': 'scrap_dealer', 'preferred_language': 'mr'})
    assert reg_res.status_code == 201
    user_data = reg_res.json()
    assert user_data['phone_number'] == phone
    assert user_data['preferred_language'] == 'mr'
    assert user_data['role'] == 'scrap_dealer'
    login_res = client.post('/api/v1/auth/login-json', json={'phone_number': phone, 'password': pin})
    assert login_res.status_code == 200
    token_data = login_res.json()
    assert 'access_token' in token_data
    token = token_data['access_token']
    headers = {'Authorization': f'Bearer {token}'}
    me_res = client.get('/api/v1/auth/me', headers=headers)
    assert me_res.status_code == 200
    assert me_res.json()['phone_number'] == phone

def test_duplicate_registration_fails(client):
    res = client.post('/api/v1/auth/register', json={'phone_number': '9876543210', 'password': '1234', 'role': 'scrap_dealer'})
    assert res.status_code == 400
    assert 'already registered' in res.json()['detail'].lower()

def test_invalid_login_credentials(client):
    res = client.post('/api/v1/auth/login-json', json={'phone_number': '9876543210', 'password': 'wrongpassword'})
    assert res.status_code == 401
    assert 'incorrect' in res.json()['detail'].lower()

def test_role_buyer_privilege_enforcement(client, dealer_token, buyer_token):
    res_dealer = client.put('/api/v1/rates/copper_bare_bright?new_spot_rate_per_kg=710.0', headers={'Authorization': f'Bearer {dealer_token}'})
    assert res_dealer.status_code == 403
    assert 'requires recycler / buyer privileges' in res_dealer.json()['detail'].lower()
    res_buyer = client.put('/api/v1/rates/copper_bare_bright?new_spot_rate_per_kg=710.0', headers={'Authorization': f'Bearer {buyer_token}'})
    assert res_buyer.status_code == 200
    assert res_buyer.json()['current_spot_rate'] == 710.0