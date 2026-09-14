import math
from typing import List, Optional
from sqlalchemy.orm import Session
from app.models.recycler import AuthorizedRecycler
from app.schemas.recycler import NearestRecyclerItem, NearestRecyclersResponse, RecyclerOut

class GeoService:
    EARTH_RADIUS_KM = 6371.0
    AVERAGE_COMMERCIAL_SPEED_KMPH = 30.0
    CATEGORY_DISPLAY_MAP = {'e_waste': 'E-Waste', 'battery_hazmat': 'Battery & Hazmat', 'non_ferrous': 'Non-Ferrous Metals', 'ferrous': 'Ferrous Metals', 'plastics': 'Plastics', 'paper_cardboard': 'Paper & Cardboard'}

    @staticmethod
    def calculate_haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        phi1, phi2 = (math.radians(lat1), math.radians(lat2))
        delta_phi = math.radians(lat2 - lat1)
        delta_lambda = math.radians(lon2 - lon1)
        a = math.sin(delta_phi / 2.0) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2.0) ** 2
        c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))
        return round(GeoService.EARTH_RADIUS_KM * c, 2)

    @staticmethod
    def find_nearest_recyclers(db: Session, user_lat: float, user_lon: float, category_filter: Optional[str]=None, max_radius_km: float=150.0, limit: int=5) -> NearestRecyclersResponse:
        query = db.query(AuthorizedRecycler).filter(AuthorizedRecycler.is_active_license == True)
        all_recyclers = query.all()
        results: List[NearestRecyclerItem] = []
        for r in all_recyclers:
            if category_filter:
                accepted_cats = [c.strip().lower() for c in r.accepted_category_codes.split(',')]
                if category_filter.lower() not in accepted_cats and 'all' not in accepted_cats:
                    continue
            dist_km = GeoService.calculate_haversine_distance(user_lat, user_lon, r.latitude, r.longitude)
            if dist_km <= max_radius_km:
                transit_mins = int(dist_km / GeoService.AVERAGE_COMMERCIAL_SPEED_KMPH * 60)
                if transit_mins < 5:
                    transit_mins = 5
                maps_url = f'https://www.google.com/maps/dir/?api=1&origin={user_lat},{user_lon}&destination={r.latitude},{r.longitude}&travelmode=driving'
                category_summaries = [GeoService.CATEGORY_DISPLAY_MAP.get(c.strip().lower(), c.strip().replace('_', ' ').title()) for c in r.accepted_category_codes.split(',')]
                results.append(NearestRecyclerItem(recycler=RecyclerOut.model_validate(r), distance_km=dist_km, estimated_driving_time_mins=transit_mins, google_maps_directions_url=maps_url, accepted_materials_summary=category_summaries))
        results.sort(key=lambda item: item.distance_km)
        final_list = results[:limit]
        if final_list:
            top = final_list[0]
            hi_nav = f"निकटतम अधिकृत रिसायकलर '{top.recycler.name}' है, जो यहाँ से {top.distance_km} किलोमीटर दूर {top.recycler.district} में स्थित है। ड्राइविंग में लगभग {top.estimated_driving_time_mins} मिनट लगेंगे।"
            mr_nav = f"सर्वात जवळचे अधिकृत रिसायकलिंग केंद्र '{top.recycler.name}' असून ते येथून {top.distance_km} किमी अंतरावर {top.recycler.district} येथे आहे. पोहोचण्यासाठी सुमारे {top.estimated_driving_time_mins} मिनिटे लागतील."
        else:
            hi_nav = f'{max_radius_km} किलोमीटर के दायरे में कोई अधिकृत रिसायकलर नहीं मिला। कृपया दायरा बढ़ाएं।'
            mr_nav = f'{max_radius_km} किमी च्या परिसरात कोणतेही अधिकृत केंद्र आढळले नाही. कृपया शोध क्षेत्र वाढवा.'
        return NearestRecyclersResponse(origin_latitude=user_lat, origin_longitude=user_lon, queried_category=category_filter, total_found=len(final_list), nearby_recyclers=final_list, voice_navigation_instruction_hi=hi_nav, voice_navigation_instruction_mr=mr_nav)