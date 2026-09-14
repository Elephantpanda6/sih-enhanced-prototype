from fastapi import APIRouter
from app.api.auth import router as auth_router
from app.api.voice import router as voice_router
from app.api.vision import router as vision_router
from app.api.pricing import router as pricing_router
from app.api.rates import router as rates_router
from app.api.recyclers import router as recyclers_router
from app.api.transactions import router as transactions_router
from app.api.security import router as security_router
from app.api.facilities import router as facilities_router
api_router = APIRouter()
api_router.include_router(auth_router)
api_router.include_router(voice_router)
api_router.include_router(vision_router)
api_router.include_router(pricing_router)
api_router.include_router(rates_router)
api_router.include_router(recyclers_router)
api_router.include_router(transactions_router)
api_router.include_router(security_router)
api_router.include_router(facilities_router)