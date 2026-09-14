import logging
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from starlette.exceptions import HTTPException as StarletteHTTPException
from app.config import settings, logger
from app.database import engine, Base
from app.api import api_router
Base.metadata.create_all(bind=engine)
app = FastAPI(title=settings.PROJECT_NAME, version=settings.VERSION, description='Enterprise Production Backend API for SIH Smart Scrap & E-Waste Valuation, Exclusive Hindi & Marathi Voice-to-Command Engine, CPCB/SPCB Recycler Geospatial Locator, and Offline CPU-Optimized Computer Vision Grading Pipeline.', docs_url='/docs', redoc_url='/redoc')
import os
import firebase_admin
from firebase_admin import credentials

try:
    if os.path.exists(settings.FIREBASE_CREDENTIALS):
        cred = credentials.Certificate(settings.FIREBASE_CREDENTIALS)
        firebase_admin.initialize_app(cred)
    else:
        firebase_admin.initialize_app()
except Exception as e:
    logger.warning(f"Firebase initialization skipped or failed: {e}")

app.add_middleware(CORSMiddleware, allow_origins=['*'], allow_credentials=True, allow_methods=['*'], allow_headers=['*'])

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    error_details = []
    for err in exc.errors():
        field = ' -> '.join((str(loc) for loc in err.get('loc', [])))
        error_details.append({'field': field, 'message': err.get('msg'), 'type': err.get('type')})
    logger.warning(f'Validation failure on {request.method} {request.url.path}: {error_details}')
    return JSONResponse(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, content={'detail': 'Request payload validation failed', 'success': False, 'error': {'code': 'VALIDATION_ERROR', 'message': 'One or more fields failed validation constraints', 'details': error_details}})

@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    logger.info(f'HTTP {exc.status_code} on {request.method} {request.url.path}: {exc.detail}')
    return JSONResponse(status_code=exc.status_code, content={'detail': exc.detail, 'success': False, 'error': {'code': f'HTTP_{exc.status_code}', 'message': str(exc.detail)}})

@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    logger.error(f'Unhandled server exception on {request.method} {request.url.path}: {exc}', exc_info=True)
    return JSONResponse(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, content={'detail': 'Internal server error occurred. Please consult backend logs.', 'success': False, 'error': {'code': 'INTERNAL_SERVER_ERROR', 'message': str(exc)}})
app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get('/', tags=['Health & Status'])
def root():
    return {'status': 'online', 'service': settings.PROJECT_NAME, 'version': settings.VERSION, 'documentation': '/docs', 'supported_languages': ['hi', 'mr'], 'compliance': 'CPCB & SPCB Hazardous & E-Waste Management Rules 2022'}