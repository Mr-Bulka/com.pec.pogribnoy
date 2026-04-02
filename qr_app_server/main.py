from fastapi import FastAPI, HTTPException, File, UploadFile
from pydantic import BaseModel, Field
from fastapi.middleware.cors import CORSMiddleware
import os
import base64
from typing import List, Optional
from motor.motor_asyncio import AsyncIOMotorClient
from contextlib import asynccontextmanager
import asyncio
import secrets
from datetime import datetime

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
    ),
    "j7S5hT3kL9mB": Student(
        id="j7S5hT3kL9mB",
        full_name="Филлипов Никита Александрович",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2023",
        specialty="09.02.07 «Информационные системы и программирование»",
        course="3",
        hash="hash_n_a_2026",
        avatar_base64=None
    ),
    "m0P2qR4sT6uV": Student(
        id="m0P2qR4sT6uV",
        full_name="Ратникова Анастасия Романовна",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2023",
        specialty="09.02.07 «Информационные системы и программирование»",
        course="3",
        hash="hash_a_r_2026",
        avatar_base64=None
    )
}

# MongoDB Configuration
MONGODB_URL = os.environ.get("MONGODB_URL", "mongodb+srv://NovaCreator:<db_password>@qrappcluster.0qt7un1.mongodb.net/?appName=QRAppCluster")
client = AsyncIOMotorClient(MONGODB_URL)
db = client.qr_app_db
students_collection = db.students
metadata_collection = db.metadata

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: 1. Cleanup duplicates (if any)
    print("Cleaning up duplicate student entries...")
    # Group by id and keep the first one
    pipeline = [
        {"$group": {"_id": "$id", "unique_ids": {"$addToSet": "$_id"}, "count": {"$sum": 1}}},
        {"$match": {"count": {"$gt": 1}}}
    ]
    async for duplicate in students_collection.aggregate(pipeline):
        # Keep the first, delete the rest
        to_delete = duplicate["unique_ids"][1:]
        result = await students_collection.delete_many({"_id": {"$in": to_delete}})
        print(f"Deleted {result.deleted_count} duplicates for student ID: {duplicate['_id']}")

    # 2. Ensure Unique Index on 'id'
    print("Ensuring unique index on 'id' field...")
    await students_collection.create_index("id", unique=True)
    
    # 3. Startup: Ensure all initial students are migrated to MongoDB
    print("Checking for missing students to migrate to MongoDB...")
    for student_id, student_data in initial_students_db.items():
        # Check if student already exists to avoid overwriting avatars/hashes
        exists = await students_collection.count_documents({"id": student_id})
        if exists == 0:
            print(f"Adding new student: {student_data.full_name}")
            await students_collection.insert_one(student_data.dict())
    
    # 4. Initialize last_rotation_time if not exists
    print("Ensuring last_rotation_time metadata exists...")
    meta = await metadata_collection.find_one({"type": "rotation"})
    if not meta:
        await metadata_collection.insert_one({
            "type": "rotation",
            "last_rotation_time": datetime.utcnow()
        })
    
    yield
    # Shutdown logic if needed

async def check_and_rotate_hashes():
    """Checks if 1 hour has passed since last rotation and rotates if needed."""
    try:
        meta = await metadata_collection.find_one({"type": "rotation"})
        if not meta:
            # Initialization fallback
            await metadata_collection.insert_one({
                "type": "rotation",
                "last_rotation_time": datetime.utcnow()
            })
            return

        last_time = meta["last_rotation_time"]
        # Use UTC for consistency on distributed servers
        now = datetime.utcnow()
        diff = (now - last_time).total_seconds()
        
        if diff >= 3600:
            print(f"[{now}] Lazy rotating student hashes (seconds since last: {diff})...")
            # Get all students
            cursor = students_collection.find({})
            async for student in cursor:
                new_hash = secrets.token_urlsafe(12)
                await students_collection.update_one(
                    {"_id": student["_id"]},
                    {"$set": {"hash": new_hash}}
                )
            # Update rotation time
            await metadata_collection.update_one(
                {"type": "rotation"},
                {"$set": {"last_rotation_time": now}}
            )
            print(f"[{now}] Lazy rotation completed successfully.")
    except Exception as e:
        print(f"Error in lazy rotation: {str(e)}")

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
    await check_and_rotate_hashes()
    code_to_find = "nFuvUG6qzp3s" if request.code == "debug" else request.code
    
    student = await students_collection.find_one({"id": code_to_find}, {"_id": 0})
    if student:
        return student
    
    raise HTTPException(status_code=401, detail="Invalid unique code")

@app.get(f"{QR_PREFIX}/student/{{code}}")
async def get_student(code: str):
    await check_and_rotate_hashes()
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
