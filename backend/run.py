import sys
import uvicorn
from app.config import settings
from app.database import engine, Base
from app.models.category import MaterialCategory, MaterialSubCategory
from app.models.recycler import AuthorizedRecycler
from sqlalchemy.orm import Session

def preflight_check():
    Base.metadata.create_all(bind=engine)
    with Session(bind=engine) as session:
        cat_count = session.query(MaterialCategory).count()
        subcat_count = session.query(MaterialSubCategory).count()
        recycler_count = session.query(AuthorizedRecycler).count()
    print('\n' + '=' * 70)
    print(' SIH SMART SCRAP & E-WASTE DYNAMIC PRICING ENGINE - BACKEND SERVICE')
    print('=' * 70)
    print(f' [OK] Version: {settings.VERSION}')
    print(f' [OK] Database: {settings.DATABASE_URL}')
    print(f' [OK] Active Categories: {cat_count} | Subcategories: {subcat_count}')
    print(f' [OK] CPCB/SPCB Registered Recyclers: {recycler_count}')
    print(f' [OK] Exclusive Vernacular Languages: Hindi (hi), Marathi (mr)')
    print(f' [OK] Interactive Swagger API Docs: http://localhost:8000/docs')
    print(f' [OK] Alternative ReDoc API Docs:   http://localhost:8000/redoc')
    print('=' * 70 + '\n')
if __name__ == '__main__':
    preflight_check()
    uvicorn.run('app.main:app', host='0.0.0.0', port=8000, reload=True)