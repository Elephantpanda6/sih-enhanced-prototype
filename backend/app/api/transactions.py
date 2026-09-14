from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app.models.transaction import ScrapTransaction
from app.models.category import MaterialSubCategory
from app.models.user import User
from app.schemas.transaction import TransactionCreate, TransactionUpdate, TransactionOut, TransactionBatchCreate
from app.api.deps import get_current_user

router = APIRouter(prefix='/transactions', tags=['Pickup & Transactions'])

@router.post('/', response_model=TransactionOut, status_code=status.HTTP_201_CREATED)
def create_pickup_order(tx_in: TransactionCreate, current_user: User=Depends(get_current_user), db: Session=Depends(get_db)):
    subcat = db.query(MaterialSubCategory).filter(MaterialSubCategory.code == tx_in.material_code).first()
    if subcat:
        rate = subcat.current_spot_rate
        material_name = subcat.name
        purity = tx_in.purity_factor or subcat.default_purity
    else:
        rate = 35.0
        material_name = tx_in.material_code.replace('_', ' ').title()
        purity = tx_in.purity_factor or 0.9
    calculated_price = round(tx_in.weight_kg * rate * purity * 0.85, 2)
    tx = ScrapTransaction(
        dealer_id=current_user.id,
        material_code=tx_in.material_code,
        material_name=material_name,
        weight_kg=tx_in.weight_kg,
        purity_factor=purity,
        calculated_price=calculated_price,
        pickup_address=tx_in.pickup_address,
        pickup_latitude=tx_in.pickup_latitude,
        pickup_longitude=tx_in.pickup_longitude,
        notes=tx_in.notes,
        voice_input_transcript=tx_in.voice_input_transcript,
        status='pending'
    )
    db.add(tx)
    db.commit()
    db.refresh(tx)
    return tx

@router.post('/submit', status_code=status.HTTP_201_CREATED)
def submit_offline_transaction(payload: dict, db: Session=Depends(get_db)):
    mat_code = payload.get('material_code', 'copper_bare_bright')
    subcat = db.query(MaterialSubCategory).filter(MaterialSubCategory.code == mat_code).first()
    mat_name = subcat.name if subcat else mat_code.replace('_', ' ').title()
    calc_price = float(payload.get('calculated_price', 0.0))
    tx = ScrapTransaction(
        dealer_id=1,
        material_code=mat_code,
        material_name=mat_name,
        weight_kg=float(payload.get('weight_kg', 1.0)),
        purity_factor=float(payload.get('purity_factor', 1.0)),
        calculated_price=calc_price,
        pickup_latitude=payload.get('pickup_latitude'),
        pickup_longitude=payload.get('pickup_longitude'),
        notes=payload.get('receipt_hash'),
        voice_input_transcript=payload.get('voice_input_transcript'),
        status='accepted'
    )
    db.add(tx)
    db.commit()
    db.refresh(tx)
    return {'success': True, 'id': tx.id, 'transaction_id': tx.id, 'status': 'accepted', 'receipt_hash': payload.get('receipt_hash'), 'message': 'Transaction verified and logged'}

@router.get('/', response_model=List[TransactionOut])
def list_transactions(current_user: User=Depends(get_current_user), db: Session=Depends(get_db)):
    if current_user.role in ['buyer', 'admin']:
        return db.query(ScrapTransaction).all()
    return db.query(ScrapTransaction).filter(ScrapTransaction.dealer_id == current_user.id).all()

@router.get('/{tx_id}', response_model=TransactionOut)
def get_transaction(tx_id: int, current_user: User=Depends(get_current_user), db: Session=Depends(get_db)):
    tx = db.query(ScrapTransaction).filter(ScrapTransaction.id == tx_id).first()
    if not tx:
        raise HTTPException(status_code=404, detail='Transaction not found')
    return tx

@router.put('/{tx_id}/status', response_model=TransactionOut)
def update_transaction_status(tx_id: int, update_in: TransactionUpdate, current_user: User=Depends(get_current_user), db: Session=Depends(get_db)):
    tx = db.query(ScrapTransaction).filter(ScrapTransaction.id == tx_id).first()
    if not tx:
        raise HTTPException(status_code=404, detail='Transaction not found')
    if update_in.status:
        tx.status = update_in.status
    if update_in.buyer_id:
        tx.buyer_id = update_in.buyer_id
    elif current_user.role == 'buyer':
        tx.buyer_id = current_user.id
    if update_in.final_agreed_price is not None:
        tx.final_agreed_price = update_in.final_agreed_price
    if update_in.notes:
        tx.notes = update_in.notes
    db.commit()
    db.refresh(tx)
    return tx

@router.post('/batch_submit', status_code=status.HTTP_201_CREATED)
def submit_batch_transactions(payload: TransactionBatchCreate, db: Session=Depends(get_db)):
    transactions = []
    total_price = 0.0
    for tx_in in payload.items:
        subcat = db.query(MaterialSubCategory).filter(MaterialSubCategory.code == tx_in.material_code).first()
        if subcat:
            rate = subcat.current_spot_rate
            material_name = subcat.name
            purity = tx_in.purity_factor or subcat.default_purity
        else:
            rate = 35.0
            material_name = tx_in.material_code.replace('_', ' ').title()
            purity = tx_in.purity_factor or 0.9

        calculated_price = round(tx_in.weight_kg * rate * purity * 0.85, 2)
        total_price += calculated_price

        tx = ScrapTransaction(
            dealer_id=1,
            buyer_id=payload.buyer_id,
            material_code=tx_in.material_code,
            material_name=material_name,
            weight_kg=tx_in.weight_kg,
            purity_factor=purity,
            calculated_price=calculated_price,
            pickup_address=tx_in.pickup_address,
            pickup_latitude=tx_in.pickup_latitude,
            pickup_longitude=tx_in.pickup_longitude,
            notes=payload.receipt_hash or tx_in.notes,
            voice_input_transcript=tx_in.voice_input_transcript,
            status='accepted'
        )
        db.add(tx)
        transactions.append(tx)

    db.commit()
    return {
        'success': True,
        'count': len(transactions),
        'total_price': total_price,
        'receipt_hash': payload.receipt_hash,
        'transaction_ids': [t.id for t in transactions]
    }