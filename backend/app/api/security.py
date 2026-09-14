from fastapi import APIRouter
import time

router = APIRouter(prefix='/security', tags=['Security & Emergency Duress SOS'])

@router.post('/duress-alert')
def trigger_duress_sos_alert(payload: dict):
    epoch = payload.get('epoch_millis', int(time.time() * 1000))
    code = payload.get('trigger_code', '911')
    return {
        'status': 'dispatched',
        'alert_id': f'SOS-ERSS-112-{code}-{epoch % 10000}',
        'decoy_mode_active': True,
        'timestamp': epoch
    }
