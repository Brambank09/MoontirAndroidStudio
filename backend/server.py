from datetime import datetime, timezone
from pathlib import Path
from typing import List, Optional
import logging
import os
import uuid

import bcrypt
import httpx
import jwt
from dotenv import load_dotenv
from fastapi import APIRouter, Depends, FastAPI, HTTPException, Query, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from motor.motor_asyncio import AsyncIOMotorClient
from pydantic import BaseModel, EmailStr, Field
from starlette.middleware.cors import CORSMiddleware

ROOT_DIR = Path(__file__).parent
load_dotenv(ROOT_DIR / ".env")

mongo_url = os.environ["MONGO_URL"]
client = AsyncIOMotorClient(mongo_url)
db = client[os.environ.get("DB_NAME", "moontir")]
JWT_SECRET = os.environ["JWT_SECRET"]
JWT_ALGORITHM = "HS256"
security = HTTPBearer(auto_error=False)

app = FastAPI(title="Moontir API")
api_router = APIRouter(prefix="/api")
logger = logging.getLogger("moontir")

# Service groups:
#   emergency   — 24/7 roadside emergency services
#   car_service — comprehensive at-home maintenance & mechanical care
#   detail      — exterior and interior detailing
#   special     — Moontir Special Care bundles that combine detailing + light service
SERVICES = [
    # EMERGENCY SERVICE (24/7 Roadside Assistance)
    {"id": "emg_tow", "group": "emergency", "name": "Emergency Towing", "name_id": "Derek Mobil Darurat", "category": "Emergency Service", "category_id": "Layanan Darurat", "description": "24/7 flatbed emergency towing to your preferred workshop or home safely.", "description_id": "Layanan derek gendong darurat 24/7 ke bengkel pilihan atau rumah dengan aman.", "duration": "30–45 mins arrival", "duration_id": "30–45 mnt tiba", "price": 350000, "featured": True, "features": ["24/7 rapid dispatch", "Flatbed towing", "Up to 15 km included", "Safe harness locking"]},
    {"id": "emg_jump", "group": "emergency", "name": "Emergency Battery Jumper", "name_id": "Jumper Aki Darurat", "category": "Emergency Service", "category_id": "Layanan Darurat", "description": "On-site battery jumpstart service and alternator output test for stalled cars.", "description_id": "Layanan jumpstart aki di lokasi dan tes pengisian alternator untuk mobil mogok.", "duration": "20–30 mins arrival", "duration_id": "20–30 mnt tiba", "price": 120000, "featured": True, "features": ["Rapid roadside arrival", "Heavy-duty jumper pack", "Alternator check", "Terminal cleaning"]},
    {"id": "emg_tire", "group": "emergency", "name": "Emergency Spare Tire Change", "name_id": "Ganti Ban Serep Darurat", "category": "Emergency Service", "category_id": "Layanan Darurat", "description": "Rapid on-road spare tire swap, torque tightening, and tire pressure check.", "description_id": "Pemasangan ban serep darurat di jalan, pengencangan baut torsi, dan cek tekanan angin.", "duration": "25–35 mins arrival", "duration_id": "25–35 mnt tiba", "price": 150000, "featured": True, "features": ["Hydraulic jack lift", "Lug bolt torque check", "Spare tire inflation", "Punctured tire secure"]},

    # CAR SERVICE (Comprehensive Maintenance & Care)
    {"id": "car_check", "group": "car_service", "name": "Essential Car Checkup", "name_id": "Pemeriksaan Mobil Esensial", "category": "Car Service", "category_id": "Servis Mobil", "description": "Comprehensive 35-point safety inspection covering brakes, suspension, belts, and engine vitals.", "description_id": "Inspeksi keselamatan 35 titik komprehensif mencakup rem, suspensi, belt, dan kondisi mesin.", "duration": "60 minutes", "duration_id": "60 menit", "price": 175000, "featured": False, "features": ["35-point safety check", "Brake & pad wear test", "Fluid condition test", "Digital health report"]},
    {"id": "oil_change", "group": "car_service", "name": "Oil Change", "name_id": "Ganti Oli Mesin", "category": "Car Service", "category_id": "Servis Mobil", "description": "Full engine oil replacement with premium synthetic oil, OEM oil filter, and drain plug seal.", "description_id": "Penggantian oli mesin menyeluruh dengan oli sintetis premium, filter oli OEM, dan ring karter.", "duration": "45 minutes", "duration_id": "45 menit", "price": 290000, "featured": True, "features": ["Fully synthetic motor oil", "Genuine OEM oil filter", "Crush washer renewal", "Eco-friendly oil recycling"]},
    {"id": "fluid_refresh", "group": "car_service", "name": "Full Fluid Refresh", "name_id": "Segarkan Semua Fluida", "category": "Car Service", "category_id": "Servis Mobil", "description": "Drain, flush, and refill coolant, brake fluid, transmission fluid check, and windshield washer.", "description_id": "Kuras dan isi ulang coolant radiator, minyak rem, cek oli transmisi, dan air wiper.", "duration": "60 minutes", "duration_id": "60 menit", "price": 240000, "featured": False, "features": ["Radiator coolant flush", "DOT4 brake fluid flush", "Power steering check", "Windshield washer top-up"]},
    {"id": "full_tune", "group": "car_service", "name": "Full Tune-Up", "name_id": "Tune-Up Menyeluruh", "category": "Car Service", "category_id": "Servis Mobil", "description": "Complete engine optimization: throttle body cleaning, spark plug renewal, air and cabin filter renewal.", "description_id": "Optimalisasi performa mesin: bersihkan throttle body, ganti busi, dan ganti filter udara serta kabin.", "duration": "90 minutes", "duration_id": "90 menit", "price": 350000, "featured": True, "features": ["Throttle body clean", "Spark plug renewal", "Engine air filter", "OBD2 computer diagnostics"]},
    {"id": "battery_change", "group": "car_service", "name": "Battery Change", "name_id": "Ganti Aki Mobil", "category": "Car Service", "category_id": "Servis Mobil", "description": "Home delivery and installation of maintenance-free car battery with old battery trade-in included.", "description_id": "Pengantaran dan pemasangan aki bebas perawatan (MF) di rumah dengan tukar tambah aki lama.", "duration": "30 minutes", "duration_id": "30 menit", "price": 850000, "featured": False, "features": ["High-capacity MF battery", "Alternator test", "Terminal corrosion clean", "12-month replacement warranty"]},
    {"id": "ac_refresh", "group": "car_service", "name": "Air Conditioner Refresh", "name_id": "Segarkan AC Mobil", "category": "Car Service", "category_id": "Servis Mobil", "description": "AC cabin ozone sterilization, evaporator foam cleaning, cabin filter renewal, and refrigerant pressure test.", "description_id": "Sterilisasi ozon kabin, busa pembersih evaporator, pembersihan filter kabin, dan tes freon.", "duration": "60 minutes", "duration_id": "60 menit", "price": 280000, "featured": False, "features": ["Evaporator foam wash", "Freon pressure check", "Cabin ozone mist", "Blower fan clean"]},

    # CAR DETAILING
    {"id": "shine", "group": "detail", "name": "Exterior Detailing", "name_id": "Detailing Eksterior", "category": "Car Detailing", "category_id": "Detailing Mobil", "description": "Multi-stage foam wash, clay decontamination, single-step swirl polish, and hydrophobic ceramic wax seal.", "description_id": "Cuci busa bertahap, dekontaminasi clay, poles hilangkan baret halus, dan segel wax keramik.", "duration": "2–3 hours", "duration_id": "2–3 jam", "price": 320000, "featured": True, "features": ["pH-neutral snow foam", "Clay bar decontamination", "Machine paint polish", "Hydrophobic ceramic wax"]},
    {"id": "interior", "group": "detail", "name": "Interior Detailing", "name_id": "Detailing Interior", "category": "Car Detailing", "category_id": "Detailing Mobil", "description": "Deep cabin hot-water extraction, leather and fabric conditioning, stain removal, and odor elimination.", "description_id": "Vakum ekstraksi uap panas jok, perawatan bahan kulit & kain, pembersihan noda, dan penghilang bau.", "duration": "2 hours", "duration_id": "2 jam", "price": 260000, "featured": True, "features": ["Hot extraction shampoo", "Leather & dash dressing", "Headliner dry clean", "Anti-bacterial mist"]},
    {"id": "glass", "group": "detail", "name": "Crystal Glass Coating", "name_id": "Lapisan Kaca Kristal", "category": "Car Detailing", "category_id": "Detailing Mobil", "description": "Hydrophobic glass coating on all windshields and windows for clarity and safety in heavy rain.", "description_id": "Lapisan kaca hidrofobik untuk seluruh kaca mobil agar pandangan jernih saat hujan deras.", "duration": "90 minutes", "duration_id": "90 menit", "price": 195000, "featured": False, "features": ["Water-spot removal", "Hydrophobic coating", "Wiper blade conditioning", "All windows treated"]},

    # MOONTIR SPECIAL CARE — bundles of detail + light
    {"id": "full", "group": "special", "name": "Full Moon Package", "name_id": "Paket Full Moon", "category": "Special Care", "category_id": "Perawatan Spesial", "description": "Signature all-in-one bundle: Exterior Detailing + Interior Detailing with 35-point mechanical inspection.", "description_id": "Paket andalan lengkap: Detailing Eksterior + Detailing Interior dengan inspeksi mekanis 35 titik.", "duration": "4 hours", "duration_id": "4 jam", "price": 495000, "featured": True, "features": ["Exterior detail", "Interior detail", "35-point safety check", "Engine bay dressing"]},
    {"id": "eclipse", "group": "special", "name": "Total Eclipse Care", "name_id": "Perawatan Total Eclipse", "category": "Special Care", "category_id": "Perawatan Spesial", "description": "Signature bundle: detailing, coating, and full tune-up.", "description_id": "Bundel andalan: detailing, coating, dan tune-up penuh.", "duration": "5 hours", "duration_id": "5 jam", "price": 685000, "featured": False, "features": ["Full detail", "Glass coating", "Home tune-up"]},
]

VEHICLE_MULTIPLIERS = {"Sedan": 1.00, "Hatchback": 1.00, "MPV": 1.15, "SUV": 1.25, "Truck": 1.40, "Pickup": 1.30}
DEFAULT_MULTIPLIER = 1.0


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


class RegisterInput(BaseModel):
    name: str = Field(min_length=2, max_length=80)
    email: EmailStr
    password: str = Field(min_length=6, max_length=128)


class LoginInput(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1, max_length=128)


class ProfileUpdate(BaseModel):
    name: str = Field(min_length=2, max_length=80)


class UserOut(BaseModel):
    id: str
    name: str
    email: EmailStr


class AuthResponse(BaseModel):
    token: str
    user: UserOut


class VehicleCreate(BaseModel):
    nickname: str = Field(min_length=1, max_length=40)
    make: str = Field(min_length=1, max_length=40)
    model: str = Field(min_length=1, max_length=40)
    year: str = Field(min_length=4, max_length=4)
    plate: str = Field(min_length=3, max_length=12)
    type: str = Field(default="Sedan", max_length=20)


class VehicleUpdate(BaseModel):
    nickname: Optional[str] = Field(default=None, min_length=1, max_length=40)
    make: Optional[str] = Field(default=None, min_length=1, max_length=40)
    model: Optional[str] = Field(default=None, min_length=1, max_length=40)
    year: Optional[str] = Field(default=None, min_length=4, max_length=4)
    plate: Optional[str] = Field(default=None, min_length=3, max_length=12)
    type: Optional[str] = Field(default=None, max_length=20)


class VehicleOut(VehicleCreate):
    id: str
    created_at: str


class AddressInput(BaseModel):
    label: str = Field(min_length=5, max_length=400)
    latitude: Optional[float] = Field(default=None, ge=-90, le=90)
    longitude: Optional[float] = Field(default=None, ge=-180, le=180)


class OrderCreate(BaseModel):
    service_id: str
    vehicle_id: str
    address: AddressInput
    schedule_date: str = Field(min_length=8, max_length=20)
    schedule_time: str = Field(min_length=3, max_length=30)
    notes: str = Field(default="", max_length=500)


class InvoiceItem(BaseModel):
    label: str
    label_id: Optional[str] = None
    amount: int


class RatingInput(BaseModel):
    stars: int = Field(ge=1, le=5)
    note: str = Field(default="", max_length=500)


class RatingOut(BaseModel):
    stars: int
    note: str
    rated_at: str


class OrderOut(BaseModel):
    id: str
    service_id: str
    service_name: str
    vehicle: VehicleOut
    address: AddressInput
    schedule_date: str
    schedule_time: str
    notes: str
    status: str
    status_history: List[dict]
    items: List[InvoiceItem]
    total: int
    payment_status: str
    created_at: str
    rating: Optional[RatingOut] = None


class QuoteInput(BaseModel):
    service_id: str
    vehicle_type: str = Field(default="Sedan", max_length=20)


def token_for(user: dict) -> str:
    return jwt.encode({"sub": user["id"], "exp": datetime.now(timezone.utc).timestamp() + 60 * 60 * 24 * 30}, JWT_SECRET, algorithm=JWT_ALGORITHM)


async def current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> dict:
    if not credentials:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Please sign in first.")
    try:
        payload = jwt.decode(credentials.credentials, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        user_id = payload.get("sub")
    except jwt.PyJWTError as exc:
        raise HTTPException(status_code=401, detail="Your session has expired.") from exc
    user = await db.users.find_one({"id": user_id}, {"_id": 0})
    if not user:
        raise HTTPException(status_code=401, detail="Account not found.")
    return user


def user_out(user: dict) -> UserOut:
    return UserOut(id=user["id"], name=user["name"], email=user["email"])


def auth_response(user: dict) -> AuthResponse:
    return AuthResponse(token=token_for(user), user=user_out(user))


def _round_rupiah(value: float) -> int:
    return int(round(value / 500.0) * 500)


def price_breakdown(service: dict, vehicle_type: str) -> dict:
    multiplier = VEHICLE_MULTIPLIERS.get(vehicle_type, DEFAULT_MULTIPLIER)
    base = int(service["price"])
    total = _round_rupiah(base * multiplier)
    surcharge = total - base
    items: List[dict] = [{"label": service["name"], "label_id": service["name_id"], "amount": base}]
    if surcharge > 0:
        items.append({"label": f"{vehicle_type} handling surcharge", "label_id": f"Biaya penanganan {vehicle_type}", "amount": surcharge})
    return {"items": items, "total": total, "multiplier": multiplier, "vehicle_type": vehicle_type, "base": base, "surcharge": surcharge}


@api_router.get("/")
async def root():
    return {"message": "Moontir API is ready"}


@api_router.get("/health")
async def health():
    await db.command("ping")
    return {"ok": True}


@api_router.post("/auth/register", response_model=AuthResponse)
async def register(input: RegisterInput):
    email = str(input.email).lower()
    if await db.users.find_one({"email": email}, {"_id": 0}):
        raise HTTPException(status_code=409, detail="An account with this email already exists.")
    user = {"id": str(uuid.uuid4()), "name": input.name.strip(), "email": email, "password_hash": bcrypt.hashpw(input.password.encode(), bcrypt.gensalt()).decode(), "created_at": now_iso()}
    await db.users.insert_one(user)
    return auth_response(user)


@api_router.post("/auth/login", response_model=AuthResponse)
async def login(input: LoginInput):
    user = await db.users.find_one({"email": str(input.email).lower()}, {"_id": 0})
    if not user or not bcrypt.checkpw(input.password.encode(), user["password_hash"].encode()):
        raise HTTPException(status_code=401, detail="Email or password is incorrect.")
    return auth_response(user)


@api_router.get("/me", response_model=UserOut)
async def me(user: dict = Depends(current_user)):
    return user_out(user)


@api_router.patch("/me", response_model=UserOut)
async def update_me(input: ProfileUpdate, user: dict = Depends(current_user)):
    name = input.name.strip()
    await db.users.update_one({"id": user["id"]}, {"$set": {"name": name}})
    user["name"] = name
    return user_out(user)


@api_router.get("/services")
async def services():
    return SERVICES


@api_router.post("/quote")
async def quote(input: QuoteInput):
    service = next((item for item in SERVICES if item["id"] == input.service_id), None)
    if not service:
        raise HTTPException(status_code=404, detail="Service package not found.")
    return price_breakdown(service, input.vehicle_type)


@api_router.get("/vehicles", response_model=List[VehicleOut])
async def vehicles(user: dict = Depends(current_user)):
    rows = await db.vehicles.find({"user_id": user["id"]}, {"_id": 0, "user_id": 0}).sort("created_at", -1).to_list(100)
    return rows


@api_router.post("/vehicles", response_model=VehicleOut)
async def create_vehicle(input: VehicleCreate, user: dict = Depends(current_user)):
    vehicle = {**input.model_dump(), "id": str(uuid.uuid4()), "user_id": user["id"], "created_at": now_iso()}
    await db.vehicles.insert_one(vehicle)
    return VehicleOut(**{k: v for k, v in vehicle.items() if k != "user_id"})


@api_router.patch("/vehicles/{vehicle_id}", response_model=VehicleOut)
async def update_vehicle(vehicle_id: str, input: VehicleUpdate, user: dict = Depends(current_user)):
    updates = {k: v for k, v in input.model_dump().items() if v is not None}
    if not updates:
        raise HTTPException(status_code=400, detail="Nothing to update.")
    result = await db.vehicles.find_one_and_update(
        {"id": vehicle_id, "user_id": user["id"]},
        {"$set": updates},
        return_document=True,
        projection={"_id": 0, "user_id": 0},
    )
    if not result:
        raise HTTPException(status_code=404, detail="Vehicle not found.")
    return VehicleOut(**result)


@api_router.delete("/vehicles/{vehicle_id}")
async def delete_vehicle(vehicle_id: str, user: dict = Depends(current_user)):
    deleted = await db.vehicles.delete_one({"id": vehicle_id, "user_id": user["id"]})
    if deleted.deleted_count == 0:
        raise HTTPException(status_code=404, detail="Vehicle not found.")
    return {"ok": True, "id": vehicle_id}


async def nominatim(path: str, params: dict):
    headers = {"User-Agent": os.getenv("NOMINATIM_USER_AGENT", "Moontir/1.0 contact@moontir.app")}
    try:
        async with httpx.AsyncClient(timeout=8) as http:
            response = await http.get(f"https://nominatim.openstreetmap.org/{path}", params=params, headers=headers)
        if response.status_code == 429:
            raise HTTPException(status_code=429, detail="Map search is busy. Please try again in a moment.")
        response.raise_for_status()
        return response.json()
    except HTTPException:
        raise
    except httpx.HTTPError as exc:
        logger.warning("Nominatim request failed: %s", exc)
        raise HTTPException(status_code=503, detail="Map search is temporarily unavailable.") from exc


@api_router.get("/geocode")
async def geocode(q: str = Query(min_length=3, max_length=200)):
    rows = await nominatim("search", {"q": q, "format": "jsonv2", "addressdetails": 1, "limit": 5, "countrycodes": "id", "accept-language": "id,en"})
    return [{"displayName": row.get("display_name", ""), "latitude": float(row["lat"]), "longitude": float(row["lon"]), "osmId": row.get("osm_id")} for row in rows]


@api_router.get("/reverse-geocode")
async def reverse_geocode(lat: float = Query(ge=-90, le=90), lon: float = Query(ge=-180, le=180)):
    row = await nominatim("reverse", {"lat": lat, "lon": lon, "format": "jsonv2", "addressdetails": 1, "accept-language": "id,en"})
    return {"displayName": row.get("display_name", ""), "latitude": lat, "longitude": lon}


@api_router.get("/orders", response_model=List[OrderOut])
async def orders(user: dict = Depends(current_user)):
    rows = await db.orders.find({"user_id": user["id"]}, {"_id": 0, "user_id": 0}).sort("created_at", -1).to_list(100)
    return rows


@api_router.post("/orders", response_model=OrderOut)
async def create_order(input: OrderCreate, user: dict = Depends(current_user)):
    service = next((item for item in SERVICES if item["id"] == input.service_id), None)
    if not service:
        raise HTTPException(status_code=404, detail="Service package not found.")
    vehicle = await db.vehicles.find_one({"id": input.vehicle_id, "user_id": user["id"]}, {"_id": 0, "user_id": 0})
    if not vehicle:
        raise HTTPException(status_code=404, detail="Vehicle not found in your garage.")
    breakdown = price_breakdown(service, vehicle.get("type", "Sedan"))
    created = now_iso()
    order = {
        "id": str(uuid.uuid4()),
        "service_id": service["id"],
        "service_name": service["name"],
        "vehicle": vehicle,
        "address": input.address.model_dump(),
        "schedule_date": input.schedule_date,
        "schedule_time": input.schedule_time,
        "notes": input.notes.strip(),
        "status": "dispatch",
        "status_history": [{"key": "dispatch", "label": "Dispatch confirmed", "label_id": "Dispatch dikonfirmasi", "at": created}],
        "items": breakdown["items"],
        "total": breakdown["total"],
        "payment_status": "unpaid",
        "created_at": created,
        "user_id": user["id"],
    }
    await db.orders.insert_one(order)
    return OrderOut(**{k: v for k, v in order.items() if k != "user_id"})


@api_router.post("/orders/{order_id}/complete", response_model=OrderOut)
async def complete_order(order_id: str, user: dict = Depends(current_user)):
    completed_at = now_iso()
    entry = {"key": "completed", "label": "Service completed", "label_id": "Layanan selesai", "at": completed_at}
    result = await db.orders.find_one_and_update(
        {"id": order_id, "user_id": user["id"]},
        {"$set": {"status": "completed"}, "$push": {"status_history": entry}},
        return_document=True,
        projection={"_id": 0, "user_id": 0},
    )
    if not result:
        raise HTTPException(status_code=404, detail="Order not found.")
    return OrderOut(**result)


@api_router.post("/orders/{order_id}/rating", response_model=OrderOut)
async def rate_order(order_id: str, input: RatingInput, user: dict = Depends(current_user)):
    rating = {"stars": int(input.stars), "note": input.note.strip(), "rated_at": now_iso()}
    result = await db.orders.find_one_and_update(
        {"id": order_id, "user_id": user["id"]},
        {"$set": {"rating": rating}},
        return_document=True,
        projection={"_id": 0, "user_id": 0},
    )
    if not result:
        raise HTTPException(status_code=404, detail="Order not found.")
    return OrderOut(**result)


app.include_router(api_router)
app.add_middleware(CORSMiddleware, allow_credentials=False, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])


@app.on_event("shutdown")
async def shutdown_db_client():
    client.close()
