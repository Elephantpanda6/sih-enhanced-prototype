import pytest
from datetime import datetime, timedelta, timezone
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool
from fastapi.testclient import TestClient
from app.main import app
from app.database import Base, get_db
from app.models.category import MaterialCategory, MaterialSubCategory
from app.models.recycler import AuthorizedRecycler
from app.models.user import User
from app.services.auth_service import get_password_hash, create_access_token
TEST_DATABASE_URL = 'sqlite:///:memory:'
engine_test = create_engine(TEST_DATABASE_URL, connect_args={'check_same_thread': False}, poolclass=StaticPool)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine_test)

def seed_test_database(db):
    utc_now = datetime.now(timezone.utc)
    cat_nf = MaterialCategory(code='non_ferrous', name='Non-Ferrous Metals', name_hi='अलौह धातुएं', name_mr='अलोह धातू', description='Non-magnetic metals')
    db.add(cat_nf)
    db.flush()
    subcat_copper = MaterialSubCategory(category_id=cat_nf.id, code='copper_bare_bright', name='Copper Bare Bright Wire', name_hi='शुद्ध तांबा (बेयर ब्राइट तार)', name_mr='शुद्ध तांब्याची ताार', standard_density_kg_m3=8960.0, default_purity=0.99, current_spot_rate=695.0, volatility_min_rate=660.0, volatility_max_rate=740.0, degradation_factor_rust=0.05, oxidation_multiplier=1.0, contamination_factor=0.04, precious_metal_yield_gold_g_tonne=0.0, precious_metal_yield_silver_g_tonne=0.0, precious_metal_yield_palladium_g_tonne=0.0, hsn_code='740400')
    subcat_brass = MaterialSubCategory(category_id=cat_nf.id, code='brass_honey', name='Brass Honey Scrap', name_hi='पीतल स्क्रैप (हनी ब्रास)', name_mr='पिवळे पितळ स्क्रॅप', standard_density_kg_m3=8500.0, default_purity=0.95, current_spot_rate=465.0, volatility_min_rate=430.0, volatility_max_rate=490.0, degradation_factor_rust=0.05, oxidation_multiplier=1.0, contamination_factor=0.05, hsn_code='740400')
    db.add_all([subcat_copper, subcat_brass])
    cat_fe = MaterialCategory(code='ferrous', name='Ferrous Metals', name_hi='लौह धातुएं', name_mr='लोह धातू', description='Secondary steel scrap')
    db.add(cat_fe)
    db.flush()
    subcat_iron = MaterialSubCategory(category_id=cat_fe.id, code='iron_hms_heavy', name='Heavy Melting Steel (HMS / Sariya)', name_hi='भारी लोहा / सरिया', name_mr='जाड लोखंड / सळई', standard_density_kg_m3=7850.0, default_purity=0.95, current_spot_rate=39.5, volatility_min_rate=34.0, volatility_max_rate=44.0, degradation_factor_rust=0.2, oxidation_multiplier=1.25, contamination_factor=0.1, hsn_code='720449')
    db.add(subcat_iron)
    cat_ew = MaterialCategory(code='e_waste', name='E-Waste / WEEE', name_hi='ई-कचरा व इलेक्ट्रॉनिक्स', name_mr='ई-कचरा व इलेक्ट्रॉनिक्स', description='Regulated electronic scrap')
    db.add(cat_ew)
    db.flush()
    subcat_pcb = MaterialSubCategory(category_id=cat_ew.id, code='e_waste_pcb_high', name='High-Grade Telecom / Server PCB', name_hi='उच्च-गुणवत्ता सर्वर पीसीबी', name_mr='हाय-ग्रेड सर्व्हर पीसीबी', standard_density_kg_m3=1850.0, default_purity=0.95, current_spot_rate=320.0, volatility_min_rate=280.0, volatility_max_rate=420.0, degradation_factor_rust=0.02, oxidation_multiplier=1.0, contamination_factor=0.05, precious_metal_yield_gold_g_tonne=180.0, precious_metal_yield_silver_g_tonne=650.0, precious_metal_yield_palladium_g_tonne=35.0, hsn_code='854911')
    db.add(subcat_pcb)
    cat_pa = MaterialCategory(code='paper_cardboard', name='Paper & Cardboard', name_hi='कागज व रद्दी गत्ता', name_mr='कागद व पुठ्ठा')
    db.add(cat_pa)
    db.flush()
    subcat_carton = MaterialSubCategory(category_id=cat_pa.id, code='paper_corrugated_carton', name='Corrugated Cardboard (Gatta / Khoka)', name_hi='कार्टन गत्ता', name_mr='पुठ्ठ्याचे खोके', standard_density_kg_m3=180.0, default_purity=0.9, current_spot_rate=14.5, volatility_min_rate=11.0, volatility_max_rate=17.0, degradation_factor_rust=0.0, hsn_code='470710')
    subcat_news = MaterialSubCategory(category_id=cat_pa.id, code='paper_old_newspaper', name='Old Newspaper (Raddi Akhbar)', name_hi='पुराना अखबार (रद्दी)', name_mr='जुने वर्तमानपत्र (रद्दी)', standard_density_kg_m3=220.0, default_purity=0.95, current_spot_rate=18.0, volatility_min_rate=15.0, volatility_max_rate=21.0, degradation_factor_rust=0.0, hsn_code='470790')
    db.add_all([subcat_carton, subcat_news])
    recycler_mumbai = AuthorizedRecycler(name='Eco Recycling Limited (Ecoreco)', authorization_number='CPCB/EW/MH/2021/001', regulatory_board='MPCB', statutory_rule='E-Waste (Management) Rules, 2022', state='Maharashtra', district='Mumbai Suburban', address='Plot No. 422, MIDC Industrial Area, Mahape, Navi Mumbai', pin_code='400710', latitude=19.1128, longitude=73.0112, contact_person='B. K. Soni', contact_phone='+91-22-40052951', contact_email='compliance@ecoreco.com', annual_capacity_metric_tonnes=7200.0, accepted_category_codes='e_waste,battery_hazmat,non_ferrous', is_active_license=True, license_valid_until=utc_now + timedelta(days=730))
    recycler_pune = AuthorizedRecycler(name='Green Enviro Recycling Hub', authorization_number='MPCB/RO-PUNE/E-WASTE/2022/104', regulatory_board='MPCB', statutory_rule='E-Waste (Management) Rules, 2022', state='Maharashtra', district='Pune', address='Gat No. 1582, Chakan Industrial Phase II, Taluka Khed, Pune', pin_code='410501', latitude=18.758, longitude=73.856, contact_person='Sanjay Deshmukh', contact_phone='+91-20-67184200', contact_email='ops@greenenviropune.org', annual_capacity_metric_tonnes=5400.0, accepted_category_codes='e_waste,battery_hazmat,ferrous,non_ferrous', is_active_license=True, license_valid_until=utc_now + timedelta(days=900))
    recycler_delhi = AuthorizedRecycler(name='Attero Recycling Private Limited', authorization_number='CPCB/REG/EW/UP/2020/003', regulatory_board='CPCB', statutory_rule='E-Waste (Management) Rules, 2022', state='Uttar Pradesh (Delhi-NCR)', district='Gautam Buddha Nagar', address='Greater Noida Industrial Hub', pin_code='201301', latitude=28.5355, longitude=77.391, contact_person='Rohan Gupta', contact_phone='+91-120-4088000', annual_capacity_metric_tonnes=19500.0, accepted_category_codes='e_waste,battery_hazmat,non_ferrous', is_active_license=True, license_valid_until=utc_now + timedelta(days=1100))
    db.add_all([recycler_mumbai, recycler_pune, recycler_delhi])
    user_seeded = User(phone_number='9876543210', full_name='Ramesh Patil (स्थानिक भंगार व्यावसायिक)', hashed_password=get_password_hash('1234'), role='scrap_dealer', preferred_language='mr')
    buyer_seeded = User(phone_number='9123456780', full_name='EcoReco Western India Smelter Hub', hashed_password=get_password_hash('1234'), role='buyer', preferred_language='en')
    db.add_all([user_seeded, buyer_seeded])
    db.commit()

@pytest.fixture(scope='session', autouse=True)
def setup_test_db():
    Base.metadata.create_all(bind=engine_test)
    db = TestingSessionLocal()
    seed_test_database(db)
    db.close()

    def override_get_db():
        session = TestingSessionLocal()
        try:
            yield session
        finally:
            session.close()
    app.dependency_overrides[get_db] = override_get_db
    yield
    app.dependency_overrides.clear()
    Base.metadata.drop_all(bind=engine_test)

@pytest.fixture
def client():
    return TestClient(app)

@pytest.fixture
def dealer_token():
    return create_access_token(data={'sub': '9876543210', 'role': 'scrap_dealer'})

@pytest.fixture
def buyer_token():
    return create_access_token(data={'sub': '9123456780', 'role': 'buyer'})