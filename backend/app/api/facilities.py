from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from typing import Optional
from app.database import get_db
from app.schemas.recycler import NearestRecyclersResponse
from app.services.geo_service import GeoService

router = APIRouter(prefix='/facilities', tags=['Recycling Facilities Geospatial Locator'])

@router.get('/nearby', response_model=NearestRecyclersResponse)
def get_nearby_recycling_facilities(lat: Optional[float]=Query(18.5204), lon: Optional[float]=Query(73.8567), radius_km: float=Query(100.0), limit: int=Query(10), db: Session=Depends(get_db)):
    return GeoService.find_nearest_recyclers(db=db, user_lat=lat, user_lon=lon, category_filter=None, max_radius_km=radius_km, limit=limit)
