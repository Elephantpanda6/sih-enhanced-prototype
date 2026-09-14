from pydantic import BaseModel, Field, ConfigDict
from typing import Optional, List
from datetime import datetime

class TransactionCreate(BaseModel):
    material_code: str = Field(..., examples=['iron_hms_heavy'])
    weight_kg: float = Field(..., gt=0, examples=[25.0])
    purity_factor: Optional[float] = Field(1.0, ge=0.1, le=1.0)
    pickup_address: Optional[str] = Field(None, examples=['Shop 4, Gandhi Chowk, Sector 12'])
    pickup_latitude: Optional[float] = None
    pickup_longitude: Optional[float] = None
    notes: Optional[str] = None
    voice_input_transcript: Optional[str] = None

class TransactionUpdate(BaseModel):
    status: Optional[str] = Field(None, examples=['accepted'])
    buyer_id: Optional[int] = None
    final_agreed_price: Optional[float] = None
    notes: Optional[str] = None

class TransactionOut(BaseModel):
    id: int
    dealer_id: int
    buyer_id: Optional[int]
    material_code: str
    material_name: str
    weight_kg: float
    purity_factor: float
    calculated_price: float
    final_agreed_price: Optional[float]
    status: str
    pickup_address: Optional[str]
    created_at: datetime
    updated_at: datetime
    model_config = ConfigDict(from_attributes=True)

class TransactionBatchCreate(BaseModel):
    items: List[TransactionCreate]
    receipt_hash: Optional[str] = None
    buyer_id: Optional[int] = None