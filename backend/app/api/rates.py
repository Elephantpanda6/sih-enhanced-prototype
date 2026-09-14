from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from typing import List, Optional
from app.database import get_db
from app.models.category import MaterialCategory, MaterialSubCategory
from app.schemas.category import CategoryOut, SubCategoryOut
from app.api.deps import get_current_buyer
from app.models.user import User
router = APIRouter(prefix='/rates', tags=['Hierarchical Scrap Rates'])

@router.get('/categories', response_model=List[CategoryOut])
def get_hierarchical_categories(db: Session=Depends(get_db)):
    return db.query(MaterialCategory).all()

@router.get('/subcategories', response_model=List[SubCategoryOut])
def get_all_subcategories(category_code: Optional[str]=Query(None, examples=['non_ferrous']), db: Session=Depends(get_db)):
    query = db.query(MaterialSubCategory)
    if category_code:
        cat = db.query(MaterialCategory).filter(MaterialCategory.code == category_code).first()
        if cat:
            query = query.filter(MaterialSubCategory.category_id == cat.id)
    return query.all()

@router.get('/live', response_model=List[SubCategoryOut])
def get_live_market_rates(db: Session=Depends(get_db)):
    return db.query(MaterialSubCategory).all()

@router.get('/', response_model=List[SubCategoryOut])
def get_all_market_rates(db: Session=Depends(get_db)):
    return db.query(MaterialSubCategory).all()

@router.get('/{code}', response_model=SubCategoryOut)
def get_rate_by_code(code: str, db: Session=Depends(get_db)):
    subcat = db.query(MaterialSubCategory).filter(MaterialSubCategory.code == code).first()
    if not subcat:
        raise HTTPException(status_code=404, detail='Material subcategory not found')
    return subcat

@router.put('/{code}', response_model=SubCategoryOut)
def update_spot_rate(code: str, new_spot_rate_per_kg: float=Query(..., gt=0), new_purity: Optional[float]=Query(None, ge=0.1, le=1.0), current_buyer: User=Depends(get_current_buyer), db: Session=Depends(get_db)):
    subcat = db.query(MaterialSubCategory).filter(MaterialSubCategory.code == code).first()
    if not subcat:
        raise HTTPException(status_code=404, detail='Material subcategory not found')
    subcat.current_spot_rate = new_spot_rate_per_kg
    if new_purity is not None:
        subcat.default_purity = new_purity
    db.commit()
    db.refresh(subcat)
    return subcat