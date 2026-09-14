from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional

class RecyclerBase(BaseModel):
    name: str
    authorization_number: str
    regulatory_board: str = 'CPCB'
    statutory_rule: str
    state: str
    district: str
    address: str
    pin_code: str
    latitude: float
    longitude: float
    contact_person: Optional[str] = None
    contact_phone: str
    contact_email: Optional[str] = None
    annual_capacity_metric_tonnes: float
    accepted_category_codes: str
    is_active_license: bool = True

class RecyclerOut(RecyclerBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class NearestRecyclerQuery(BaseModel):
    latitude: float = Field(..., ge=-90.0, le=90.0, examples=[19.076])
    longitude: float = Field(..., ge=-180.0, le=180.0, examples=[72.8777])
    category_code: Optional[str] = Field(None, examples=['e_waste'])
    max_distance_km: float = Field(150.0, ge=1.0, le=1000.0, examples=[50.0])
    limit: int = Field(5, ge=1, le=20, examples=[5])

class NearestRecyclerItem(BaseModel):
    recycler: RecyclerOut
    distance_km: float
    estimated_driving_time_mins: int
    google_maps_directions_url: str
    accepted_materials_summary: List[str]

class NearestRecyclersResponse(BaseModel):
    origin_latitude: float
    origin_longitude: float
    queried_category: Optional[str]
    total_found: int
    nearby_recyclers: List[NearestRecyclerItem]
    voice_navigation_instruction_hi: str
    voice_navigation_instruction_mr: str