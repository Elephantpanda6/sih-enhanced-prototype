import io
import pytest
from app.services.voice_service import VoiceService
from app.schemas.voice import SupportedLanguage

def test_marathi_voice_price_inquiry(client):
    phrase = 'don kilo tamba aani paach kilo lokhand bhav sanga'
    res = client.post('/api/v1/voice/parse', json={'transcript': phrase, 'language': 'mr'})
    assert res.status_code == 200
    data = res.json()
    assert data['detected_language'] == 'marathi'
    assert data['detected_intent'] == 'price_inquiry'
    assert len(data['detected_items']) == 2
    items_by_code = {it['subcategory_code']: it for it in data['detected_items']}
    assert 'copper_bare_bright' in items_by_code
    assert 'iron_hms_heavy' in items_by_code
    assert items_by_code['copper_bare_bright']['weight_kg'] == 2.0
    assert items_by_code['iron_hms_heavy']['weight_kg'] == 5.0
    assert data['total_estimated_price_inr'] > 1000.0
    assert '₹' in data['feedback_audio_text_mr']
    assert data['feedback_audio_base64'].startswith('data:audio/wav;base64,')

def test_marathi_vernacular_number_parsing(client):
    test_cases = [('ardha kilo tamba bhav sanga', 'copper_bare_bright', 0.5), ('ek kilo pital dar sanga', 'brass_honey', 1.0), ('don kilo lokhand mulya sanga', 'iron_hms_heavy', 2.0), ('paach kilo tambe', 'copper_bare_bright', 5.0), ('daha kilo raddi gatta', 'paper_corrugated_carton', 10.0), ('pandhra kilo raddi akhbar', 'paper_old_newspaper', 15.0), ('vis kilo sariya wajan dakhva', 'iron_hms_heavy', 20.0), ('adhich kilo pital mulya sanga', 'brass_honey', 2.5), ('dedh kilo tamba', 'copper_bare_bright', 1.5)]
    for transcript, expected_code, expected_weight in test_cases:
        res = client.post('/api/v1/voice/parse', json={'transcript': transcript, 'language': 'mr'})
        assert res.status_code == 200, f'Failed for {transcript}'
        data = res.json()
        assert len(data['detected_items']) >= 1, f'No items detected for: {transcript}'
        item = data['detected_items'][0]
        assert item['subcategory_code'] == expected_code, f'Mismatch code for {transcript}'
        assert item['weight_kg'] == expected_weight, f"Mismatch weight for {transcript}: got {item['weight_kg']}"

def test_hindi_voice_pickup_request_and_numbers(client):
    phrase = 'das kilo gatta aur teen kilo loha pickup bhejo'
    res = client.post('/api/v1/voice/parse', json={'transcript': phrase, 'language': 'hi'})
    assert res.status_code == 200
    data = res.json()
    assert data['detected_language'] == 'hindi'
    assert data['detected_intent'] == 'pickup_request'
    assert len(data['detected_items']) == 2
    assert 'पिकअप दर्ज' in data['feedback_audio_text_hi']

def test_regional_slang_and_dialect_terms(client):
    slang_phrases = [('bhangar lohand dha kilo', 'iron_hms_heavy'), ('chokha tamba paon kilo', 'copper_bare_bright'), ('patryacha dabba chaar kilo', 'iron_light_sheet'), ('plastic batlya teen kilo', 'plastic_pet_bottles'), ('peetal bartan dhai kilo', 'brass_honey')]
    for phrase, expected_code in slang_phrases:
        res = client.post('/api/v1/voice/parse', json={'transcript': phrase})
        assert res.status_code == 200
        data = res.json()
        assert len(data['detected_items']) >= 1
        assert data['detected_items'][0]['subcategory_code'] == expected_code

def test_command_intent_detection(client):
    res_price = client.post('/api/v1/voice/parse', json={'transcript': 'tambe mulya sanga'})
    assert res_price.json()['detected_intent'] == 'price_inquiry'
    res_pickup = client.post('/api/v1/voice/parse', json={'transcript': 'tempo pathva bhangar ghyaun ja'})
    assert res_pickup.json()['detected_intent'] == 'pickup_request'
    res_weight = client.post('/api/v1/voice/parse', json={'transcript': 'kiti kilo ahe wajan dakhva'})
    assert res_weight.json()['detected_intent'] == 'weight_query'

def test_devanagari_voice_recognition(client):
    phrase = 'दोन किलो तांबे आणि पाच किलो लोखंड भाव सांगा'
    res = client.post('/api/v1/voice/parse', json={'transcript': phrase, 'language': 'mr'})
    assert res.status_code == 200
    data = res.json()
    assert data['detected_intent'] == 'price_inquiry'
    assert len(data['detected_items']) == 2
    items = {it['subcategory_code']: it['weight_kg'] for it in data['detected_items']}
    assert items['copper_bare_bright'] == 2.0
    assert items['iron_hms_heavy'] == 5.0

def test_offline_audio_file_decoding(client):
    sample_wav = VoiceService.synthesize_vernacular_speech('दोन किलो तांबे', SupportedLanguage.MARATHI)
    res = client.post('/api/v1/voice/decode-audio?preferred_language=mr', files={'file': ('scrap_command_mr.wav', sample_wav, 'audio/wav')})
    assert res.status_code == 200
    data = res.json()
    assert 'decoded_transcript' in data
    assert 'parse_result' in data
    assert data['parse_result']['total_estimated_price_inr'] > 0
    assert 'Offline CPU Acoustic Recognizer' in data['offline_engine_used']

def test_vernacular_tts_streaming(client):
    text = 'दोन किलो तांब्याचे अंदाजे मूल्य ₹१३९० होईल.'
    res = client.get(f'/api/v1/voice/tts?text={text}&language=mr')
    assert res.status_code == 200
    assert res.headers['content-type'] == 'audio/wav'
    assert len(res.content) > 1000
    assert res.content[:4] == b'RIFF'
    assert res.content[8:12] == b'WAVE'