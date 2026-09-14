from pydantic import BaseModel, Field, ConfigDict
from typing import Optional
from datetime import datetime

class UserBase(BaseModel):
    phone_number: str = Field(..., examples=['9876543210'])
    full_name: Optional[str] = Field(None, examples=['Ramesh Kumar'])
    role: str = Field('scrap_dealer', examples=['scrap_dealer'])
    preferred_language: str = Field('hi', examples=['hi'])

class UserCreate(UserBase):
    password: str = Field(..., min_length=4, examples=['1234'])

class UserLogin(BaseModel):
    phone_number: str = Field(..., examples=['9876543210'])
    password: str = Field(..., examples=['1234'])

class UserOut(UserBase):
    id: int
    is_active: bool
    created_at: datetime
    model_config = ConfigDict(from_attributes=True)

class Token(BaseModel):
    access_token: str
    token_type: str = 'bearer'
    user: UserOut

class TokenData(BaseModel):
    phone_number: Optional[str] = None
    role: Optional[str] = None