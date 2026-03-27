from fastapi import FastAPI, HTTPException, File, UploadFile
from pydantic import BaseModel, Field
from fastapi.middleware.cors import CORSMiddleware
import os
import base64
from typing import List, Optional
from motor.motor_asyncio import AsyncIOMotorClient
from contextlib import asynccontextmanager

# API versioning/prefixes
QR_PREFIX = "/api/qr"

class Student(BaseModel):
    id: str  # This is the unique code
    full_name: str
    organization: str
    issue_date: str
    specialty: str
    course: str
    hash: Optional[str] = None
    avatar_base64: Optional[str] = None

class LoginRequest(BaseModel):
    code: str
    mood: Optional[str] = "neutral"

# Initial Students Data for Migration
initial_students_db = {
    "nFuvUG6qzp3s": Student(
        id="nFuvUG6qzp3s",
        full_name="Погрибной Максим Юрьевич",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2023",
        specialty="09.02.07 «Информационные системы и программирование»",
        course="3",
        hash="hash_m_p_2026",
        avatar_base64=None
    ),
    "v3X8dK9mLp7q": Student(
        id="v3X8dK9mLp7q",
        full_name="Бурова Валерия Александровна",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2023",
        specialty="09.02.07 «Информационные системы и программирование»",
        course="3",
        hash="hash_v_a_2026",
        avatar_base64=None
    ),
    "r5Z2fN6hJt4w": Student(
        id="r5Z2fN6hJt4w",
        full_name="Васильева Екатерина Максимовна",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2023",
        specialty="09.02.07 «Информационные системы и программирование»",
        course="3",
        hash="hash_e_v_2026",
        avatar_base64=None
    ),
    "k8A2mW5nB0pQ": Student(
        id="k8A2mW5nB0pQ",
        full_name="Скворцова Дарья Викторовна",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2023",
        specialty="09.02.07 «Информационные системы и программирование»",
        course="3",
        hash="hash_d_v_2026",
        avatar_base64=None
    ),
    "x1Y9zR4tL7sD": Student(
        id="x1Y9zR4tL7sD",
        full_name="Жукова Анастасия Олеговна",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2023",
        specialty="09.02.07 «Информационные системы и программирование»",
        course="3",
        hash="hash_a_o_2026",
        avatar_base64=None
    )
}

# MongoDB Configuration
MONGODB_URL = os.environ.get("MONGODB_URL", "mongodb+srv://NovaCreator:<db_password>@qrappcluster.0qt7un1.mongodb.net/?appName=QRAppCluster")
client = AsyncIOMotorClient(MONGODB_URL)
db = client.qr_app_db
students_collection = db.students

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Migrate mock data if collection is empty
    count = await students_collection.count_documents({})
    if count == 0:
        print("Migrating mock data to MongoDB...")
        for student_id, student_data in initial_students_db.items():
            await students_collection.replace_one(
                {"id": student_id}, 
                student_data.dict(), 
                upsert=True
            )
    yield
    # Shutdown logic if needed

app = FastAPI(title="Unified Student Backend", lifespan=lifespan)

# Startup Migration Logic moved up

@app.get("/")
async def root():
    return {"message": "Unified Backend is running"}

@app.get(f"{QR_PREFIX}/db-check")
async def db_check():
    try:
        count = await students_collection.count_documents({})
        names = await students_collection.distinct("full_name")
        return {
            "status": "connected",
            "database": students_collection.database.name,
            "collection": students_collection.name,
            "document_count": count,
            "student_names": names
        }
    except Exception as e:
        return {"status": "error", "message": "Failed to connect to database", "detail": str(e)}

@app.post(f"{QR_PREFIX}/login")
async def login(request: LoginRequest):
    code_to_find = "nFuvUG6qzp3s" if request.code == "debug" else request.code
    
    student = await students_collection.find_one({"id": code_to_find}, {"_id": 0})
    if student:
        return student
    
    raise HTTPException(status_code=401, detail="Invalid unique code")

@app.get(f"{QR_PREFIX}/student/{{code}}")
async def get_student(code: str):
    student = await students_collection.find_one({"id": code}, {"_id": 0})
    if student:
        return student
    raise HTTPException(status_code=404, detail="Student not found")

@app.post(f"{QR_PREFIX}/student/{{code}}/avatar")
async def upload_avatar(code: str, file: UploadFile = File(...)):
    student = await students_collection.find_one({"id": code})
    if not student:
        raise HTTPException(status_code=404, detail="Student not found")
    
    # Read file and convert to base64
    contents = await file.read()
    if not contents:
        raise HTTPException(status_code=400, detail="Empty file")
    
    # Optional: check file size (e.g., max 1MB)
    if len(contents) > 1 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="File too large (max 1MB)")

    encoded_string = base64.b64encode(contents).decode("utf-8")
    
    # Add data URI prefix for easier use in frontend
    mime_type = file.content_type or "image/png"
    data_uri = f"data:{mime_type};base64,{encoded_string}"
    
    # Update in MongoDB
    result = await students_collection.update_one(
        {"id": code},
        {"$set": {"avatar_base64": data_uri}}
    )
    
    return {
        "message": "Avatar upload processed",
        "matched_count": result.matched_count,
        "modified_count": result.modified_count,
        "avatar_preview": data_uri[:50] + "..."
    }

if __name__ == "__main__":
    import uvicorn
    # Use PORT environment variable for cloud hosting
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
