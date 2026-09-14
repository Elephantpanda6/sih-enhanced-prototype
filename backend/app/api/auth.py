from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import User
from app.schemas.user import UserCreate, UserLogin, UserOut, Token
from app.services.auth_service import get_password_hash, verify_password, create_access_token
from app.api.deps import get_current_user
router = APIRouter(prefix='/auth', tags=['Authentication'])

@router.post('/register', response_model=UserOut, status_code=status.HTTP_201_CREATED)
def register(user_in: UserCreate, db: Session=Depends(get_db)):
    existing = db.query(User).filter(User.phone_number == user_in.phone_number).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail='Phone number already registered')
    user = User(phone_number=user_in.phone_number, full_name=user_in.full_name, hashed_password=get_password_hash(user_in.password), role=user_in.role, preferred_language=user_in.preferred_language)
    db.add(user)
    db.commit()
    db.refresh(user)
    return user

@router.post('/login', response_model=Token)
def login(form_data: OAuth2PasswordRequestForm=Depends(), db: Session=Depends(get_db)):
    user = db.query(User).filter(User.phone_number == form_data.username).first()
    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail='Incorrect phone number or password/PIN')
    access_token = create_access_token(data={'sub': user.phone_number, 'role': user.role})
    return {'access_token': access_token, 'token_type': 'bearer', 'user': user}

@router.post('/login-json', response_model=Token)
def login_json(credentials: UserLogin, db: Session=Depends(get_db)):
    user = db.query(User).filter(User.phone_number == credentials.phone_number).first()
    if not user or not verify_password(credentials.password, user.hashed_password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail='Incorrect phone number or password/PIN')
    access_token = create_access_token(data={'sub': user.phone_number, 'role': user.role})
    return {'access_token': access_token, 'token_type': 'bearer', 'user': user}

@router.get('/me', response_model=UserOut)
def read_current_user(current_user: User=Depends(get_current_user)):
    return current_user