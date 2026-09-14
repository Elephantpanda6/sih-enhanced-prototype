import logging
from fastapi import APIRouter, Depends, Query, HTTPException, status
from sqlalchemy.orm import Session
from typing import List, Optional
from app.database import get_db
from app.models.recycler import AuthorizedRecycler
from app.schemas.recycler import RecyclerOut, NearestRecyclersResponse
from app.services.geo_service import GeoService
logger = logging.getLogger('sih.scrap.recyclers')
router = APIRouter(prefix='/recyclers', tags=['CPCB & SPCB Authorized Recyclers'])

@router.get('/nearest', response_model=NearestRecyclersResponse)
def get_nearest_authorized_recyclers(lat: Optional[float]=Query(None, ge=-90.0, le=90.0, description='Latitude (alias lat)', examples=[19.076]), lon: Optional[float]=Query(None, ge=-180.0, le=180.0, description='Longitude (alias lon)', examples=[72.8777]), latitude: Optional[float]=Query(None, ge=-90.0, le=90.0, description='Latitude', examples=[19.076]), longitude: Optional[float]=Query(None, ge=-180.0, le=180.0, description='Longitude', examples=[72.8777]), category: Optional[str]=Query(None, description='Filter by material: e_waste, battery_hazmat, non_ferrous, ferrous, plastics, paper_cardboard', examples=['e_waste']), max_radius_km: float=Query(150.0, ge=1.0, le=2000.0, examples=[50.0]), limit: int=Query(5, ge=1, le=50, examples=[5]), db: Session=Depends(get_db)):
    target_lat = lat if lat is not None else latitude
    target_lon = lon if lon is not None else longitude
    if target_lat is None or target_lon is None:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail='Missing coordinate parameters: provide either (lat, lon) or (latitude, longitude).')
    logger.info(f'Geospatial search request: lat={target_lat}, lon={target_lon}, category={category}, radius={max_radius_km}km')
    return GeoService.find_nearest_recyclers(db=db, user_lat=target_lat, user_lon=target_lon, category_filter=category, max_radius_km=max_radius_km, limit=limit)

@router.get('/', response_model=List[RecyclerOut])
def list_all_authorized_recyclers(state: Optional[str]=Query(None, examples=['Maharashtra']), category: Optional[str]=Query(None, examples=['e_waste']), regulatory_board: Optional[str]=Query(None, examples=['MPCB']), db: Session=Depends(get_db)):
    query = db.query(AuthorizedRecycler).filter(AuthorizedRecycler.is_active_license == True)
    if state:
        query = query.filter(AuthorizedRecycler.state.ilike(f'%{state}%'))
    if category:
        query = query.filter(AuthorizedRecycler.accepted_category_codes.ilike(f'%{category}%'))
    if regulatory_board:
        query = query.filter(AuthorizedRecycler.regulatory_board.ilike(f'%{regulatory_board}%'))
    return query.all()

@router.get('/{recycler_id}', response_model=RecyclerOut)
def get_recycler_by_id(recycler_id: int, db: Session=Depends(get_db)):
    recycler = db.query(AuthorizedRecycler).filter(AuthorizedRecycler.id == recycler_id).first()
    if not recycler:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail='Authorized Recycler record not found')
    return recycler