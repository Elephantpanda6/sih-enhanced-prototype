from fastapi import APIRouter, UploadFile, File, HTTPException, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.schemas.vision import OfflineVisionAnalysisResponse, MultimodalVisionAnalysisResponse
from app.services.vision_service import VisionService

router = APIRouter(prefix='/vision', tags=['E-Waste Multimodal Vision & Condition Assessment'])

@router.post('/analyze-image', response_model=OfflineVisionAnalysisResponse)
async def analyze_scrap_image_offline(file: UploadFile=File(...)):
    if not file.content_type or not file.content_type.startswith('image/'):
        raise HTTPException(status_code=400, detail='Uploaded file must be a valid JPEG/PNG image')
    image_bytes = await file.read()
    return VisionService.analyze_scrap_image(image_bytes, file.filename or 'captured_scrap.jpg')

@router.post('/analyze-multimodal', response_model=MultimodalVisionAnalysisResponse)
async def analyze_scrap_image_multimodal(file: UploadFile=File(...), db: Session=Depends(get_db)):
    if not file.content_type or not file.content_type.startswith('image/'):
        raise HTTPException(status_code=400, detail='Uploaded file must be a valid JPEG/PNG image')
    image_bytes = await file.read()
    return await VisionService.analyze_scrap_multimodal(image_bytes, file.filename or 'scrap_capture.jpg', db=db)